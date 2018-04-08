import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class AdderHandler
{
	private final int serverPort = 3126;		// target server port.
	private final int maxAttempts = 5;			// no of connection attempts.
	private final int timeoutConnect = 5000;	// timeout in ms.
	private final int timeoutCommand = 60000;	// timeout in ms.
	
	private String tcpPort;						// local TCP port.
	private InetAddress serverAddress;			// target server address.
	private Adder[] adders;						// array of summer adders.
	private Socket socket;						// local socket.
	
	public AdderHandler(String tcpPort, String serverAddress)
			throws InstantiationException, UnknownHostException
	{
		// Check the number of arguments and ensure the first argument is an
		// integer in range [1024, 65535].
		if (!tcpPort.matches("^\\d+$")
				|| Integer.parseInt(tcpPort) < 1024
				|| 65535 < Integer.parseInt(tcpPort))
			throw new InstantiationException("The port must be an integer "
					+ "between 1024 and 65535.");
		
		this.tcpPort = tcpPort;
		this.serverAddress = InetAddress.getByName(serverAddress);
	}
	
	/**
	 * Runs the Summer. Establishes a connection to a server and sums together
	 * the received integers.
	 * @throws IOException
	 */
	public void run()
			throws IOException
	{
		try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(tcpPort)))
		{
			System.out.println(String.format("A local TCP server socket created"
					+ " at port %s.", serverSocket.getLocalPort()));
			
			// Set up a connection between the local port and the server and
			// set its timeout to 'timeoutConnect' ms.
			socket = requestTCPConnection(serverSocket);
			socket.setSoTimeout(timeoutConnect);
			System.out.println(String.format("Connection to %s:%s established.",
					serverAddress.getHostAddress(), serverPort));
			
			// Create object streams for input and output.
			ObjectInputStream objInStream = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
			
			// Create the adders.
			try
			{
				adders = createAdders(objInStream.readInt());
			}
			catch (SocketTimeoutException ste)
			{
				System.err.println("The server did not return the number of"
						+ " adders within the allotted time.");
				objOutStream.writeInt(-1);
				objOutStream.flush();
				throw ste;
			}
			
			// Send the port addresses of the adders to the server.
			for (Adder adder : adders)
			{
				objOutStream.writeInt(adder.getPort());
				objOutStream.flush();
			}
			
			// Wait for commands from the server. Set the socket timeout to
			// 'timeoutCommand' seconds.
			socket.setSoTimeout(timeoutCommand);
			int cmd = 1, response = -1;
			
			while (cmd != 0)
			{
				cmd = objInStream.readInt();
				
				switch (cmd)
				{
					// If the command is 0 (zero), close the output stream and
				    // the adders.
					case 0: objOutStream.close();
							for (Adder adder : adders)
					        	adder.close();
					        break;
					// If the command is 1 (one), respond with the current
					// total sum.
					case 1: response = 0;
							for (Adder adder : adders)
					        	response += adder.getSum();
					        break;
					// If the command is 2 (two), respond with the current
					// largest sum out of all of the adders.
					case 2: response = maxSumIndex(adders);
							break;
					// If the command is 3 (three), respond with the current
					// total amount of received numbers.
					case 3: response = 0;
							for (Adder adder : adders)
								response += adder.getNumbers();
							break;
				}
				
				if (cmd != 0)
				{
					objOutStream.writeInt(response);
					objOutStream.flush();
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Execution finished with an error.");
			e.printStackTrace();
		}
		finally
		{
			// Close the TCP socket.
			if (socket != null)
				socket.close();
			
			// Close each of the remaining adders.
			if (adders != null)
			{
				for (Adder adder : adders)
				{
					if (adder != null)
					{
						adder.close();

						System.out.println(String.format("The sockets of the"
								+ " adder at port %s have been closed.",
								adder.getPort()));
					}
				}
			}
		}
	}
	
	/**
	 * Requests a TCP connection from the server by sending a local TCP port
	 * number to the server over UDP.
	 * @param serverSocket a ServerSocket used to receive a connection.
	 * @return a Socket connected to the server.
	 * @throws IOException
	 */
	private Socket requestTCPConnection(ServerSocket serverSocket)
			throws IOException
	{
		// Declare a socket.
		Socket socket = null;
		
		// Inform the server that this Summer is running.
		for (int attempt = 1; attempt <= maxAttempts; attempt++)
		{
			// Attempt to send a UDP packet with local TCP port info.
			try (DatagramSocket dgSocket = new DatagramSocket())
			{
				System.out.println("UDP socket created.");
				
				// Get the local TCP port as bytes.
				byte[] port = tcpPort.getBytes();
				
				// Attempt to send the local TCP port info to the server.
				dgSocket.send(new DatagramPacket(port, port.length,
						serverAddress, serverPort));
				
				System.out.println(String.format("UDP packet sent to address %s:%s.",
						serverAddress.getHostAddress(), serverPort));
			}
			catch (IOException ioe)
			{
				System.err.println("Error encountered while sending local"
						+ " TCP port information to the server.");
				System.exit(1);
			}
			
			// Attempt to receive a connection from the server.
			try
			{
				System.out.println(String.format("Establishing TCP connection."
						+ " Attempt %d/%d", attempt, maxAttempts));
				
				// Listen for a connection from the server.
				socket = serverSocket.accept();
				
				break;
			}
			catch (SocketTimeoutException ste)
			{
				System.err.println("The server did not respond within"
						+ " the allotted time.");
				
				if (attempt == maxAttempts)
					throw ste;
			}
		}
		
		return socket;
	}
	
	/**
	 * Creates the requested amount of adders.
	 * @param count the amount of adders.
	 * @return an array of adders of length 'count'.
	 * @throws IOException
	 */
	private Adder[] createAdders(int count)
			throws IOException
	{
		// Initialise a adder array.
		Adder[] adders = new Adder[count];
		
		// Create 'count' adders.
		for (int i = 0; i < count; i++)
		{
			// Create a new adder.
			Adder adder = new Adder();
			
			// Start the new adder in a new thread.
			new Thread(adder).start();
			
			// Save a pointer to the new adder in the adder array.
			adders[i] = adder;
		}
		
		return adders;
	}
	
	/**
	 * Finds the index of the maximum sum over an array of adders.
	 * @param adders an array of adders
	 * @return the index of the maximum sum from the adders of the array.
	 */
	private int maxSumIndex(Adder[] adders)
	{
		// Set the maximum sum index initially as that of the first adder.
		int maxInd = 0;
		
		// Iterate over the rest of the adders and replace the value of the
		// index with that of a larger sum if one is found.
		for (int i = 1; i < adders.length; i++)
		{
			if (adders[maxInd].getSum() < adders[i].getSum())
				maxInd = i;
		}
		
		return maxInd + 1;
	}
	
	public static void main(String[] args)
	{
		try
		{
			AdderHandler adder = new AdderHandler(args[0], args[1]);
			adder.run();
		}
		catch (Exception e)
		{
			System.err.println(String.format("ERROR: %s", e.getMessage()));
		}
	}
}

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Adder implements Runnable
{
	final private ServerSocket serverSocket;	// local server socket.
	private Socket socket;						// local socket.
	private int sum, numbers;					// sum and amount of numbers.
	
	public Adder() throws IOException
	{
		this.serverSocket = new ServerSocket(0);
		this.sum = 0;
		this.numbers = 0;
		
		System.out.println(String.format("A new adder created at port"
				+ " %d.", serverSocket.getLocalPort()));
	}
	
	/**
	 * Returns the amount of integers received by the adder.
	 * @return the amount of integers received by the adder.
	 */
	public int getNumbers()
	{
		return numbers;
	}
	
	/**
	 * Returns the port at which the adder is running.
	 * @return the port at which the adder is running.
	 */
	public int getPort()
	{
		return serverSocket.getLocalPort();
	}
	
	/**
	 * Returns the sum of the integers received by the adder.
	 * @return the sum of the integers received by the adder.
	 */
	public int getSum()
	{
		return sum;
	}
	
	/**
	 * Runs the adder. Receives a stream of integers from a server.
	 * Counts the number of received integers and sums them up.
	 */
	@Override
	public void run()
	{
		try
		{
			// Lock this port indefinitely.
			serverSocket.setSoTimeout(0);
			
			// Listen for an incoming connection and then close the serverSocket.
			socket = serverSocket.accept();
			serverSocket.close();
			
			// Lock this port indefinitely.
			socket.setSoTimeout(0);
			
			// Start object streams for input and output.
			ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

			// Declare the value variable.
			int value;
			
			while (true)
			{
				value = 0;
			
				try
				{
					value = inStream.readInt();					
				}
				catch (EOFException e)
				{
					System.out.println(String.format("The object input stream"
							+ " of the adder at port %s has ended.", getPort()));
				}
				
				if(value == 0)
					break;
				
				sum += value;
				numbers++;
			}
		}
		catch (IOException e)
		{
			System.err.println(String.format("The adder at port %s "
					+ "returned an error: %s", serverSocket.getLocalPort(), 
					e.getMessage()));
			e.printStackTrace();
		}
		
		try
		{
			close();
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Closes the sockets of the adder.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		if (socket != null && !socket.isClosed())
			socket.close();
		
		if (serverSocket != null && !serverSocket.isClosed())
			serverSocket.close();
	}

}

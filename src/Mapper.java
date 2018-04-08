import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Mapper {
	private final int[] axisLimit;
	private final JLabel imgLabel;
	private final String xmlURL = "http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1"
			+ "&REQUEST=GetCapabilities";
	private final String mapURL = "http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1"
			+ "&REQUEST=GetMap&BBOX=%d,%d,%d,%d&SRS=EPSG:4326" + "&WIDTH=953&HEIGHT=480&LAYERS=%s"
			+ "&STYLES=&FORMAT=image/png&TRANSPARENT=true";

	private int[] centre, ds;
	private String layers;

	private int nextRequestId; // Id of next update request
	private int mapId; // Id of most recent map updated by Mapper

	/**
	 * Constructs a Mapper, with half-width xMax and half-height yMax, used to
	 * update a map image in imgLabel.
	 * 
	 * @param imgLabel
	 *            JLabel of a map image that will be used
	 * @param xMax
	 *            half-width (distance between the centre and the horizontal ends of
	 *            the frame)
	 * @param yMax
	 *            half-height (distance between the centre and the vertical ends of
	 *            the frame)
	 */
	public Mapper(JLabel imgLabel, int xMax, int yMax) {
		this.imgLabel = imgLabel;
		this.axisLimit = new int[] { xMax, yMax };

		this.centre = new int[2];
		this.ds = axisLimit.clone();
		this.layers = "bluemarble,cities";
		nextRequestId = Integer.MIN_VALUE;
		mapId = Integer.MIN_VALUE;
	}

	public void setLayers(String layers) {
		this.layers = layers;
	}
	
	/**
	 * Returns a list of all available map layers.
	 * @return a list of all available map layers.
	 * @throws Exception
	 */
	public ArrayList<Tuple<String, String>> fetchLayers() throws Exception {
		ArrayList<Tuple<String, String>> layerList = new ArrayList<>();

		// Make a GetCapabilities request.
		URL getCapabilitiesURL = new URL(xmlURL);

		// Build an XML document from the response.
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(getCapabilitiesURL.openStream());

		// Find all the possible layers from the document.
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer", doc,
				XPathConstants.NODESET);

		// Go through every layer, get the name and title, and add it to the layer list.
		for (int i = 0; i < nodeList.getLength(); i++) {
			NodeList layer = (NodeList) xPath.evaluate("Name|Title", nodeList.item(i), XPathConstants.NODESET);
			layerList.add(new Tuple<String, String>(layer.item(0).getTextContent(), layer.item(1).getTextContent()));
		}

		return layerList;
	}

	/**
	 * Move along an 'axis' (0 for x, 1 for y) by a 'factor', an amount proportional to the current
	 * width, or height, respectively.
	 * @param factor a proportion of the current width or height
	 * @param axis axis along which the movement is done
	 */
	public void move(double factor, int axis) {
		// Calculate the amount moved.
		int shift = (int) (Math.signum(factor) * Math.max(1, Math.abs(2 * ds[axis] * factor)));

		// If moved right and it goes over the boundary, move it to the boundary.
		if (axisLimit[axis] < centre[axis] + shift + ds[axis])
			centre[axis] = axisLimit[axis] - ds[axis];
		// If moved left and it goes over the boundary, move it to the boundary.
		else if (centre[axis] + shift - ds[axis] < -axisLimit[axis])
			centre[axis] = -axisLimit[axis] + ds[axis];
		// Otherwise just move it.
		else
			centre[axis] += shift;

		System.out.println(String.format("%d, %d", centre[axis], ds[axis]));
	}

	/**
	 * Zoom in or out on the image. Zooming in occurs if 'factor' < 1, and zooming out of 'factor' > 1.
	 * @param factor factor = (new height or width)/(old height or width)
	 */
	public void zoom(double factor) {
		// Calculate the new half-height.
		int newdy = ds[1];
		newdy *= factor;

		// If the new half-height is larger than the y-limit, reset the box.
		if (axisLimit[1] < newdy)
			reset();
		// Otherwise zoom out.
		else {
			// If the new dy is less than one, make it one to avoid a 0-height image.
			newdy = newdy < 1 ? 1 : newdy;

			// If the new dy is the same as the old one while zooming out, increment it.
			newdy += (newdy == ds[1] && 1 < factor) ? 1 : 0;

			// Calculate the new half-width.
			int newdx = (int) (newdy * (double) axisLimit[0] / axisLimit[1]);

			// If the new box exceeds the x-limits, move it horizontally.
			if (axisLimit[0] < centre[0] + newdx)
				centre[0] = axisLimit[0] - newdx;
			else if (centre[0] - newdx < -axisLimit[0])
				centre[0] = -axisLimit[0] + newdx;

			// If the new box exceeds the y-limits, move it vertically.
			if (axisLimit[1] < centre[1] + newdy)
				centre[1] = axisLimit[1] - newdy;
			else if (centre[1] - newdy < -axisLimit[1])
				centre[1] = -axisLimit[1] + newdy;

			// Save the new size.
			ds = new int[] { newdx, newdy };
		}
	}

	/**
	 * Reset the image's centre and dimensions to the original settings.
	 */
	public void reset() {
		centre = new int[2];
		ds = axisLimit.clone();
	}

	/**
	 * Get id for map update request.
	 * @return map update request id
	 */
	synchronized private int getNextRequestId() {
		return ++nextRequestId;
	}

	/**
	 * Update map image if requestId >= mapId.
	 * @param image
	 * @param requestId
	 */
	synchronized private void updateImage(ImageIcon image, int requestId) {
		System.out.println(String.format("requestId: %d, currentMapId: %d", requestId, mapId));
		if ((mapId == Integer.MAX_VALUE && requestId == Integer.MIN_VALUE) || (requestId >= mapId)) {
			imgLabel.setIcon(image);
			mapId = requestId;
		}
	}
	
	/**
	 * Update the map with the latest settings.
	 * @param waitUpdate
	 * @throws Exception
	 */
	public void updateMap(boolean waitUpdate) throws Exception {
		int requestId = getNextRequestId();
		Thread t = new Thread(() -> {
			try {
				ImageIcon image = new ImageIcon(new URL(String.format(mapURL, centre[0] - ds[0], centre[1] - ds[1],
						centre[0] + ds[0], centre[1] + ds[1], layers)));
				updateImage(image, requestId);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		});
		t.start();
		if (waitUpdate)
			t.join();
	}

}

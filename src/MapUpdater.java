import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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

public class MapUpdater implements Runnable {
	private final JLabel imageLabel;
	private final String getXML = "http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1" + 
    		"&REQUEST=GetCapabilities";
	private final String getMap = "http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1" + 
			"&REQUEST=GetMap&BBOX=%d,%d,%d,%d&SRS=EPSG:4326" + 
			"&WIDTH=953&HEIGHT=480&LAYERS=%s" + 
			"&STYLES=&FORMAT=image/png&TRANSPARENT=true";
	
	// Kartan rajat
	private final int xBound = 180;
	private final int yBound = 90;
	private final int aspectRatio = 2;
	
	// Kartan koordinaatit
	private int xMin, xMax, yMin, yMax;
	
	// Kartan aktiiviset kerrokset
	private String layers;
	
	public MapUpdater(JLabel imageLabel) {
		this.imageLabel = imageLabel;
		this.xMin = -xBound;
		this.xMax = xBound;
		this.yMin = -yBound;
		this.yMax = yBound;
		this.layers = "bluemarble,cities";
	}
	
	public void reset()
	{
		xMin = -xBound;
		xMax = xBound;
		yMin = -yBound;
		yMax = yBound;
	}
	
	public void zoom(double zoomFactor) {
		int xCenter = (xMax + xMin) / 2;
		int xWidth = xMax - xMin;
		int yCenter = (yMax + yMin) / 2;
		int yHeight = yMax - yMin;
		
		// Muutetaan ensin Y-akselin pituus
		int newYHeight = (int) (yHeight * zoomFactor);
		newYHeight = (newYHeight + 1) / 2 * 2; // Muutetaan pituus kahdella jaolliseksi (muutos aina ylöspäin)
		newYHeight = Math.max(newYHeight, 2); // Minimipituus 2
		newYHeight = Math.min(newYHeight, 2 * yBound); // Maksimipituus 2*yBound
		int deltaYHeight = newYHeight - yHeight;

		// Ja sitten X-akseli Y:n muutoksen ja aspect ration mukaan
		int newXWidth = xWidth + deltaYHeight * aspectRatio;
		
		// Päivitetään kartan koordinaatit
		xMax = xCenter + newXWidth / 2;
		xMin = xCenter - newXWidth / 2;
		yMax = yCenter + newYHeight / 2;
		yMin = yCenter - newYHeight / 2;
		
		System.out.println(String.format("xmin: %d, xmax: %d; ymin: %d, ymax: %d",xMin,xMax,yMin,yMax));
	}
	
	/**
	 * Siirtyy kartalla X-akselin suuntaisesti.
	 * @param moveFactor kerroin, joka määrä kartan siirtymän nykyisestä X-akselin pituudesta. Negatiivinen arvo siirtää vasemmalle ja positiivinen oikealle.
	 */
	public void moveX(double moveFactor) {
		int xWidth = xMax - xMin;
		int moveAmount = (int) (xWidth * moveFactor);
		if (xMin + moveAmount < -xBound) { // Tarkastetaan onko vasen reuna edelleen alueella
			moveAmount = -xBound - xMin; // Rajoitetaan siirtymän määrä alueen reunaan jos tarpeen
		}
		if (xBound < xMax + moveAmount) { // Tarkastetaan onko oikea reuna edelleen alueella
			moveAmount = xBound - xMax; // Rajoitetaan siirtymän määrä alueen reunaan jos tarpeen
		}
		xMin += moveAmount;
		xMax += moveAmount;
	}
	
	/**
	 * Siirtyy kartalla Y-akselin suuntaisesti.
	 * @param moveFactor kerroin, joka määrä kartan siirtymän nykyisestä Y-akselin pituudesta. Negatiivinen arvo siirtää alas ja positiivinen ylös.
	 */
	public void moveY(double moveFactor) {
		int yWidth = yMax - yMin;
		int moveAmount = (int) Math.max(yWidth * moveFactor, 1.0);
		if (yMin + moveAmount < -yBound) { // Tarkastetaan onko alareuna edelleen alueella
			moveAmount = -yBound - yMin; // Rajoitetaan siirtymän määrä alueen reunaan jos tarpeen
		}
		if (yBound < yMax + moveAmount) { // Tarkastetaan onko yläreuna edelleen alueella
			moveAmount = yBound - yMax; // Rajoitetaan siirtymän määrä alueen reunaan jos tarpeen
		}
		System.out.println(moveAmount);
		yMin += moveAmount;
		yMax += moveAmount;
	}
	
	public void setLayers (String layers) {
		this.layers = layers;
	}
	
	public ArrayList<Tuple<String, String>> fetchLayers() throws Exception
	{
		ArrayList<Tuple<String, String>> layerList = new ArrayList<>();
		
		// Tee GetCapabilities kysely
        URL getCapabilitiesURL = new URL(getXML);        
        
        // Rakenna vastauksesta XML-dokumentti
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse(getCapabilitiesURL.openStream());
        
        // Hae kaikki mahdolliset Layerit dokumentista XPathilla
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer",
        		doc, XPathConstants.NODESET);
        
        // Käydään jokainen layer läpi, haetaan niille nimitiedot ja lisätään layereille checkboxit käyttöliittymään
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList layer = (NodeList) xPath.evaluate("Name|Title", nodeList.item(i), XPathConstants.NODESET);
            layerList.add(new Tuple<String, String>(layer.item(0).getTextContent(), layer.item(1).getTextContent()));
        }
        
        return layerList;
	}

	@Override
	public void run() {
		try {
			imageLabel.setIcon(new ImageIcon(new URL(String.format(getMap, xMin, yMin, xMax, yMax, layers))));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
	}
	

}

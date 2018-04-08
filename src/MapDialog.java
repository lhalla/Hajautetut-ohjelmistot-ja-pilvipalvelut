
// GUI of a map-viewing program
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MapDialog extends JFrame {

	// Components of the UI

	private JLabel imageLabel = new JLabel();
	private JPanel leftPanel = new JPanel();

	private JButton refreshB = new JButton("Refresh");
	private JButton leftB = new JButton("<");
	private JButton rightB = new JButton(">");
	private JButton upB = new JButton("^");
	private JButton downB = new JButton("v");
	private JButton zoomInB = new JButton("+");
	private JButton zoomOutB = new JButton("-");
	private JButton resetB = new JButton("Reset");

	private Mapper mapUpdater;

	public MapDialog() throws Exception {

		// Prepare the window and add the components to it

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());

		// THE FOLLOWING TEST LINE CAN BE REPLACED WITH A LINE THAT LOADS ANY OTHER
		// STARTING VIEW
		// imageLabel.setIcon(new ImageIcon(new
		// URL("http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&BBOX=-180,-90,180,90&SRS=EPSG:4326&WIDTH=953&HEIGHT=480&LAYERS=bluemarble,cities&STYLES=&FORMAT=image/png&TRANSPARENT=true")));

		add(imageLabel, BorderLayout.EAST);

		ButtonListener bl = new ButtonListener();
		refreshB.addActionListener(bl); // Refresh-button
		leftB.addActionListener(bl); // Move left -button
		rightB.addActionListener(bl); // Move right -button
		upB.addActionListener(bl); // Move up -button
		downB.addActionListener(bl); // Move down -button
		zoomInB.addActionListener(bl); // Zoom in -button
		zoomOutB.addActionListener(bl); // Zoom out -button
		resetB.addActionListener(bl); // Reset-button

		// Set a top-down layout manager.
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		// Set an empty border.
		leftPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		// Set the maximum size of the panel as 100x600
		leftPanel.setMaximumSize(new Dimension(100, 600));

		// Create a Mapper for updating the image.
		mapUpdater = new Mapper(imageLabel, 180, 90);

		// Fetch a list of all available layers.
		ArrayList<Tuple<String, String>> layerList = mapUpdater.fetchLayers();

		// Go through every layer and add a checkbox to the GUI.
		for (Tuple<String, String> layer : layerList) {
			LayerCheckBox layerCheckBox = new LayerCheckBox(layer.first(), layer.last(), false);
			layerCheckBox.setSelected(true); // Set the layer as selected
			leftPanel.add(layerCheckBox);
		}

		// Update the image.
		updateImage(true);

		// Add the buttons to the frame.
		leftPanel.add(refreshB);
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(leftB);
		leftPanel.add(rightB);
		leftPanel.add(upB);
		leftPanel.add(downB);
		leftPanel.add(zoomInB);
		leftPanel.add(zoomOutB);
		leftPanel.add(resetB);

		add(leftPanel, BorderLayout.WEST);

		pack();
		setVisible(true);

	}

	public static void main(String[] args) throws Exception {
		new MapDialog();
	}

	// Control button listener
	// updateImage()-method should be able to be used with every button
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// Refresh
			if (e.getSource() == refreshB) {
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Move left and update the image.
			if (e.getSource() == leftB) {
				mapUpdater.move(-0.3, 0);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Move right and update the image.
			if (e.getSource() == rightB) {
				mapUpdater.move(0.3, 0);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Move up and update the image.
			if (e.getSource() == upB) {
				mapUpdater.move(0.3, 1);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Move down and update the image.
			if (e.getSource() == downB) {
				mapUpdater.move(-0.3, 1);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Zoom in and update the image.
			if (e.getSource() == zoomInB) {
				mapUpdater.zoom(0.8);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Zoom out and update the image.
			if (e.getSource() == zoomOutB) {
				mapUpdater.zoom(1.25);
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			// Reset the image.
			if (e.getSource() == resetB) {
				mapUpdater.reset();
				try {
					updateImage(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	// Checkbox, which remembers the name of the map layer.
	private class LayerCheckBox extends JCheckBox {
		private String name = "";

		public LayerCheckBox(String name, String title, boolean selected) {
			super(title, null, selected);
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	// Check which layers have been selected, make a new request to the server
	// and update the image.
	public void updateImage(boolean waitUpdate) throws Exception {
		String s = "";

		// Find out which checkboxes are selected and construct
		// a comma-separated list of the layer names in 's'.
		Component[] components = leftPanel.getComponents();
		for (Component com : components) {
			if (com instanceof LayerCheckBox)
				if (((LayerCheckBox) com).isSelected())
					s += com.getName() + ",";
		}
		s = s.replaceAll(",$", "");

		// Update image in a separate thread.
		mapUpdater.setLayers(s);
		mapUpdater.updateMap(waitUpdate);
	}

} // MapDialog

/**
 * OSM2GpsMid 
 *  
 *
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerRectangle;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea;

import de.ueller.osmToGpsMid.model.Bounds;


public class GuiConfigWizard extends JFrame implements Runnable, ActionListener, SelectionListener {

	Configuration config;
	String planet;
	JComboBox jcbProperties;
	JComboBox jcbPlanet;
	JComboBox jcbPhone;
	JTextField jtfStyle;
	JCheckBox  jcbRouting;
	
	String [] planetFiles = {"osmXapi", "load File"};
	
	JMapViewer map;

	boolean dialogFinished = false;

	public GuiConfigWizard() {
		//this.config = c;
	}

	public Configuration startWizard() {
		System.out.println("Starting configuration wizard");
		config = new Configuration();
		//askOsmFile();
		setupWizard();
		return config;
	}

	public void setupWizard() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		this.setLayout(gbl);
		
		map = new JMapViewer();
		SelectionMapController mapController = new SelectionMapController(map,this);
		map.setSize(600, 400);
		gbc.gridwidth = 4;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(map, gbc);
		

		
		JLabel jlPlanet = new JLabel("Osm file: ");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		add(jlPlanet, gbc);

		jcbPlanet = new JComboBox(planetFiles);
		jcbPlanet.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		add(jcbPlanet, gbc);
		
		config.setPlanetName((String)jcbPlanet.getSelectedItem());
		
		JLabel jlStyle = new JLabel("Style: ");
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weighty = 0;
		add(jlStyle, gbc);

		jtfStyle = new JTextField(planet);
		jtfStyle.setEditable(false);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 3;
		gbc.gridy = 1;
		add(jtfStyle, gbc);


		Vector<String> propertiesName = enumerateBuiltinProperties();
		propertiesName.add(0, "load .properties file");
		propertiesName.add(0, "custom properties");
		jcbProperties = new JComboBox(propertiesName.toArray());
		jcbProperties.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		add(jcbProperties, gbc);
		
		jcbRouting = new JCheckBox("enable Routing");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		add(jcbRouting, gbc);
		
		jcbPhone = new JComboBox(enumerateAppParam().toArray());
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		add(jcbPhone, gbc);
		

		JButton jbOk = new JButton("Ok");
		jbOk.setActionCommand("OK-click");
		jbOk.addActionListener(this);
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 3;
		add(jbOk, gbc);

		JButton jbCancel = new JButton("Cancel");
		jbCancel.setActionCommand("Cancel-click");
		jbCancel.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.gridx = 2;
		gbc.gridy = 3;
		add(jbCancel, gbc);

		pack();
		setVisible(true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Window closing");
				if (dialogFinished != true) {
					System.exit(0);
				}
			}
		});
		
		resetPropertiesSelectors();

		Thread t = new Thread(this);
		t.start();
		try {
			t.join();
			System.out.println("Thread finished");
		} catch (InterruptedException e) {
			// Nothing to do
		}
	}
	
	private void resetPropertiesSelectors() {
		map.setMapMarkerAreaList(new LinkedList<MapMarkerArea>());
		Bounds [] bounds = config.getBounds();
		for (Bounds b : bounds) {
			System.out.println("Bounds: " + b);
			MapMarkerRectangle boundMarker = new MapMarkerRectangle(Color.BLACK,new Color(0x2fffffaf,true),b.maxLat,b.maxLon,b.minLat, b.minLon);
			map.addMapMarkerArea(boundMarker);
		}
		jtfStyle.setText(config.getStyleFileName());
		jcbRouting.setSelected(config.useRouting);
		jcbPhone.setSelectedItem(config.getString("app"));
	}

	private Vector<String> enumerateAppParam() {
		Vector<String> res = new Vector<String>();
		try {
			File jarFileName = new File(this.getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI());
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> jes = jarFile.entries();
			while (jes.hasMoreElements()) {
				String entryName = jes.nextElement().getName();
				if ((entryName.startsWith("GpsMid-")) && (entryName.endsWith(".jar"))) {
					System.out.println(entryName);
					res.add(entryName.substring(0, entryName.lastIndexOf("-")));
				}

			}
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	private Vector<String> enumerateBuiltinProperties() {
		Vector<String> res = new Vector<String>();
		try {
			File jarFileName = new File(this.getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI());
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> jes = jarFile.entries();
			while (jes.hasMoreElements()) {
				String entryName = jes.nextElement().getName();
				if (entryName.endsWith(".properties")
						&& !entryName.endsWith("version.properties")) {
					res.add(entryName.substring(0, entryName.length() - 11));
				}

			}
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	private void askOsmFile() {

		JFileChooser chooser = new JFileChooser();
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getAbsolutePath().endsWith(".osm")
						|| f.getAbsolutePath().endsWith(".osm.bz2")
						|| f.getAbsolutePath().endsWith(".osm.gz"))
					return true;

				return false;
			}

			@Override
			public String getDescription() {
				return "Openstreetmap file";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			planet = chooser.getSelectedFile().getAbsolutePath();
			config.setPlanetName(planet);
			jcbPlanet.addItem(planet);
			jcbPlanet.setSelectedItem(planet);
		}
		
	}

	private void askPropFile() {
		JFileChooser chooser = new JFileChooser();
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getAbsolutePath().endsWith(".properties"))
					return true;

				return false;
			}

			@Override
			public String getDescription() {
				return ".properties files";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String propName = chooser.getSelectedFile().getAbsolutePath();
			try {
				config.loadPropFile(new FileInputStream(propName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * Simply do nothing while the the dialog is still open. This is used to
	 * block on the dialog and make it modal
	 */
	@Override
	public void run() {
		while (!dialogFinished) {

			synchronized (this) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					// Nothing to do
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Action Performed: " + e);
		if ("OK-click".equalsIgnoreCase(e.getActionCommand())) {
			System.out.println("OK clicked");

			dialogFinished = true;
			setVisible(false);
			dispose();
		}
		if ("Cancel-click".equalsIgnoreCase(e.getActionCommand())) {
			System.out.println("Cancel clicked");
			System.exit(0);
		}
		
		if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
			if (e.getSource() == jcbProperties) {
				
				String chosenProperty = (String) jcbProperties.getSelectedItem();
				System.out.println(".properties changed to " + chosenProperty);
				if (chosenProperty
						.equalsIgnoreCase("load .properties file")) {
					askPropFile();
					resetPropertiesSelectors();
				} else {
					try {
						InputStream is = getClass().getResourceAsStream("/"+chosenProperty+".properties");
						if (is == null) {
							System.out.println("Something went wrong");
						}
						if (1 == 0)
							throw new IOException();
						config.loadPropFile(is);
						resetPropertiesSelectors();
					} catch (IOException ioe) {
						ioe.printStackTrace();
						return;
					}
				}
			}
			if (e.getSource() == jcbPlanet) {
				
				String chosenProperty = (String) jcbPlanet.getSelectedItem();
				System.out.println("planet changed to " + chosenProperty);
				
				if (chosenProperty
						.equalsIgnoreCase("load file")) {
					askOsmFile();
					resetPropertiesSelectors();
				} else {
					config.setPlanetName(chosenProperty);
				}
				
			}
		}

	}

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#regionSelected(de.ueller.osmToGpsMid.model.Bounds)
	 */
	@Override
	public void regionSelected(Bounds bound) {
		config.addBounds(bound);
		resetPropertiesSelectors();
	}

}

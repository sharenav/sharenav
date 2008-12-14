/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 * 
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
	JComboBox jcbStyle;
	JCheckBox  jcbRouting;
	JTextField jtfName;
	
	private static final String XAPI_SRC = "osmXapi";
	private static final String ROMA_SRC = "ROMA";
	private static final String FILE_SRC = "load .osm.bz2 File";
	String [] planetFiles = {XAPI_SRC, ROMA_SRC, FILE_SRC};
	
	private static final String LOAD_PROP = "load .properties file";
	private static final String CUSTOM_PROP = "custom properties";
	String [] propertiesList = {LOAD_PROP, CUSTOM_PROP};
	
	private static final String DEFAULT_STYLE = "Default style file";
	private static final String LOAD_STYLE = "load custom style file";
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

		this.setTitle("Osm2GpsMid V" + Configuration.getConfiguration().getVersion() + " (" + Configuration.getConfiguration().getBundleDate() + ")");
		this.setLayout(gbl);
		
		map = new JMapViewer();
		SelectionMapController mapController = new SelectionMapController(map,this);
		map.setSize(600, 400);
		gbc.gridwidth = 6;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(map, gbc);
		

		
		JPanel jpFiles = new JPanel(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		add(jpFiles, gbc);
		
		JLabel jlPlanet = new JLabel("Openstreetmap data source: ");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		jpFiles.add(jlPlanet, gbc);

		jcbPlanet = new JComboBox(planetFiles);
		jcbPlanet.addActionListener(this);
		jcbPlanet.setToolTipText("Select the .osm file to use in conversion. ROMA and osmXapi are online servers and should only be used for small areas");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 1;
		gbc.gridy = 0;
		jpFiles.add(jcbPlanet, gbc);
		
		config.setPlanetName((String)jcbPlanet.getSelectedItem());
		
		JLabel jlStyle = new JLabel("Style file: ");
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		jpFiles.add(jlStyle, gbc);

		jcbStyle = new JComboBox();
		jcbStyle.addItem(DEFAULT_STYLE);
		jcbStyle.addItem(LOAD_STYLE);
		jcbStyle.addActionListener(this);
		jcbStyle.setToolTipText("Select the style file to determin which features  of the raw data get included in the midlet");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 1;
		gbc.gridy = 2;
		jpFiles.add(jcbStyle, gbc);

		
		JLabel jlProps = new JLabel("Properties template: ");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpFiles.add(jlProps, gbc);

		Vector<String> propertiesName = enumerateBuiltinProperties();
		propertiesName.add(0, LOAD_PROP);
		propertiesName.add(0, CUSTOM_PROP);
		jcbProperties = new JComboBox(propertiesName.toArray());
		jcbProperties.addActionListener(this);
		jcbProperties.setToolTipText("Select a predefined configuration file");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		jpFiles.add(jcbProperties, gbc);
		
		JPanel jpOptions = new JPanel(new GridBagLayout());
		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		add(jpOptions, gbc);
		
		jcbRouting = new JCheckBox("enable Routing");
		jcbRouting.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		jpOptions.add(jcbRouting, gbc);
		
		JLabel jlName = new JLabel("Midlet name:");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpOptions.add(jlName, gbc);
		jtfName = new JTextField();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpOptions.add(jtfName, gbc);
		
		JLabel jlPhone = new JLabel("Phone capabilities template: ");
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		jpOptions.add(jlPhone, gbc);
		
		jcbPhone = new JComboBox(enumerateAppParam().toArray());
		jcbPhone.setToolTipText("Select the compilation version that contains the features supported by your phone. Generic-full should work well in most cases");
		jcbPhone.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		jpOptions.add(jcbPhone, gbc);
		

		JButton jbOk = new JButton("Create GpsMid midlet");
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
		
		JButton jbHelp = new JButton("Help");
		jbHelp.setActionCommand("Help-click");
		jbHelp.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.gridx = 4;
		gbc.gridy = 3;
		add(jbHelp, gbc);

		pack();
		setVisible(true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
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
		} catch (InterruptedException e) {
			// Nothing to do
		}
	}
	
	private void addMapMarkers() {	
		map.setMapMarkerAreaList(new LinkedList<MapMarkerArea>());
		Bounds [] bounds = config.getBounds();
		for (Bounds b : bounds) {
			MapMarkerRectangle boundMarker = new MapMarkerRectangle(Color.BLACK,new Color(0x2fffffaf,true),b.maxLat,b.maxLon,b.minLat, b.minLon);
			map.addMapMarkerArea(boundMarker);
		}
	}
	
	private void resetPropertiesSelectors() {
		String styleFile = config.getStyleFileName();
		if (styleFile != null) {
			System.out.println("Style: " +styleFile);
			//jcbStyle.removeItem(styleFile);
			boolean isAlreadyIn = false;
			for (int i = 0; i <  jcbStyle.getItemCount(); i++) {
				if (((String)jcbStyle.getItemAt(i)).equalsIgnoreCase(styleFile)) {
					isAlreadyIn = true;
				}
			}
			if (!isAlreadyIn)
				jcbStyle.addItem(styleFile);
			jcbStyle.setSelectedItem(styleFile);
		}
		jcbRouting.setSelected(config.useRouting);
		jcbPhone.setSelectedItem(config.getString("app"));
		jtfName.setText(config.getString("midlet.name"));
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

		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
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
				return "Openstreetmap file (*.osm.bz2, *.osm)";
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
	
	private void askStyleFile() {

		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getAbsolutePath().endsWith(".xml"))
					return true;
				return false;
			}

			@Override
			public String getDescription() {
				return "style file";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String styleName = chooser.getSelectedFile().getAbsolutePath();
			config.setStyleFileName(styleName);
			jcbStyle.addItem(styleName);
			jcbStyle.setSelectedItem(styleName);
		}
		
	}

	private void askPropFile() {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
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
		if ("OK-click".equalsIgnoreCase(e.getActionCommand())) {
			System.out.println("Configuration wizard has finished");

			dialogFinished = true;
			setVisible(false);
			dispose();
		}
		if ("Cancel-click".equalsIgnoreCase(e.getActionCommand())) {
			System.exit(0);
		}
		
		if ("help-click".equalsIgnoreCase(e.getActionCommand())) {
			JOptionPane.showMessageDialog(
					this,   "Welcome to the Osm2GpsMid Wizard!\n\n" +
							"Osm2GpsMid and GpsMid are licensed by GPL2 (http://www.gnu.org/)\n" + 
							"OpenStreetMap Data is licensed by CC 2.0 (http://www.creativecommons.org/)\n" +
							"\n" + 
							"Osm2GpsMid is a conversion program to package map data from OpenStreetMap into a 'midlet' called GpsMid.\n" +
							"The resulting midlet includes the specified map data and can be uploaded to J2ME ready mobiles for offline navigation.\n" +
							"\n" +
							"Usage:\n" +
							"1. Specify which region the world you want to include in your midlet. \n" +
							" This can either be done by dragging over an area on the world map with the right mouse button\n" +
							" or by specifying a .properties file that already contains the area you want.\n" +
							" If you want to set all the parameters using this wizard, please leave the .properties file on custom.\n" +
							"2. Specify a source for the OpenStreetMap data. Currently three sources are directly supported:\n" +
							" a) ROMA: This is the Read Only Map Api and downloads data directly from the API server (only for small regions like towns)\n" +
							" b) osmXapi: This is an alternative server and very similar to ROMA (only for small regions like towns)\n" +
							" c) load from file: Use a .osm or .osm.bz2 file previously downloaded to your computer (recommended)\n" +
							"    Country level extracts in .osm.bz2 file format are available\n" +
							"    i.e. at http://download.geofabrik.de/osm/ and http://downloads.cloudmade.com/\n" +
							"3. Press 'Create GpsMid midlet'\n" +
							"\n" +
							"For more information please visit http://gpsmid.sourceforge.net/ and http://gpsmid.wiki.sourceforge.net/");
			
		}
		
		if ("enable Routing".equalsIgnoreCase(e.getActionCommand())) {
			config.setRouting(((JCheckBox)e.getSource()).isSelected());
		}
		
		//if (e.getSource() == jtfName) {
			config.setMidletName(jtfName.getText());
		//}
		if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
			if (e.getSource() == jcbProperties) {
				
				config.resetConfig();
				String chosenProperty = (String) jcbProperties.getSelectedItem();
				if (chosenProperty
						.equalsIgnoreCase(LOAD_PROP)) {
					askPropFile();
				} else if (chosenProperty
						.equalsIgnoreCase(CUSTOM_PROP)){
				} else {
					try {
						InputStream is = getClass().getResourceAsStream("/"+chosenProperty+".properties");
						if (is == null) {
							System.out.println("Something went wrong");
						}
						if (1 == 0)
							throw new IOException();
						config.loadPropFile(is);
					} catch (IOException ioe) {
						ioe.printStackTrace();
						return;
					}
				}
				addMapMarkers();
				resetPropertiesSelectors();
			}
			if (e.getSource() == jcbPlanet) {
				
				String chosenProperty = (String) jcbPlanet.getSelectedItem();
				if (chosenProperty
						.equalsIgnoreCase(FILE_SRC)) {
					askOsmFile();
					//resetPropertiesSelectors();
				} else {
					config.setPlanetName(chosenProperty);
				}
				
			}
			if (e.getSource() == jcbStyle) {
				String chosenProperty = (String) jcbStyle.getSelectedItem();
				if (chosenProperty
						.equalsIgnoreCase(LOAD_STYLE)) {
					askStyleFile();
					//resetPropertiesSelectors();
				} else  if(chosenProperty
						.equalsIgnoreCase(DEFAULT_STYLE)) {
					config.setStyleFileName("style-file.xml");
					
				} else {
					config.setStyleFileName(chosenProperty);
				}
			}
			if (e.getSource() == jcbPhone) {
				config.setCodeBase((String)jcbPhone.getSelectedItem());
			}
		}

	}

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#regionSelected(de.ueller.osmToGpsMid.model.Bounds)
	 */
	@Override
	public void regionSelected(Bounds bound) {
		config.addBounds(bound);
		addMapMarkers();
	}

}

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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerRectangle;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea;

import de.ueller.osmToGpsMid.model.Bounds;


public class GuiConfigWizard extends JFrame implements Runnable, ActionListener, SelectionListener {
	
	protected class StreamGobbler extends OutputStream {
		
		JTextArea jta;
		JScrollPane jsp;

		protected StreamGobbler(JTextArea jta, JScrollPane jsp) {
			this.jta = jta;
			this.jsp = jsp;
		}
		
		public void write(byte[] b, int off, int len) {
			jta.append(new String(b,off,len));
			int max = jsp.getVerticalScrollBar().getMaximum();
			jsp.getVerticalScrollBar().setValue(max);
		}
		
		public void write(byte [] b) {
			jta.append(new String(b));
		}
		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		@Override
		public void write(int b) throws IOException {
			throw new IOException("Not yet Implemented");
		}
		
	}

	Configuration config;
	String planet;
	JComboBox jcbProperties;
	JComboBox jcbPlanet;
	JComboBox jcbPhone;
	JComboBox jcbStyle;
	JTextField jtfRouting;
	JTextField jtfName;
	JComboBox jcbSoundFormats;
	JCheckBox jcbEditing;
	JComboBox jcbCellSource;
	
	private static final String XAPI_SRC = "OsmXapi";
	private static final String ROMA_SRC = "ROMA";
	private static final String FILE_SRC = "Load .osm.bz2 File";
	private static final String CELL_SRC_NONE = "Include no Cell IDs";
	private static final String CELL_SRC_FILE = "Load cell ID file";
	private static final String CELL_SRC_DLOAD = "Download cell ID DB";
	
	private static final String SOUND_NONE = "Include no sound files";
	private static final String SOUND_AMR = "Include AMR sound files";
	private static final String SOUND_WAV = "Include WAV sound files";
	private static final String SOUND_WAV_AMR = "Include WAV and AMR files";
	
	private static final String JCB_EDITING = "Enable online OSM editing support";
	String [] planetFiles = {XAPI_SRC, ROMA_SRC, FILE_SRC};
	String [] cellidFiles = {CELL_SRC_NONE, CELL_SRC_FILE, CELL_SRC_DLOAD};
	String [] soundFormats = {SOUND_NONE, SOUND_AMR, SOUND_WAV, SOUND_WAV_AMR};
	
	private static final String LOAD_PROP = "Load .properties file";
	private static final String CUSTOM_PROP = "Custom properties";
	String [] propertiesList = {LOAD_PROP, CUSTOM_PROP};
	
	private static final String DEFAULT_STYLE = "Default style file";
	private static final String LOAD_STYLE = "Load custom style file";
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

		this.setTitle("Osm2GpsMid V" + Configuration.getConfiguration().getVersion() 
				+ " (" + Configuration.getConfiguration().getBundleDate() + ")");
		this.setLayout(gbl);
		
		map = new JMapViewer();
		SelectionMapController mapController = new SelectionMapController(map, this);
		map.setSize(600, 400);
		gbc.gridwidth = 9;
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
		jcbPlanet.setToolTipText("Select the .osm file to use in conversion. ROMA and OsmXapi are online servers and should only be used for small areas.");
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
		jcbStyle.setToolTipText("Select the style file to determine which features of the raw data get included in the midlet");
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
		
		JLabel jlRouting = new JLabel("Routing modes:");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		jpOptions.add(jlRouting, gbc);
		jtfRouting = new JTextField();
		jtfRouting.setText("motorcar, bicycle");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		jpOptions.add(jtfRouting, gbc);
		
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
		jcbPhone.setToolTipText("Select the compilation version that contains the features supported by your phone. Generic-full should work well in most cases.");
		jcbPhone.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		jpOptions.add(jcbPhone, gbc);
		
		JPanel jpOptions2 = new JPanel(new GridBagLayout());
		gbc.gridx = 6;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		add(jpOptions2, gbc);
				
		jcbEditing = new JCheckBox(JCB_EDITING);
		jcbEditing.addActionListener(this);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0;
		jpOptions2.add(jcbEditing, gbc);
		
		jcbSoundFormats = new JComboBox(soundFormats);
		jcbSoundFormats.setSelectedIndex(1);
		jcbSoundFormats.addActionListener(this);
		jcbSoundFormats.setToolTipText("Select sound formats to include into the midlet, e.g. most Windows Mobile devices support .wav but cannot replay .amr. GpsMid will use the first successful playing sound format included");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpOptions2.add(jcbSoundFormats, gbc);
		
		jcbCellSource = new JComboBox(cellidFiles);
		jcbCellSource.addActionListener(this);
		jcbCellSource.setToolTipText("Select a source of the Cell ID db for cell based location.");
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		jpOptions2.add(jcbCellSource, gbc);		
		
		JButton jbOk = new JButton("Create GpsMid midlet");
		jbOk.setActionCommand("Create-click");
		jbOk.addActionListener(this);
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 3;
		add(jbOk, gbc);

		JButton jbCancel = new JButton("Close");
		jbCancel.setActionCommand("Close-click");
		jbCancel.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		gbc.gridx = 3;
		gbc.gridy = 3;
		add(jbCancel, gbc);
		
		JButton jbHelp = new JButton("Help");
		jbHelp.setActionCommand("Help-click");
		jbHelp.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		gbc.gridx = 6;
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
		
		jbCancel.setEnabled(false);
		jbOk.setEnabled(false);
		jcbPlanet.setEnabled(false);
		jcbProperties.setEnabled(false);
		jcbStyle.setEnabled(false);
		jtfRouting.setEnabled(false);
		jtfName.setEnabled(false);
		jcbPhone.setEnabled(false);
		jcbSoundFormats.setEnabled(false);
		jcbCellSource.setEnabled(false);
		jcbEditing.setEnabled(false);
		
		JTextArea jtaConsoleOut = new JTextArea();
		jtaConsoleOut.setAutoscrolls(true);
		JScrollPane jspConsoleOut = new JScrollPane(jtaConsoleOut);
		jspConsoleOut.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Console Output:"));
		jspConsoleOut.setMinimumSize(new Dimension(400, 300));
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 9;
		gbc.weighty = 9;
		gbc.gridx = 0;
		gbc.gridy = 4;
		add(jspConsoleOut, gbc);
		
		JTextArea jtaConsoleErr = new JTextArea();
		jtaConsoleErr.setAutoscrolls(true);
		JScrollPane jspConsoleErr = new JScrollPane(jtaConsoleErr);
		jspConsoleErr.setBorder(BorderFactory.createTitledBorder("Errors:"));
		jspConsoleErr.setMinimumSize(new Dimension(400, 200));
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 9;
		gbc.weighty = 3;
		gbc.gridx = 0;
		gbc.gridy = 5;
		add(jspConsoleErr, gbc);
		
		remove(map);
		this.validate();

		
		System.setOut(new PrintStream(new StreamGobbler(jtaConsoleOut, jspConsoleOut), false));
		System.setErr(new PrintStream(new StreamGobbler(jtaConsoleErr, jspConsoleErr), true));
		
	}
	
	private void addMapMarkers() {	
		map.setMapMarkerAreaList(new LinkedList<MapMarkerArea>());
		Vector<Bounds> bounds = config.getBounds();
		for (Bounds b : bounds) {
			MapMarkerRectangle boundMarker = new MapMarkerRectangle(Color.BLACK, 
					new Color(0x2fffffaf, true), b.maxLat, b.maxLon, b.minLat, b.minLon);
			map.addMapMarkerArea(boundMarker);
		}
	}
	
	private void resetPropertiesSelectors() {
		String styleFile = config.getStyleFileName();
		if (styleFile != null) {
			System.out.println("Style: " + styleFile);
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
		jtfRouting.setText(config.useRouting);
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
					if (!res.contains(entryName.substring(0, entryName.lastIndexOf("-")))) {
						res.add(entryName.substring(0, entryName.lastIndexOf("-")));
					}
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
						|| f.getAbsolutePath().endsWith(".osm.gz")) {
					return true;
				}
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
				if (f.isDirectory() || f.getAbsolutePath().endsWith(".xml")) {
					return true;
				}
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
				if (f.isDirectory()	|| f.getAbsolutePath().endsWith(".properties")) {
					return true;
				}
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
				System.out.println("Loading properties specified by GUI: " + propName);
				config.loadPropFile(new FileInputStream(propName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	private void askCellFile() {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getAbsolutePath().endsWith(".txt.gz")) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return ".txt.gz files";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String cellSource = chooser.getSelectedFile().getAbsolutePath();
			System.out.println("Setting CellID source: " + cellSource);
			config.setCellSource(cellSource);
		}

	}

	/*
	 * Simply do nothing while the dialog is still open. This is used to
	 * block on the dialog i.e. to prevent the Midlet creation process
	 * from starting immediately.
	 */
	public void run() {
		while (dialogFinished == false) {
			synchronized (this) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					// Nothing to do
				}
			}
		}
	}

	/**
	 * 
	 */
	private void writeProperties(String fileName) {
		File file = new File(fileName);
		try {
			FileWriter fw = new FileWriter(file);

			fw.write("# Properties file generated by the Osm2GpsMid Wizard"); 
			fw.write("\r\n");
			fw.write("# You can have up to 9 regions.\r\n");
			fw.write("# Ways and POIs in any of the regions will be written to the bundle.\r\n");
			Vector<Bounds> bounds = config.getBounds();
			if (bounds != null) {
				int i = 1;
				for (Bounds b : bounds) {
					fw.write(b.toPropertyString(i++));
				}
			}
			fw.write("\r\n");
			fw.write("# To choose a different device specific build use the app property.\r\n");
			fw.write("# GpsMid-Generic-full should work for most phones (except BlackBerry)\r\n");
			String app = config.getAppParam(); 
			fw.write("app = " + app + "\r\n");

			Vector<String>apps = enumerateAppParam();
			// write out available app parameters except for the selected one
			for (String a: apps) {
				if (! a.equals(app)) {
					fw.write("#app = " + a + "\r\n");
				}
			}
			fw.write("\r\n");
			fw.write("# Routing ability can be disabled to save space in the midlet by setting to false\r\n");
			fw.write("# or set to one or more defined in the style-file, e.g. motorcar, bicycle, foot\r\n");
			fw.write("useRouting = " + config.useRouting + "\r\n");
			fw.write("\r\n");
			
			fw.write("# == Advanced parameters for configuring number of files in the midlet ===\r\n");
			fw.write("#  With less files more memory will be required on the device to run GpsMid\r\n");	
			fw.write("#  Larger dictionary depth will reduce the number of dictionary files in GpsMid\r\n");
			fw.write("maxDictDepth = " + config.getMaxDictDepth() + "\r\n");
			fw.write("#  Larger tile size will reduce the number of tile files in the midlet\r\n");
			fw.write("# Maximum route tile size in bytes\r\n");
			fw.write("routing.maxTileSize = " + config.getMaxRouteTileSize() + "\r\n");
			fw.write("# Maximum tile size in bytes\r\n");
			fw.write("maxTileSize = " + config.getMaxTileSize() + "\r\n");
			fw.write("# Maximum ways contained in tiles for level 0-3\r\n");
			for (int i=0;i < 4; i++) {
				fw.write("maxTileWays" + i + " = " + config.getMaxTileWays(i) + "\r\n");
			}
			fw.write("\r\n");

			fw.write("# Style-file containing which way, area and POI types to include in the Midlet\r\n");
			fw.write("# This will default to style-file.xml, set style-file=min-style-file.xml for a smaller style-file with less features in the map\r\n");
			fw.write("#	 If there is no internal version in Osm2GpsMid for the png / sound files, you must provide external versions\r\n");
			fw.write("#	 in the directory or a sound sub-directory of the Osm2GpsMid directory (when using internal style-file)");
			fw.write("#	 or in the directory or a png / sound sub-directory of the style-file. \r\n");
			fw.write("style-file = " + config.getStyleFileName() + "\r\n");
			fw.write("\r\n");
			
			fw.write("# Sound formats to be included in the midlet, default is useSounds=amr\r\n");
			fw.write("#  Osm2GpsMid includes from all sound files wav, amr and mp3 variants\r\n");
			fw.write("#  wav is the most compatible, loudest but also the most size intensive format\r\n");
			fw.write("#  example to include wav AND amr: useSounds=wav, amr\r\n");
			fw.write("#  GpsMid will try a fallback to another included sound format when trying to play a format unsupported by the device\r\n");
			fw.write("useSounds = " + config.getUseSounds() + "\r\n");
			fw.write("\r\n");
			
			fw.write("# Whether to include icons for icon menu and their size to include\r\n");
			fw.write("#  possible values: false|small|true|big  true is the default medium size\r\n");
			fw.write("useIcons = " + config.getUseIcons() + "\r\n");
			fw.write("\r\n");
			fw.write("# Name of the Midlet on the phone\r\n");
			fw.write("midlet.name = " + config.getMidletName() + "\r\n");
			fw.close();		
		} catch (IOException ioe) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ("Create-click".equalsIgnoreCase(e.getActionCommand())) {
			config.setMidletName(jtfName.getText());
			config.setRouting(jtfRouting.getText());
			System.out.println("Create Midlet clicked");

			dialogFinished = true;
			writeProperties("last.properties");
		}
		if ("Close-click".equalsIgnoreCase(e.getActionCommand())) {
			writeProperties("last.properties");
			System.exit(0);
		}
		
		if ("Help-click".equalsIgnoreCase(e.getActionCommand())) {
			JOptionPane.showMessageDialog(
					this,   "Welcome to the Osm2GpsMid Wizard!\n\n" +
							"Osm2GpsMid and GpsMid are licensed under GPL2 (http://www.gnu.org/)\n" + 
							"OpenStreetMap Data is licensed under CC 2.0 (http://www.creativecommons.org/)\n" +
							"\n" + 
							"Osm2GpsMid is a conversion program to package map data from OpenStreetMap into a 'midlet' called GpsMid.\n" +
							"The resulting midlet includes the specified map data and can be uploaded to J2ME ready mobiles for offline navigation.\n" +
							"\n" +
							"Usage:\n" +
							"1. Specify which region of the world you want to include in your midlet. \n" +
							" This can either be done by dragging over an area on the world map with the right mouse button\n" +
							" or by specifying a .properties file that already contains the area you want.\n" +
							" You can delete boxes by double-clicking on them.\n" +
							" If you want to set all the parameters using this wizard, please leave 'Properties template' on 'Custom'.\n" +
							"2. Specify a source for the OpenStreetMap data. Currently three sources are directly supported:\n" +
							" a) ROMA: This is the Read Only Map Api and downloads data directly from the API server (only for small regions like towns)\n" +
							" b) OsmXapi: This is an alternative server and very similar to ROMA (only for small regions like towns)\n" +
							" c) Load from file: Use a .osm or .osm.bz2 file previously downloaded to your computer (recommended)\n" +
							"    Country level extracts in .osm.bz2 file format are available\n" +
							"    i.e. at http://download.geofabrik.de/osm/ and http://downloads.cloudmade.com/\n" +
							"3. Press 'Create GpsMid midlet'\n" +
							"\n" +
							"Your changes in the wizard are written to last.properties so you can use this as\n" +
							"a starting point for your .properties file.\n" +
							"\n" +
							"For more information please visit http://gpsmid.sourceforge.net/ and http://gpsmid.wiki.sourceforge.net/");
		}
		
		if ("enable Routing".equalsIgnoreCase(e.getActionCommand())) {
			// TODO: expose different vehicles for routing in GuiConfigWizard instead of always assuming motorcar
			if ( ((JCheckBox)e.getSource()).isSelected() ) {
				config.setRouting("motorcar");
			}
		}
		
		if (JCB_EDITING.equalsIgnoreCase(e.getActionCommand())) {
				config.enableEditingSupport = ((JCheckBox)e.getSource()).isSelected();
				if (config.enableEditingSupport && !((String)jcbPhone.getSelectedItem()).contains("Editing")) {
					//TODO: Need to find a way to select item without refering to hardcoded string names
					jcbPhone.setSelectedItem("GpsMid-Generic-editing");
					JOptionPane.showMessageDialog(this, "Editing requires online support. Changed Phone capabilities setting accordingly");
				}
		}
		
		if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
			if (e.getSource() == jcbProperties) {
				
				config.resetConfig();
				String chosenProperty = (String) jcbProperties.getSelectedItem();
				if (chosenProperty.equalsIgnoreCase(LOAD_PROP)) {
					askPropFile();
				} else if (chosenProperty.equalsIgnoreCase(CUSTOM_PROP)) {
				} else {
					try {
						System.out.println("Loading built in properties (" + chosenProperty + ")");
						InputStream is = getClass().getResourceAsStream("/" + chosenProperty + ".properties");
						if (is == null) {
							System.out.println("Something went wrong");
						}
						if (1 == 0) {
							throw new IOException();
						}
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
				if (chosenProperty.equalsIgnoreCase(FILE_SRC)) {
					askOsmFile();
					//resetPropertiesSelectors();
				} else {
					config.setPlanetName(chosenProperty);
				}
				
			}
			if (e.getSource() == jcbStyle) {
				String chosenProperty = (String) jcbStyle.getSelectedItem();
				if (chosenProperty.equalsIgnoreCase(LOAD_STYLE)) {
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
			if (e.getSource() == jcbCellSource) {
				
				String chosenProperty = (String) jcbCellSource.getSelectedItem();
				if (CELL_SRC_NONE.equalsIgnoreCase(chosenProperty)) {
					config.setCellOperator("false");
				} else if (CELL_SRC_DLOAD.equalsIgnoreCase(chosenProperty)) {
					config.setCellOperator("true");
					config.setCellSource("http://myapp.fr/cellsIdData/cells.txt.gz");
				} else if (CELL_SRC_FILE.equalsIgnoreCase(chosenProperty)) {
					config.setCellOperator("true");
					askCellFile();
				}
			}
			if (e.getSource() == jcbSoundFormats) {
				
				String chosenProperty = (String) jcbSoundFormats.getSelectedItem();
				if (SOUND_NONE.equalsIgnoreCase(chosenProperty)) {
					config.setSounds("false");
				} else if (SOUND_AMR.equalsIgnoreCase(chosenProperty)) {
					config.setSounds("amr");
				} else if (SOUND_WAV.equalsIgnoreCase(chosenProperty)) {
					config.setSounds("wav");
				} else if (SOUND_WAV_AMR.equalsIgnoreCase(chosenProperty)) {
					config.setSounds("wav, amr");
				}
			}

		}

	}

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#regionMarked(de.ueller.osmToGpsMid.model.Bounds)
	 */
	public void regionMarked(Bounds bound) {
		if (config.getBounds().size() < Configuration.MAX_BOUND_BOXES)
		{
			config.addBounds(bound);
			addMapMarkers();
		} else {
			JOptionPane.showMessageDialog(this, "You cannot create more than " +
					Configuration.MAX_BOUND_BOXES + " bounding boxes.");
		}
	}

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#pointDoubleClicked(float, float)
	 */
	public void pointDoubleClicked(float lat, float lon) {
		System.out.println("Double click at lat=" + lat + "|lon=" + lon);
		Vector<Bounds> bounds = config.getBounds();
		for (int i = 0; i < bounds.size(); i++) {
			if (bounds.elementAt(i).isIn(lat, lon)) {
				System.out.println("  Deleting box " + i + " " + 
						bounds.elementAt(i).toString());
				config.removeBoundsAt(i);
				addMapMarkers();
				break;
			}
		}
	}

}

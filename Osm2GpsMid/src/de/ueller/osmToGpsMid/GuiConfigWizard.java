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

import de.ueller.osmToGpsMid.Configuration;
import static de.ueller.osmToGpsMid.GetText._;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapArea;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.route.Location;
import de.ueller.osmToGpsMid.route.LocationTableModel;
import de.ueller.osmToGpsMid.route.Route;


public class GuiConfigWizard extends JFrame implements Runnable, ActionListener, SelectionListener {
	
	protected class TeeOutputStream extends OutputStream {
		OutputStream s1;
		OutputStream s2;
		protected TeeOutputStream(OutputStream stream1, OutputStream stream2) {
			s1 = stream1;
			s2 = stream2;
		}
		
		@Override
		public void write(byte[] b, int off, int len){
			try {
				s1.write(b,off,len);
				s2.write(b,off,len);
			} catch (IOException ioe) {
				System.err.println("Error in writing to stream: " + ioe.getMessage());
			}
		}
		
		@Override
		public void write(byte [] b) {
			try{
				s1.write(b);
				s2.write(b);
			} catch (IOException ioe) {
				System.err.println("Error in writing to stream: " + ioe.getMessage());
			}
		}
		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(int)
		 */
		@Override
		public void write(int i) throws IOException {
			try{
				s1.write(i);
				s2.write(i);
			} catch (IOException ioe) {
				System.err.println("Error in writing to stream: " + ioe.getMessage());
			}
		}
	}
	
	protected class StreamGobbler extends OutputStream {
		
		JTextArea jta;
		JScrollPane jsp;

		protected StreamGobbler(JTextArea jta, JScrollPane jsp) {
			this.jta = jta;
			this.jsp = jsp;
		}
		
		@Override
		public void write(byte[] b, int off, int len) {
			jta.append(new String(b,off,len));
			int max = jsp.getVerticalScrollBar().getMaximum();
			jsp.getVerticalScrollBar().setValue(max);
		}
		
		@Override
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

	
	/** Needed as this class is somehow serializable. */
	private static final long serialVersionUID = 1L;

	private static final String CHOOSE_SRC = "Choose your map data source";
	private static final String FILE_SRC = "Load .osm.bz2/.osm.pbf File (recommended)";
	private static final String XAPI_SRC = "OsmXapi";
	private static final String ROMA_SRC = "ROMA";
	private static final String CELL_SRC_NONE = "Include no Cell IDs";
	private static final String CELL_SRC_FILE = "Load cell ID file";
	private static final String CELL_SRC_DLOAD = "Download cell ID DB";
	
	private static final String SOUND_NONE = "Include no sound files";
	private static final String SOUND_AMR = "Include AMR sound files";
	private static final String SOUND_WAV = "Include WAV sound files";
	private static final String SOUND_WAV_AMR = "Include WAV and AMR files";
	
	private static final String JCB_EDITING = "Enable online OSM editing support";
	
	private static final String JCB_HOUSENUMBERS = "Enable house number support";

	private static final String JCB_CELLIDNOLAC = "Store cellids in a format usable by phones with no LAC support";
	private static final String JCB_GENERATESEA = "Generate sea from coastlines";
	
	private static final String ORS_URL="http://openrouteservice.org/php/OpenLSRS_DetermineRoute.php";
	
	private String useLang = null;
	private String useLangName = null;

	String [] planetFiles = {CHOOSE_SRC, FILE_SRC, XAPI_SRC, ROMA_SRC};
	Vector cellidFiles = new Vector();
	String [] soundFormats = {SOUND_NONE, SOUND_AMR, SOUND_WAV, SOUND_WAV_AMR};
	
	private static final String LOAD_PROP = "Load .properties file";
	private static final String LAST_PROP = "Last used properties";
	private static final String CUSTOM_PROP = "Custom properties";
	String [] propertiesList = {LOAD_PROP, LAST_PROP, CUSTOM_PROP};
	
	private static final String BUILTIN_STYLE_NORMAL = "Built-in style-file.xml";
	private static final String BUILTIN_STYLE_MINI = "Built-in mini-style-file.xml";
	private static final String BUILTIN_STYLE_REDUCED = "Built-in reduced-style-file.xml";
	private static final String BUILTIN_STYLE_STREET = "Built-in street-style-file.xml";
	private static final String LOAD_STYLE = "Load custom style file";
	
	private boolean customSoundfiles = false;
	private String origUseLang = "*";

	/** Preferences stored in a location determined automatically by the runtime */
	Preferences prefs;
	
	Configuration config;
	String planet;
	JComboBox jcbPlanet;
	JComboBox jcbProperties;
	JComboBox jcbPhone;
	JComboBox jcbTileSize;
	JComboBox jcbStyle;
	JTextField jtfRouting;
	JTextField jtfName;
	JComboBox jcbSoundFormats;
	JCheckBox jcbEditing;
	JCheckBox jcbcellIDnoLAC;
	JCheckBox jcbGenerateSea;
	JCheckBox jcbHousenumbers;
	String langList[] = {
		"*",
		"en",
		"cs",
		"de",
		"es",
		"fi",
		"fr",
		"it",
		"pl",
		"ru",
		"sk"
	};
	String langNameList[] = {
		"",
		"English(5)",
		"Čeština(5)",
		"Deutsch(4)",
		"Spanish(2)",
		"suomi(4)",
		"French(1)",
		"Italian(2)",
		"Portugese(2)",
		"Russian(4)",
		"Slovak(5)"
	};
	JCheckBox languages[] = new JCheckBox[langList.length];
	JComboBox jcbCellSource;
	JButton jbCreate;
	JButton jbCreateZip;
	JButton jbClose;
	JButton jbClearRoute;
	JButton jbCalcRoute;
	
	/** File chooser dialog for OSM file */
	JFileChooser jOsmFileChooser;
	/** File chooser dialog for bundle .properties file */
	JFileChooser jPropFileChooser;
	/** File chooser dialog for style file */
	JFileChooser jStyleFileChooser;
	/** Component handling the map display */
	JMapViewer map;
	Pattern startPattern=Pattern.compile("<gml:LineString");
	Pattern posPattern=Pattern.compile("<gml:pos>([0-9.]+) ([0-9.]+)</gml:pos>");
	Vector<Coordinate> routeResult=new Vector<Coordinate>();
	boolean dialogFinished = false;

	private JTable destList;


	public GuiConfigWizard() {
		//this.config = c;
		// Load preferences, the package name of this class is relevant for
		// finding them again. The runtime chooses an appropriate location for
		// them, usually in the user's directory.
        prefs = Preferences.userNodeForPackage(this.getClass());
	}

	public Configuration startWizard(String[] args) {
		System.out.println("Starting configuration wizard");
		config = new Configuration(args);
		setupWizard();
		return config;
	}

	public void setupWizard() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		this.setTitle("Osm2GpsMid V" + Configuration.getConfiguration().getVersion()
				+ " (" + Configuration.getConfiguration().getBundleDate() + ")");
		this.setLayout(gbl);
		
		// Default constructor uses the DefaultMapController, so we need to use
		// the specialized constructor.
		map = new JMapViewer(new MemoryTileCache(), 4);
		SelectionMapController mapController = new SelectionMapController(map, this);
		map.setSize(600, 400);
		gbc.gridwidth = 8;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(map, gbc);


		JPanel jpRouteCorridor = new JPanel(new GridBagLayout());
		gbc.gridx = 8;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		add(jpRouteCorridor, gbc);

		JLabel jlSeparator1 = new JLabel(" ");
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 0;
		jpRouteCorridor.add(jlSeparator1, gbc);

		JLabel jlRouteCorridor = new JLabel("Optional Route Corridor Destinations");
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 1;
		jpRouteCorridor.add(jlRouteCorridor, gbc);

		destList=new JTable(new LocationTableModel(config.getRouteList()));
		destList.setToolTipText("Add route corridor destinations with Alt+Click or Shift+Click on the map");
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		jpRouteCorridor.add(destList, gbc);
		
		
		jbClearRoute = new JButton("Clear Route Corridor");
		jbClearRoute.setActionCommand("ClearRoute-click");
		jbClearRoute.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 3;
		jpRouteCorridor.add(jbClearRoute, gbc);

		jbCalcRoute = new JButton("Calculate Route Corridor");
		jbCalcRoute.setActionCommand("CalculateRoute-click");
		jbCalcRoute.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 4;
		jpRouteCorridor.add(jbCalcRoute, gbc);

		JPanel langOptions = new JPanel(new GridBagLayout());
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 5;
		jpRouteCorridor.add(langOptions, gbc);
		
		JLabel langLabel = new JLabel("Lang:");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0;
		langOptions.add(langLabel, gbc);

		for (int i = 0; i < langList.length ; i++) {
			languages[i] = new JCheckBox(langList[i]);
			languages[i].addActionListener(this);
			// enable * and English by default
			gbc.gridx = i+1;
			gbc.gridy = 0;
			gbc.weighty = 0;
			langOptions.add(languages[i], gbc);
			if (i == 0 || i == 1) {
				languages[i].setSelected(true);
			}
		}

		JPanel jpFiles = new JPanel(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		add(jpFiles, gbc);
		
		JLabel jlPlanet = new JLabel(_("Openstreetmap data source: "));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		jpFiles.add(jlPlanet, gbc);

		jcbPlanet = new JComboBox(planetFiles);
		jcbPlanet.setSelectedItem(CHOOSE_SRC);
		jcbPlanet.addActionListener(this);
		jcbPlanet.setToolTipText("Select the .osm file to use in conversion. ROMA and OsmXapi are online servers and should only be used for small areas.");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 1;
		gbc.gridy = 0;
		jpFiles.add(jcbPlanet, gbc);
		
		if (!CHOOSE_SRC.equals((String)jcbPlanet.getSelectedItem())) {
			config.setPlanetName((String)jcbPlanet.getSelectedItem());
		}
		
		JLabel jlStyle = new JLabel("Style file: ");
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		jpFiles.add(jlStyle, gbc);

		jcbStyle = new JComboBox();
		jcbStyle.addItem(BUILTIN_STYLE_NORMAL);
		jcbStyle.addItem(BUILTIN_STYLE_MINI);
		jcbStyle.addItem(BUILTIN_STYLE_REDUCED);
		jcbStyle.addItem(BUILTIN_STYLE_STREET);
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
		propertiesName.add(0, LAST_PROP);
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
		
		JLabel jlName = new JLabel("Midlet/map name:");
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

		JLabel jlTileSize = new JLabel("Map tile file count vs. size: ");
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weighty = 0;
		jpOptions.add(jlTileSize, gbc);

		Vector vTileSize = new Vector();
		vTileSize.addElement("Many small map tile files");
		vTileSize.addElement("Average map tile file size and count");
		vTileSize.addElement("Fewer but big map tile files");
		vTileSize.addElement("Even fewer and large map tile files");
		vTileSize.addElement("Custom - loaded from .properties");
		jcbTileSize = new JComboBox(vTileSize);
		jcbTileSize.setToolTipText("Some devices do not support many files in the jar, other require small map tile files to be able to load them into their limited RAM");
		jcbTileSize.addActionListener(this);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		jpOptions.add(jcbTileSize, gbc);

		
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
		
		jcbHousenumbers = new JCheckBox(JCB_HOUSENUMBERS);
		jcbHousenumbers.addActionListener(this);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpOptions2.add(jcbHousenumbers, gbc);
		
		jcbcellIDnoLAC = new JCheckBox(JCB_CELLIDNOLAC);
		jcbcellIDnoLAC.addActionListener(this);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		jpOptions2.add(jcbcellIDnoLAC, gbc);

		jcbGenerateSea = new JCheckBox(JCB_GENERATESEA);
		jcbGenerateSea.addActionListener(this);
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weighty = 0;
		jpOptions2.add(jcbGenerateSea, gbc);
		
		jcbSoundFormats = new JComboBox(soundFormats);
		jcbSoundFormats.setSelectedIndex(1);
		jcbSoundFormats.addActionListener(this);
		jcbSoundFormats.setToolTipText("Select sound formats to include into the midlet, e.g. most Windows Mobile devices support .wav but cannot replay .amr. GpsMid will use the first successful playing sound format included");
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0;
		jpOptions2.add(jcbSoundFormats, gbc);
		
		cellidFiles.addElement(CELL_SRC_NONE);
		cellidFiles.addElement(CELL_SRC_FILE);
		cellidFiles.addElement(CELL_SRC_DLOAD);
		jcbCellSource = new JComboBox(cellidFiles);
		if (!config.getString("cellSource").equals("")) {
			cellidFiles.addElement(config.getString("cellSource"));
			jcbCellSource.setSelectedIndex(3);
		}
		jcbCellSource.addActionListener(this);
		jcbCellSource.setToolTipText("Select a source of the Cell ID db for cell based location.");
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weighty = 0;
		jpOptions2.add(jcbCellSource, gbc);
		
		jbCreate = new JButton("Create GpsMid midlet");
		jbCreate.setActionCommand("Create-midlet");
		jbCreate.addActionListener(this);
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 4;
		add(jbCreate, gbc);

		jbCreateZip = new JButton("Create GpsMid map zip");
		jbCreateZip.setActionCommand("Create-map");
		jbCreateZip.addActionListener(this);
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 2;
		gbc.gridy = 4;
		add(jbCreateZip, gbc);

		jbClose = new JButton("Close");
		jbClose.setActionCommand("Close-click");
		jbClose.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.gridx = 4;
		gbc.gridy = 4;
		add(jbClose, gbc);
		
		JButton jbHelp = new JButton("Help");
		jbHelp.setActionCommand("Help-click");
		jbHelp.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 3;
		gbc.weighty = 0;
		gbc.gridx = 6;
		gbc.gridy = 4;
		add(jbHelp, gbc);

		pack();
		setVisible(true);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exitApplication();
			}
		});
		
		updatePropertiesSelectors();

		Thread t = new Thread(this);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// Nothing to do
		}
		
		jbClose.setEnabled(false);
		jbCreate.setEnabled(false);
		jbCreateZip.setEnabled(false);
		jcbPlanet.setEnabled(false);
		jcbProperties.setEnabled(false);
		jcbStyle.setEnabled(false);
		jtfRouting.setEnabled(false);
		jtfName.setEnabled(false);
		jcbPhone.setEnabled(false);
		jcbTileSize.setEnabled(false);
		jcbSoundFormats.setEnabled(false);
		jcbCellSource.setEnabled(false);
		jcbEditing.setEnabled(false);
		jcbHousenumbers.setEnabled(false);
		jcbcellIDnoLAC.setEnabled(false);
		jcbGenerateSea.setEnabled(false);
		destList.setVisible(false);
		jbCalcRoute.setEnabled(false);
		jbClearRoute.setEnabled(false);
		
		JTextArea jtaConsoleOut = new JTextArea();
		jtaConsoleOut.setAutoscrolls(true);
		JScrollPane jspConsoleOut = new JScrollPane(jtaConsoleOut);
		jspConsoleOut.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),	"Console Output:"));
		jspConsoleOut.setMinimumSize(new Dimension(400, 300));
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 9;
		gbc.weighty = 9;
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridheight=1;
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
		gbc.gridy = 6;
		add(jspConsoleErr, gbc);
		
		remove(map);
		this.validate();

		System.setOut(new PrintStream(new TeeOutputStream(System.out,new StreamGobbler(jtaConsoleOut, jspConsoleOut))));
		System.setErr(new PrintStream(new TeeOutputStream(System.err, new StreamGobbler(jtaConsoleErr, jspConsoleErr))));
	}

	/** All actions that result in an exit of the application *must* call this
	 * method to allow proper saving of data.
	 */
	private void exitApplication() {
		updateSettings(true);
		writeProperties("last.properties");
		
		// Update persistent settings for next program run
		if (jOsmFileChooser != null) {
			File file = jOsmFileChooser.getSelectedFile();
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) {
					prefs.put("planet-file.lastDirectory", parent);
				}
			}
		}
		
        try {
            prefs.flush();
        } catch (BackingStoreException bse) {
			JOptionPane.showMessageDialog(this,	"Failed to save preferences, error is: "
					+ bse.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			bse.printStackTrace();
        }

		System.exit(0);
	}

	/** Takes the bounds from the config object and puts them on the map.
	 */
	private void addMapMarkers() {
		LinkedList<MapRectangle> rects = new LinkedList<MapRectangle>();
		Vector<Bounds> bounds = config.getBounds();
		for (Bounds b : bounds) {
			Coordinate boundTopLeft = new Coordinate(b.maxLat, b.maxLon);
			Coordinate boundBottomRight = new Coordinate(b.minLat, b.minLon);
			MapArea boundMarker = new MapArea(Color.BLACK,
					new Color(0x2fffff70, true), boundTopLeft, boundBottomRight);
			rects.add(boundMarker);
		}
		map.setMapRectangleList(rects);
	}

	/** Takes the route destinations from the config object and puts them on the map.
	 */
	private void addRouteDestMarkers() {
		LinkedList<MapMarker> mapMarkers = new LinkedList<MapMarker>();
		Vector<Location> locations = config.getRouteList();
		for (Location lc : locations) {
			MapMarkerDot d = new MapMarkerDot(lc.getNode().lat, lc.getNode().lon);
			mapMarkers.add(d);
		}
		map.setMapMarkerList(mapMarkers);
	}

	
	/** Updates the GUI elements from the settings currently found in config.
	 * This is usually needed after reading a bundle file.
	 */
	private void updatePropertiesSelectors() {
		String styleFile = config.getStyleFileName();
		String mapSource = config.getPlanetName();
		if (mapSource != null && !mapSource.equals("")) {
			planet = mapSource;
			jcbPlanet.addItem(mapSource);
			jcbPlanet.setSelectedItem(mapSource);
		}
		if (styleFile != null) {
			System.out.println("Updating GUI elements\n  Style: " + styleFile);
			//jcbStyle.removeItem(styleFile);
			boolean isAlreadyIn = false;
			for (int i = 0; i <  jcbStyle.getItemCount(); i++) {
				if (((String)jcbStyle.getItemAt(i)).equalsIgnoreCase(styleFile)) {
					isAlreadyIn = true;
				}
			}
			if (!isAlreadyIn) {
				jcbStyle.addItem(styleFile);
			}
			// Avoid trigger of another setStyleFile() through handleComboBoxChanged()
			jcbStyle.removeActionListener(this);
			jcbStyle.setSelectedItem(styleFile);
			jcbStyle.addActionListener(this);
		}
		System.out.println("  useRouting: " + config.useRouting);
		jtfRouting.setText(config.useRouting);
		System.out.println("  app: " + config.getString("app"));
		jcbPhone.removeActionListener(this);
		jcbPhone.setSelectedItem(config.getString("app"));
		jcbPhone.addActionListener(this);
		jcbTileSize.removeActionListener(this);
		jcbTileSize.setSelectedIndex(config.getTileSizeVsCountId());
		jcbTileSize.addActionListener(this);
		System.out.println("  cellSource: " + config.getString("cellSource"));
		if (!config.getString("cellSource").equals("")) {
			jcbCellSource.removeActionListener(this);
			cellidFiles.addElement(config.getString("cellSource"));
			jcbCellSource.setSelectedIndex(3);
			jcbCellSource.addActionListener(this);
		}
		System.out.println("  midlet.name: " + config.getString("midlet.name"));
		jtfName.setText(config.getString("midlet.name"));
		guiSettingsFromConfig();
		jcbEditing.setSelected(config.enableEditingSupport);
		jcbHousenumbers.setSelected(config.useHouseNumbers);
		jcbcellIDnoLAC.setSelected(config.getCellIDnoLAC());
		jcbGenerateSea.setSelected(config.getGenerateSea());
	}

	/** Finds all files in the Osm2GpsMid JAR that match the pattern "GpsMid-*.jar"
	 * and puts their names in a vector, cutting off at the last "-".
	 * E.g. GpsMid-Generic-multi-0.6.4-map65.jar -> GpsMid-Generic-multi
	 * 
	 * @return Vector containing the names
	 */
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
					if (!res.contains(entryName.substring(0, entryName.lastIndexOf("-", entryName.lastIndexOf("-")-1)))) {
						res.add(entryName.substring(0, entryName.lastIndexOf("-", entryName.lastIndexOf("-")-1)));
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
	
	/** Finds all files in the Osm2GpsMid JAR that match the pattern "*.properties"
	 * and puts their names in a vector, without the ".properties".
	 * E.g. Cologne.properties -> Cologne
	 * 
	 * @return Vector containing the names
	 */
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

	/** Opens a file chooser dialog for the OSM XML (.osm or .bz2 or .gz) file.
	 * Updates the config when the file was chosen.
	 */
	private boolean askOsmFile() {
		if (jOsmFileChooser == null) {
			// Use the previously chosen directory if available, else use the user directory.
			String chosenDir = prefs.get("planet-file.lastDirectory",
					System.getProperty("user.dir"));
			jOsmFileChooser = new JFileChooser(chosenDir);
			FileFilter ff = new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory() || f.getAbsolutePath().endsWith(".osm")
							|| f.getAbsolutePath().endsWith(".osm.bz2")
							|| f.getAbsolutePath().endsWith(".osm.pbf")
							|| f.getAbsolutePath().endsWith(".osm.gz")) {
						return true;
					}
					return false;
				}
	
				@Override
				public String getDescription() {
					return "Openstreetmap file (*.osm.pbf, *.osm.bz2, *.osm)";
				}
			};
			jOsmFileChooser.setFileFilter(ff);
		}
		
		int returnVal = jOsmFileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			// Update configuration
			planet = jOsmFileChooser.getSelectedFile().getAbsolutePath();
			if (!planet.equalsIgnoreCase(CHOOSE_SRC)) {
				config.setPlanetName(planet);
				// Add as entry to the drop down list
				jcbPlanet.addItem(planet);
				jcbPlanet.setSelectedItem(planet);
			}
			return true;
		} else {
			return false;
		}
	}
	
	/** Opens a file chooser dialog for the style file.
	 * Lets the config read the file when it was chosen.
	 */
	private void askStyleFile() {
		if (jStyleFileChooser == null) {
			jStyleFileChooser = new JFileChooser(System.getProperty("user.dir"));
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
			jStyleFileChooser.setFileFilter(ff);
		}

		int returnVal = jStyleFileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String styleName = jStyleFileChooser.getSelectedFile().getAbsolutePath();
				config.setStyleFileName(styleName);
				jcbStyle.addItem(styleName);
				jcbStyle.setSelectedItem(styleName);
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this,	ioe.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace();
			}
		}
	}

	/** Sets GUI settings from the loaded .properties file.
	 */
	private void guiSettingsFromConfig() {
		// Set desired languages
		String propLang[] = config.getUseLang().split("[;,]", 200);
		for (int i = 2; i < languages.length ; i++) {
			languages[i].setSelected(false);
			for (int j = 0; j < propLang.length ; j++) {
				//System.out.println ("Comparing strings: " + propLang[j] + " " + langList[i]);
				if (propLang[j].equals(langList[i])) {
					languages[i].setSelected(true);
				}
				if (propLang[j].equals("*")) {
					languages[0].setSelected(true);
				}
			}
		}
		origUseLang = getSelectedUseLang();
	}

	/** Opens a file chooser dialog for the bundle .properties file.
	 * Lets the config read the file when it was chosen.
	 */
	private void askPropFile() {
		if (jPropFileChooser == null) {
			jPropFileChooser = new JFileChooser(System.getProperty("user.dir"));
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
			jPropFileChooser.setFileFilter(ff);
		}
		
		int returnVal = jPropFileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String propName = jPropFileChooser.getSelectedFile().getAbsolutePath();
			try {
				System.out.println("Loading properties specified by GUI: " + propName);
				config.loadPropFile(new FileInputStream(propName));
				addRouteDestMarkers();
				destList.repaint();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this,
						"Failed to load properties file. Error is: "
						+ ioe.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace();
			}
		}
	}
	
	/** Opens a file chooser dialog for the file containing the CellID data.
	 * Updates config when file was chosen.
	 */
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
	 * Writes the current properties to a .properties file
	 * TODO: Shouldn't useCellID be written too?
	 * TODO: Add useHouseNumbers and useWordSearch
	 * And what about the cellSource variable from Configuration.java?
	 * @param fileName Path name of file to write
	 */
	private void writeProperties(String fileName) {
		File file = new File(fileName);
		try {
			FileWriter fw = new FileWriter(file);

			fw.write("# Properties file generated by the Osm2GpsMid Wizard\r\n");
			fw.write("\r\n");
			if (config.getPlanetName() != null && !"".equals(config.getPlanetName())) {
				// quote possible backslashes
				fw.write("mapSource = " + config.getPlanetName().replace("\\", "\\\\") + "\r\n");
			}
			if (!"".equals(config.getCellSource())) {
				// quote possible backslashes
				fw.write("cellSource = " + config.getCellSource().replace("\\", "\\\\") + "\r\n");
				fw.write("useCellID = " + config.getString("useCellID") + "\r\n");
			}
			fw.write("# Store cellids for phones without LAC.\r\n");
			fw.write("cellIDnoLAC = " + config.getCellIDnoLAC() + "\r\n");
			fw.write("\r\n");

			fw.write("# Generate sea from coastlines.\r\n");
			fw.write("generateSea = " + config.getGenerateSea() + "\r\n");
			fw.write("\r\n");

			fw.write("# Build word indexes.\r\n");
			fw.write("useWordSearch = " + config.getUseWordSearch() + "\r\n");
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
			
			fw.write("# Route corridor destinations\r\n");
			fw.write("# Coordinates of route destinations which are used to calculate the route corridor.\r\n");
			fw.write("# Osm2GpsMid " + config.getVersion() + " supports this only with Osm2GpsMid Wizard.\r\n");
			if (config.getRouteList() != null) {
				int i = 1;
				for (Location lc : config.getRouteList()) {
					fw.write(lc.toPropertyString(i++));
				}
			}
			fw.write("\r\n");
			

			fw.write("# To choose a different device specific build, use the app property.\r\n");
			fw.write("# GpsMid-Generic-full should work for most phones (except BlackBerry).\r\n");
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

			fw.write("# Add this line to .manifest / .jad.\r\n");
			fw.write("# e.g. MIDlet-Touch-Support:true to hide keyboard on Samsung Bada mobiles\r\n");				
			fw.write("# or LGE-MIDlet-Display-Nav-Keypad:no on LG mobiles\r\n");				
			fw.write("addToManifest = " + config.getAddToManifest() + "\r\n");
			fw.write("\r\n");			
			
			fw.write("# File endings of files to not compress.\r\n");
			fw.write("# e.g. for Android and WinCE uncompressed WAV files are required\r\n");
			fw.write("# Example to not compress files ending with wav: dontCompress = wav\r\n");
			fw.write("dontCompress = " + config.getDontCompress() + "\r\n");
			fw.write("\r\n");			

			fw.write("# Editing support.\r\n");
			fw.write("enableEditing = " + config.enableEditingSupport + "\r\n");
			fw.write("\r\n");

			fw.write("# Housenumber support.\r\n");
			fw.write("useHouseNumbers = " + config.useHouseNumbers + "\r\n");
			fw.write("\r\n");

			fw.write("# Routing ability can be disabled to save space in the midlet by setting to false.\r\n");
			fw.write("# Or set to one or more defined in the style-file, e.g. motorcar, bicycle, foot.\r\n");
			fw.write("useRouting = " + config.useRouting + "\r\n");
			fw.write("\r\n");
			
			fw.write("# == Advanced parameters for configuring number of files in the midlet ===\r\n");
			fw.write("#  With less files more memory will be required on the device to run GpsMid.\r\n");
			fw.write("#  Larger dictionary depth will reduce the number of dictionary files in GpsMid.\r\n");
			fw.write("maxDictDepth = " + config.getMaxDictDepth() + "\r\n");
			fw.write("#  Larger tile size will reduce the number of tile files in the midlet.\r\n");
			fw.write("# Maximum route tile size in bytes\r\n");
			fw.write("routing.maxTileSize = " + config.getMaxRouteTileSize() + "\r\n");
			fw.write("# Maximum tile size in bytes\r\n");
			fw.write("maxTileSize = " + config.getMaxTileSize() + "\r\n");
			fw.write("# Maximum ways contained in tiles for level 0-3\r\n");
			for (int i=0;i < 4; i++) {
				fw.write("maxTileWays" + i + " = " + config.getMaxTileWays(i) + "\r\n");
			}
			fw.write("\r\n");

			fw.write("# Style-file containing which way, area and POI types to include in the Midlet.\r\n");
			fw.write("# This will default to style-file.xml, set style-file=min-style-file.xml for a smaller version with less features in the map.\r\n");
			fw.write("#	 If there is no internal version in Osm2GpsMid for the png / sound files, you must provide external versions\r\n");
			fw.write("#	 in the current directory or sub-directories 'sound' and 'png' inside Osm2GpsMid.jar (when using internal style-file)\r\n");
			fw.write("#	 or sub-directories 'sound' and 'png' in the same directory as the external style-file.\r\n");
			fw.write("style-file = " + config.getStyleFileName().replace("\\", "\\\\") + "\r\n");
			fw.write("\r\n");
			
			fw.write("# Sound formats to be included in the midlet, default is useSounds=amr.\r\n");
			fw.write("#  Osm2GpsMid includes from all sound files wav, amr and mp3 variants.\r\n");
			fw.write("#  Wav is the most compatible, loudest but also the most size intensive format.\r\n");
			fw.write("#  Example to include wav AND amr: useSounds=wav, amr\r\n");
			fw.write("#  GpsMid will try a fallback to another included sound format when trying to play a format unsupported by the device.\r\n");
			fw.write("useSounds = " + config.getUseSounds() + "\r\n");
			fw.write("\r\n");

			if (useLang != null) {
				fw.write("# Languages to be included in the midlet\r\n");
				fw.write("lang = " + useLang + "\r\n");
				fw.write("\r\n");
			}

			if (useLangName != null) {
				fw.write("# Language names to be included in the midlet\r\n");
				fw.write("langName = " + useLangName + "\r\n");
				fw.write("\r\n");
			}

			fw.write("# Directory/Directories with sound files and syntax.cfg, default is useSoundFilesWithSyntax=sound\r\n");
			fw.write("#  syntax.cfg is a text file defining which sound files\r\n");
			fw.write("#  are played by GpsMid for the various routing instructions in which order (to respect grammar)\r\n");
			fw.write("#  Osm2GpsMid includes all sound files referenced in the syntax.cfg either from an internal folder\r\n");
			fw.write("#  or the directory with the specified name relative to the style-file.\r\n");
			fw.write("#  File Format examples are at:\r\n");
			fw.write("#   English: http://gpsmid.cvs.sourceforge.net/viewvc/gpsmid/Osm2GpsMid/resources/media/sound/syntax.cfg?view=markup.\r\n");
			fw.write("#   German: http://gpsmid.cvs.sourceforge.net/viewvc/gpsmid/Osm2GpsMid/resources/media/sound-de/syntax.cfg?view=markup.\r\n");
			fw.write("#  Currently the following sound-files with syntax are internal to Osm2GpsMid:\r\n");
			fw.write("#   English: sound  German: sound-de Finnish: sound-fi\r\n");			
			fw.write("#  Example to include the Finnish and German sound files: useSoundFilesWithSyntax=sound-de, sound-fi\r\n");			
			fw.write("#  Generally there's no need to set this if you just want to use the standard sound files with the windowed Osm2GpsMid\r\n");			
			fw.write("#  - it's set automatically for you from selected languages\r\n");

			// comment out if we didn't read a custom soundfile setting, created automatically
			// from selected language(s)
			if (! customSoundfiles || !getSelectedUseLang().equals(origUseLang)) {
				fw.write("# ");
			}
			fw.write("useSoundFilesWithSyntax = " + config.getSoundFiles() + "\r\n");
			fw.write("\r\n");
			
			fw.write("# Whether to include icons for icon menu and their size to include.\r\n");
			fw.write("#  Possible values: false|small|true|big|large|huge, true is the default medium size\r\n");
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
	public void actionPerformed(ActionEvent event) {
		if ("Create-midlet".equalsIgnoreCase(event.getActionCommand())) {
			handleCreateClicked(true);
		} else if ("Create-map".equalsIgnoreCase(event.getActionCommand())) {
			handleCreateClicked(false);
		} else if ("Close-click".equalsIgnoreCase(event.getActionCommand())) {
			exitApplication();
		} else if ("Help-click".equalsIgnoreCase(event.getActionCommand())) {
			handleHelpClicked();
		} else if ("enable Routing".equalsIgnoreCase(event.getActionCommand())) {
			// TODO: expose different vehicles for routing in GuiConfigWizard instead of always assuming motorcar
			if ( ((JCheckBox)event.getSource()).isSelected() ) {
				config.setRouting("motorcar");
			}
		} else if ("ClearRoute-click".equalsIgnoreCase(event.getActionCommand())) {
			config.getRouteList().clear();
			map.setMapMarkerList(new LinkedList<MapMarker>());
			destList.repaint();
		} else if (JCB_HOUSENUMBERS.equalsIgnoreCase(event.getActionCommand())) {
			config.useHouseNumbers = ((JCheckBox)event.getSource()).isSelected();
		} else if (JCB_CELLIDNOLAC.equalsIgnoreCase(event.getActionCommand())) {
			config.setCellIDnoLAC(((JCheckBox)event.getSource()).isSelected());
		} else if (JCB_GENERATESEA.equalsIgnoreCase(event.getActionCommand())) {
			config.setGenerateSea(((JCheckBox)event.getSource()).isSelected());
		} else if ("CalculateRoute-click".equalsIgnoreCase(event.getActionCommand())) {
			handleCalculateRoute();
		} else if (JCB_EDITING.equalsIgnoreCase(event.getActionCommand())) {
			config.enableEditingSupport = ((JCheckBox)event.getSource()).isSelected();
			if (config.enableEditingSupport && !((String)jcbPhone.getSelectedItem()).contains("Editing")) {
				//TODO: Need to find a way to select item without refering to hardcoded string names
				jcbPhone.setSelectedItem("GpsMid-Generic-editing");
				JOptionPane.showMessageDialog(this, "Editing requires online support. Changed Phone capabilities setting accordingly");
			}
		} else if ("comboBoxChanged".equalsIgnoreCase(event.getActionCommand())) {
			handleComboBoxChanged(event);
		}
	}

	/**
	 * 
	 */
	private void handleCalculateRoute() {
		if (config.getRouteList().size() > 1) {
			Route route = new Route(config.getRouteList(), 10000,map);
			Area a=route.createArea();
			config.setArea(a);
		} else {
			JOptionPane.showMessageDialog(this,	"Please add first at least two route destinations with Alt+Click or Shift+Click on the map"
					, "Route Corridor Calculation", JOptionPane.ERROR_MESSAGE);
		}

	}

	private String getSelectedUseLang() {
		String langString = "";
		for (int i = 2; i < langList.length ; i++) {
			if (languages[i].isSelected()) {
				//System.out.println("Lang selected: " + langList[i]);
				langString += "," + langList[i];
			}
		}
		return langString;
	}

	private void updateSettings(boolean midlet) {
		String soundFiles = "sound";
		useLang = langList[1];
		useLangName = langNameList[1];
		for (int i = 2; i < langList.length ; i++) {
			if (languages[i].isSelected()) {
				//System.out.println("Lang selected: " + langList[i]);
				useLang += "," + langList[i];
				useLangName += "," + langNameList[i];
				// existence of sound dir will be checked later
				soundFiles += ",sound-" + langList[i];
			}
		}
		// preserve custom sounds
		if (config.getSoundFiles().equals("sound") || (!getSelectedUseLang().equals(origUseLang))) {
			customSoundfiles = false;
			config.setSoundFiles(soundFiles);
		} else {
			customSoundfiles = true;
			soundFiles = config.getSoundFiles();
		}
		config.setUseLang(useLang);
		config.setUseLangName(useLangName);
		// "*" is last
		if (languages[0].isSelected()) {
			config.setAllLang(true);
			useLang += ",*";
		}
		if (languages[0].isSelected()) {
			config.setAllLang(true);
			useLangName += ",*";
		}
		config.setMidletName(jtfName.getText());
		config.setRouting(jtfRouting.getText());
		config.setCodeBase((String)jcbPhone.getSelectedItem());
		config.mapzip = !midlet;
	}

	/** Handles the case that the button "Create Midlet" was clicked.
	 */
	private void handleCreateClicked(boolean midlet) {
		if (((String)jcbPlanet.getSelectedItem()).equalsIgnoreCase(CHOOSE_SRC)) {
			if (askOsmFile() == false) {
				JOptionPane.showMessageDialog(this,
					"Osm2GpsMid can't create a map without suitable OpenStreetMap data.\n" +
					"Please choose an appropriate OpenStreetMap data source. See help for more details.",
					"OpenStreetMap data", JOptionPane.PLAIN_MESSAGE);
				return;
			}
		}
		updateSettings(midlet);
		System.out.println("Create Map or Midlet clicked");

		dialogFinished = true;
		writeProperties("last.properties");
	}

	/** Handles the case that the button "Help" was clicked.
	 */
	private void handleHelpClicked() {
		final JEditorPane jepHelpMsg = new JEditorPane();
		final JScrollPane scrPane = new JScrollPane();
		jepHelpMsg.setPreferredSize(new Dimension(4000,4000));
		jepHelpMsg.setEditable(false);
		jepHelpMsg.setContentType("text/html");
		scrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); 
		scrPane.getViewport().add(jepHelpMsg);
		jepHelpMsg.setText(
			"<html><body>" +
			"<h1>Welcome to the Osm2GpsMid Wizard!</h1>" +
			"Osm2GpsMid and GpsMid are licensed under <a href =\"http://www.gnu.org/licenses/old-licenses/gpl-2.0.html\">GPLv2</a><br>" +
			"OpenStreetMap Data is licensed under <a href =\"http://creativecommons.org/licenses/by-sa/2.0/\">CC-BY-SA</a><br>" +
			"<br>" +
			"Osm2GpsMid is a conversion program to package map data from <a href=\"http://www.openstreetmap.org\">OpenStreetMap</a> into a 'midlet' called GpsMid.\n" +
			"The resulting midlet includes the specified map data and can be uploaded to J2ME ready mobiles for offline navigation.\n" +
			"<br><br>" +
			"Usage:<br><ol>" +
			"<li> Specify which region of the world you want to include in your midlet. \n" +
			" This can be done in different ways. \n" +
			"<ol><li>by dragging over an area on the world map with the right mouse button\n</li>" +
			"<li>by specifying a .properties file that already contains the area you want.\n</li>" +
			"<li>by defining a route corridor with shift+click or alt+click (equivalent) on at least\n" +
			"two places on the map, and after that clicking on \"Calculate Route Corridor\"\n</li></ol>" +
			" You can delete boxes by double-clicking on them.\n" +
			" If you want to set all the parameters using this wizard, please leave 'Properties template' on 'Custom'.\n" +
			"<li> Specify a source for the OpenStreetMap data. Currently three sources are directly supported:\n" +
			"<ul>" +
			" <li> ROMA: This is the Read Only Map Api and downloads data directly from the API server (only for small regions like towns)</li>" +
			" <li> OsmXapi: This is an alternative server and very similar to ROMA (only for small regions like towns)</li>" +
			" <li> Load from file: Use a .osm or .osm.bz2 file previously downloaded to your computer (recommended)\n" +
			"    Country level extracts in .osm.bz2 file format are available\n" +
			"    i.e. at <a href=\"http://download.geofabrik.de/osm/\">GeoFabrik</a> and <a href=\"http://downloads.cloudmade.com/\">CloudMade</a></li>" +
			"</ul>" +
			"<li> Select languages for which to include on-screen translation and navigation instructions (when available).\n" +
			"The language selection \"*\" includes all available languages for on-screen use, to be selected with\n" + 
			"language code or as device's default language\n" +
			"<li> Press 'Create GpsMid midlet' or 'Create GpsMid map zip'\n" +
			"</ol><br>" +
			"Your changes in the wizard are written to last.properties so you can use this as\n" +
			"a starting point for your .properties file.<br>" +
			"<br>" +
			"For more information please visit our <a href=\"http://gpsmid.sourceforge.net/\">Homepage</a> and <a href=\"http://sourceforge.net/apps/mediawiki/gpsmid/\">Wiki</a>"+
			"</body></html>");
		
		jepHelpMsg.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				Window window = SwingUtilities.getWindowAncestor(jepHelpMsg);
				if (window instanceof Dialog) {
					Dialog dialog = (Dialog)window;
					if (!dialog.isResizable()) {
						dialog.setResizable(true);
					}
					dialog.setSize(800, 650);
				}
			}
		});
		
		jepHelpMsg.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (Exception ex) {
							//Nothing to do if we can't open a browser
						}
					}
				}
			}
		});
		
		JOptionPane.showMessageDialog(this, scrPane, "Help", JOptionPane.PLAIN_MESSAGE);
	}

	/** Handles change events for the combo boxes
	 * @param event ActionEvent describing the change
	 */
	private void handleComboBoxChanged(ActionEvent event) {
		if (event.getSource() == jcbProperties) {
			String chosenProperty = (String) jcbProperties.getSelectedItem();
			if (chosenProperty.equalsIgnoreCase(LOAD_PROP)) {
				askPropFile();
			} else if (chosenProperty.equalsIgnoreCase(LAST_PROP)) {
				// Entries added by askPropFile() have a full path name
				try {
					System.out.println("Loading properties from last.properties");
					config.loadPropFile(new FileInputStream("last.properties"));
					addRouteDestMarkers();
					destList.repaint();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,
							"Failed to load properties file. Error is: "
							+ ioe.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace();
				}
			} else if (chosenProperty.equalsIgnoreCase(CUSTOM_PROP)) {
				config.resetConfig();
			} else if (chosenProperty.contains("/") || chosenProperty.contains("\\")) {
				// Entries added by askPropFile() have a full path name
				try {
					System.out.println("Loading properties specified by GUI: " +
							chosenProperty);
					config.loadPropFile(new FileInputStream(chosenProperty));
					addRouteDestMarkers();
					destList.repaint();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,
							"Failed to load properties file. Error is: "
							+ ioe.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace();
				}
			} else {
				// These are entries added with enumerateBuiltinProperties()
				try {
					System.out.println("Loading built in properties '" + chosenProperty + "'");
					InputStream is = getClass().getResourceAsStream("/" + chosenProperty + ".properties");
					if (is == null) {
						throw new IOException("Properties file could not be opened.");
					}
					config.loadPropFile(is);
					addRouteDestMarkers();
					destList.repaint();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,
							"Failed to load built in properties. Error is: "
							+ ioe.getMessage() + " Please report this bug.",
							"Error",
							JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace();
					return;
				}
			}
			addMapMarkers();
			map.setDisplayToFitMapRectangle();
			updatePropertiesSelectors();
		}
		if (event.getSource() == jcbPlanet) {
			
			String chosenProperty = (String) jcbPlanet.getSelectedItem();
			if (chosenProperty.equalsIgnoreCase(FILE_SRC)) {
				if (!askOsmFile()) {
					jcbPlanet.setSelectedItem(CHOOSE_SRC);
				}
			} else {
				config.setPlanetName(chosenProperty);
			}
			
		}
		if (event.getSource() == jcbStyle) {
			try {
				String chosenProperty = (String) jcbStyle.getSelectedItem();
				if (chosenProperty.equalsIgnoreCase(LOAD_STYLE)) {
					askStyleFile();
				} else if (chosenProperty.equalsIgnoreCase(BUILTIN_STYLE_NORMAL)) {
					config.setStyleFileName("/style-file.xml");
				} else if (chosenProperty.equalsIgnoreCase(BUILTIN_STYLE_MINI)) {
					config.setStyleFileName("/mini-style-file.xml");
				} else if (chosenProperty.equalsIgnoreCase(BUILTIN_STYLE_REDUCED)) {
					config.setStyleFileName("/reduced-style-file.xml");
				} else if (chosenProperty.equalsIgnoreCase(BUILTIN_STYLE_STREET)) {
					config.setStyleFileName("/street-style-file.xml");
				} else {
					config.setStyleFileName(chosenProperty);
				}
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this,	ioe.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace();
			}
		}
		if (event.getSource() == jcbPhone) {
			config.setCodeBase((String)jcbPhone.getSelectedItem());
		}
		if (event.getSource() == jcbTileSize) {
			config.setTileSizeVsCountId(jcbTileSize.getSelectedIndex());
		}
		if (event.getSource() == jcbCellSource) {
			
			String chosenProperty = (String) jcbCellSource.getSelectedItem();
			if (CELL_SRC_NONE.equalsIgnoreCase(chosenProperty)) {
				config.setCellOperator("false");
			} else if (CELL_SRC_DLOAD.equalsIgnoreCase(chosenProperty)) {
				config.setCellOperator("true");
				config.setCellSource("http://dump.opencellid.org/cellsIdData/cells.txt.gz");
			} else if (CELL_SRC_FILE.equalsIgnoreCase(chosenProperty)) {
				config.setCellOperator("true");
				askCellFile();
			} else if (!chosenProperty.equals("")) {
				config.setCellOperator("true");
			}
		}
		if (event.getSource() == jcbSoundFormats) {
			
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

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#regionMarked(de.ueller.osmToGpsMid.model.Bounds)
	 */
	public void regionMarked(Bounds bound) {
		System.out.println("Region marked: " + bound.toString());
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

	/** Used to reenable the "Close" button from BundleGpsMid (after Midlet creation).
	 */
	public void reenableClose() {
		jbClose.setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see de.ueller.osmToGpsMid.SelectionListener#addRouteDestination(org.openstreetmap.gui.jmapviewer.Coordinate)
	 */
	@Override
	public void addRouteDestination(Coordinate clickPoint) {
		Location location = new Location((float)clickPoint.getLat(),(float)clickPoint.getLon());
		config.addRouteDestination(location);
		destList.repaint();
	}
		
}

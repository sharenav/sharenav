/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2008  Kai Krueger
 * Copyright (C) 2008  sk750
 * 
 */
package de.ueller.osmToGpsMid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.SoundDescription;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.WayDescription;

/**
 * @author hmueller
 *
 */
public class Configuration {
	
	/**
	 * Specifies the format of the map on disk we are about to write
	 * This constant must be in sync with GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 27;
	
		private ResourceBundle rb;
		private ResourceBundle vb;
		private String tmp=null;
		private String planet;
		private String cell;
		private String propFile;
		private String bundleName;
		private String midletName;
		private String appParam;
		/**
		 * defines which routing options from the style-file get used 
		 */
		public String useRouting = "motorcar";
		public boolean enableEditingSupport=false;
		public int maxTileSize=20000;
		public int maxRouteTileSize=3000;
		public String styleFile;
		private Bounds[] bounds;
		
		public int background_color;
		public int routeColor = 0x0000C0C0;
		public int routeBorderColor = 0x0064FFFF;
		public int priorRouteColor = 0x00007070;
		public int priorRouteBorderColor = 0x00647777;
		public int routeDotColor = 0x0000C0C0;
		public int routeDotBorderColor = 0x0064FFFF;
		public String changeSoundFileExtensionTo = "";
		
		private LegendParser legend;
		
		// array containing real scale for pseudo zoom 0..32
		private static float realScale [] = new float[33]; 

		private static Configuration conf;

//		private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//				.getBundle(BUNDLE_NAME);

		public Configuration(String [] args) {
			
			//Set singleton
			conf = this;
			
			for (String arg : args) {
				if (arg.startsWith("--")) {
					if (arg.startsWith("--bounds=")) {
						String bound = arg.substring(9);
						System.out.println("Found bound: " + bound);
						String [] boundValues = bound.split(",");
						if (boundValues.length == 4) {
							if (bounds == null) {
								bounds = new Bounds[1];
							} else {
								Bounds [] tmp = new Bounds[bounds.length + 1];
								System.arraycopy(bounds, 0, tmp, 0, bounds.length);
								bounds = tmp;
							}
							Bounds b = new Bounds();
							try {								
								b.minLon = Float.parseFloat(boundValues[0]);
								b.minLat = Float.parseFloat(boundValues[1]);
								b.maxLon = Float.parseFloat(boundValues[2]);
								b.maxLat = Float.parseFloat(boundValues[3]);
							} catch (NumberFormatException nfe) {
								System.out.println("ERROR: invalid coordinate specified in bounds");
								nfe.printStackTrace();
								System.exit(1);
							}
							bounds[bounds.length - 1] = b;
						} else {
							System.out.println("ERROR: Invalid bounds parameter, should be specified as --bounds=left,bottom,right,top");
							System.exit(1);
						}
					}
					if (arg.startsWith("--cellID=")) {
						//Filename for a list of GSM cellIDs with their coordinates.
						//This file can be obtained from http://myapp.fr/cellsIdData/ (OpenCellID.org)
						cell = arg.substring(9);
					}
					if (arg.startsWith("--help")) {
						System.err.println("Usage: Osm2GpsMid [--bounds=left,bottom,right,top] [--cellID=filename] planet.osm.bz2 [location]");
						System.err.println("  \"--bounds=\" specifies the set of bounds to use in GpsMid ");
						System.err.println("       Can be left out to use the regions specified in location.properties");
						System.err.println("       or if you want to create a GpsMid for the whole region");
						System.err.println("       contained in the.osm(.bz2) file");
						System.err.println("  \"--cellID=\" specifies the file from which to load cellIDs for cell based positioning");
						System.err.println("       The data comes from OpenCellId.org and the file can be found at http://myapp.fr/cellsIdData/");
						System.err.println("  planet.osm.bz2: points to a (compressed) .osm file");
						System.err.println("       By specifying osmXapi, the data can be fetched straight from the server (only works for small areas)");
						System.err.println("  location: points to a .properties file specifying additional parameters");
						System.exit(0);
					}
					
				} else if (planet == null) {
					planet = arg;
				} else {
					propFile = arg;
				}
			}
			
			
			initialiseRealScale();
			
			try {
				InputStream cf;
				if (propFile != null) {
					try {
						System.out.println("Loading properties: " + propFile);
						if (propFile.endsWith(".properties")) {
							cf = new FileInputStream(propFile);
						} else {
							cf = new FileInputStream(propFile+".properties");
						}
					} catch (FileNotFoundException e) {
						System.out.println(propFile + ".properties not found, try bundled version");
						cf=getClass().getResourceAsStream("/"+propFile+".properties");
						if (cf == null){
							throw new IOException(propFile + " is not a valid region");
						}
					}
				} else {
					//No .properties file was specified, so use the default one
					System.out.println("Loading built in default properties (version.properties)");
					cf = getClass().getResourceAsStream("/version.properties");
				}
				loadPropFile(cf);
			} catch (IOException e) {
				System.out.println("Could not load the configuration properly for conversion");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		/**
		 * 
		 */
		public Configuration() {
			//Set singleton
			conf = this;
			initialiseRealScale();
			resetConfig();
			planet="TEST";
			
		}
		
		private void initialiseRealScale() {
			// precalculate real scale levels for pseudo zoom levels
			// pseudo zoom level 0 equals to scale 0
			realScale[0]=0;
			// startup pseudo zoom level 23
			realScale[23]=15000;
			// pseudo zoom level 0..21
			for(int i=22;i>0;i--) {
				realScale[i]=realScale[i+1]*1.5f;
			}
			// pseudo zoom level 23..32
			for(int i=24;i<realScale.length;i++) {
				realScale[i]=realScale[i-1]/1.5f;
			}
			// subtract 100 to avoid wrong bounds due to rounding errors
			for(int i=1;i<realScale.length;i++) {
				realScale[i]-=100;
				//System.out.println("Pseudo Zoom Level: " + i + " Real Scale: " + realScale[i]);
			}
		}
		
		public void resetConfig() {
			try {
				System.out.println("Loading built in default properties (version.properties)");
				loadPropFile(getClass().getResourceAsStream("/version.properties"));
				bounds = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void loadPropFile(InputStream propIS) throws IOException {
			if (propIS == null)
				throw new IOException("Invalid properties file");
			rb= new PropertyResourceBundle(propIS);
			vb=new PropertyResourceBundle(getClass().getResourceAsStream("/version.properties"));
			useRouting=getString("useRouting");
			if (useRouting == null || attrToBoolean(useRouting) > 0) {
				useRouting = "motorcar";
			}
			TravelModes.stringToTravelModes(useRouting);
			
			maxRouteTileSize=Integer.parseInt(getString("routing.maxTileSize"));
			maxTileSize=Integer.parseInt(getString("maxTileSize"));
			setStyleFileName(getString("style-file"));
			appParam = getString("app");
			enableEditingSupport = getString("EnableEditing").equalsIgnoreCase("true");
			
		}

		
		public void setPlanetName(String p) {
			planet = p;
		}
		public void setPropFileName(String p) {
			propFile = p;
		}
		
		public String getStyleFileName() {
			return styleFile;
		}
		
		public void setStyleFileName(String name) {
			styleFile = name;
			InputStream is;
			try {
				is = new FileInputStream(styleFile);
			} catch (IOException e) {
				styleFile = "/style-file.xml";
				System.out.println("Warning: Style file (" + styleFile + ") not found. Using internal one!"); 
				is = getClass().getResourceAsStream(styleFile);
			}
			legend = new LegendParser(is);
		}
		
		public boolean use(String key){
			if ("true".equalsIgnoreCase(getString(key))){
				return true;
			} else return false;
		}
		public  String getString(String key) {
			try {
				return rb.getString(key).trim();
			} catch (MissingResourceException e) {
				return vb.getString(key).trim();
			}
		}
		public float getFloat(String key){
			return Float.parseFloat(getString(key));
		}
		public String getName(){
			if (bundleName != null) {
				return bundleName;
			}
			return getString("bundle.name");
		}
		
		public void setName(String name){
			bundleName = name;
		}
		
		public void setMidletName(String name){
			midletName = name;
		}
				
		public String getMidletName(){
			if (midletName != null) {
				return midletName;
			}
			return getString("midlet.name");
		}
		
		public void setCodeBase (String app) {
			appParam = app;
		}
		
		public InputStream getJarFile(){
			String baseName = appParam;
			if ("false".equals(baseName)){
				return null;
			}			
			return getClass().getResourceAsStream("/"+baseName
			+"-"+getVersion()
			+".jar");
		}		
		public String getAppParam(){
			return appParam;
		}
		public String getJarFileName(){
			return appParam
			+"-"+getVersion()
			+".jar";
		}
		public String getTempDir(){
			return getTempBaseDir()+"/"+"map";
		}
		public String getTempBaseDir(){
			if (tmp==null){
				tmp="temp"+ Math.abs(new Random(System.currentTimeMillis()).nextLong());
			}
			return tmp;
//			return getString("tmp.dir");
		}
		
		public boolean cleanupTmpDirAfterUse() {
			if ("true".equalsIgnoreCase(getString("keepTemporaryFiles"))){
				return false;
			} else return true;
		}
		
		public File getPlanet(){
			return new File(planet);
		}
		public InputStream getPlanetSteam() throws IOException {
			InputStream fr = null;
			if (planet.equalsIgnoreCase("osmxapi") || planet.equalsIgnoreCase("ROMA")) {
				Bounds[] bounds = getBounds();
				if (bounds.length > 1) {
					System.out.println("Can't deal with multiple bounds when requesting from a Server yet");
					throw new IOException("Can't handle specified bounds with online data");
				}
				Bounds bound = bounds[0];
				URL url = null;
				if (planet.equalsIgnoreCase("osmxapi")) {
					url = new URL("http://osmxapi.informationfreeway.org/api/0.6/*[bbox=" + 
							bound.minLon + "," + bound.minLat + "," + bound.maxLon + "," + bound.maxLat + "]");
				} else if (planet.equalsIgnoreCase("ROMA")){
					url = new URL("http://api1.osm.mat.cc:8080/api/0.6/map?bbox=" + 
							bound.minLon + "," + bound.minLat + "," + bound.maxLon + "," + bound.maxLat);
				}
				 
				System.out.println("Connecting to server: " + url);
				System.out.println("This may take a while!");
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestProperty("User-Agent", "Osm2GpsMid");
				conn.setRequestProperty("Accept-Encoding", "gzip; deflate");
				conn.connect();
				String encoding = conn.getContentEncoding();
				System.out.println("Encoding: " + encoding);
				InputStream apiStream;
				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
					apiStream = new GZIPInputStream(conn.getInputStream());
				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
					apiStream = new InflaterInputStream(conn.getInputStream(), new Inflater(true));
				} else {
					apiStream = conn.getInputStream();
				}

				fr = new TeeInputStream(apiStream,new FileOutputStream(new File(getTempDir() + "Online.osm")));
			} else {
				System.out.println("Opening planet file: " + planet);
				
				fr= new FileInputStream(planet);
				if (planet.endsWith(".bz2") || planet.endsWith(".gz")){
					if (planet.endsWith(".bz2")) {
						fr.read();
						fr.read();
						fr = new CBZip2InputStream(fr);
					} else if (planet.endsWith(".gz")) {
						fr = new GZIPInputStream(fr);
					}
					/*int availableProcessors = Runtime.getRuntime().availableProcessors();
					if (availableProcessors > 1){
						System.out.println("found " + availableProcessors + " CPU's: uncompress in seperate thread");
						fr = new ThreadBufferedInputStream(fr);				
					} else {						
						System.out.println("only one CPU: uncompress in same thread");						
					}*/
				}
				/**
				 * The BufferedInputStream isn't doing a particularly good job at buffering,
				 * so we need to use our own implementation of a BufferedInputStream.
				 * As it uses a separate thread for reading, it should paralyse any CPU intensive
				 * read operations such as decomressing bz2. However even when only reading from
				 * a named pipe and let an external program (bzCat) do the decompression, somehow
				 * still the standard BufferedInputStream does poorly and blocks a lot leaving
				 * CPUs idle.   
				 */
				fr = new ThreadBufferedInputStream(fr);
			}
			return fr;
		}
		
		public InputStream getCellStream() throws IOException {
			InputStream fr = null;
			
			System.out.println("Opening cellID file: " + cell);
			if (cell == null) {
				throw new IOException("No file for CellIDs was specified");
			}
			
			fr= new FileInputStream(cell);
			if (cell.endsWith(".bz2") || cell.endsWith(".gz")){
				if (cell.endsWith(".bz2")) {
					fr.read();
					fr.read();
					fr = new CBZip2InputStream(fr);
				} else if (cell.endsWith(".gz")) {
					fr = new GZIPInputStream(new BufferedInputStream(fr));
				}
			}
			return fr;
		}

		public InputStream getCharMapStream() throws IOException{
			InputStream cmis = null;
			try {
				cmis = new FileInputStream("charMap.txt");
			} catch (FileNotFoundException e) {
				try {
					cmis = new FileInputStream(getTempDir() + "/charMap.txt");
					if (cmis == null){
						throw new IOException("Could not find a valid charMap.txt");
					}
				} catch (FileNotFoundException fnfe) {
					throw new IOException("Could not find a valid charMap.txt");
				}
			}
			return cmis;
		}

		public Bounds[] getBounds(){
			if (bounds != null)
				return bounds;
			int i;
			i=0;
			try {
				while (i<10000){
					getFloat("region."+(i+1)+".lat.min");
					i++;
				}
			} catch (RuntimeException e) {
				;
			}
			
			if (i>0) {
				System.out.println("found " + i + " bounds");
				Bounds[] ret=new Bounds[i];
				for (int l=0;l < i;l++){
					ret[l]=new Bounds();
					ret[l].extend(getFloat("region."+(l+1)+".lat.min"),
							getFloat("region."+(l+1)+".lon.min"));
					ret[l].extend(getFloat("region."+(l+1)+".lat.max"),
							getFloat("region."+(l+1)+".lon.max"));
				}
				return ret;				
			} else {
				System.out.println("Warning: No bounds were given - using [-180,-90,180,90]");
				System.out.println("This will try to create a GpsMid for the whole region");
				System.out.println("contained in " + planet);
				Bounds[] ret=new Bounds[1];
				ret[0]=new Bounds();
				ret[0].extend(-90.0, -180.0);
				ret[0].extend(90.0, 180.0);
				return ret;	
			}
		}
		
		public void addBounds(Bounds bound) {
			Bounds [] tmp;
			if (bounds == null) {
				tmp = new Bounds[1];
				tmp[0] = bound;
			} else {
				tmp = new Bounds[bounds.length + 1];
				System.arraycopy(bounds, 0, tmp, 0, bounds.length);
				tmp[bounds.length] = bound;
			}
			bounds = tmp;
		}
		
		public void setRouting(String routing) {
			useRouting = routing;
		}

		/**
		 * @return
		 */
		public String getVersion() {
			return vb.getString("version");
		}

		public String getBundleDate() {
	        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	        return format.format(new Date());	
		}

		public int getMaxTileSize() {
			return maxTileSize;
		}

		public int getMaxRouteTileSize() {
			return maxRouteTileSize;
		}

		public Hashtable<String, Hashtable<String,Set<EntityDescription>>> getPOIlegend() {
			return legend.getPOIlegend();
		}
		
		public Hashtable<String, Hashtable<String,Set<EntityDescription>>> getWayLegend() {
			return legend.getWayLegend();
		}
		
		public static Configuration getConfiguration() {
			return conf;
		}
		
		public POIdescription getpoiDesc(byte t) {
			return legend.getPOIDesc(t);
		}
		
		public WayDescription getWayDesc(byte t) {
			return legend.getWayDesc(t);
		}
		
		public Collection<EntityDescription> getPOIDescs() {
			return legend.getPOIDescs();
		}
		public Collection<EntityDescription> getWayDescs() {
			return legend.getWayDescs();
		}
		public Vector<SoundDescription> getSoundDescs() {
			return legend.getSoundDescs();
		}

		public SoundDescription getSoundDescription(String Name) {			
			for (SoundDescription sound : getSoundDescs()) {
				if (sound.name.equals(Name)) {
					return sound;
				}			
			}
			return null;
		}
		
		
		
		
		/*
		 * returns the real scale level
		 * 
		 * for scale 0..32 a pseudo zoom level is assumed
		 * and it is converted to a real scale level
		 */  

		public int getRealScale(int scale) {
			if (scale<realScale.length) {
				return (int) realScale[scale];
			}
			else {
				return scale;
			}
		}
		
		/**
		 * attrToBoolean canonicalises textual boolean values e.g. yes, true and 1
		 * 
		 * @param a string to be canonicalised
		 * @return It returns 1 if it is a valid true, -1 if it is a valid false and 0 otherwise
		 */
		public static int attrToBoolean(String attr) {
			if (attr == null)
				return 0;
			if (attr.equalsIgnoreCase("yes"))
				return 1;
			if (attr.equalsIgnoreCase("true"))
				return 1;
			if (attr.equalsIgnoreCase("1"))
				return 1;
			if (attr.equalsIgnoreCase("no"))
				return -1;
			if (attr.equalsIgnoreCase("false"))
				return -1;
			if (attr.equalsIgnoreCase("0"))
				return -1;
			return 0;
		}
		
		public String toString() {
			String confString = "Osm2GpsMid configuration:\n";
			confString += "\t Bundle name: " + getName() + "\n";
			confString += "\t Midlet name: " + getMidletName() + "\n";
			confString += "\t Code base: " + appParam + "\n";
			confString += "\t Keeping map files after .jar creation: " + !cleanupTmpDirAfterUse() + "\n";
			confString += "\t Enable routing: " + useRouting + "\n";
			confString += "\t used Style-file: " + getStyleFileName() + "\n";
			confString += "\t Planet source: " + planet + "\n";
			confString += "\t include CellID data: " + getString("useCellID") + "\n";
			confString += "\t CellID source: " + cell + "\n";
			confString += "\t Enable editing support: " + enableEditingSupport + "\n";
			Bounds[] bounds = getBounds();
			if (bounds != null) {
				confString += "\t Using " + bounds.length + " bounding boxes\n";
				for (Bounds b : bounds) {
					confString += "\t\t" + b + "\n";
				}
			} else {
				confString += "\t Using the complete osm file\n";
			}
			
			return confString;
		}
}

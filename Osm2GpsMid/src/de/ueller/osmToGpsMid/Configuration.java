/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.net.URL;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.SoundDescription;
import de.ueller.osmToGpsMid.model.POIdescription;
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
	public final static short MAP_FORMAT_VERSION = 16;
	
		private ResourceBundle rb;
		private ResourceBundle vb;
		private String tmp=null;
		private String planet;
		public boolean useRouting=false;
		public int maxTileSize=20000;
		public int maxRouteTileSize=3000;
		public String styleFile;
		private Bounds[] bounds;
		
		public int background_color;
				
		private LegendParser legend;
		
		// array containing real scale for pseudo zoom 0..32
		private static float realScale [] = new float[33]; 
		
		private static Configuration conf;

//		private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//				.getBundle(BUNDLE_NAME);

		public Configuration(String [] args) {
			String propFile = null;
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
					
				} else if (planet == null) {
					planet = arg;
				} else {
					propFile = arg;
				}				
			}
			
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
				
			conf = this;
			try {
				InputStream cf;
				if (propFile != null) {
					try {
						cf = new FileInputStream(propFile+".properties");
					} catch (FileNotFoundException e) {
						System.out.println(propFile + ".properties not found, try bundled version");
						cf=getClass().getResourceAsStream("/"+propFile+".properties");
						if (cf == null){
							throw new IOException(propFile + " is not a valid region");
						}
					}
					rb= new PropertyResourceBundle(cf);
				} else if (bounds != null) {
					//No .properties file was specified, so use the default one
					rb=new PropertyResourceBundle(getClass().getResourceAsStream("/version.properties"));
				} else {
					System.out.println("ERROR: No bounds specified on the command line and no property file");
				}
				vb=new PropertyResourceBundle(getClass().getResourceAsStream("/version.properties"));

				useRouting=use("useRouting");
				maxRouteTileSize=Integer.parseInt(getString("routing.maxTileSize"));
				maxTileSize=Integer.parseInt(getString("maxTileSize"));
				styleFile = getString("style-file");
				InputStream is;
				try {
					is = new FileInputStream(styleFile);
				} catch (IOException e) {
					System.out.println("Warning: Style file (" + styleFile + ") not found. Using internal one!"); 
					is = getClass().getResourceAsStream("/style-file.xml");
				}
				legend = new LegendParser(is);
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
			planet="TEST";
		}

		
		
		public boolean use(String key){
			if ("true".equalsIgnoreCase(getString(key))){
				return true;
			} else return false;
		}
		public  String getString(String key) {
			try {
				return rb.getString(key);
			} catch (MissingResourceException e) {
				return vb.getString(key);
			}
		}
		public float getFloat(String key){
			return Float.parseFloat(getString(key));
		}
		public String getName(){
			return getString("bundle.name");
		}
		public String getMidletName(){
		    String mn=getString("midlet.name");
		    if (mn != null)
		    	return mn;
		    else 
		    	return "GpsMid";
		}
		
		public InputStream getJarFile(){
			String baseName = getString("app");			
			if ("false".equals(baseName)){
				return null;
			}			
			return getClass().getResourceAsStream("/"+baseName
			+"-"+getVersion()
			+".jar");
		}
		public String getJarFileName(){
			return getString("app")
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
			if (planet.equalsIgnoreCase("osmxapi")) {
				Bounds[] bounds = getBounds();
				if (bounds.length > 1) {
					System.out.println("Can't deal with multiple bounds when requesting from osmxapi yet");
					throw new IOException("Can't handle specified bounds with osmxapi");
				}
				Bounds bound = bounds[0];
				URL url = new URL("http://osmxapi.informationfreeway.org/api/0.5/*[bbox=" + 
						bound.minLon + "," + bound.minLat + "," + bound.maxLon + "," + bound.maxLat + "]");
				System.out.println("Connecting to Osmxapi: " + url);
				System.out.println("This may take a while!");
				fr = new TeeInputStream(url.openStream(),new FileOutputStream(new File(getTempDir() + "osmXapi.osm")));				
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
				System.out.println("found " + i + " bounds");
			}
			Bounds[] ret=new Bounds[i];
			for (int l=0;l < i;l++){
				ret[l]=new Bounds();
				ret[l].extend(getFloat("region."+(l+1)+".lat.min"),
						getFloat("region."+(l+1)+".lon.min"));
				ret[l].extend(getFloat("region."+(l+1)+".lat.max"),
						getFloat("region."+(l+1)+".lon.max"));
			}
			return ret;
		}

		/**
		 * @return
		 */
		public String getVersion() {
			return vb.getString("version");
		}

		public int getMaxTileSize() {
			return maxTileSize;
		}

		public int getMaxRouteTileSize() {
			return maxRouteTileSize;
		}

		public Hashtable<String, Hashtable<String,Set<POIdescription>>> getPOIlegend() {
			return legend.getPOIlegend();
		}
		
		public Hashtable<String, Hashtable<String,Set<WayDescription>>> getWayLegend() {
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
		
		public Collection<POIdescription> getPOIDescs() {
			return legend.getPOIDescs();
		}
		public Collection<WayDescription> getWayDescs() {
			return legend.getWayDescs();
		}
		public Vector<SoundDescription> getSoundDescs() {
			return legend.getSoundDescs();
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
}

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
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import java.net.URL;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.ueller.osmToGpsMid.model.Bounds;

/**
 * @author hmueller
 *
 */
public class Configuration {
		private ResourceBundle rb;
		private ResourceBundle vb;
		private String tmp=null;
		private final String planet;
		private boolean highway_only=false;
		public boolean useHighway=true;
		public boolean useRailway=true;
		public boolean useRiver=true;
		public boolean useCycleway=true;
		public boolean useAmenity=true;
		public boolean useLanduse=true;
		public boolean useNatural=true;
		public boolean useLeisure=true;
		public boolean useWaterway=true;
		public boolean useRouting=false;
		public int maxTileSize=20000;
		public int maxRouteTileSize=3000;

//		private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//				.getBundle(BUNDLE_NAME);

		public Configuration(String planet,String file) {
			this.planet = planet;
			try {
				InputStream cf;
				try {
					cf = new FileInputStream(file+".properties");
				} catch (FileNotFoundException e) {
					System.out.println(file + ".properties not found, try bundled version");
					cf=getClass().getResourceAsStream("/"+file+".properties");
					if (cf == null){
						throw new IOException(file + " is not a valid region");
					}
				}
				rb= new PropertyResourceBundle(cf);
//				try {
				vb=new PropertyResourceBundle(getClass().getResourceAsStream("/version.properties"));
//				} catch(Exception e) {
//					vb=new PropertyResourceBundle(getClass().getResourceAsStream("/resources/version.properties"));
//				}
				useHighway=use("useHighway");
				useRailway=use("useRailway");
				useRiver=use("useRiver");
				useCycleway=use("useCycleway");
				useAmenity=use("useAmenity");
				useLanduse=use("useLanduse");
				useNatural=use("useNatural");
				useLeisure=use("useLeisure");
				useWaterway=use("useWaterway");
				useRouting=use("useRouting");
				maxRouteTileSize=Integer.parseInt(getString("routing.maxTileSize"));
				maxTileSize=Integer.parseInt(getString("maxTileSize"));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
//			try {
				return Float.parseFloat(getString(key));
//			} catch (Exception e) {
//				System.err.println(getString(key) + " not a number");
//				return Float.NaN;
//			}
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
				fr = url.openStream();
			} else {
				System.out.println("Opening planet file: " + planet);
				fr= new BufferedInputStream(new FileInputStream(planet), 4096);
				if (planet.endsWith(".bz2") || planet.endsWith(".gz")){
					int availableProcessors = Runtime.getRuntime().availableProcessors();
					if (availableProcessors > 1){
						System.out.println("found " + availableProcessors + " CPU's: uncompress in seperate thread");
						fr = new Bzip2Reader(fr);						
					} else {						
						System.out.println("only one CPU: uncompress in same thread");
						if (planet.endsWith(".bz2")) {
							fr.read();
							fr.read();
							fr = new CBZip2InputStream(fr);
						} else if (planet.endsWith(".gz")) {
							fr = new GZIPInputStream(fr);							
						}
					}
				}
			}
			return fr;
		}
		public Bounds[] getBounds(){
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

		public boolean isHighway_only() {
			return highway_only;
		}

		public int getMaxTileSize() {
			return maxTileSize;
		}

		public int getMaxRouteTileSize() {
			return maxRouteTileSize;
		}
}

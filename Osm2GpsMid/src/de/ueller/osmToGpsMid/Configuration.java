/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import de.ueller.osmToGpsMid.model.Bounds;

/**
 * @author hmueller
 *
 */
public class Configuration {
		private final String file;
		private ResourceBundle rb;

//		private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//				.getBundle(BUNDLE_NAME);

		public Configuration(String file) {
			this.file = file;
			try {
				FileInputStream cf=new FileInputStream(file+".properties");
				rb= new PropertyResourceBundle(cf);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public  String getString(String key) {
			try {
				
				return rb.getString(key);
			} catch (MissingResourceException e) {
				return '!' + key + '!';
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
		public File getJarFile(){
			return new File(getString("application")
			+"-"+getString("application.version")
			+".jar");
		}
		public String getTempDir(){
			return getString("tmp.dir");
		}
		public File getPlanet(){
			return new File(getString("planet.osm"));
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
			return getString("application.version");
		}
}

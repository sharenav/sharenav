/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

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

}

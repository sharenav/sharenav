/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.OxParser;
import de.ueller.osmToGpsMid.model.RouteAccessRestriction;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;

/**
 * @author hmueller
 *
 */
public class Main {
	
    EntityManagerFactory factory;
    EntityManager manager;
	static Calendar startTime;
	
	static Configuration config;

   
    public void init() {
        factory = Persistence.createEntityManagerFactory("osm");
        manager = factory.createEntityManager();
//        manager.getTransaction().begin();
//        manager.createNativeQuery("create index indexLat on node (lat)").executeUpdate();
//        manager.createNativeQuery("create index indexLon on node (lon)").executeUpdate();
//        manager.createNativeQuery("create index indexOsmId on entity (osmid)").executeUpdate();
//        manager.getTransaction().commit();
        manager.close();
        
    }

    private void shutdown() {
        
        factory.close();
    }
    
	public void run() {
		InputStream fr;
		
		try {
			
			validateConfig(config);
			System.out.println(config.toString());

			// the legend must be parsed after the configuration to apply parameters to the travel modes specified in useRouting
			TravelModes.stringToTravelModes(config.useRouting);
			config.parseLegend();
			
			startTime = Calendar.getInstance();

			TravelMode tm = null;
			if (Configuration.attrToBoolean(config.useRouting) >= 0) {
				for (int i = 0; i < TravelModes.travelModeCount; i++) {				
					tm = TravelModes.travelModes[i];
					System.out.println("Route rules in " + config.getStyleFileName() + " for " + tm.getName() + ":");
					if ( (tm.travelModeFlags & TravelMode.AGAINST_ALL_ONEWAYS) > 0) {
						System.out.println(" Going against all accessible oneways is allowed");					
					}
					if ( (tm.travelModeFlags & TravelMode.BICYLE_OPPOSITE_EXCEPTIONS) > 0) {
						System.out.println(" Opposite direction exceptions for bicycles get applied");					
					}
		        	int routeAccessRestrictionCount = 0;
		            if (TravelModes.getTravelMode(i).getRouteAccessRestrictions().size() > 0) {
		            	for (RouteAccessRestriction r: tm.getRouteAccessRestrictions()) {
		            		routeAccessRestrictionCount++;
		            		System.out.println(" " + r.toString());
		            	}
		            }
		            if (routeAccessRestrictionCount == 0) {
		        		System.out.println("Warning: No access restrictions in " + config.getStyleFileName() + " for " + tm.getName());            	
		            }
				}
				System.out.println("");
			}
			String tmpDir = config.getTempDir();
			System.out.println("Unpacking application to " + tmpDir);
//			expand(config, tmpDir);
			File target = new File(tmpDir);
			createPath(target);
			
			fr = config.getPlanetSteam();
			ToDBParser parser = new ToDBParser(factory,fr,config);
//			OxParser parser = new OxParser(fr,config);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
		/**
		 * ensures that the path denoted whit <code>f</code> will exist
		 * on the file-system. 
		 * @param f
		 */
		private static void createPath(File f) {
			if (! f.canWrite() && f.getParent() != null)
				createPath(f.getParentFile());
			f.mkdir();
		}
		
		/**
		 * remove a directory and all its subdirectories and files
		 * @param path
		 * @return
		 */
		static private boolean deleteDirectory(File path) {
			if (path.exists()) {
				File[] files = path.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectory(files[i]);
					}
					else {
						files[i].delete();
					}
				}
			}
			return( path.delete() );
		}
		
		static private void validateConfig(Configuration config) {
			if ((config.enableEditingSupport) && !(config.getAppParam().equalsIgnoreCase("GpsMid-Generic-editing"))) {
				System.out.println("ERROR: You are creating a map with editing support, but use a app version that does not support editing\n"
						+ "     please fix your .properties file");
				System.exit(1);
			}
		}

   
    public static void main(String[] args) {
        // TODO code application logic here
        Main main = new Main();
        main.init();
        try {
			config = new Configuration(args);
			main.run();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            main.shutdown();
        }
    }
}


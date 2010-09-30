/**
 * This file is part of OSM2GpsMid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */

package de.ueller.osmToGpsMid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.ueller.osmToGpsMid.model.Damage;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.RouteAccessRestriction;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;


/**
 * This is the main class of Osm2GpsMid.
 * It triggers all the steps necessary to create a GpsMid JAR file
 * ready for downloading to the mobile phone.
 */
public class BundleGpsMid implements Runnable {
	static boolean compressed = true;
	static Calendar startTime;
	
	static Configuration config;
	
	private static volatile boolean createSuccessfully;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		String dataModel = System.getProperty("sun.arch.data.model");
		System.out.println("Available memory: " + maxMem + "MB (" + dataModel + " bit system)");
		String warning = null;
		if (
				(maxMem < 800 && dataModel.equals("32"))
				||
				(maxMem < 1500 && dataModel.equals("64"))			
		
		) {
			warning = 	"Heap space might be not set or set too low! (available memory of " + dataModel + " bit system is " + maxMem + "MB)\r\n" +
						"   Use command line options to avoid out-of-memory errors during map making.\r\n" +
						"   On 32 bit systems start Osm2GpsMid e.g. with:\r\n" +
						"     java -Xmx1024M -jar Osm2GpsMid-xxxx.jar\r\n" +
						"     to increase the heap space to 1024 MB\r\n" +			
						"   On 64 bit systems use e.g.:\r\n" +
						"     java -Xmx4096M -XX:+UseCompressedOops -jar Osm2GpsMid-xxxx.jar\r\n" +
						"     for 4096 MB heap space and an option to reduce memory requirements\r\n";
		}
		
		BundleGpsMid bgm = new BundleGpsMid();
		GuiConfigWizard gcw = null;
		
		Configuration c;
		if (args.length == 0) {
			if (warning != null) {
				JFrame frame = new JFrame("Alert");
				JOptionPane.showMessageDialog(frame,
					    warning,
					    "Osm2GpsMid",
					    JOptionPane.WARNING_MESSAGE);

			}
			gcw = new GuiConfigWizard();
			c = gcw.startWizard();
		} else {
			if (warning != null) {
				System.out.println("WARNING:");
				System.out.println(warning);
			}
			c = new Configuration(args);
		}
		/**
		 * Decouple the computational thread from
		 * the GUI thread to make the GUI more smooth
		 * Not sure if this is actually necessary, but
		 * it shouldn't harm either.
		 */
		config = c;
		Thread t = new Thread(bgm);
		createSuccessfully = false;
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// Nothing to do
		}
		if (gcw != null) {
			if (createSuccessfully) {
				JOptionPane.showMessageDialog(gcw, "A GpsMid midlet was successfully created and can now be copied to your phone.");
			} else {
				JOptionPane.showMessageDialog(gcw, "A fatal error occured during processing. Please have a look at the output logs.");
			}
			gcw.reenableClose();
		}
	}

	private static void expand(Configuration c, String tmpDir) throws ZipException, IOException {
		System.out.println("Preparing " + c.getJarFileName());
		InputStream appStream = c.getJarFile();
		if (appStream == null) {
			System.out.println("ERROR: Couldn't find the jar file for " + c.getJarFileName());
			System.out.println("Check the app parameter in the properties file for misspellings");
			System.exit(1);
		}
		File file = new File(c.getTempBaseDir() + "/" + c.getJarFileName());
		writeFile(appStream, file.getAbsolutePath());
		
		ZipFile zf = new ZipFile(file.getCanonicalFile());
		for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
			ZipEntry ze = e.nextElement();
			if (ze.isDirectory()) {
//				System.out.println("dir  " + ze.getName());
			} else {
//				System.out.println("file " + ze.getName());
				InputStream stream = zf.getInputStream(ze);
				writeFile(stream, tmpDir + "/" + ze.getName());
			}
		}
	}
	
	/**
	 * Rename the Copying files to .txt suffix for easy access on all OS's
	 * 
	 */
	private static void renameCopying(Configuration c) {
		String tmpDir = c.getTempDir();
		File copying = new File(tmpDir + "/COPYING");
		File copying2 = new File(tmpDir + "/COPYING.txt");
		File copyingosm = new File(tmpDir + "/COPYING-OSM");
		File copyingosm2 = new File(tmpDir + "/COPYING-OSM.txt");
		copying.renameTo(copying2);
		copyingosm.renameTo(copyingosm2);
	}
	/**
	 * Rewrite or remove the Manifest file to change the bundle name to reflect the one
	 * specified in the properties file.
	 * 
	 * @param c
	 */
	private static void rewriteManifestFile(Configuration c, boolean rename) {
		String tmpDir = c.getTempDir();
		try {
			File manifest = new File(tmpDir + "/META-INF/MANIFEST.MF");
			File manifest2 = new File(tmpDir + "/META-INF/MANIFEST.tmp");
			FileWriter fw = null;

			if (rename) {
				BufferedReader fr = new BufferedReader(new FileReader(manifest));
				fw = new FileWriter(manifest2);
				String line;
				Pattern p1 = Pattern.compile("MIDlet-(\\d):\\s(.*),(.*),(.*)");
				while (true) {
					line = fr.readLine();
					if (line == null) {
						break;
					}

					Matcher m1 = p1.matcher(line);
					if (m1.matches()) {
						fw.write("MIDlet-" + m1.group(1) + ": " + c.getMidletName()
							 + "," + m1.group(3) + "," + m1.group(4) + "\n");
					} else if (line.startsWith("MIDlet-Name: ")) {
						fw.write("MIDlet-Name: " + c.getMidletName() + "\n");
					} else {
						fw.write(line + "\n");
					}
				}
				fr.close();
			}
			manifest.delete();
			if (rename) {
				fw.close();
				manifest2.renameTo(manifest);
			}

		} catch (IOException ioe) {
			System.out.println("Something went wrong rewriting the manifest file");
			return;
		}

	}

	private static void writeJADfile(Configuration c,  long jarLength) throws IOException {
		String tmpDir = c.getTempDir();
		File manifest = new File(tmpDir + "/META-INF/MANIFEST.MF");
		BufferedReader fr = new BufferedReader(new FileReader(manifest));
		File jad = new File(c.getMidletFileName() + ".jad");
		FileWriter fw = new FileWriter(jad);

		/**
		 * Copy over the information from the manifest file, to the jad file.
		 * This way we use the information generated by the build process
		 * of GpsMid, to duplicate as little data as possible.
		 */
		try {
			String line;
			while (true) {
				line = fr.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith("MIDlet") || line.startsWith("MicroEdition")) {
					fw.write(line + "\n");
				}
			}
		} catch (IOException ioe) {
			//This will probably be the end of the file
		}
		/**
		 * Add some additional fields to the jad file, that aren't present in the manifest file.
		 */
		fw.write("MIDlet-Jar-Size: " + jarLength + "\n");
		fw.write("MIDlet-Jar-URL: " + c.getMidletFileName() + ".jar\n");
		fw.close();
		fr.close();
	}

	private static void pack(Configuration c) throws ZipException, IOException {
		File n = null;
		if (config.getMapName().equals("")) {
			n = new File(c.getMidletFileName() + ".jar");
			rewriteManifestFile(c, true);
		} else {
			n = new File(c.getMapFileName());
			rewriteManifestFile(c, false);
			renameCopying(c);
		}
		FileOutputStream fo = new FileOutputStream(n);
		ZipOutputStream zf = new ZipOutputStream(fo);
		zf.setLevel(9);
		if (compressed == false) {
			zf.setMethod(ZipOutputStream.STORED);
		}
		File src = new File(c.getTempDir());
		if (src.isDirectory() == false) {
			throw new Error("TempDir is not a directory");
		}
		packDir(zf, src, "");
		zf.close();
		if (config.getMapName().equals("")) {
			writeJADfile(c, n.length());
		}
		Calendar endTime = Calendar.getInstance();
		Calendar duration = Calendar.getInstance();
		duration.setTimeInMillis(endTime.getTimeInMillis() - startTime.getTimeInMillis());
		System.out.println(n.getName() + " created successfully with " + (n.length() / 1024 / 1024) + " MiB in " +
				(duration.get(Calendar.HOUR) - 1) + ":" + duration.get(Calendar.MINUTE) + ":" + duration.get(Calendar.SECOND));
	}
	
	private static void packDir(ZipOutputStream os, File d, String path) throws IOException {
		File[] files = d.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				if (path.length() > 0) {
					packDir(os, files[i], path + "/" + files[i].getName());
				} else {
					packDir(os, files[i], files[i].getName());
				}
			} else {
//				System.out.println();
				ZipEntry ze = null;
				if (path.length() > 0) {
				   ze = new ZipEntry(path + "/" + files[i].getName());
				} else {
				   ze = new ZipEntry(files[i].getName());
				}
				int ch;
				int count = 0;
				//byte buffer to read in larger chunks
				byte[] bb = new byte[4096];
				FileInputStream stream = new FileInputStream(files[i]);
				if (!compressed) {
					CRC32 crc = new CRC32();
					count = 0;
					while ((ch = stream.read(bb)) != -1) {
						crc.update(bb, 0, ch);
					}
					ze.setCrc(crc.getValue());
					ze.setSize(files[i].length());
				}
//				ze.
				os.putNextEntry(ze);
				count = 0;
				stream.close();
				stream = new FileInputStream(files[i]);
				while ((ch = stream.read(bb)) != -1) {
					os.write(bb, 0, ch);
					count += ch;
				}
				stream.close();
//				System.out.println("Wrote " + path + "/" + files[i].getName() + " byte:" + count);

			}
		}
		
	}
	/**
	 * @param stream
	 * @param string
	 */
	private static void writeFile(InputStream stream, String name) {
		File f = new File(name);
		try {
			if (! f.canWrite()) {
				createPath(f.getParentFile());
			}
			FileOutputStream fo = new FileOutputStream(name);
			int ch;
			int count = 0;
			byte[] bb = new byte[4096];
			while ((ch = stream.read(bb)) != -1) {
				fo.write(bb, 0, ch);
				count += ch;
			}
			fo.close();
//			System.out.println("Wrote " + name + " byte:" + count);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Failed to write " + name + " err:" + e.getMessage());
		}
	}

	/**
	 * Ensures that the path denoted with <code>f</code> will exist
	 * on the file-system.
	 * @param f File whose directory must exist
	 */
	private static void createPath(File f) {
		if (! f.canWrite()) {
			createPath(f.getParentFile());
		}
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
				} else {
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

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
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
					System.out.println("Route rules in " + config.getStyleFileName()
							+ " for " + tm.getName() + ":");
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
		        		System.out.println("Warning: No access restrictions in "
		        				+ config.getStyleFileName() + " for " + tm.getName());
		            }
				}
				System.out.println("");
			}
			
            if (LegendParser.getDamages().size() == 0) {
        		System.out.println("No damage markers in "	+ config.getStyleFileName());
            } else {
            	System.out.println("Rules specified in " + config.getStyleFileName() + " for marking damages:");
				for (Damage damage: LegendParser.getDamages()) {
	        		System.out.println(" Ways/Areas with key " + damage.key + "=" + damage.values); 
				}
            }
			String tmpDir = config.getTempDir();
			System.out.println("Unpacking application to " + tmpDir);
			expand(config, tmpDir);
			File target = new File(tmpDir);
			createPath(target);
			
			fr = config.getPlanetSteam();
			OxParser parser = new OxParser(fr, config);
			new TriangulateRelations(parser, config);

			/**
			 * Display some stats about the type of relations we currently aren't handling
			 * to see which ones would be particularly useful to deal with eventually
			 */
			Hashtable<String, Integer> relTypes = new Hashtable<String, Integer>();
			for (Relation r : parser.getRelations()) {
				String type = r.getAttribute("type");
				if (type == null) {
					type = "unknown";
				}
				Integer count = relTypes.get(type);
				if (count != null) {
					count = new Integer(count.intValue() + 1);
				} else {
					count = new Integer(1);
				}
				relTypes.put(type, count);
			}
			System.out.println("Types of relations present but ignored: ");
			for (Entry<String, Integer> e : relTypes.entrySet()) {
				System.out.println("   " + e.getKey() + ": " + e.getValue());

			}
			relTypes = null;
			int numWays = parser.getWays().size();
			new SplitLongWays(parser);
			System.out.println("Splitting long ways increased ways from "
					+ numWays + " to " + parser.getWays().size());
			
			RouteData rd = null;
			if (Configuration.attrToBoolean(config.useRouting) >= 0 ) {
				rd = new RouteData(parser, target.getCanonicalPath());
				System.out.println("Remembering " + parser.trafficSignalCount + " traffic signal nodes");
				rd.rememberDelayingNodes();
			}
			
			System.out.println("Removing unused nodes");
			new CleanUpData(parser, config);

			if (Configuration.attrToBoolean(config.useRouting) >= 0 ) {
				System.out.println("Creating route data");
				System.out.println("===================");
				rd.create();
				System.out.println("Optimizing route data");
				rd.optimise();
			}
			CreateGpsMidData cd = new CreateGpsMidData(parser, target.getCanonicalPath());
			//				rd.write(target.getCanonicalPath());
			//				cd.setRouteData(rd);
			cd.setConfiguration(config);

			new CalcNearBy(parser);
			cd.exportMapToMid();
			//Drop parser to conserve Memory
			parser = null;
			cd = null;
			rd = null;
			
			if (!config.getCellOperator().equalsIgnoreCase("false")) {
				CellDB cellDB = new CellDB();
				cellDB.parseCellDB();
			}
			
			pack(config);

			//Cleanup after us again. The .jar and .jad file are in the main directory,
			//so these won't get deleted
			if (config.cleanupTmpDirAfterUse()) {
				File tmpBaseDir = new File(config.getTempBaseDir());
				System.out.println("Cleaning up temporary directory " + tmpBaseDir);
				deleteDirectory(tmpBaseDir);
			}
			
			createSuccessfully = true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


}

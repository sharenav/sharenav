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

import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.RouteAccessRestriction;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;


/**
 * This is the main class of Osm2GpsMid.
 * It triggers all the steps necessary to create a GpsMid JAR file
 * ready for downloading to the mobile phone.
 */
public class BundleGpsMid {
	static boolean compressed = true;
	static Calendar startTime;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		InputStream fr;
		try {
			Configuration c;
			if (args.length == 0) {
				GuiConfigWizard gcw = new GuiConfigWizard();
				c = gcw.startWizard();
			} else {
				c = new Configuration(args);
			}
			
			validateConfig(c);
			System.out.println(c.toString());

			// the legend must be parsed after the configuration to apply parameters to the travel modes specified in useRouting
			TravelModes.stringToTravelModes(c.useRouting);
			c.parseLegend();
			
			startTime = Calendar.getInstance();

			TravelMode tm = null;
			if (Configuration.attrToBoolean(c.useRouting) >= 0) {
				for (int i = 0; i < TravelModes.travelModeCount; i++) {				
					tm = TravelModes.travelModes[i];
					System.out.println("Route rules in " + c.getStyleFileName() + " for " + tm.getName() + ":");
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
		        		System.out.println("Warning: No access restrictions in " + c.getStyleFileName() + " for " + tm.getName());            	
		            }
				}
				System.out.println("");
			}
			String tmpDir = c.getTempDir();
			System.out.println("unpack Application to " + tmpDir);
			expand(c, tmpDir);
			File target = new File(tmpDir);
			createPath(target);
			
			fr = c.getPlanetSteam();
			OxParser parser = new OxParser(fr,c);
			System.out.println("read Nodes " + parser.getNodes().size());
			System.out.println("read Ways  " + parser.getWays().size());
			System.out.println("read Relations  " + parser.getRelations().size());

			/**
			 * Display some stats about the type of relations we currently aren't handling
			 * to see which ones would be particularly useful to deal with eventually 
			 */
			Hashtable<String,Integer> relTypes = new Hashtable<String,Integer>();
			for (Relation r : parser.getRelations()) {
				String type = r.getAttribute("type");
				if (type == null) type = "unknown";	
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

			System.out.println("split long ways " + parser.getWays().size());
			new SplitLongWays(parser);
			System.out.println("splited long ways to " + parser.getWays().size());
			
			System.out.println("reorder Ways");
			new CleanUpData(parser,c);

			if (Configuration.attrToBoolean(c.useRouting) >= 0 ){
				RouteData rd=new RouteData(parser,target.getCanonicalPath());
				System.out.println("create Route Data");
				rd.create();
				System.out.println("optimize Route Date");
				rd.optimise();
			}
			CreateGpsMidData cd = new CreateGpsMidData(parser,target.getCanonicalPath());
			//				rd.write(target.getCanonicalPath());
			//				cd.setRouteData(rd);
			cd.setConfiguration(c);

			new CalcNearBy(parser);
			cd.exportMapToMid();
			//Drop parser to conserve Memory
			parser = null;
			
			if (!c.getCellOperator().equalsIgnoreCase("false")) {
				CellDB cellDB = new CellDB();
				cellDB.parseCellDB();
			}
			
			pack(c);

			//Cleanup after us again. The .jar and .jad file are in the main directory,
			//so these won't get deleted
			if (c.cleanupTmpDirAfterUse()) {
				File tmpBaseDir = new File(c.getTempBaseDir());
				System.out.println("Cleaning up temporary directory " + tmpBaseDir);
				deleteDirectory(tmpBaseDir);
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void expand(Configuration c, String tmpDir) throws ZipException, IOException {
		System.out.println("prepare " + c.getJarFileName());
		InputStream appStream=c.getJarFile();
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
	 * Rewrite the Manifet file to change the bundle name to reflect the one
	 * specified in the properties file.
	 * 
	 * @param c
	 */
	private static void rewriteManifestFile(Configuration c) {
		String tmpDir = c.getTempDir();
		try {
			File manifest = new File(tmpDir + "/META-INF/MANIFEST.MF");
			File manifest2 = new File(tmpDir + "/META-INF/MANIFEST.tmp");

			BufferedReader fr = new BufferedReader(new FileReader(manifest));
			FileWriter fw = new FileWriter(manifest2);
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
			fw.close();
			fr.close();
			manifest.delete();
			manifest2.renameTo(manifest);

		} catch (IOException ioe) {
			System.out.println("Something went wrong rewriting the manifest file");
			return;
		}

	}

	private static void writeJADfile(Configuration c,  long jarLength) throws IOException {
		String tmpDir = c.getTempDir();
		File manifest = new File(tmpDir + "/META-INF/MANIFEST.MF");
		BufferedReader fr = new BufferedReader(new FileReader(manifest));
		File jad = new File(c.getMidletName() + "-" + c.getName() 
				+ "-" + c.getVersion() + ".jad");
		FileWriter fw = new FileWriter(jad);

		/**
		 * Copy over the information from the manifest file, to the jad file
		 * by this we use the information generated by the build process
		 * of GpsMid, to dupplicate as little data as possible
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
		 * Add some additional fields to the jad file, that aren't present in the manifest file
		 */
		fw.write("MIDlet-Jar-Size: " + jarLength + "\n");
		fw.write("MIDlet-Jar-URL: " + c.getMidletName() + "-" + c.getName() + "-" + c.getVersion() + ".jar\n");
		fw.close();
		fr.close();
	}

	private static void pack(Configuration c) throws ZipException, IOException {
		rewriteManifestFile(c);
		File n = new File(c.getMidletName() + "-" + c.getName() 
				+ "-" + c.getVersion() + ".jar");
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
		writeJADfile(c, n.length());
		Calendar endTime = Calendar.getInstance();
		Calendar duration = Calendar.getInstance();
		duration.setTimeInMillis(endTime.getTimeInMillis() - startTime.getTimeInMillis());
		System.out.println(n.getName() + " created successfully with " + (n.length() / 1024 / 1024) + " Mb in " +
				(duration.get(Calendar.HOUR) - 1) + ":" + duration.get(Calendar.MINUTE) + ":" + duration.get(Calendar.SECOND));
	}
	
	private static void packDir(ZipOutputStream os, File d,String path) throws IOException {
		File[] files = d.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				if (path.length() > 0) {
					packDir(os, files[i],path + "/" + files[i].getName());
				} else {
					packDir(os, files[i],files[i].getName());					
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
//				System.out.println("wrote " + path + "/" + files[i].getName() + " byte:" + count);

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
//			System.out.println("wrote " + name + " byte:" + count);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("fail to write " + name + " err:" + e.getMessage());
		}
	}

	/**
	 * ensures that the path denoted whit <code>f</code> will exist
	 * on the file-system. 
	 * @param f
	 */
	private static void createPath(File f) {
		if (! f.canWrite())
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


}

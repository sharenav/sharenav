/**
 * This file is part of OSM2ShareNav
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */

package net.sharenav.osmToShareNav;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;
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

import net.sharenav.osmToShareNav.area.Area;
import net.sharenav.osmToShareNav.area.SeaGenerator;
import net.sharenav.osmToShareNav.area.SeaGenerator2;
import net.sharenav.osmToShareNav.model.Damage;
import net.sharenav.osmToShareNav.model.Relation;
import net.sharenav.osmToShareNav.model.RouteAccessRestriction;
import net.sharenav.osmToShareNav.model.TollRule;
import net.sharenav.osmToShareNav.model.TravelMode;
import net.sharenav.osmToShareNav.model.TravelModes;


/**
 * This is the main class of Osm2ShareNav.
 * It triggers all the steps necessary to create a ShareNav JAR file
 * ready for downloading to the mobile phone.
 */
public class BundleShareNav implements Runnable {
	static boolean compressed = true;
	static Calendar startTime;
	
	static Configuration config;
	
	static String dontCompress[] = null;

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
						"   On 32 bit systems start Osm2ShareNav e.g. with:\r\n" +
						"     java -Xmx1024M -jar Osm2ShareNav-xxxx.jar\r\n" +
						"     to increase the heap space to 1024 MB\r\n" +			
						"   On 64 bit systems use e.g.:\r\n" +
						"     java -Xmx4096M -XX:+UseCompressedOops -jar Osm2ShareNav-xxxx.jar\r\n" +
						"     for 4096 MB heap space and an option to reduce memory requirements\r\n";
		}
		
		BundleShareNav bgm = new BundleShareNav();
		GuiConfigWizard gcw = null;
		
		Configuration c;
		if (args.length == 0 || (args.length == 1 && args[0].startsWith("--properties="))) {
			if (warning != null) {
				JFrame frame = new JFrame("Alert");
				JOptionPane.showMessageDialog(frame,
					    warning,
					    "Osm2ShareNav",
					    JOptionPane.WARNING_MESSAGE);

			}
			gcw = new GuiConfigWizard();
			c = gcw.startWizard(args);
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
		if (c.getDontCompress().equals("*")) {
			compressed = false;
		} else {
			dontCompress = c.getDontCompress().split("[;,]");
			if (dontCompress[0].equals("")) {
				dontCompress = null;
			}
		}

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
				JOptionPane.showMessageDialog(gcw, "A ShareNav bundle was successfully created and can now be copied to device or run.");
			} else {
				JOptionPane.showMessageDialog(gcw, "A fatal error occured during processing. Please have a look at the output logs.");
			}
			gcw.reenableClose();
		}
	}

	private static void expand(Configuration c, String tmpDir) throws ZipException, IOException {
		if (c.verbose >= 0) {
			System.out.println("Preparing " + c.getBaseBundleFileName());
		}
		InputStream appStream = c.getBaseBundleFile();
		if (appStream == null) {
			System.out.println("ERROR: Couldn't find the jar file for " + c.getBaseBundleFileName());
			System.out.println("Check the app parameter in the properties file for misspellings");
			System.exit(1);
		}
		File file = new File(c.getTempBaseDir() + "/" + c.getBaseBundleFileName());
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
		File copyingmaps = new File(tmpDir + "/COPYING-MAPS");
		File copyingmaps2 = new File(tmpDir + "/COPYING-MAPS.txt");
		copying.renameTo(copying2);
		copyingosm.renameTo(copyingosm2);
		copyingmaps.renameTo(copyingmaps2);
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
						fw.write("MIDlet-" + m1.group(1) + ": " + c.getBundleName()
							 + "," + m1.group(3) + "," + m1.group(4) + "\n");
					} else if (line.startsWith("MIDlet-Name: ")) {
						fw.write("MIDlet-Name: " + c.getBundleName() + "\n");
					} else {
						fw.write(line + "\n");
					}
					if (line.startsWith("MicroEdition-Profile: ") ) {
						if (config.getAddToManifest().length() != 0) {
							fw.write(config.getAddToManifest() + "\n");
						}
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
		File jad = new File(c.getBundleFileName() + ".jad");
		FileWriter fw = new FileWriter(jad);

		/**
		 * Copy over the information from the manifest file, to the jad file.
		 * This way we use the information generated by the build process
		 * of ShareNav, to duplicate as little data as possible.
		 */
		try {
			String line = fr.readLine();;
			while (true) {
				if (line == null) {
					break;
				}
				String nextline = fr.readLine();
				if (nextline != null && nextline.substring(0, 1).equals(" ")) {
					line += nextline.substring(1);
					continue;
				}
				if (line.startsWith("MIDlet") || line.startsWith("MicroEdition") || (config.getAddToManifest().length() != 0 && line.startsWith(config.getAddToManifest())) ) {
					fw.write(line + "\n");
				}
				line = nextline;
			}
		} catch (IOException ioe) {
			//This will probably be the end of the file
		}
		/**
		 * Add some additional fields to the jad file, that aren't present in the manifest file.
		 */
		fw.write("MIDlet-Jar-Size: " + jarLength + "\n");
		fw.write("MIDlet-Jar-URL: " + c.getBundleFileName() + ".jar\n");
		fw.close();
		fr.close();
	}

	private static void pack(Configuration c) throws ZipException, IOException {
		File n = null;
		if (config.getMapName().equals("")) {
			n = new File(c.getBundleFileName() + (config.sourceIsApk ? ".apk": ".jar"));
			rewriteManifestFile(c, true);
		} else {
			n = new File(c.getMapFileName());
			rewriteManifestFile(c, false);
			renameCopying(c);
		}
		BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(n));
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
		String bundleName = n.getAbsolutePath();
		String jarSigner = config.getJarsignerPath();

		// resolve Windows environment variables case-insensitive
		java.util.Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
              jarSigner = jarSigner.replaceAll("(?i)%" + envName + "%", Matcher.quoteReplacement(env.get(envName)));
        }
		
		zf.close();

		if (config.sourceIsApk && config.signApk) {
			Process signer = null;
			// FIXME add "-storepass" handling with a password field on GUI

			String command[] = { jarSigner,
					     "-verbose",
					     "-verbose",
					     "-verbose",
					     "-digestalg",
					     "SHA1",
					     "-sigalg",
					     "MD5withRSA",
					     bundleName,
					     "sharenav" };
			String passString = config.getSignApkPassword();
			if (! passString.equals("")) {
				command[2] = "-storepass";
				command[3] = passString;
			}
			try {
				String jarsignerOutputLine = null;
				System.out.println("Signing with external program " + command[0] + " (set jarsignerPath=<jarsigner-path-or-commandname> in .properties to change)");
				ProcessBuilder pBuilder = new ProcessBuilder(command);
				pBuilder.redirectErrorStream(true);
				signer = pBuilder.start();
				//signer = Runtime.getRuntime().exec(command);
				// Runtime.getRuntime().exec(jarSigner + " -verbose -digestalg SHA1 -sigalg MD5withRSA " + bundleName + " sharenav");

				//DataInputStream jarsignerOutputDataStream = new InputStream(signer.getInputStream());
				BufferedReader jarsignerOutput = new BufferedReader(new InputStreamReader(signer.getInputStream()));
				// if jarsigner asks for a password, this makes it stop
				// asking and show the query/error output
				signer.getOutputStream().flush();
				signer.getOutputStream().close();
				while ((jarsignerOutputLine = jarsignerOutput.readLine()) != null) {
					System.out.println(jarsignerOutputLine);
				}
			} catch (IOException ioe) {
				System.out.println("Error: IO exception " + ioe);
				showSigningMessage(bundleName);
			}
			if (signer != null) {
				try {
					signer.waitFor();
					int exitStatus = signer.exitValue();
					if (exitStatus != 0) {
						System.out.println("ERROR: jarsigner exited with exit status " + exitStatus + ", signing failed");
						showSigningMessage(bundleName);
					}
				} catch (InterruptedException ie) {
					System.out.println("Error: interrupted execution " + ie);
					showSigningMessage(bundleName);
				}
			}
		}
		// System.out.println("Bundlename: " + bundleName + " jarSigner: " + jarSigner);

		if (config.getMapName().equals("") && !config.sourceIsApk) {
			writeJADfile(c, n.length());
		}
		Calendar endTime = Calendar.getInstance();
		if (config.verbose >= 0) {
			System.out.println(n.getName() + " created successfully with " + (n.length() / 1024 / 1024) + " MiB in " +
					   getDuration(endTime.getTimeInMillis() - startTime.getTimeInMillis()));
		}
	}
	
	public static void showSigningMessage(String bundleName) {
		System.out.println("Error: Wasn't able to sign " + bundleName);
		System.out.println("You may need to set up jarsigner path and/or settings for signing, see the ShareNav Wiki Properties page under \"Signing an apk\"");
			System.out.println("You may also need to install the java development environment");
	}

	private static String getDuration(long duration) {
		final int millisPerSecond = 1000;
		final int millisPerMinute = 1000*60;
		final int millisPerHour = 1000*60*60;
		final int millisPerDay = 1000*60*60*24;
		int days = (int) (duration / millisPerDay);
		int hours = (int) (duration % millisPerDay / millisPerHour);
		int minutes = (int) (duration % millisPerHour / millisPerMinute);
		int seconds = (int) (duration % millisPerMinute / millisPerSecond);
		return String.format("%d %02d:%02d:%02d", days, hours, minutes, seconds);
	}

	private static void packDir(ZipOutputStream os, File d, String path) throws IOException {
		File[] files = d.listFiles();
		if (config.sourceIsApk) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()
				    && files[i].getName().equals("META-INF")
				    && path.length() == 0) {
					// put META-INF first, not sure if it matters but is customary
					File tmp = files[0];
					files[0] = files[i];
					files[i] = tmp;
				}
			}
		}
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
				boolean storethis = false;

				if (compressed && dontCompress != null) {
					for (String extension : dontCompress) {
						if (files[i].getName().toLowerCase().endsWith(extension)) {
							ze.setMethod(ZipOutputStream.STORED);
							storethis = true;
						}
					}
				}

				//byte buffer to read in larger chunks
				byte[] bb = new byte[4096];
				FileInputStream stream = new FileInputStream(files[i]);
				if ((!compressed) || storethis) {
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
		if (config == null) {
			System.out.println("ERROR: can't find config, exiting");
			System.exit(1);
		}
		if ((config.enableEditingSupport) && (!(config.getAppParam().contains("editing")) && !(config.getAppParam().contains("full-connected")))) {
			System.out.println("ERROR: You are creating a map with editing support, but use a app version that does not support editing\n"
					+ "     please fix your .properties file (full-connected app versions have editing support)");
			System.exit(1);
		}
		if ((config.getPlanetName() == null || config.getPlanetName().equals(""))) {
			System.out.println("ERROR: You haven't specified a planet file\n"
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
			if (config.verbose >= 0) {
				System.out.println(config.toString());
			}
			
			// the legend must be parsed after the configuration to apply parameters to the travel modes specified in useRouting
			TravelModes.stringToTravelModes(config.useRouting);
			config.parseLegend();
			
			// Maybe some of these should be configurable in the future.
			SeaGenerator.setOptions(config, false, false, false, true, 100);
			
			startTime = Calendar.getInstance();

			TravelMode tm = null;
			if (Configuration.attrToBoolean(config.useRouting) >= 0) {
				for (int i = 0; i < TravelModes.travelModeCount; i++) {
					tm = TravelModes.travelModes[i];
					if (config.verbose >= 0) {
						System.out.println("Route and toll rules in " + config.getStyleFileName()
								   + " for " + tm.getName() + ":");
						if ( (tm.travelModeFlags & TravelMode.AGAINST_ALL_ONEWAYS) > 0) {
							System.out.println(" Going against all accessible oneways is allowed");
						}
						if ( (tm.travelModeFlags & TravelMode.BICYLE_OPPOSITE_EXCEPTIONS) > 0) {
							System.out.println(" Opposite direction exceptions for bicycles get applied");
						}
					}
		        	int routeAccessRestrictionCount = 0;
		            if (TravelModes.getTravelMode(i).getRouteAccessRestrictions().size() > 0) {
		            	for (RouteAccessRestriction r: tm.getRouteAccessRestrictions()) {
		            		routeAccessRestrictionCount++;
					if (config.verbose >= 0) {
						System.out.println(" " + r.toString());
					}
		            	}
		            }
		            if (routeAccessRestrictionCount == 0) {
		        		System.out.println("Warning: No access restrictions in "
		        				+ config.getStyleFileName() + " for " + tm.getName());
		            }
		        	int tollRuleCount = 0;
		            if (TravelModes.getTravelMode(i).getTollRules().size() > 0) {
		            	for (TollRule r: tm.getTollRules()) {
		            		tollRuleCount++;
					if (config.verbose >= 0) {
						System.out.println(" " + r.toString());
					}
		            	}
		            }
		            if (tollRuleCount == 0) {
		        		System.out.println("Warning: No toll rules in "
		        				+ config.getStyleFileName() + " for " + tm.getName());
		            }
				}
				System.out.println("");
			}
			
            if (LegendParser.getDamages().size() == 0) {
        		System.out.println("No damage markers in "	+ config.getStyleFileName());
            } else {
		    if (config.verbose >= 0) {
			    System.out.println("Rules specified in " + config.getStyleFileName() + " for marking damages:");
			    for (Damage damage: LegendParser.getDamages()) {
				    System.out.println(" Ways/Areas with key " + damage.key + "=" + damage.values); 
			    }
		    }
            }
			String tmpDir = config.getTempDir();
			if (config.verbose >= 0) {
				System.out.println("Unpacking application to " + tmpDir);
			}
			expand(config, tmpDir);
			File target = new File(tmpDir);
			createPath(target);
			if (config.sourceIsApk) {
				// create /assets
				File targetAssets = new File(tmpDir + "/assets");
				createPath(targetAssets);
				// unsign the APK by deleting files from META-INF (e.g. from debug signing) to be able to sign the APK with the private key
				File f = new File(tmpDir + "/META-INF/MANIFEST.MF");
				f.delete();
				f = new File(tmpDir + "/META-INF/CERT.SF");
				f.delete();
				f = new File(tmpDir + "/META-INF/CERT.RSA");
				f.delete();
			}
			
			OsmParser parser = config.getPlanetParser();
			
			SeaGenerator2 sg2 = new SeaGenerator2();
			long startTime = System.currentTimeMillis();
			long time;
			SeaGenerator2.setOptions(config, true, true, true, true, 100);
			if (config.getGenerateSea()) {
				if (config.verbose >= 0) {
					System.out.println("Starting SeaGenerator");
				}
				sg2.generateSea(parser);
				time = (System.currentTimeMillis() - startTime);
				if (config.verbose >= 0) {
					System.out.println("SeaGenerator run");
					System.out.println("  Time taken: " + time / 1000 + " seconds");
				}
			}
			
			if (config.verbose >= 0) {
				System.out.println("Starting relation handling");
			}
			startTime = System.currentTimeMillis();
			Area.setParser(parser);
			new Relations(parser, config);
			if (config.verbose >= 0) {
				System.out.println("Relations processed");
				time = (System.currentTimeMillis() - startTime);
				System.out.println("  Time taken: " + time / 1000 + " seconds");
			}

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
			if (config.verbose >= 0) {
				System.out.println("Types of relations present but ignored: ");
				for (Entry<String, Integer> e : relTypes.entrySet()) {
					System.out.println("   " + e.getKey() + ": " + e.getValue());

				}
			}
			relTypes = null;
			if (config.verbose >= 0) {
				System.out.println("Splitting long ways");
			}
			int numWays = parser.getWays().size();
			startTime = System.currentTimeMillis();
			new SplitLongWays(parser);
			time = (System.currentTimeMillis() - startTime);
			if (config.verbose >= 0) {
				System.out.println("Splitting long ways increased ways from "
						   + numWays + " to " + parser.getWays().size());
				System.out.println("  Time taken: " + time / 1000 + " seconds");
			}
			OxParser.printMemoryUsage(1);
			
			RouteData rd = null;
			if (Configuration.attrToBoolean(config.useRouting) >= 0 ) {
				rd = new RouteData(parser, target.getCanonicalPath());
				if (config.verbose >= 0) {
					System.out.println("Remembering " + parser.trafficSignalCount + " traffic signal nodes");
				}
				rd.rememberDelayingNodes();
			}
			
			if (config.verbose >= 0) {
				System.out.println("Removing unused nodes");
			}
			new CleanUpData(parser, config);

			if (Configuration.attrToBoolean(config.useRouting) >= 0 ) {
				if (config.verbose >= 0) {
					System.out.println("Creating route data");
					System.out.println("===================");
				}
				rd.create(config);
				rd.optimise();
				OsmParser.printMemoryUsage(1);
			}
			CreateShareNavData cd = new CreateShareNavData(parser, target.getCanonicalPath());
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
				if (config.verbose >= 0) {
					System.out.println("Cleaning up temporary directory " + tmpBaseDir);
				}
				deleteDirectory(tmpBaseDir);
			}
			
			createSuccessfully = true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


}

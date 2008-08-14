package de.ueller.osmToGpsMid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.ueller.osmToGpsMid.model.Relation;



public class BundleGpsMid {
	static boolean  compressed=true;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1){
			InputStream fr;
			try {
				Configuration c=new Configuration(args[0],args[1]);
				System.out.println("create Bundle for " + c.getName());
				System.out.println("Midlet Name: " + c.getMidletName());
				String tmpDir = c.getTempDir();
				System.out.println("unpack Application to " + tmpDir);
				expand(c, tmpDir);
				File target=new File(tmpDir);
				createPath(target);				
				fr= c.getPlanetSteam();
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
				
				System.out.println("reorder Ways");
				new CleanUpData(parser,c);
				
				if (c.useRouting){
					RouteData rd=new RouteData(parser,target.getCanonicalPath());
					System.out.println("create Route Data");
					rd.create();
					System.out.println("optimize Route Date");
					rd.optimise();
				}
				CreateGpsMidData cd=new CreateGpsMidData(parser,target.getCanonicalPath());
//				rd.write(target.getCanonicalPath());
//				cd.setRouteData(rd);
				cd.setConfiguration(c);
				System.out.println("split long ways " + parser.getWays().size());
				new SplitLongWays(parser);
				System.out.println("splited long ways to " + parser.getWays().size());
				new CalcNearBy(parser);
				cd.exportMapToMid();
				//Drop parser to conserve Memory
				parser=null;
				pack(c);
				
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			System.err.println("please give arguments");
			System.err.println("  arg1: planet file");
			System.err.println("  arg2: location configfile");		
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
		File file=new File(c.getTempBaseDir()+"/"+c.getJarFileName());
		writeFile(appStream, file.getAbsolutePath());
		
		ZipFile zf=new ZipFile(file.getCanonicalFile());
		for (Enumeration<? extends ZipEntry> e=zf.entries();e.hasMoreElements();) {
			ZipEntry ze=e.nextElement();
			if (ze.isDirectory()){
//				System.out.println("dir  "+ze.getName());
			} else {
//				System.out.println("file "+ze.getName());
				InputStream stream = zf.getInputStream(ze);
				writeFile(stream,tmpDir+"/"+ze.getName());
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
			File manifest=new File(tmpDir+"/META-INF/MANIFEST.MF");
			File manifest2=new File(tmpDir+"/META-INF/MANIFEST.tmp");

			BufferedReader fr=new BufferedReader(new FileReader(manifest));
			FileWriter fw=new FileWriter(manifest2);
			String line;
			Pattern p1 = Pattern.compile("MIDlet-(\\d):\\s(.*),(.*),(.*)");			
			while (true) {
				line = fr.readLine();
				if (line == null) {				
					break;				
				}

				Matcher m1 = p1.matcher(line);				
				if (m1.matches()) {					
					fw.write("MIDlet-" + m1.group(1) + ": " + c.getMidletName() + "," + m1.group(3) + "," + m1.group(4) + "\n");
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

	private static void writeJADfile(Configuration c,  long jarLength) throws IOException{
		String tmpDir = c.getTempDir();
		File manifest=new File(tmpDir+"/META-INF/MANIFEST.MF");
		BufferedReader fr=new BufferedReader(new FileReader(manifest));
		File jad=new File(c.getMidletName()+"-" 
				+ c.getName() 
				+ "-" + c.getVersion()
				+ ".jad");
		FileWriter fw=new FileWriter(jad);
		
		/**
		 * Copy over the information from the manifest file, to the jad file
		 * by this we use the information generated by the build process
		 * of GpsMid, to dupplicate as little data as possible
		 */
		try {
			String line;
			while (true) {
				line = fr.readLine();
				if (line == null)
					break;
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
		fw.write("MIDlet-Jar-Size: "+jarLength+"\n");
		fw.write("MIDlet-Jar-URL: "+c.getMidletName()+"-"+c.getName()+"-"+c.getVersion()+".jar\n");
		fw.close();
	}

	private static void pack(Configuration c) throws ZipException, IOException{
		rewriteManifestFile(c);
		File n=new File(c.getMidletName()+"-" 
				+ c.getName() 
				+ "-" + c.getVersion()
				+ ".jar");
		FileOutputStream fo=new FileOutputStream(n);
		ZipOutputStream zf=new ZipOutputStream(fo);
		zf.setLevel(9);
		if (!compressed){
			zf.setMethod(ZipOutputStream.STORED);
		}
		File src=new File(c.getTempDir());
		if (! src.isDirectory() ){
			throw new Error("TempDir is not a directory");
		}
		packDir(zf, src,"");
		zf.close();
		writeJADfile(c, n.length());
	}
	
	private static void packDir(ZipOutputStream os, File d,String path) throws IOException{
		File[] files = d.listFiles();
		for (int i=0; i<files.length;i++){
			if (files[i].isDirectory()){
				if (path.length() > 0){
					packDir(os, files[i],path + "/" + files[i].getName());
				} else {
					packDir(os, files[i],files[i].getName());					
				}
			} else {
//				System.out.println();
				ZipEntry ze=null;
				if (path.length() > 0){
				   ze = new ZipEntry(path+"/"+files[i].getName());
				} else {
				   ze = new ZipEntry(files[i].getName());				
				}
				int ch;
				int count=0;
				//byte buffer to read in larger chunks
				byte[] bb = new byte[4096];
				FileInputStream stream=new FileInputStream(files[i]);
				if (!compressed){
					CRC32 crc=new CRC32();
					count=0;
					while ((ch=stream.read(bb)) != -1){
						crc.update(bb,0,ch);
					}
					ze.setCrc(crc.getValue());
					ze.setSize(files[i].length());
				}
//				ze.
				os.putNextEntry(ze);
				count=0;
				stream.close();
				stream=new FileInputStream(files[i]);
				while ((ch=stream.read(bb)) != -1){
					os.write(bb,0,ch);
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
		File f=new File(name);
		try {
			if (! f.canWrite()){
				createPath(f.getParentFile());
			}
			FileOutputStream fo=new FileOutputStream(name);
			int ch;
			int count=0;
			byte[] bb = new byte[4096];
			while ((ch=stream.read(bb)) != -1){
				fo.write(bb,0,ch);
				count+=ch;
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

}

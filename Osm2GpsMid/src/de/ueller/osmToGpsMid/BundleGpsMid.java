package de.ueller.osmToGpsMid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;



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
				File planet = c.getPlanet();
				fr= new BufferedInputStream(new FileInputStream(planet), 4096);
				if (planet.getName().endsWith(".bz2") || planet.getName().endsWith(".gz")){
					int availableProcessors = Runtime.getRuntime().availableProcessors();
					if (availableProcessors > 1){
						System.out.println("found " + availableProcessors + " CPU's: uncompress in seperate thread");
						fr = new Bzip2Reader(fr);						
					} else {						
						System.out.println("only one CPU: uncompress in same thread");
						if (planet.getName().endsWith(".bz2")) {
							fr.read();
							fr.read();
							fr = new CBZip2InputStream(fr);
						} else if (planet.getName().endsWith(".gz")) {
							fr = new GZIPInputStream(fr);							
						}
					}
				} 
				OxParser parser = new OxParser(fr,c);
				System.out.println("read Nodes " + parser.nodes.size());
				System.out.println("read Ways  " + parser.ways.size());
				System.out.println("read Relations  " + parser.relations.size());
				System.out.println("reorder Ways");
				new CleanUpData(parser,c);
				RouteData rd=new RouteData(parser,target.getCanonicalPath());
				if (c.useRouting){
					System.out.println("create Route Data");
					rd.create();
					System.out.println("optimize Route Date");
					rd.optimise();
				}
				CreateGpsMidData cd=new CreateGpsMidData(parser,target.getCanonicalPath());
//				rd.write(target.getCanonicalPath());
//				cd.setRouteData(rd);
				cd.setConfiguration(c);
				System.out.println("split long ways " + parser.ways.size());
				new SplitLongWays(parser);
				System.out.println("splited long ways to " + parser.ways.size());
				new CalcNearBy(parser);
				cd.exportMapToMid();
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
			return;
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
		writeManifestFile(c, tmpDir);
	}

	/**
	 * @param c
	 * @param tmpDir
	 * @throws IOException
	 */
	private static void writeManifestFile(Configuration c, String tmpDir)
			throws IOException {
		File manifest=new File(tmpDir+"/META-INF/MANIFEST.MF");
		FileWriter fw=new FileWriter(manifest);
		fw.write("Manifest-Version: 1.0\n");
		fw.write("MIDlet-Name: "+c.getMidletName()+"\n");
		fw.write("MIDlet-Version: "+c.getVersion()+"\n");
		fw.write("MIDlet-Vendor: Harald Mueller"+"\n");
		fw.write("MIDlet-Icon: /GpsMid.png"+"\n");
		fw.write("MIDlet-Info-URL: http://gpsmid.sourceforge.net"+"\n");
		fw.write("MIDlet-1: "+c.getMidletName()+",,GpsMid\n");
		fw.write("MIDlet-Delete-Confirm: Do you really want to kill me?"+"\n");
		fw.write("MicroEdition-Configuration: CLDC-1.1"+"\n");
		fw.write("MicroEdition-Profile: MIDP-2.0"+"\n");
		fw.close();
	}

	private static void pack(Configuration c) throws ZipException, IOException{
		File n=new File(c.getMidletName()+"-" 
				+ c.getName() 
				+ "-" + c.getVersion()
				+ ".jar");
		File jad=new File(c.getMidletName()+"-" 
				+ c.getName() 
				+ "-" + c.getVersion()
				+ ".jad");
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
	    FileWriter fw=new FileWriter(jad);
		fw.write("MIDlet-1: "+c.getMidletName()+", GpsMid.png, GpsMid\n");
		fw.write("MIDlet-Jar-URL: "+c.getMidletName()+"-"+c.getName()+"-"+c.getVersion()+".jar\n");
		fw.write("MIDlet-Name: "+c.getMidletName()+"\n");
		fw.write("MIDlet-Jar-Size: "+n.length()+"\n");
		fw.write("MIDlet-Vendor: Harald Mueller\n");
		fw.write("MIDlet-Version: "+c.getVersion()+"\n");
		fw.write("MicroEdition-Configuration: CLDC-1.1\n");
		fw.write("MicroEdition-Profile: MIDP-2.0\n");
		fw.close();
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

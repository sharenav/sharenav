package de.ueller.osmToGpsMid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;



public class BundleGpsMid {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0){
			FileInputStream fr;
			try {
				Configuration c=new Configuration(args[0]);
				System.out.println("create Bundle for " + c.getName());
				String tmpDir = c.getTempDir();
				System.out.println("unpack Application to " + tmpDir);
				expand(c, tmpDir);
				fr = new FileInputStream(c.getPlanet());
				OxParser parser = new OxParser(fr,c.getBounds());
				System.out.println("read Nodes " + parser.nodes.size());
				System.out.println("read Lines " + parser.lines.size());
				System.out.println("read Ways  " + parser.ways.size());
				File target=new File(tmpDir+"/map");
				createPath(target);
				CreateGpsMidData cd=new CreateGpsMidData(parser,target.getCanonicalPath());
				new SplitLongWays(parser);
				cd.exportMapToMid();
				pack(c);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			System.err.println("please give the Configfile as argument");
		}
	}

	private static void expand(Configuration c, String tmpDir) throws ZipException, IOException {
		File file=c.getJarFile();
		ZipFile zf=new ZipFile(file.getCanonicalFile());
		for (Enumeration<? extends ZipEntry> e=zf.entries();e.hasMoreElements();) {
			ZipEntry ze=e.nextElement();
			if (ze.isDirectory()){
				System.out.println("dir  "+ze.getName());
			} else {
				System.out.println("file "+ze.getName());
				InputStream stream = zf.getInputStream(ze);
				writeFile(stream,tmpDir+"/"+ze.getName());
			}
		}
	}

	private static void pack(Configuration c) throws ZipException, IOException{
		File n=new File("GpsMid-" 
				+ c.getName() 
				+ "-" + c.getVersion()
				+ ".jar");
		FileOutputStream fo=new FileOutputStream(n);
		ZipOutputStream zf=new ZipOutputStream(fo);
		zf.setLevel(9);
		File src=new File(c.getTempDir());
		if (! src.isDirectory() ){
			throw new Error("TempDir is not a directory");
		}
		packDir(zf, src,"");
		zf.close();
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
				ZipEntry ze = new ZipEntry(path+"/"+files[i].getName());
				os.putNextEntry(ze);
				FileInputStream stream=new FileInputStream(files[i]);
				int ch;
				int count=0;
				while ((ch=stream.read()) != -1){
					os.write(ch);
					count++;
				}
				System.out.println("wrote " + path + "/" + files[i].getName() + " byte:" + count);
				
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
			while ((ch=stream.read()) != -1){
				fo.write(ch);
				count++;
			}
			System.out.println("wrote " + name + " byte:" + count);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("fail to write " + e.getMessage());
		}
	}

	/**
	 * @param f
	 */
	private static void createPath(File f) {
		if (! f.canWrite())
			createPath(f.getParentFile());
		System.out.println("create dir " + f);
		f.mkdir();
	}

}

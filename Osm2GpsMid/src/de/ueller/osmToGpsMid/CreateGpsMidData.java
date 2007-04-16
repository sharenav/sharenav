package de.ueller.osmToGpsMid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.Way;



public class CreateGpsMidData {
	OxParser parser;
	Tile tile[]= new Tile[4];
	private final String	path;
	TreeSet<String> names;
//	private Bounds[] bounds=null;
	
	public CreateGpsMidData(OxParser parser,String path) {
		super();
		this.parser = parser;
		this.path = path;
	}
	
//	/**
//	 * @param parser2
//	 * @param string
//	 * @param bounds
//	 */
//	public CreateGpsMidData(OxParser parser, String path, Bounds[] bounds) {
//		super();
//		this.parser = parser;
//		this.path = path;
//		this.bounds = bounds;
//	}

	private int getEqualCount(String s1, String s2){
		if (s1== null || s2 == null)
			return 0;
		int l1=s1.length();
		int l2=s2.length();
		int l=(l1 < l2)? l1 : l2;
		for (int loop=0; loop < l;loop++){
			if (s1.charAt(loop) != s2.charAt(loop))
				return loop;
		}
		return l;
	}

	public void exportMapToMid(){
		File dir=new File(path);
		if (dir.isDirectory()){
			File[] files = dir.listFiles();
			for (File f : files) {
				f.delete();
			}
		}
		names = getNames();
		System.out.println("Names="+names.size());

		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			FileOutputStream foi = new FileOutputStream(path+"/names-idx.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			String lastStr=null;
			fo = new FileOutputStream(path+"/names-0.dat");
			ds = new DataOutputStream(fo);
			int curPos=0;
			short idx=0;
			short fnr=1;
			short fcount=0;
			for (String string : names) {
				int eq=getEqualCount(string,lastStr);
				if ((eq==0 && fcount>100) || (fcount > 150 && eq < 2)){
					dsi.writeShort(idx);
					if (ds != null) ds.close();
					fo = new FileOutputStream(path+"/names-"+fnr+".dat");
					ds = new DataOutputStream(fo);
					System.out.println("wrote names " + fnr + " with "+ fcount + " names");
					fnr++;
					curPos=0;
					eq=0;
					fcount=0;
					lastStr=null;
				}
				ds.writeByte(eq-curPos);
				ds.writeUTF(string.substring(eq));
				System.out.println("" + (eq-curPos) + "'" +string.substring(eq)+"' '"+string);
				curPos=eq;
				lastStr=string;
				idx++;
				fcount++;
//				ds.writeUTF(string);
			}
			dsi.writeShort(idx);
			ds.close();
			dsi.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i=0;i<=3;i++)
		exportMapToMid(i);
	}
	
	private TreeSet<String> getNames(){
		TreeSet<String> wayNames = new TreeSet<String>();
		for (Way w : parser.ways) {
			addName(wayNames,w.getName());
			addName(wayNames, w.tags.get("nat_ref"));
			addName(wayNames, w.tags.get("is_in"));
			addName(wayNames, w.tags.get("ref"));
			
		}
		for (Node n : parser.nodes.values()) {
			addName(wayNames,n.getName());
			addName(wayNames, n.tags.get("nat_ref"));
			addName(wayNames, n.tags.get("ref"));
			addName(wayNames, n.tags.get("is_in"));
		}
		System.out.println("found " + wayNames.size() + " names");
		return (wayNames);
	}

	private void addName(TreeSet<String> wayNames, String v) {
		if (v != null){
			String tv=v.trim();
			if (tv.length() > 0)
				wayNames.add(v);
		}
	}
	private int getWayNameIndex(String name){
		int index=0;
		for (String s : names) {
			if (s.equals(name)) {
//				System.out.println("found String " + name + " at " + index);
				return index;
			}
			index++;
		}
		return -1;
	}
	
	public void exportMapToMid(int zl){
		System.out.println("Total ways : " + parser.ways.size() + "  Nodes : " + parser.nodes.size());
		try {
			FileOutputStream fo = new FileOutputStream(path+"/dict-"+zl+".dat");
			DataOutputStream ds = new DataOutputStream(fo);
//			Node min=new Node(90f,180f,0);
//			Node max=new Node(-90f,-180f,0);		
			ds.writeUTF("DictMid"); // magig number
			Bounds allBound=new Bounds();
			for (Iterator wi = parser.ways.iterator(); wi.hasNext();) {
				Way w1=(Way)wi.next();
				if (w1.getZoomlevel() != zl) continue;
				w1.used=false;
				
				for (Iterator li = w1.lines.iterator(); li.hasNext();){
					Line l1=(Line) li.next();
					if (l1.isValid()) {
						allBound.extend(l1.from.lat, l1.from.lon);
						allBound.extend(l1.to.lat, l1.to.lon);
					} 
				}
			}
			for (Node n : parser.nodes.values()) {
				if (n.getZoomlevel() != zl) continue;
				allBound.extend(n.lat,n.lon);
			}
			tile[zl]=new Tile((byte) zl);
			exportTile(tile[zl],1, parser.ways,parser.nodes.values(), (byte) 1,zl,allBound);
			tile[zl].recalcBounds();
			tile[zl].write(ds);
			ds.writeUTF("END"); // magig number
			fo.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int exportTile(Tile t,int fid,Collection<Way> parentWays,Collection<Node> parentNodes,byte cut,int zl,Bounds tileBound) throws IOException{
		Bounds realBound=new Bounds();
		LinkedList<Way> ways=getWaysInBound(parentWays, zl,tileBound,realBound);
		if (ways.size() == 0){
			t.type=3;
			return fid;
		}
		int mostlyInBound=ways.size();
		addWaysCompleteInBound(ways,parentWays,zl,realBound);
		if (ways.size() > 2*mostlyInBound){
			realBound=new Bounds();
			ways=getWaysInBound(parentWays, zl,tileBound,realBound);
		}
		Collection<Node> nodes=getNodesInBound(parentNodes,zl,realBound);
		byte [] out=createMidContent(ways,nodes);
		if (ways.size() > 255 || (out.length > 10000 && ways.size() > 2)){
			System.out.println("create Subtiles size="+out.length+" ways=" + ways.size());
			t.bounds=realBound.clone();
			t.type=2;
			t.t1=new Tile((byte) zl);
			t.t2=new Tile((byte) zl);
			if ((tileBound.maxLat-tileBound.minLat) > (tileBound.maxLon-tileBound.minLon)){
				cut=2;
			} else {
				cut=1;
			}
			if (cut==1){
				float splitLon=(tileBound.minLon+tileBound.maxLon)/2;
				Bounds nextTileBound=tileBound.clone();
				nextTileBound.maxLon=splitLon;
				fid=exportTile(t.t1,fid,ways,nodes,cut,zl,nextTileBound);
				nextTileBound=tileBound.clone();
				nextTileBound.minLon=splitLon;
				fid=exportTile(t.t2,fid,ways,nodes,cut,zl,nextTileBound);
			} else {
				float splitLat=(tileBound.minLat+tileBound.maxLat)/2;
				Bounds nextTileBound=tileBound.clone();
				nextTileBound.maxLat=splitLat;
				fid=exportTile(t.t1,fid,ways,nodes,cut,zl,nextTileBound);
				nextTileBound=tileBound.clone();
				nextTileBound.minLat=splitLat;
				fid=exportTile(t.t2,fid,ways,nodes,cut,zl,nextTileBound);
			}
			ways=null;
			nodes=null;
			
//			System.gc();
		} else {
			if (ways.size() > 0){
				Collections.sort(ways);
				Bounds bBox=new Bounds();
				for (Node n : nodes) {
					bBox.extend(n.lat, n.lon);
				}
				t.bounds=realBound.clone();
				t.fid=fid;
				t.type=1;
				t.setWays(ways);
				FileOutputStream fo = new FileOutputStream(path+"/t"+zl+fid+".d");
				DataOutputStream tds = new DataOutputStream(fo);
				tds.write(out);
				fo.close();
				fid++;
				// mark ways as written to MidStorage
				for (Iterator wi = ways.iterator(); wi.hasNext();) {
					Way w1=(Way)wi.next();
					w1.used=true;
				}
			} else {
				//emty box
				t.type=3;
			}
		}
		return fid;
	}
	
	
	public LinkedList<Way> getWaysInBound(Collection<Way> parentWays,int zl,Bounds targetTile,Bounds realBound){
		LinkedList<Way> ways = new LinkedList<Way>();
		System.out.println("search for ways mostly in " + targetTile + " from " + parentWays.size() + " ways");
		// collect all way that are in this rectangle
		for (Way w1 : parentWays) {
			byte type=w1.getType();
			if (type == 0) continue;
			if (w1.getZoomlevel() != zl) continue;
			if (w1.used) continue;
			Bounds wayBound=w1.getBounds();
			if (targetTile.isMostlyIn(wayBound)){
				realBound.extend(wayBound);
				ways.add(w1);
			}
		}
		System.out.println("getWaysInBound found " + ways.size() + " ways");
		return ways;
	}
	public LinkedList<Way> addWaysCompleteInBound(LinkedList<Way> ways,Collection<Way> parentWays,int zl,Bounds targetTile){
		// collect all way that are in this rectangle
		System.out.println("search for ways total in " + targetTile + " from " + parentWays.size() + " ways");
		for (Way w1 : parentWays) {
			byte type=w1.getType();
			if (type == 0) continue;
			if (w1.getZoomlevel() != zl) continue;
			if (w1.used) continue;
			if (ways.contains(w1)) continue;
			Bounds wayBound=w1.getBounds();
			if (targetTile.isCompleteIn(wayBound)){
				ways.add(w1);
			}
		}
		System.out.println("addWaysCompleteInBound found " + ways.size() + " ways");
		return ways;
	}
	
	public Collection<Node> getNodesInBound(Collection<Node> parentNodes,int zl,Bounds targetBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			if (node.getType() == 0) continue;
			if (node.getZoomlevel() != zl) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
			nodes.add(node);
		}
		System.out.println("getNodesInBound found " + nodes.size() + " nodes");
		return nodes;
	}

	public byte[] createMidContent(Collection<Way> ways,Collection<Node> nodes) throws IOException{
		Map<Long,Node> wayNodes = new HashMap<Long,Node>();
		int ren=0;
		for (Way way : ways) {
			for (Line l : way.lines) {
				if (l.from != null)
					l.from.used=false;
				if (l.to != null)
					l.to.used=false;
			}
		}
		for (Node n1 : nodes){
			n1.used=true;
		}
		// find all point that are part of a way but not in interestNodes
		for (Iterator wi = ways.iterator(); wi.hasNext();) {
			Way w1=(Way)wi.next();

			for (Iterator li = w1.lines.iterator(); li.hasNext();){
				Line l1=(Line) li.next();
				if (l1.from != null){
				Long id=new Long(l1.from.id);
				if ((!wayNodes.containsKey(id)) && !l1.from.used){
					wayNodes.put(id, l1.from);
				}
				}
				if (l1.to != null){
					Long id=new Long(l1.to.id);
				if ((!wayNodes.containsKey(id))  && !l1.to.used){
					wayNodes.put(id, l1.to);
				}
				}
			}
		}
		System.out.println("test ways : " + ways.size() + "  Nodes : " + wayNodes.size());
		System.out.println("interrest Nodes : " + nodes.size());
		ByteArrayOutputStream fo = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(fo);
		ds.writeByte(0x54); // magig number
		ds.writeShort(nodes.size()+wayNodes.size());
		for (Node n : nodes) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds);
		}
		for (Node n : wayNodes.values()) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds);			
		}
		ds.writeByte(0x55); // magig number
		ds.writeByte(ways.size());
		for (Iterator iter = ways.iterator(); iter.hasNext();) {
			Way w=(Way)iter.next();
			writeWay(w, ds);
		}
		ds.writeByte(0x56); // magig number
		fo.close();
		return fo.toByteArray();
	}

	private void writeNode(Node n,DataOutputStream ds) throws IOException{
		int flags=0;
//		System.out.println("wirte node id="+n.renumberdId );
		ds.writeFloat(degToRad(n.lat));
		ds.writeFloat(degToRad(n.lon));
		if (n.getType() != 0){
			flags=1;
		}
		ds.writeByte(flags);
		if ((flags & 1) == 1){
			String name = n.getName();
			if (name != null)
				ds.writeShort(getWayNameIndex(name));
//				ds.writeUTF(name.trim());
			else 
				ds.writeUTF("");
			ds.writeByte(n.getType());
		}
	}

	private void writeWay(Way w,DataOutputStream ds) throws IOException{
		Bounds b=new Bounds();
//		System.out.println("write way "+w);
		int flags=0;
		int maxspeed=50;
		if (w.getName() != null){
			flags+=1;
		}
		if (w.tags.containsKey("maxspeed")){
			try {
				maxspeed=Integer.parseInt((String) w.tags.get("maxspeed"));
				flags+=2;
			} catch (NumberFormatException e) {
			}
		}
		byte type=w.getType();
		Integer p1=null;
		ArrayList<ArrayList<Integer>> paths=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> path = new ArrayList<Integer>();
		boolean isWay=false;
		boolean multipath=false;
		paths.add(path);
		isWay=false;
		for (Iterator iterw = w.lines.iterator(); iterw.hasNext();) {
			try {
				Line l=(Line) iterw.next();
				if (p1 == null){
					p1=new Integer(l.from.renumberdId);
//					System.out.println("Start Way at " + l.from);
					path.add(p1);
					b.extend(l.from.lat, l.from.lon);
				} else {
					if (l.from.renumberdId != p1.intValue()){
						// non continues path so open a new Path
						multipath=true;
						path = new ArrayList<Integer>();
						paths.add(path);
						p1=new Integer(l.from.renumberdId);
//						System.out.println("\tStart Way-Segment at " + l.from);
						path.add(p1);
						b.extend(l.from.lat, l.from.lon);
					} else if (path.size() > 254){
						// path to long
						multipath=true;
						path.add(p1); // close old Path with actual point
						path = new ArrayList<Integer>(); // start new path
						paths.add(path); // add to pathlist
						path.add(p1); // add same point as start to new path
					}
				}
//				System.out.println("\t\tContinues Way " + l.to);
				path.add(new Integer(l.to.renumberdId));
				isWay=true;
				p1=new Integer(l.to.renumberdId);
				b.extend(l.to.lat,l.to.lon);
			} catch (RuntimeException e) {
			}
		}
		if (isWay){
			if (multipath ){
				flags+=4;
			}
			ds.writeByte(flags);
			ds.writeFloat(degToRad(b.minLat));
			ds.writeFloat(degToRad(b.minLon));
			ds.writeFloat(degToRad(b.maxLat));
			ds.writeFloat(degToRad(b.maxLon));
//			ds.writeByte(0x58);
			ds.writeByte(type);
			if ((flags & 1) == 1){
				ds.writeShort(getWayNameIndex(w.getName()));
//				ds.writeUTF(w.getName().trim());
			}
			if ((flags & 2) == 2){
				ds.writeByte(maxspeed);
			}
			if ((flags & 4) == 4){
				ds.writeByte(paths.size());
			} 
//			System.out.print("Way Paths="+paths.size());
			for (ArrayList<Integer> subPath : paths){
				ds.writeByte(subPath.size());
//				System.out.print("Path="+subPath.size());
				for (Integer l : subPath) {
//					System.out.print(" "+l.intValue());
					ds.writeShort(l.intValue());
				}
// only for test integrity
//				System.out.println("   write magic code 0x59");
//				ds.writeByte(0x59);
			}
		} else {
			ds.write(128); // flag that mark there is no way
		}
	}

    public float degToRad(double deg) {
        return (float) (deg * (Math.PI / 180.0d));
    }

}

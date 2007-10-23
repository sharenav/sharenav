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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.HiLo;
import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.MapName;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.Sequence;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.name.Names;



public class CreateGpsMidData {
//	private final static int MAX_TILE_FILESIZE=20000;
//	private final static int MAX_ROUTETILE_FILESIZE=5000;
	public  final static int MAX_DICT_DEEP=5;
	public  final static int ROUTEZOOMLEVEL=4;
	OxParser parser;
	Tile tile[]= new Tile[ROUTEZOOMLEVEL+1];
	private final String	path;
	TreeSet<MapName> names;
	Names names1;
	
	private final static int INODE=1;
	private final static int SEGNODE=2;
//	private Bounds[] bounds=null;
	private Configuration configuration;
	private int totalWaysWritten=0;
	private int totalSegsWritten=0;
	private int totalNodesWritten=0;
	private int totalPOIsWritten=0;
	private RouteData rd;
	
	
	public CreateGpsMidData(OxParser parser,String path) {
		super();
		this.parser = parser;
		this.path = path;
		File dir=new File(path);
		// first of all, delete all data-files from a previous run or files that comes
		// from the mid jar file
		if (dir.isDirectory()){
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".d") || f.getName().endsWith(".dat")){
					if (! f.delete()){
						System.out.println("failed to delete file " + f.getName());
					}
				}
			}
		}
	}
	

	public void exportMapToMid(){
		names1=getNames1();
		SearchList sl=new SearchList(names1);
		sl.createNameList(path);
		for (int i=0;i<=3;i++){
			System.out.println("export Tiles for zoomlevel " + i);
			exportMapToMid(i);
		}
		System.out.println("export RouteTiles");
		exportMapToMid(ROUTEZOOMLEVEL);
//		for (int x=1;x<12;x++){
//			System.out.print("\n" + x + " :");
//			tile[ROUTEZOOMLEVEL].printHiLo(1, x);
//		}
//		System.exit(2);
		sl.createSearchList(path);
		System.out.println("Total Ways:"+totalWaysWritten 
				         + " Seg:"+totalSegsWritten
				         + " Pkt:"+totalNodesWritten
				         + " POI:"+totalPOIsWritten);
	}
	

	private Names getNames1(){
		Names na=new Names();
		for (Way w : parser.ways) {
			na.addName(w);		
		}
		for (Node n : parser.nodes.values()) {
			na.addName(n);
		}
		System.out.println("found " + na.getNames().size() + " names " + na.getNames().size() + " canon");
		na.calcNameIndex();
		return (na);
	}

	
	public void exportMapToMid(int zl){
//		System.out.println("Total ways : " + parser.ways.size() + "  Nodes : " + parser.nodes.size());
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
				allBound.extend(w1.getBounds());
			}
			if (zl == ROUTEZOOMLEVEL){
				// for RouteNodes
				for (Node n : parser.nodes.values()) {
					n.used=false;
					if (n.routeNode == null) continue;
					allBound.extend(n.lat,n.lon);
				}
			} else {
				for (Node n : parser.nodes.values()) {
					if (n.getZoomlevel() != zl) continue;
					allBound.extend(n.lat,n.lon);
				}
			}
			tile[zl]=new Tile((byte) zl);
			Sequence routeNodeSeq=new Sequence();
			Sequence tileSeq=new Sequence();
			tile[zl].ways=parser.ways;
			tile[zl].nodes=parser.nodes.values();
			// create the tiles and write the content 
			exportTile(tile[zl],tileSeq,allBound,routeNodeSeq);
			tile[zl].recalcBounds();
			if (zl == ROUTEZOOMLEVEL){
				Sequence rnSeq=new Sequence();
				tile[zl].renumberRouteNode(rnSeq);
				tile[zl].calcHiLo();
				tile[zl].writeConnections(path);
		        tile[zl].type=Tile.TYPE_ROUTECONTAINER;
			} 
			Sequence s=new Sequence();
			tile[zl].writeTileDict(ds,1,s,path);
			ds.writeUTF("END"); // Magic number
			fo.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exportTile(Tile t,Sequence tileSeq,Bounds tileBound, Sequence routeNodeSeq) throws IOException{
		Bounds realBound=new Bounds();
		LinkedList<Way> ways=new LinkedList<Way>();
		Collection<Node> nodes=new ArrayList<Node>();
		byte [] out=new byte[1];
		byte[] connOut=null;
		int maxSize;
		// Reduce the content of t.ways and t.nodes to all relevant elements
		// in the given bounds and create the binary midlet representation
		if (t.zl != ROUTEZOOMLEVEL){
			maxSize=configuration.getMaxTileSize();
			ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);
			if (ways.size() == 0){
				t.type=3;
			}
			int mostlyInBound=ways.size();
			addWaysCompleteInBound(ways,t.ways,t.zl,realBound);
			if (ways.size() > 2*mostlyInBound){
				realBound=new Bounds();
				ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);
			}
			nodes=getNodesInBound(t.nodes,t.zl,realBound);
			if (ways.size() <= 255){
				out=createMidContent(ways,nodes,t);
			}
			t.nodes=nodes;
			t.ways=ways;
		} else {
			// Route Nodes
			maxSize=configuration.getMaxRouteTileSize();
			nodes=getRouteNodesInBound(t.nodes,tileBound,realBound);
			byte[][] erg=createMidContent(nodes,t);
			out=erg[0];
			connOut = erg[1];
			t.nodes=nodes;
		}
		
		// split tile if more then 255 Ways or binary content > MAX_TILE_FILESIZE but not if only one Way
		if (ways.size() > 255 || (out.length > maxSize && ways.size() != 1)){
//			System.out.println("create Subtiles size="+out.length+" ways=" + ways.size());
			t.bounds=realBound.clone();
			if (t.zl != ROUTEZOOMLEVEL){
				t.type=Tile.TYPE_CONTAINER;				
			} else {
				t.type=Tile.TYPE_ROUTECONTAINER;
			}
			t.t1=new Tile((byte) t.zl,ways,nodes);
			t.t2=new Tile((byte) t.zl,ways,nodes);
			t.setRouteNodes(null);
			if ((tileBound.maxLat-tileBound.minLat) > (tileBound.maxLon-tileBound.minLon)){
				// split to half latitude
				float splitLat=(tileBound.minLat+tileBound.maxLat)/2;
				Bounds nextTileBound=tileBound.clone();
				nextTileBound.maxLat=splitLat;
				exportTile(t.t1,tileSeq,nextTileBound,routeNodeSeq);
				nextTileBound=tileBound.clone();
				nextTileBound.minLat=splitLat;
				exportTile(t.t2,tileSeq,nextTileBound,routeNodeSeq);
			} else {
				// split to half longitude
				float splitLon=(tileBound.minLon+tileBound.maxLon)/2;
				Bounds nextTileBound=tileBound.clone();
				nextTileBound.maxLon=splitLon;
				exportTile(t.t1,tileSeq,nextTileBound,routeNodeSeq);
				nextTileBound=tileBound.clone();
				nextTileBound.minLon=splitLon;
				exportTile(t.t2,tileSeq,nextTileBound,routeNodeSeq);
			}
			t.ways=null;
			t.nodes=null;
			
//			System.gc();
		} else {
			if (ways.size() > 0 || nodes.size() > 0){
				// Write as dataTile
				t.fid=tileSeq.next();
				if (t.zl != ROUTEZOOMLEVEL) {
					t.setWays(ways);
					writeRenderTile(t, tileBound, realBound, nodes, out);
				} else {
					writeRouteTile(t, tileBound, realBound, nodes, out);
				}

			} else {
				//Write as emty box
				t.type=Tile.TYPE_EMPTY;
			}
		}
		return;
	}


	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRouteTile(Tile t, Bounds tileBound, Bounds realBound,
			Collection<Node> nodes, byte[] out) {
		System.out.println("Write renderTile "+t.zl+":"+t.fid + " nodes:"+nodes.size());
		t.type=Tile.TYPE_MAP;
		t.bounds=tileBound.clone();
		t.type=Tile.TYPE_ROUTEDATA;
		for (RouteNode n:t.getRouteNodes()){
			n.node.used=true;
		}
	}
	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param ways
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRenderTile(Tile t, Bounds tileBound, Bounds realBound,
			 Collection<Node> nodes, byte[] out)
			throws FileNotFoundException, IOException {
		System.out.println("Write routeTile "+t.zl+":"+t.fid+ " ways:"+t.ways.size() + " nodes:"+nodes.size());
		totalNodesWritten+=nodes.size();
		totalWaysWritten+=t.ways.size();
		Collections.sort(t.ways);
		for (Way w: t.ways){
			totalSegsWritten+=w.getLineCount();
		}
		if (t.zl != ROUTEZOOMLEVEL) {
			for (Node n : nodes) {
				if (n.type > 0 )
					totalPOIsWritten++;
			}
		}
		
		t.type=Tile.TYPE_MAP;
		// RouteTiles will be written later because of renumbering
		if (t.zl != ROUTEZOOMLEVEL) {
			t.bounds=realBound.clone();
			FileOutputStream fo = new FileOutputStream(path + "/t" + t.zl
					+ t.fid + ".d");
			DataOutputStream tds = new DataOutputStream(fo);
			tds.write(out);
			fo.close();
			// mark ways as written to MidStorage
			for (Iterator wi = t.ways.iterator(); wi.hasNext();) {
				Way w1=(Way)wi.next();
				w1.used=true;
				w1.fid=t.fid;
			}
		} else {
			t.bounds=tileBound.clone();
			t.type=Tile.TYPE_ROUTEDATA;
			for (RouteNode n:t.getRouteNodes()){
				n.node.used=true;
			}
		}
	}
	
	
	public LinkedList<Way> getWaysInBound(Collection<Way> parentWays,int zl,Bounds targetTile,Bounds realBound){
		LinkedList<Way> ways = new LinkedList<Way>();
//		System.out.println("search for ways mostly in " + targetTile + " from " + parentWays.size() + " ways");
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
//		System.out.println("getWaysInBound found " + ways.size() + " ways");
		return ways;
	}
	public LinkedList<Way> addWaysCompleteInBound(LinkedList<Way> ways,Collection<Way> parentWays,int zl,Bounds targetTile){
		// collect all way that are in this rectangle
//		System.out.println("search for ways total in " + targetTile + " from " + parentWays.size() + " ways");
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
//		System.out.println("addWaysCompleteInBound found " + ways.size() + " ways");
		return ways;
	}
	
	public Collection<Node> getNodesInBound(Collection<Node> parentNodes,int zl,Bounds targetBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			if (node.getType(configuration) == 0) continue;
			if (node.getZoomlevel() != zl) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
			nodes.add(node);
		}
//		System.out.println("getNodesInBound found " + nodes.size() + " nodes");
		return nodes;
	}
	public Collection<Node> getRouteNodesInBound(Collection<Node> parentNodes,Bounds targetBound,Bounds realBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			if (node.routeNode == null) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
//			System.out.println(node.used);
			if (! node.used) {
				realBound.extend(node.lat,node.lon);
				nodes.add(node);
//				node.used=true;
			} 
		}
		return nodes;
	}

	/**
	 * Create the data-content for a route-tile. Containing a list of nodes and a list
	 * of connections from each node.
	 * @param interestNodes list of all Nodes that should included in this tile
	 * @param t the Tile that holds the meta-data
	 * @return in array[0][] the file-format for all nodes and in array[1][] the
	 * file-format for all connections whithin this tile.
	 * @throws IOException
	 */
	public byte[][] createMidContent(Collection<Node> interestNodes, Tile t) throws IOException{
		ByteArrayOutputStream nfo = new ByteArrayOutputStream();
		DataOutputStream nds = new DataOutputStream(nfo);
		ByteArrayOutputStream cfo = new ByteArrayOutputStream();
		DataOutputStream cds = new DataOutputStream(cfo);
		nds.writeByte(0x54); // magic number
		
		nds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			writeRouteNode(n,nds,cds);
				if (n.routeNode != null){
					t.addRouteNode(n.routeNode);
				}

		}

		nds.writeByte(0x56); // magic number
		nfo.close();
		cfo.close();
		byte [][] ret = new byte[2][];
		ret[0]=nfo.toByteArray();
		ret[1]=cfo.toByteArray();
		return ret;
	}

	/**
	 * Create the Data-content for a SingleTile in memory. This will later directly 
	 * written on Disk if the byte array is not to big otherwise this tile will
	 * splitted in smaller tiles. 
	 * @param ways a Collection of ways that are chosen to be in this tile.
	 * @param interestNodes all additional Nodes like places, parking and so on 
	 * @param t the Tile, holds the metadata for this area.
	 * @return a byte array that represents a file content. This could be written
	 * directly ond disk.
	 * @throws IOException
	 */
	public byte[] createMidContent(Collection<Way> ways,Collection<Node> interestNodes, Tile t) throws IOException{
		Map<Long,Node> wayNodes = new HashMap<Long,Node>();
		int ren=0;
		// reset all used flags of all Nodes that are part of ways in <code>ways</code>
		for (Way way : ways) {
			for (SubPath sp:way.getSubPaths()){
				for (Node n:sp.getNodes()){
					n.used=false;
				}
			}
		}
		// mark all interestNodes as used
		for (Node n1 : interestNodes){
			n1.used=true;
		}
		// find all nodes that are part of a way but not in interestNodes
		for (Way w1: ways) {
			for (SubPath sp:w1.getSubPaths()){
				for (Node n:sp.getNodes()){
					Long id=new Long(n.id);
					if ((!wayNodes.containsKey(id)) && !n.used){
						wayNodes.put(id, n);
					}

				}
			}
		}

		// create a byte arrayStream which holds the Singeltile-Data
		// this is created in memory and written later if file is 
		// not to big.
		ByteArrayOutputStream fo = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(fo);
		ds.writeByte(0x54); // Magic number
		ds.writeShort(interestNodes.size()+wayNodes.size());
		ds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds,INODE);
		}
		for (Node n : wayNodes.values()) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds,SEGNODE);
		}
		ds.writeByte(0x55); // Magic number
		ds.writeByte(ways.size());
		for (Way w : ways){
			w.write(ds, names1);
		}
		ds.writeByte(0x56); // Magic number
		fo.close();
		return fo.toByteArray();
	}
	
	private void writeRouteNode(Node n,DataOutputStream nds,DataOutputStream cds) throws IOException{
		nds.writeByte(4);
		nds.writeFloat(MyMath.degToRad(n.lat));
		nds.writeFloat(MyMath.degToRad(n.lon));
		nds.writeInt(cds.size());
		nds.writeByte(n.routeNode.connected.size());
		for (Connection c : n.routeNode.connected){
			cds.writeInt(c.to.node.renumberdId);
			cds.writeShort((int) c.time);
			cds.writeShort((int) c.length);
			cds.writeByte(c.startBearing);
			cds.writeByte(c.endBearing);
		}
	}

	private void writeNode(Node n,DataOutputStream ds,int type) throws IOException{
		// flags
		// 1 : 1=routeNodelink 0=mapNode
		// 2 : 1=has Name
		// 4 : 1=routeNode
		// 8 : free
		int flags=0;
		if (n.routeNode != null){
			flags += Constants.NODE_MASK_ROUTENODELINK;
		}
		if (type == INODE){
			if (! "".equals(n.getName())){
				flags += Constants.NODE_MASK_NAME;
			}
			if (n.getType(configuration) != 0){
				flags += Constants.NODE_MASK_TYPE;
			}
		}
		ds.writeByte(flags);
		if ((flags & Constants.NODE_MASK_ROUTENODELINK) > 0){
			ds.writeShort(n.routeNode.id);
			ds.writeFloat(MyMath.degToRad(n.lat));
			ds.writeFloat(MyMath.degToRad(n.lon));
		} else {
			ds.writeFloat(MyMath.degToRad(n.lat));
			ds.writeFloat(MyMath.degToRad(n.lon));
		}

		if ((flags & Constants.NODE_MASK_NAME) > 0){
			String name = n.getName();
			ds.writeShort(names1.getNameIdx(name));
		}
		if ((flags & Constants.NODE_MASK_TYPE) > 0){
			ds.writeByte(n.getType(configuration));
		}

	}



	/**
	 * @param c
	 */
	public void setConfiguration(Configuration c) {
		this.configuration = c;
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param rd
	 */
	public void setRouteData(RouteData rd) {
		this.rd = rd;
	}

}

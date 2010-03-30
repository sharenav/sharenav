package de.ueller.osmToGpsMid.area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ueller.osmToGpsMid.model.Bounds;

public class Area {
	private ArrayList<Outline>	outlineList	= new ArrayList<Outline>();
	private ArrayList<Outline>	holeList	= new ArrayList<Outline>();
	Outline						outline		= new Outline();
	public Triangle			triangle;
	ArrayList<Triangle> triangleList=null;
	static DebugViewer			viewer		= null;
	public Vertex	edgeInside;
	public  boolean debug=false;

	public Area() {
	}
	

	public void addOutline(Outline p) {
		if (p.isValid()){
			outlineList.add(p);
			p.calcNextPrev();
		}
	}

	public void addHole(Outline p) {
		if (p.isValid()){
			holeList.add(p);
		}
	}

	public void clean() {
		outlineList = new ArrayList<Outline>();
		holeList = new ArrayList<Outline>();
		// tri= new ArrayList<Triangle>();
		outline = new Outline();
	}

	public List<Triangle> triangulate() {
		if (debug) {
		if (viewer == null){
			viewer=new DebugViewer(this);
		} else {
			viewer.setArea(this);
		}
		}

		int dir = 0;
		ArrayList<Triangle> ret = new ArrayList<Triangle>();
		triangleList=ret;
		repaint();
		int loop = 0;
		while (outlineList.size() > 0) {
			outline = outlineList.get(0);
			if (! outline.isValid()){
				outlineList.remove(0);
				continue;
			}
			outline.calcNextPrev();
			outlineList.remove(0);
			while (outline.vertexCount() > 2) {
				loop++;
				if (loop > 10000) {
					System.err.println("Break because of infinite loop for outline " + outline.getWayId());
					System.err.println(" see http://www.openstreetmap.org/?way="+outline.getWayId());
					break;
				}
				ret.add(cutOneEar(outline, holeList, dir));
				dir = (dir + 1) % 4;
			}
		}
		// System.out.println(ret);
		// System.out.println("loops :" + loop);
//		optimize();
		return ret;

	}
	private void optimize(){
		for (Triangle t:triangleList){
			t.opt=false;
		}
		while (true){
			for (Triangle t1:triangleList){
				if (! t1.opt){
					for (Triangle t2:triangleList){
						if (t1.equalVert(t2) == 2){
							optimize(t1,t2);
						}
					}
				}
			}
		}
	}

	/**
	 * @param t1
	 * @param t2
	 */
	private void optimize(Triangle t1, Triangle t2) {
		
	}

	/**
	 * 
	 */
	private void repaint() {
		if (debug) {
			if (viewer == null){
				viewer=DebugViewer.getInstanz(this);
			} else {
				viewer.setArea(this);
			}


		if (viewer != null){
		viewer.repaint();
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			
		}
//		System.out.println("Area.repaint()");
		}
		}
	}

	private Triangle cutOneEar(Outline outline, ArrayList<Outline> holeList, int dir) {
		List<Vertex> orderedOutline = outline.getOrdered(dir);
		while (true) {
			Vertex n = orderedOutline.get(0);
			triangle = new Triangle(n, n.getNext(), n.getPrev());
			edgeInside = findEdgeInside(outline, triangle,dir);
			repaint();
			if (edgeInside == null) {
				// this is an ear with nothing in it so cut the ear
				outline.remove(n);
				return triangle;
			} else {
				boolean handeld=false;
				// at least one edge is inside this ear
				if (edgeInside.partOf(outline)) {
					handeld=true;
					// node of the outline is in the ear so we have to cut the outline into two parts
					// one will handled now and the other goes to the stack
					outline.clean();
					Vertex nt = n;
					// create a fresh copy of the old outline starting from outer edge of the expected ear
					while (nt != edgeInside) {
						outline.append(nt);
						nt = nt.getNext();
					}
					// go ahead the edge that was found inside the test triangle
					outline.append(edgeInside);
					Outline newOutline = new Outline();
					newOutline.setWayId(outline.getWayId());
					while (nt != n) {
						newOutline.append(nt);
						nt = nt.getNext();
					}
					newOutline.append(n);
					if (newOutline.isValid()){
						addOutline(newOutline);
					}
					// reinititalisize outline;
					outline.calcNextPrev();
					orderedOutline = outline.getOrdered(dir);
				} else {
					for (Outline p : holeList) {
						if (edgeInside.partOf(p)) {
							// now we have an edge of a hole inside the rectangle
							// lets join the hole with the outline and have a next try
							Outline hole = edgeInside.getOutline();
							hole.calcNextPrev();
							repaint();
//							Outline newOutline = new Outline();
							Vertex nt = n;
							boolean clockWise = outline.isClockWise();
							outline.clean();
							do {
								outline.append(nt);
								if (clockWise) {
									nt = nt.getNext();
								} else {
									nt = nt.getPrev();
								}
							} while (nt != n);
							repaint();
							outline.append(n.clone());
							repaint();
							nt = edgeInside;
							clockWise = hole.isClockWise();
							do {
								outline.append(nt);
								if (clockWise) {
									nt = nt.getPrev();
								} else {
									nt = nt.getNext();
								}
								repaint();
							} while (nt != edgeInside);
							outline.append(edgeInside.clone());
							holeList.remove(hole);
							outline.calcNextPrev();
							orderedOutline = outline.getOrdered(dir);
							handeld=true;
							break; // we found the hole so break this for loop
						}
					}
					if (!handeld){
					System.err.println("someting strange happens, there is an edge inside, but the member outline "+edgeInside.getOutline().getWayId()+" wasn't found");
					System.err.println(" see http://www.openstreetmap.org/?node="+edgeInside.getId());
//					debug=true;
//					repaint();
					return triangle;
					}
				}
			}
		}
	}

//	private Vertex findEdgeInside(Outline outline, Triangle triangle) {
//		Vertex leftmost = null;
//		Vertex n = outline.findVertexInside(triangle);
//		if (leftmost == null) {
//			leftmost = n;
//		} else {
//			if (n.getX() < leftmost.getX()) {
//				leftmost = n;
//			}
//		}
//		for (Outline p : holeList) {
//			n = p.findVertexInside(triangle);
//			if (leftmost == null) {
//				leftmost = n;
//			} else {
//				if (n != null && n.getX() < leftmost.getX()) {
//					leftmost = n;
//				}
//			}
//		}
//		return leftmost;
//	}
	private Vertex findEdgeInside(Outline outline, Triangle triangle, int dir) {
		ArrayList<Vertex> ret;
		ret = outline.findVertexInside(triangle);
		for (Outline p : holeList) {
			ret.addAll(p.findVertexInside(triangle));
		}
		if (ret.size() == 0){
			return null;
		}
		Collections.sort(ret, new DirectionComperator(dir));
		return ret.get(0);
	}
	
	public Bounds extendBounds(Bounds b){
		if (b==null){
			b=new Bounds();
		}
		for (Outline o:outlineList){
			o.extendBounds(b);
		}
		for (Outline o:holeList){
			o.extendBounds(b);
		}
		if (triangleList != null){
			for (Triangle t: triangleList){
				t.extendBound(b);
			}
		}

		return b;
	}
	
	public ArrayList<Outline> getOutlineList() {
		return outlineList;
	}

	public ArrayList<Outline> getHoleList() {
		return holeList;
	}


}

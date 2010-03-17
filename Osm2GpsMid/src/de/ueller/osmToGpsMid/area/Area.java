package de.ueller.osmToGpsMid.area;



import java.util.ArrayList;
import java.util.List;



public class Area  { 
	private List<Outline> outlineList = new ArrayList<Outline>();
	private List<Outline> holeList = new ArrayList<Outline>();
	Outline outline=new Outline();
	private Triangle	triangle;
	public Area() {

	}
	
	public void addOutline(Outline p){
		outlineList.add(p);
	}
	public void addHole(Outline p){
		holeList.add(p);
	}
	
	public void clean(){
		outlineList = new ArrayList<Outline>();
		holeList = new ArrayList<Outline>();
//		tri= new ArrayList<Triangle>();
		outline=new Outline();	
	}

	public List<Triangle> triangulate() {

		ArrayList<Triangle> ret = new ArrayList<Triangle>();
int loop=0;
		while (outlineList.size() > 0) {
			outline = outlineList.get(0);
			outline.CalcNextPrev();
			outlineList.remove(0);
			List<Vertex> orderedOutline = outline.getLonOrdered();
			while (outline.vertexCount() > 2) {
				loop++;
				if (loop > 10000){
					System.err.println("Break because of infinite loop");
					break;
				}
				Vertex n = orderedOutline.get(0);
				triangle = new Triangle(n, n.getNext(), n.getPrev());
				Vertex edgeInside = findEdgeInside(outline,triangle);
				if (edgeInside == null) {
					// this is an ear with nothing in it so cut the ear
					ret.add(triangle);
//					tri=ret;
					outline.remove(n);
				} else {
					// at leased one edge is inside this ear
					if (edgeInside.partOf(outline)) {
						// node of the outline is in the ear so we have to cut the outline into two parts
						// one will handled now and the other goes to the stack
						outline=new Outline();
						Vertex nt=n;
						while (nt != edgeInside){
							outline.append(nt);
							nt=nt.getNext();
						}
						outline.append(edgeInside);
						Outline newOutline=new Outline();
						while (nt != n){
							newOutline.append(nt);
							nt=nt.getNext();
						}
						newOutline.append(n);
						addOutline(newOutline);
						//reinititalisize outline;
						outline.CalcNextPrev();
						orderedOutline = outline.getLonOrdered();
					} else {
						for (Outline p : holeList) {
							if (edgeInside.partOf(p)) {
								// now we have an edge of a hole inside the rectangle
								// lets join the hole with the outline and have a next try
								Outline hole = edgeInside.getOutline();
								hole.CalcNextPrev();
								Outline newOutline=new Outline();
								Vertex nt=n;
								boolean clockWise = outline.isClockWise();
								do {
									newOutline.append(nt);
									if (clockWise){
										nt=nt.getNext();
									} else {
										nt=nt.getPrev();
									}
								} while (nt != n);
								newOutline.append(n.clone());
								nt=edgeInside;
								clockWise = hole.isClockWise();
								do {
									newOutline.append(nt);
									if (clockWise){
										nt=nt.getPrev();
									} else {
										nt=nt.getNext();
									}
								} while (nt != edgeInside);
								newOutline.append(edgeInside.clone());
								holeList.remove(hole);
								outline=newOutline;
								outline.CalcNextPrev();
								orderedOutline = outline.getLonOrdered();
								break; // we found tho hole so break this for loop
							}
						}
					}
				}
			}
		}
		System.out.println(ret);
		System.out.println("loops :" + loop);
		return ret;
		
	}

	private Vertex findEdgeInside(Outline outline, Triangle triangle) {
		Vertex leftmost = null;
		Vertex n = outline.findVertexInside(triangle);
		if (leftmost == null) {
			leftmost = n;
		} else {
			if (n.getX() < leftmost.getX()) {
				leftmost = n;
			}
		}
		for (Outline p : holeList) {
			n = p.findVertexInside(triangle);
			if (leftmost == null) {
				leftmost = n;
			} else {
				if (n!=null && n.getX() < leftmost.getX()) {
					leftmost = n;
				}
			}
		}
		return leftmost;
	}	
	

	
}

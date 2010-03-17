package de.ueller.osmToGpsMid.area;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Outline {
	private ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
	private ArrayList<Vertex> ordered;
	
	public void append(Vertex v){
		vertexList.add(v);
		v.setOutline(this);
	}
	
	public void CalcNextPrev(){
		Vertex prev=null;
		Vertex first=null;
		for (Vertex v:vertexList){
			if (first==null){
				first=v;
				prev=v;
			} else {
				v.setPrev(prev);
				prev.setNext(v);
				prev=v;
			}
			
		}
		first.setPrev(prev);
		prev.setNext(first);
	}
	
	@SuppressWarnings("unchecked")
	public List<Vertex> getLonOrdered(){
		ordered=(ArrayList<Vertex>) vertexList.clone();
		Collections.sort(ordered, new LonComperator());
		return ordered;
	}
	
	public Vertex findVertexInside(Triangle triangle) {
		float leftmost=Float.MAX_VALUE;
		Vertex leftmostNode=null;
		for (Vertex v:vertexList){
			if (triangle.isVertexInside(v)){
				float lon=v.getX();
				if (lon < leftmost){
					leftmost=lon;
					leftmostNode=v;
				}
			}
		}
		return leftmostNode;
	}

	public void remove(Vertex v) {
		v.getPrev().setNext(v.getNext());
		v.getNext().setPrev(v.getPrev());
		ordered.remove(v);
		vertexList.remove(v);
		
	}
	
	public int vertexCount(){
		return vertexList.size();
	}
	
	@Override
	public String toString() {
		StringBuffer b=new StringBuffer();
		for (Vertex n:vertexList){
			b.append(n);
		}
		return b.toString();
	}
	
	public boolean isClockWise(){
		boolean cw=isClockWise2();
		if (cw != isClockWise1()){
			System.out.println("not the same");
		}
		return cw;
	}
	
	public boolean isClockWise2(){
		float z=0.0f;
		for (Vertex i:vertexList){
			z+=i.cross(i.getNext());
		}
		if (z<0) return true; else return false;
	}
	
	public boolean isClockWise1(){
		Vertex j, k;
		int count = 0;
		double z;

		if (vertexCount() < 3) {
		   throw new IllegalArgumentException("polygone with < then 3 nodes is degenerated");
		}
		for (Vertex i:vertexList){
		   j=i.getNext();
		   k=j.getNext();
			z = (j.getX() - i.getX()) * (k.getY() - j.getY()) - (j.getY() - i.getY()) * (k.getX() - j.getX());
			if (z < 0) {
				count--;
			} else if (z > 0) {
				count++;
			}
		}
		if (count > 0) {
			return true;
		} else if (count < 0) {
			return false;
		} else {
			System.err.println("Triangulation Error! this should never happen");
//			 throw new IllegalArgumentException("this should never happen");
			return true;
		}
	}
}

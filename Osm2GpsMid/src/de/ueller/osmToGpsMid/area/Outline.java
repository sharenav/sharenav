package de.ueller.osmToGpsMid.area;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ueller.osmToGpsMid.model.Bounds;

public class Outline {
	private ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
//	private ArrayList<Vertex> ordered;
	
	public ArrayList<Vertex> getVertexList() {
		return vertexList;
	}

	public void clean(){
		vertexList = new ArrayList<Vertex>();
	}
	
	public void append(Vertex v){
		vertexList.add(v);
		v.setOutline(this);
	}
	
	public void CalcNextPrev(){
		Vertex prev=null;
		Vertex first=null;
		first=vertexList.get(0);
		prev=vertexList.get(vertexList.size()-1);
		if (first.equals(prev)){
			vertexList.remove(vertexList.size()-1);
		}
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
		ArrayList<Vertex> ordered=(ArrayList<Vertex>) vertexList.clone();
		Collections.sort(ordered, new LonComperator());
		return ordered;
	}
	@SuppressWarnings("unchecked")
	public List<Vertex> getOrdered(int dir){
//		return getLonOrdered();
		ArrayList<Vertex> ordered=(ArrayList<Vertex>) vertexList.clone();
		Collections.sort(ordered, new DirectionComperator(dir));
		return ordered;
	}
	
//	public Vertex findVertexInside(Triangle triangle) {
//		float leftmost=Float.MAX_VALUE;
//		Vertex leftmostNode=null;
//		for (Vertex v:vertexList){
//			if (triangle.isVertexInside(v)){
//				float lon=v.getX();
//				if (lon < leftmost){
//					leftmost=lon;
//					leftmostNode=v;
//				}
//			}
//		}
//		return leftmostNode;
//	}

	public ArrayList<Vertex> findVertexInside(Triangle triangle) {
		ArrayList<Vertex> ret=new ArrayList<Vertex>();

		for (Vertex v:vertexList){
			if (triangle.isVertexInside(v)){
				ret.add(v);
			}
		}
		return ret;
	}

	public void remove(Vertex v) {
		v.getPrev().setNext(v.getNext());
		v.getNext().setPrev(v.getPrev());
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
		boolean cw=isClockWise3();
		if (cw != isClockWise2()){
			System.out.println("2 and 3 not the same");
		}
		return cw;
	}
	
	public boolean isClockWise3(){
		CalcNextPrev();
		Vertex v=getLonOrdered().get(0);
		Vertex vp=v.getPrev();
		Vertex vn=v.getNext();
		if (((v.getX()-vp.getX())*(vn.getY()-v.getY())-(v.getY()-vp.getY())*(vn.getX()-v.getX())) <0  ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isClockWise2(){
		float z=0.0f;
		for (Vertex i : vertexList) {
//		for (int l=0;l<(vertexList.size()-1);l++){
//			Vertex i=vertexList.get(l);
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
	
	public Bounds extendBounds(Bounds b){
		for (Vertex i:vertexList){
			i.extendBounds(b);
		}
		return b;
	}
}

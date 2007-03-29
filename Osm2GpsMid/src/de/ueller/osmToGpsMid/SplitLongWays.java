package de.ueller.osmToGpsMid;

import java.util.LinkedList;
import java.util.List;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;


public class SplitLongWays {
	OxParser parser;
	LinkedList<Way> added=new LinkedList<Way>();
	LinkedList<Way> deleted=new LinkedList<Way>();
	
	
	public SplitLongWays(OxParser parser) {
		super();
		this.parser = parser;
		for (Way way : parser.ways) {
			boolean valid=false;
			for (Line line : way.lines){
				if (line.isValid())
					valid=true;
			}
			if (! valid){
				deleted.add(way);
			}
		}
		for (Way w : deleted) {
			parser.ways.remove(w);
		}
		deleted=null;
		for (Way way : parser.ways) {
			testAndSplit(way,true);
			testAndSplit(way,false);
		}
		for (Way w : added) {
			parser.ways.add(w);
		}
		added=null;
	}

	private void testAndSplit(Way way,boolean nonCont) {
		if (way.lines.size() == 1) return;
		if (way.getType() >= 50) return;
		Bounds b=way.getBounds();
		if ((b.maxLat-b.minLat) > 0.04f 
				|| (b.maxLon-b.minLon) > 0.04f ){
			if (nonCont){
				Way newWay=splitOnNonContinues(way);
				if (newWay != null){
					added.add(newWay);
					testAndSplit(way,true);
					testAndSplit(newWay,true);
				} 
			} else {
				if (way.lines.size() > 1) {
					Way newWay=splitOnHalf(way);
					if (newWay != null){
						added.add(newWay);
						testAndSplit(way,false);
						testAndSplit(newWay,false);
					} 
				}
			}
		}
	}
	private Way splitOnNonContinues(Way w){
		System.out.print("split way nonCont " + w.getName());
		Node p1=null;
		LinkedList<Line> splitList=new LinkedList<Line>();
		for (Line l : w.lines) {
			if (p1==null){
				p1=l.from;
			}
			if (p1 != l.from){
				splitList.add(l);
				p1=null;
			}
		}
		if (splitList.size() == 0) {
			System.out.println(" not Splitted");
			return null;
		} else {
			int splitp=splitList.size()/2;
			Line splitLine=splitList.get(splitp);
			Way wSplit = splitBeforeLine(w, splitLine);
			System.out.println(" Splitted");
			return wSplit;
		}
	}
	
	private Way splitOnHalf(Way w){
		System.out.println("split way half " + w.getName());
		int splitp=w.lines.size()/2;
		Line splitLine=w.lines.get(splitp);
		return splitBeforeLine(w, splitLine);
	}

	private Way splitBeforeLine(Way w, Line splitLine) {
		Way wSplit=new Way(w.id);
		wSplit.tags=w.tags;
		List<Line> oldLines=w.lines;
		wSplit.lines=new LinkedList<Line>();
		w.lines=new LinkedList<Line>();
		boolean orgLine=true;
		for (Line l : oldLines) {
			if (orgLine == true && l != splitLine){
				w.lines.add(l);
			} else {
				orgLine=false;
				wSplit.lines.add(l);
			}
		}
		w.clearBounds();
		System.out.println(" after Split w1=" + w.lines.size() + " lines w2="+wSplit.lines.size()+" lines");
		return wSplit;
	}
	
}

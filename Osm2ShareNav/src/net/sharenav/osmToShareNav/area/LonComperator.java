package net.sharenav.osmToShareNav.area;

import java.util.Comparator;


public class LonComperator implements Comparator<Vertex> {


	@Override
	public int compare(Vertex o1, Vertex o2) {
		if (o1.getX() > o2.getX()) return 1;
		else if (o1.getX() < o2.getX()) return -1;
		else return 0;
	}

}

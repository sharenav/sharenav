package net.sharenav.osmToShareNav.area;

import java.util.Comparator;

public class DirectionComperator2 implements Comparator<Vertex> {

	public DirectionComperator2() {
		super();
	}

	@Override
	public int compare(Vertex o1, Vertex o2) {
		float diff = -o1.getX() + o2.getX();
		if (diff > 0.0f)
			return 1;
		else if (diff < 0.0f)
			return -1;
		else
			return 0;
	}

}

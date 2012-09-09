package net.sharenav.osmToShareNav.area;

import java.util.Comparator;

public class DirectionComperator1 implements Comparator<Vertex> {

	public DirectionComperator1() {
		super();
	}

	@Override
	public int compare(Vertex o1, Vertex o2) {
		float diff = o1.getY() - o2.getY();
		if (diff > 0.0f)
			return 1;
		else if (diff < 0.0f)
			return -1;
		else
			return 0;
	}

}

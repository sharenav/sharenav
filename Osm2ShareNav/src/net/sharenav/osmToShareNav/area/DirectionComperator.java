package net.sharenav.osmToShareNav.area;

import java.util.Comparator;

public class DirectionComperator implements Comparator<Vertex> {

	int	dir;

	public DirectionComperator(int dir) {
		super();
		this.dir = dir;
	}

	@Override
	public int compare(Vertex o1, Vertex o2) {
		float v1;
		float v2;
		switch (dir) {
			case 0:
				v1 = o1.getX();
				v2 = o2.getX();
				break;
			case 1:
				v1 = o1.getY();
				v2 = o2.getY();
				break;
			case 2:
				v1 = -o1.getX();
				v2 = -o2.getX();
				break;
			default:
				v1 = -o1.getY();
				v2 = -o2.getY();
				break;
		}
		if (v1 > v2)
			return 1;
		else if (v1 < v2)
			return -1;
		else
			return 0;
	}

}

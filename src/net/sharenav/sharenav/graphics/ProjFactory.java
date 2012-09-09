/*
 * ShareNav - Copyright (c) 2008 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.graphics;

import de.enough.polish.util.Locale;
import net.sharenav.gps.Node;
import net.sharenav.sharenav.data.Configuration;

public final class ProjFactory {

	public static final byte NORTH_UP = 0;
	public static final byte MOVE_UP = 1;
	public static final byte MOVE_UP_ENH = 2;
	public static final byte EAGLE = 3;
	public static final byte COUNT = 4;
	public static byte type = NORTH_UP;

	public static Projection getInstance(Node center, int upDir, float scale,
			int width, int height) {
		upDir = upDir % 360;
		switch (type) {
		case NORTH_UP:
			return new Proj2D(center, scale, width, height);
		case MOVE_UP:
			return new Proj2DMoveUp(center, upDir, scale, width, height);
		case MOVE_UP_ENH:
			return new Proj2DEnh(center, upDir, scale, width, height);
		case EAGLE:
			return new Proj3D(center, upDir, scale, width, height);
		}
		return new Proj2D(center, scale, width, height);
	}

	public static void setProj(byte t) {
		type = t;
	}

	public static byte getProj() {
		return type;
	}

	public static String nextProj() {
		type = (byte)((type + 1) % 4);
		return Configuration.projectionsString[type];
	}

}

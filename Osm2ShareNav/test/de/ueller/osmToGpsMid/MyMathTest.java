/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav;

import net.sharenav.osmToShareNav.model.Node;
import junit.framework.TestCase;

/**
 * @author hmueller
 *
 */
public class MyMathTest extends TestCase {

	/**
	 * @param name
	 */
	public MyMathTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

//	public void testSpherical_distance(){
//		float lat=(float)Math.toRadians(45.0);
//		float c;
//		c=MyMath.spherical_distance(0f, 0f, (float)Math.PI, 0f);
//		System.out.println(c);
//		c=MyMath.spherical_distance(0f, 0f, 0f,(float)Math.PI);
//		System.out.println(c);
//		c=MyMath.spherical_distance(lat, 0f, lat,(float)Math.PI);
//		System.out.println(c);
//	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.MyMath#dist(net.sharenav.osmToShareNav.model.Node, net.sharenav.osmToShareNav.model.Node)}.
	 */
	public void testDist() {
		Node northpole = new Node(90f, 0f, 0);
		Node equator0 = new Node(0f, 0f, 0);
		Node gsteinach = new Node(49.3519420165977f, 11.224289273197f,0);
		Node ezelsdorf = new Node(49.3278456097341f, 11.3470017176587f,0);
		Node nuernberg = new Node(49.4473022343456f, 11.0877756401478f,0);
		Node hamburg = new Node(53.5500018787512f, 10.0000116163133f,0);
		Node wuerzburg = new Node(49.7909141367451f, 9.97069016105521f,0);
		System.out.println("Gsteinach - Ezelsdorf " + (MyMath.dist(gsteinach, ezelsdorf) / 10000f) + " km");
		System.out.println("Nürnberg - Ezelsdorf " + (MyMath.dist(nuernberg, ezelsdorf) / 10000f) + " km");
		System.out.println("Nürnberg - Hamburg " + (MyMath.dist(nuernberg, hamburg) / 10000f) + " km");
		System.out.println("Nürnberg - Würzburg " + (MyMath.dist(nuernberg, wuerzburg) / 10000f) + " km");
		System.out.println("North pole - Equator0 " + (MyMath.dist(northpole, equator0) / 10000f) + " km");
	}

	public void testInverseBearing(){
		System.out.println("Inverse of 120 " + (MyMath.inverseBearing((byte) 60))*2);
	}

}

/**
 * 
 */
package de.ueller.midlet.gps.tile;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author hmueller
 *
 */
public class NamesTest extends TestCase {
	Names n;
	/**
	 * @param name
	 */
	public NamesTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		n=new Names();
		synchronized (this) {
			while (!n.isReady()){
				
			}
		}
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for {@link de.ueller.midlet.gps.tile.Names#getName(java.lang.Short)}.
	 */
	public void testGetName() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link de.ueller.midlet.gps.tile.Names#search(java.lang.String)}.
	 */
	public void testSearch() {
		try {
			Short[] list=n.search("6862");
			for (int i=0; i<list.length;i++){
				if (list[i] != null){
//					System.out.println(n.getName(list[i]));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

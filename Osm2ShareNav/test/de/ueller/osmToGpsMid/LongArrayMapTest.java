/**
 * OSM2ShareNav 
 *  
 * Copyright (C) 2008 Kai Krueger
 */
package net.sharenav.osmToShareNav;

import java.util.Iterator;
import java.util.Random;

import junit.framework.TestCase;
import net.sharenav.osmToShareNav.LongArrayMap;

/**
 *
 *
 */
public class LongArrayMapTest extends TestCase {
	
	private long [] entries;

	/**
	 * @param name
	 */
	public LongArrayMapTest(String name) {
		super(name);		
	}
	
	private void genEntries() {
		Random r = new Random();
		entries = new long [5000];
		int idx = 0;
		long val = r.nextInt();
		for (int i = 0; i < 50; i++) {
			val += (r.nextInt(10000) + 1);
			entries[idx++] = val++;
			for (int j = 0; j < 99; j++) {
				entries[idx++] = val++;
			}
		}
	}
	
	private LongArrayMap<Long, Long> populateLAM() {
		System.out.println("Populating LAM");
		LongArrayMap<Long, Long> lam = new LongArrayMap<Long, Long>(100);
		genEntries();
		for (int i = 0; i < entries.length; i++) {			
			try {
				lam.put(new Long(entries[i]), new Long(entries[i]));
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed to populate entry " + entries[i]);
			}
		}
		return lam;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();		
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#put(java.lang.Object, java.lang.Object)}.
	 */
	public void testPut() {
		System.out.println("Testing Put");
		LongArrayMap<Long,Long> lam = new LongArrayMap<Long,Long>(100);
		genEntries();
		for (int i = 0; i < entries.length; i++) {			
			try {
				lam.put(new Long(entries[i]), new Long(entries[i]));
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed to put entry " + entries[i]);
			}
		}		
	}
	
	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#get(java.lang.Object)}.
	 */
	public void testGet() {
		System.out.println("Testing Get");
		LongArrayMap<Long,Long> lam = populateLAM();		
		System.out.println("Testing getting of existing objects");
		for (int i = 0; i < entries.length; i++) {
			Long tmp = lam.get(new Long(entries[i]));
			assertNotNull("Failed to fetch an object that should be in the Map (idx=" + i + "," + entries[i] + ")", tmp);
			assertEquals(entries[i], tmp.longValue());			
		}
		System.out.println("Testing getting of nonexisting objects");
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			long testV = r.nextLong();			
			Long tmp = lam.get(new Long(testV));
			if (tmp != null) {
				System.out.println("Testing getting of nonexisting objects: Object found");
				boolean expected = false;
				for (long l : entries) {					
					if (l == testV)
						expected = true;
				}
				assertTrue(expected);				
			}
		}
	}



	
	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#containsKey(java.lang.Object)}.
	 */
	public void testContainsKey() {
		System.out.println("Testing Contains Key");
		LongArrayMap<Long,Long> lam = populateLAM();		
		System.out.println("Testing of existing objects");
		for (int i = 0; i < entries.length; i++) {
			boolean tmp = lam.containsKey(new Long(entries[i]));
			assertTrue("ContainsKey falsely said a value is not in the Map (idx=" + i + "," + entries[i] + ")" , tmp);			
		}
		System.out.println("Testing of nonexisting objects");
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			long testV = r.nextLong();			
			boolean tmp = lam.containsKey(new Long(testV));
			if (tmp != false) {
				boolean expected = false;
				for (long l : entries) {					
					if (l == testV)
						expected = true;
				}
				assertEquals("Testing non-keys", expected, tmp);
			}
		}
	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#containsValue(java.lang.Object)}.
	 */
	public void testContainsValue() {
		System.out.println("Testing Contains Value");
		LongArrayMap<Long,Long> lam = new LongArrayMap<Long,Long>(100);
		genEntries();
		Long [] values = new Long[entries.length];
		for (int i = 0; i < entries.length; i++) {
			values[i] = new Long(entries[i]);
			lam.put(new Long(entries[i]), values[i]);
		}
		System.out.println("Testing of existing objects");
		for (int i = 0; i < entries.length; i++) {
			boolean tmp = lam.containsValue(values[i]);
			assertTrue("ContainsValue falsely said a value is not in the Map (" + entries[i] + ")" , tmp);			
		}
	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#isEmpty()}.
	 */
	public void testIsEmpty() {
		System.out.println("Testing isEmpty");
		LongArrayMap<Long,Long> lam = new LongArrayMap<Long,Long>(100);
		assertTrue("A new Map should be empty.", lam.isEmpty());
		genEntries();
		for (int i = 0; i < entries.length; i++) {
			lam.put(new Long(entries[i]), new Long(entries[i]));
		}
		assertFalse("After adding entries, a Map should not be empty", lam.isEmpty());
		
	}


	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#remove(java.lang.Object)}.
	 */
	public void testRemove() {
		System.out.println("Testing Remove");
		LongArrayMap<Long,Long> lam = populateLAM();
		Random r = new Random();		
		for (int i = 0; i < entries.length; i++) {
			if (r.nextInt(10) == 0) {
				assertTrue("Element was not in LAM before deleting", lam.containsKey(entries[i]));
				assertEquals("Failed to delete key (didn't return correct value)", entries[i], lam.remove(new Long(entries[i])).longValue());
				assertFalse("Element was still in LAM after deleting", lam.containsKey(entries[i]));
			}
		}		
	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#size()}.
	 */
	public void testSize() {
		System.out.println("Testing Size");
		LongArrayMap<Long,Long> lam = new LongArrayMap<Long,Long>(100);
		assertEquals(0,lam.size());
		genEntries();
		for (int i = 0; i < entries.length; i++) {
			lam.put(new Long(entries[i]), new Long(entries[i]));
		}
		assertEquals(entries.length,lam.size());
		lam.remove(new Long(entries[0]));
		assertEquals("Size was not decremented after delete", entries.length - 1, lam.size());
		
	}

	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#values()}.
	 */
	public void testValueIterator() {
		System.out.println("Testing Value iterator");
		LongArrayMap<Long,Long> lam = populateLAM();
		int idx = 0;
		for (Long l : lam.values()) {
			assertEquals("Iterator did not return the correct values for idx=" + idx, entries[idx++], l.longValue());
		}
		assertEquals("Iterator did not iterate through all values", entries.length, idx);
		idx = 0;
		for (Long l : lam.values()) {
			assertEquals("Iterator on repeat did not return the correct values for idx=" + idx, entries[idx++], l.longValue());
		}
		assertEquals("Iterator on repeat did not iterate through all values", entries.length, idx);
		
	}
	
	
	/**
	 * Test method for {@link net.sharenav.osmToShareNav.LongArrayMap#values()}.
	 */
	public void testValueIteratorRemove() {
		System.out.println("Testing Value iterator remove");
		LongArrayMap<Long,Long> lam = populateLAM();
		int idx = 0;
		Iterator<Long> it = lam.values().iterator();		
		while (it.hasNext()) {
			Long tmpV = it.next();
			assertEquals("Iterator did not return the correct values for idx=" + idx, entries[idx++], tmpV.longValue());
			assertNotNull("Object not deleted yet, should still be in LAM", lam.get(tmpV));
			it.remove();
			assertNull("Object was deleted, should not be in LAM anymore", lam.get(tmpV));
		}
		assertEquals("Iterator did not iterate through all values while deleting", entries.length,idx);
		
		lam = populateLAM();
		Random r = new Random();
		boolean [] removedIdx = new boolean[entries.length];
		for (int i = 0; i < entries.length; i++) {
			if (r.nextInt(10) == 0) {				
				assertTrue("Element was not in LAM before deleting", lam.containsKey(entries[i]));
				assertEquals("Failed to delete key (didn't return correct value)", entries[i], lam.remove(new Long(entries[i])).longValue());
				assertFalse("Element was still in LAM after deleting", lam.containsKey(entries[i]));
				removedIdx[i] = true;
			} else {
				removedIdx[i] = false;
			}			
		}
		for (int i = 0; i < entries.length; i++) {
			Long l = lam.get(new Long(entries[i]));
			assertEquals("Object was incorrectly removed", removedIdx[i], l == null);
			if (l != null)
				assertEquals("Key value pair does not match up", entries[i],l.longValue());
		}
		idx = 0;
		for (Long l : lam.values()) {
			while (removedIdx[idx] == true) idx++;
			assertEquals("Iterator isn't working after remove for idx=" + idx, entries[idx], l.longValue());
			idx++;
		}
	
	}

}

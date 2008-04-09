/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;
import java.util.Collection;



/**
 * @author kai
 * @param <K>
 *
 */
public class LongTri<V>  {
	
	private int size;
	private final int levelSize = 4;
	private final int level2Size = 16;
	private final long mask = 0x0f;
	
	Object[] topLevel = new Object[(level2Size)];
	
	public LongTri() {
		size = 0;		
	}
	public V get(long idx) {
		int ppidx = 64 - levelSize;
		Object[] p = topLevel;
		Object[] pn1 = null;
		int pidx; 
		while (ppidx > 0) {
			pidx = (int) ((idx >> ppidx) & mask);
			pn1 = (Object[])p[pidx];
			if (pn1 == null) {
				return null;				
			}
			ppidx -= levelSize;
			p = pn1;
		}
		pidx = (int)(idx & mask); 
		return (V)p[pidx];
	}
	
	public void put (long idx, V obj) {
		int ppidx = 64 - levelSize;
		Object[] p = topLevel;
		Object[] pn1 = null;
		int pidx; 
		while (ppidx > 0) {
			pidx = (int) ((idx >> ppidx) & mask);
			//System.out.println(pidx + " " + idx + " " + ppidx + " " + mask);
			pn1 = (Object[])p[pidx];
			if (pn1 == null) {
				pn1 = new Object[(level2Size)];
				//System.out.println("Array: " + p.length);
				p[pidx] = pn1;				
			}
			ppidx -= levelSize;
			p = pn1;
		}
		pidx = (int)(idx & mask);
		if (p[pidx] == null) size++;
		p[pidx] = obj;
	}
	
	private Collection<V> valuesLevel(Object[] level, Collection<V> values) {
		for (int i = 0; i < (level2Size); i++) {
			Object obj = level[i];
			if (obj == null) continue;
			if (!(obj instanceof Object[])) {
				values.add((V)obj);
			} else {
				values = valuesLevel((Object[])obj,values);
			}
		}
		return values;
	}
	
	public Collection<V> values() {
		Collection<V> col = new ArrayList<V>();		
		return valuesLevel(topLevel, col);		
	}
	
	public int size() {
		return size;
	}
	
	public void remove(long idx) {
		int ppidx = 64 - levelSize;
		Object[] p = topLevel;
		Object[] pn1 = null;
		int pidx; 
		while (ppidx > 0) {
			pidx = (int) ((idx >> ppidx) & mask);
			pn1 = (Object[])p[pidx];
			if (pn1 == null) {
				return;				
			}
			ppidx -= levelSize;
			p = pn1;
		}
		pidx = (int)(idx & mask);
		if (p[pidx] != null) size--;
		p[pidx] = null;
	}
}

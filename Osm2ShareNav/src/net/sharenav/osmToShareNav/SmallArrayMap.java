/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * See COPYING.
 *
 * Copyright (C) 2008  Kai Krueger
 */

package net.sharenav.osmToShareNav;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class implements a Map for small amounts of data by using an Array for 
 * internal storage. This is done to avoid the overhead of e.g. a HashMap.
 * Only put few elements in it, else the linear search through the elements will 
 * be too slow. 
 */
public class SmallArrayMap<K, V> implements Map<K, V> {

	protected  Object[] mapArray = null;
	
	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		mapArray = null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( mapArray == null ) {
			return false;
		}

		for (int i = 0; i < mapArray.length / 2; i++) {
			if (mapArray[2 * i].equals(key)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		if ( mapArray == null ) {
			return false;
		}
		for (int i = 0; i < mapArray.length / 2; i++) {
			if (mapArray[2 * i + 1].equals(value)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new Error("Method not implemented");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public V get(Object key) {
		if ( mapArray == null ) {
			return null;
		}
		for (int i = 0; i < mapArray.length / 2; i++) {
			if (mapArray[2 * i].equals(key)) {
				return (V)mapArray[2 * i + 1];
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		if ( mapArray == null ) {
			return true;
		}
		if (mapArray.length == 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<K> keySet() {
		HashSet<K> res = new HashSet<K>();

		if ( mapArray == null ) {
			return res;
		}

		for (int i = 0; i < mapArray.length / 2; i++) {
			res.add((K)mapArray[2 * i]);
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(K key, V value) {
		if ( mapArray == null ) {
			mapArray = new Object[0];
		}

		for (int i = 0; i < mapArray.length / 2; i++) {
			if (mapArray[2 * i].equals(key)) {
				V val = (V)mapArray[2 * i + 1];
				mapArray[2 * i + 1] = value;
				return val;
			}
		}

		Object [] tmp = new Object[mapArray.length + 2];
		System.arraycopy(mapArray, 0, tmp, 0, mapArray.length);
		tmp[mapArray.length] = key;
		tmp[mapArray.length + 1] = value;
		mapArray = tmp;

		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new Error("Method not yet implemented");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
		if ( mapArray == null ) {
			return null;
		}

		for (int i = 0; i < mapArray.length / 2; i++) {
			if (mapArray[2 * i].equals(key)) {
				V val = (V)mapArray[2 * i + 1];
				Object [] tmp = new Object[mapArray.length - 2];
				System.arraycopy(mapArray, 0, tmp, 0, 2 * i);
				// No second part to copy if hit is at the end of the array.
				if (2 * i + 2 < mapArray.length) {
					System.arraycopy(mapArray, 2 * i + 2, tmp, 2 * i, 
							mapArray.length - 2 * i - 2);
				}
				mapArray = tmp;
				return val;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		if (mapArray == null ) {
			return 0;
		}
		return mapArray.length / 2;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<V> values() {
		HashSet<V> res = new HashSet<V>();
		if (mapArray == null ) {
			return res;
		}
		for (int i = 0; i < mapArray.length / 2; i++) {
			res.add((V)mapArray[2 * i + 1]);
		}
		return res;
	}

}

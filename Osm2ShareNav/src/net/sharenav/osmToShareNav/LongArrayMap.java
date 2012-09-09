/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 * 
 */
package net.sharenav.osmToShareNav;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @param <K>
 * @param <V>
 *
 */
public class LongArrayMap<K extends Long, V> implements Map<Long, V> {
	
		class LongArrayMapValueSet implements Set<V> {

		/* (non-Javadoc)
		 * @see java.util.Set#add(java.lang.Object)
		 */
		
		private LongArrayMap<K,V> lam;	
		
		public LongArrayMapValueSet(LongArrayMap<K,V> lam) {
			this.lam = lam;
		}
		@Override
		public boolean add(Object e) {
			throw new Error("Function ValueSet.add is not implemented yet");			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#addAll(java.util.Collection)
		 */
		@Override
		public boolean addAll(Collection c) {
			throw new Error("Function ValueSet.addAll is not implemented yet");
		}

		/* (non-Javadoc)
		 * @see java.util.Set#clear()
		 */
		@Override
		public void clear() {
			throw new Error("Function ValueSet.clear is not implemented yet");
			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#contains(java.lang.Object)
		 */
		@Override
		public boolean contains(Object o) {
			throw new Error("Function ValueSet.contains is not implemented yet");			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#containsAll(java.util.Collection)
		 */
		@Override
		public boolean containsAll(Collection c) {
			throw new Error("Function ValueSet.containsAll is not implemented yet");
		}

		/* (non-Javadoc)
		 * @see java.util.Set#isEmpty()
		 */
		@Override
		public boolean isEmpty() {
			throw new Error("Function ValueSet.isEmpty is not implemented yet");
		}

		/* (non-Javadoc)
		 * @see java.util.Set#iterator()
		 */
		@Override
		public Iterator<V> iterator() {			
			return new Iterator<V>() {
				int entry = 0;
				int element = -1;				

				@Override
				public boolean hasNext() {
					//try {
					if ((lam.noEntries <= entry) ||
							((lam.noEntries -1 == entry) && (lam.valueTree[entry].size <= element)) ||
							(lam.noEntries <= (entry + 1)&&((lam.valueTree[entry].size <= element + 1))))
						return false;
					return true;
					/*} catch (NullPointerException npe) {
						System.out.println("entry " + entry + " element " + element + "noEntries " + noEntries);
						return false;
					}*/
				}

				@Override
				public V next() {
					element++;
					//try {
					if (lam.valueTree[entry].size <= element) {
						entry++;
						element = 0;
					}
					V o = (V)lam.valueTree[entry].values[element];
					return o;
					/*} catch (NullPointerException npe) {
						System.out.println("entry " + entry + " element " + element + "noEntries " + noEntries);
						return null;
					}*/					
				}

				@Override
				public void remove() {					
					lam.remove(lam.valueTree[entry].keys[element]);
					element--;
				}
				
			};			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#remove(java.lang.Object)
		 */
		@Override
		public boolean remove(Object o) {
			System.out.println("MapValueSet.remove");
			return false;
		}

		/* (non-Javadoc)
		 * @see java.util.Set#removeAll(java.util.Collection)
		 */
		@Override
		public boolean removeAll(Collection c) {
			throw new Error("Function ValueSet.removeAll is not implemented yet");			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#retainAll(java.util.Collection)
		 */
		@Override
		public boolean retainAll(Collection c) {
			throw new Error("Function ValueSet.retainAll is not implemented yet");
		}

		/* (non-Javadoc)
		 * @see java.util.Set#size()
		 */
		@Override
		public int size() {			
			return lam.size;
		}

		/* (non-Javadoc)
		 * @see java.util.Set#toArray()
		 */
		@Override
		public Object[] toArray() {
			throw new Error("Function ValueSet.toArray is not implemented yet");
		}

		/* (non-Javadoc)
		 * @see java.util.Set#toArray(T[])
		 */
		@Override
		public V[] toArray(Object[] a) {
			throw new Error("Function ValueSet.toArray2 is not implemented yet");
		}
		
	}
	
	private class LongArrayMapEntry<V> {
		long [] keys;
		Object[] values;
		long minKey, maxKey;
		int size;		
		
		public LongArrayMapEntry(int capacity) {
			keys = new long[capacity];
			values = new Object[capacity];
			for (int i = 0; i < keys.length; i++) {
				keys[i] = Long.MIN_VALUE;
			}
			size = 0;
		}
		
		/**
		 * 
		 * @param key
		 * @return index to where the key is stored. If the key is not part of the array, then return one plus the index of the next smallest key 
		 */
		private int key2idx(long key) {
			int minIdx = 0;
			int maxIdx = size - 1;
			if (size == 0)
				return 0;
			if (keys[maxIdx] < key) {
				return maxIdx + 1;
			}
			int pivot = (minIdx + maxIdx)/2;			
			while (keys[minIdx] < key) {
				pivot = (minIdx + maxIdx)/2;
				//System.out.println("Entry.key2idx(" + key + ") = (" + minIdx + "|" + pivot  + "|" + maxIdx + ")");
				if (keys[pivot] == key)
					return pivot;
				if (keys[pivot] > key) {
					maxIdx = pivot;
				} else {
					if (minIdx == pivot) {
						minIdx++;
					} else {
						minIdx = pivot;
					}
				}				
			}			
			if (keys[pivot] == key)
				return pivot;
			if (keys[maxIdx] == key)
				return maxIdx;
			else pivot = minIdx;
			//System.out.println("Entry.key2idx(" + key + ") = " + pivot + " size: " + size);
			return pivot;
		}
		
		public boolean containsKey(long key) {
			int idx = key2idx(key);
			if ((keys[idx] == key) && values[idx] != null) {
				return true;
			}
			return false;
		}
		
		public boolean containsValue(Object value) {
			for (int i = 0; i < size; i++) {
				if (values[i] == value)
					return true;
			}
			return false;
		}
		
		public V get(long key) {
			int idx = key2idx(key);
			if ((keys[idx] == key)) {
				return (V)values[idx];
			}
			return null;
		}
		
		public V put(long key, V value) {
			int idx = key2idx(key);
			if (values[idx] != null ) {
				
				if (keys[idx] == key) {
					//Replace value
					V oldValue = (V)values[idx]; 
					values[idx] = value;
					return oldValue;
				} else {
					if (size == keys.length) {
						System.out.println("Error: Inserting out of order too often!");
						System.exit(1);
						return null;						
					}
					System.arraycopy(keys, idx, keys, idx + 1, size - idx);
					System.arraycopy(values, idx, values, idx + 1, size - idx);
				}				
			}
			values[idx] = value;
			keys[idx] = key;
			size++;
			return null;
		}
		
		public boolean isFull() {			
			return ((size + 10 + (keys.length/1000) )> keys.length);
		}
		
		private void remove(int elementIdx) {
			System.arraycopy(keys, elementIdx + 1, keys, elementIdx, size - elementIdx);
			System.arraycopy(values, elementIdx + 1, values, elementIdx, size - elementIdx);
			size--;
		}
		
		private void remove(long elementKey) {			
			int idx = key2idx(elementKey);
			if (keys[idx] == elementKey) {							
				remove(idx);
			}
		}
		
	}
	
	long [] keyIdx;
	LongArrayMapEntry<V> [] valueTree;
	int size;
	int capPerEntry;
	int noEntries;

	public LongArrayMap(int capPerEntry) {
		this.capPerEntry = capPerEntry;
		clear();
	}
	
	private int key2idx(long key, boolean debug) {		
		int minIdx = 0;
		int maxIdx = noEntries - 1;		
		int pivot = (minIdx + maxIdx)/2;
		if (debug) 
			System.out.println("key2idx(" + key + ") = ? noEntires: " + noEntries);
		if (noEntries == 0)
			return -1;
		if (keyIdx[maxIdx] < key) {
			return maxIdx;
		}
		while (minIdx < maxIdx) {
			//System.out.println("key2idx(" + key + ") = " + pivot + " minIdx " + minIdx +  " maxIdx " + maxIdx);
			if (keyIdx[minIdx] == key) {
				//System.out.println("found it");
				return minIdx;
			}
			if (keyIdx[maxIdx] == key) {
				//System.out.println("found it");
				return maxIdx;
			}
			
			pivot = (minIdx + maxIdx)/2;
			if (keyIdx[pivot] > key) {				
				maxIdx = pivot;
			} else {
				if (minIdx == pivot) {
					
					minIdx = pivot + 1;
				} else {					
					minIdx = pivot;
				}
			}
		}
		pivot = (minIdx + maxIdx)/2;
		if (debug) {
		System.out.print("key2idx(" + key + ") = " + pivot + " maxIdx " + maxIdx +  " noEntires: " + noEntries);
		for (long i : keyIdx)
			System.out.print(" " + i + " ");
		System.out.println("");
		}
		return pivot;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		keyIdx = new long[100];
		valueTree = new LongArrayMapEntry[100];
		for (int i = 0; i < keyIdx.length; i++) {
			keyIdx[i] = Long.MAX_VALUE;
		}
		size = 0;
		noEntries = 0;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {		
		long keyLong;
		if (!(key instanceof Long))
			return false;
		keyLong = ((Long)key).longValue();		
		int idx = key2idx(keyLong, false);
		if (idx < 0) return false;
		return valueTree[idx].containsKey(keyLong);		
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		
		for (LongArrayMapEntry<V> entry : valueTree) {
			if (entry != null)
				if (entry.containsValue(value))
					return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<Long, V>> entrySet() {
		throw new Error("Function entrySet is not implemented yet");		
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public V get(Object key) {		
		long keyLong;
		if (!(key instanceof Long))
			return null;
		keyLong = ((Long)key).longValue();		
		return get(keyLong);
	}
	
	public V get(long keyLong) {
		int idx = key2idx(keyLong,false);
		if (idx < 0)
			return null;
		if (keyIdx[idx] < keyLong) {
			//System.out.println("Get key " + keyLong + " idx = " + idx + " " + keyIdx[idx]);
			//key2idx(keyLong,true);
		}
		if (idx < 0)
			return null;
		return valueTree[idx].get(keyLong);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {		
		return (size == 0);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<Long> keySet() {
		throw new Error("Function keySet is not implemented yet");		
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public V put(Long key, V value) {
		long keyLong;
		if (!(key instanceof Long))
			return null;
		keyLong = ((Long)key).longValue();
		int idx = key2idx(keyLong,false);
		if (idx == -1) {
			if (noEntries == 0) {
				valueTree[0] = new LongArrayMapEntry<V>(capPerEntry);
				noEntries++;
			}
			idx = noEntries - 1;
		}
		if (valueTree[idx].isFull()) {
			if (valueTree[idx + 1] == null) {
				//System.out.println("Is full");
				valueTree[idx + 1] = new LongArrayMapEntry<V>(capPerEntry);
				noEntries++;
				idx++;
			}
		}
		//System.out.println("Putting to idx " + idx);
		valueTree[idx].put(keyLong,value);
		
		if ((keyIdx[idx] <= keyLong) || (keyIdx[idx] == Long.MAX_VALUE)) {
			keyIdx[idx] = keyLong;
		}
		size++;
		//System.out.println("Key idx: " + keyIdx[idx]);
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends Long, ? extends V> m) {
		throw new Error("Function putAll is not implemented yet");		
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
		long keyLong;
		if (!(key instanceof Long))
			return null;
		keyLong = ((Long)key).longValue();
		int idx = key2idx(keyLong,false);
		if (idx < 0)
			return null;
		V obj = valueTree[idx].get(keyLong);
		if (obj != null) {
			valueTree[idx].remove(keyLong);
			size--;
		}
		return obj;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {		
		return size;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<V> values() {		
		return new LongArrayMapValueSet(this);
	}

}

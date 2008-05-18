package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
/**
 * This class implements a simple map using
 * a primitive int as its key. Needed to reimplement
 * this, as all the standard Java libraries seem to 
 * require an object as a key, waisting a lot of 
 * overhead in creating and storing "primitive objects"
 * 
 * This class only accepts positive keys
 *  
 * @author Kai Krueger
 * (c) 2008
 * see Copying
 *
 */
public class intTree  {
	/**
	 * keys are stored in ascending order. Empty entries are filled with -1
	 * therefore the array grows from the end downwards
	 */
	private int [] keys;
	private Object[] values;
	/**
	 * points to the first empty key. i.e, if tree grows, treeSize
	 * reduces in value, as the array is grown from the end.
	 * 
	 */
	private short treeSize = 0;
	//
	
	/**
	 * Cache the last looked up key value in case we rerequest it.
	 * This could save a binary search in the array. Not sure if this
	 * is worth the effort though.
	 */
	private int keyCache;
	private Object valueCache;
	
	public intTree() {
		removeAll();
	}
	
	private void clearCache() {
		keyCache = -1;
		valueCache = null;
	}
	
	/**
	 * Bisection search of the key array.
	 * @param key
	 * @return returns the position in the array at which the key was found.
	 *  If no key is found, returns the position at which it would be if it were in the array
	 *  
	 */
	private int bisect(int key) {		
		int rangeLow = 0;
		int rangeHigh = keys.length - 1;
		int pivot = 1;
		if (size() == 0)
			return 0;
		while (rangeLow <= rangeHigh) {
			pivot = rangeLow + ((rangeHigh - rangeLow)/2);			
			if (keys[pivot] == key) {				
				return pivot;
			} else if (keys[pivot] < key) {
				rangeLow = pivot + 1;
			} else {
				rangeHigh = pivot -1;
			}			
		}
		if (keys[pivot] > key)
			pivot--;		
		return -1 * pivot;
	}
	
	public synchronized Object get(int key) {
		if (size() == 0)
			return null;
		if (key == keyCache)
			return valueCache;		
		int idx = bisect(key);
		if (idx >= 0) {			
			keyCache = key;
			valueCache = values[idx];
			return values[idx];
		}		
		return null;		
	}
	
	public synchronized Object getValueIdx(int idx) {
		return values[idx];
	}
	
	public synchronized int getKeyIdx(int idx) {
		return keys[idx];
	}
	
	public synchronized void put (int key, Object value) {		
		clearCache();
		int idx = bisect(key);
		if (idx >= 0) { // key already in array. Overwrite
			values[idx] = value;
		} else {
			idx *= -1; // indicates position it should be stored 
			if (treeSize > 0) {	//Still space left in array			
				if (treeSize < keys.length -1) {
					/*
					 * Copy the elements before idx one position down to make
					 * space to insert the new value;
					 */
					System.arraycopy(keys, treeSize+1, keys, treeSize, idx - treeSize);
					System.arraycopy(values, treeSize+1, values, treeSize, idx - treeSize);
				}
				treeSize--;				
			} else { //Need to grow array
				//Create a new array of 20 more elements
				int [] keys2 = new int[keys.length + 20];
				Object[] values2 = new Object[keys.length + 20];
				for (int i = 0; i < 20; i++) keys2[i] = -1;				
				System.arraycopy(keys, treeSize + 1, keys2, treeSize + 20, idx - treeSize);
				System.arraycopy(values, treeSize + 1, values2, treeSize  + 20, idx - treeSize);				
				System.arraycopy(keys, idx + 1, keys2, idx + 21, keys.length - idx - 1);				
				System.arraycopy(values, idx + 1, values2, idx + 21, keys.length - idx - 1);				
				keys = keys2;
				values = values2;
				idx += 20;
				treeSize+=19;
			}
			keys[idx] = key;
			values[idx] = value;
		}		
	}
	
	public synchronized int popFirstKey() {
		if (treeSize == keys.length - 1)
			return -1;
		int key = keys[++treeSize];
		keys[treeSize] = -1;
		values[treeSize] = null;
		return key;
	}
	
	public synchronized void remove(int key) {
		clearCache();
		if (size() == 0)
			return;
		int idx = bisect(key);
		if (idx >= 0) {
			System.arraycopy(keys, treeSize, keys, treeSize+1, idx - treeSize);
			System.arraycopy(values, treeSize, values, treeSize+1, idx - treeSize);			
			values[treeSize] = null;
			keys[treeSize] = -1;
			treeSize++;
		}
	}
	
	public synchronized void removeAll() {
		int size = 20;
		keys = new int[size];
		values = new Object[size];
		for (int i = 0; i < size; i++) {
			keys[i] = -1;
		}
		treeSize = (short)(keys.length - 1);
		clearCache();		
	}
	
	public int size() {		
		return keys.length - treeSize - 1;		
	}
	
	public int capacity() {
		return keys.length;
	}
	
	public synchronized void clone(intTree clone) {
		synchronized (clone) {
			int size = clone.keys.length;
			keys = new int[size];
			values = new Object[size];
			System.arraycopy(clone.keys, 0, keys , 0, size);
			System.arraycopy(clone.values, 0, values , 0, size);			
			treeSize = clone.treeSize;
			clearCache();
		}
	}
	
	public String toString() {
		String res = "Length: " + size() + " ";
		for (int i = treeSize; i < keys.length; i++) {
			res += " (" + keys[i] + "|" + values[i] + ") ";
		}
		return res;
	}
}

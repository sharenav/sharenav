/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model.name;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.NoSuchElementException;

import de.ueller.osmToGpsMid.model.Entity;


/**
 * @author hmueller
 *
 */
public class Names {
	private TreeMap<String,Name> names1;
    private TreeSet<Name> canons;
	public Names() {
		names1=new TreeMap<String,Name>(String.CASE_INSENSITIVE_ORDER);
		canons=new TreeSet<Name>(new CaononComperator());
	}
	
	public void calcNameIndex(){
		int index=0;
		for (Name mapName : names1.values()) {
//			System.out.println(mapName+ " idx="+index);
			mapName.setIndex(index++);;
		}
	}
	public Collection<Name> getNames(){
		return names1.values();
	}

	public void addName(Entity w) {
		if (w.getName() == null )
			return;
		if (w.getName().trim().length() == 0){
			return;
		}
		Name mn =new Name(w);
//		System.out.println("adding name:" + mn.getName());
		if (names1.containsKey(mn.getName())){
//			System.out.println("name already there:" + mn);
			Name mnNext=new Name(w.getName()+"\0");
			SortedMap<String,Name> subSet=names1.subMap(mn.getName(), mnNext.getName());
			Name mnExist=subSet.get(subSet.firstKey());
			mnExist.addEntity(w);
		} else {
			names1.put(mn.getName(),mn);
		}
		if (! canons.add(mn)){
//			System.out.println("canon already there:" + mn);
			Name mnNext=new Name(w.getName()+"\0");
			mnNext.setCanon( mn.getCanon());
			try {
				SortedSet<Name> subSet=canons.tailSet(mnNext);
				Name mnExist=subSet.first();
				if (mnExist != null) {
					System.out.println("mnExist:" + mnExist);
					mnExist.addEntity(w);
				}
			}
			catch (NoSuchElementException e) {
			}
		}
	}

	public TreeSet<Name> getCanons() {
		return canons;
	}

	/**
	 * @return
	 */
	public int getNameIdx(String name) {
		if (name == null) {
			return -1;
		}
		Name nm = names1.get(name);
		if (nm != null) {
			return nm.getIndex();
		}
		System.out.println("ERROR: Did not find name in name idx: \"" + name + "\"");
		return -1;
	}

	public int getEqualCount(String s1, String s2){
		if (s1== null || s2 == null)
			return 0;
		int l1=s1.length();
		int l2=s2.length();
		int l=(l1 < l2)? l1 : l2;
		for (int loop=0; loop < l;loop++){
			if (s1.charAt(loop) != s2.charAt(loop))
				return loop;
		}
		return l;
	}

}

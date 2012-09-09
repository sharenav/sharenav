/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model.name;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.NoSuchElementException;

import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;
import net.sharenav.osmToShareNav.model.POIdescription;
import net.sharenav.osmToShareNav.model.WayDescription;
import net.sharenav.osmToShareNav.Configuration;


/**
 * @author hmueller
 *
 */
public class Names {
	// if true, index all nodes with addr:housenumber, regardless of whether there's a housenumberindex element
	private static boolean allAddrTags = false;
	private TreeMap<String,Name> names1;
	private TreeMap<String,Name> housenumbers1;
	private TreeSet<Name> canons;
	private TreeSet<Name> wordCanons;
	private TreeSet<Name> wholeWordCanons;
	private TreeSet<Name> houseNumberCanons;
	public Names() {
		names1=new TreeMap<String,Name>(String.CASE_INSENSITIVE_ORDER);
		housenumbers1=new TreeMap<String,Name>(String.CASE_INSENSITIVE_ORDER);
		canons=new TreeSet<Name>(new CaononComperator());
		wordCanons=new TreeSet<Name>(new CaononComperator());
		wholeWordCanons=new TreeSet<Name>(new CaononComperator());
		houseNumberCanons=new TreeSet<Name>(new CaononComperator());
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

	public void addName(Entity w, WayRedirect wayRedirect) {
		if (w.getName() == null )
			return;
		if (w.getName().trim().length() == 0){
			return;
		}
		boolean houseNumber = false;
		if (w instanceof Node) {
			short type = (short) ((Node) w).getType(Configuration.getConfiguration());
			POIdescription poiDesc = 
				Configuration.getConfiguration().getpoiDesc(type);
			//System.out.println("Testing node "+ w + " type " + type + " poiDesc = " + poiDesc);
			if (poiDesc != null && poiDesc.houseNumberIndex) {
				houseNumber = true;
				//System.out.println("Setting houseNumber = true for node "+ w);
			}
		}
		if (w instanceof Way) {
			short type = (short) ((Way) w).getType(Configuration.getConfiguration());
			WayDescription wayDesc =
				Configuration.getConfiguration().getWayDesc(type);

			//System.out.println("Testing way "+ w + " type " + type + "wayDesc = " + wayDesc);
			if (wayDesc != null && wayDesc.houseNumberIndex) {
				//System.out.println("Setting houseNumber = true for way "+ w);
				houseNumber = true;
			}
		}
		Name mn =new Name(w);
//		System.out.println("adding name:" + mn.getName());
		if (names1.containsKey(mn.getName())){
//			System.out.println("name already there:" + mn);
			Name mnNext=new Name(w.getName()+"\0");
			SortedMap<String,Name> subSet=names1.subMap(mn.getName(), mnNext.getName());
			Name mnExist=subSet.get(subSet.firstKey());
			long redirect = mnExist.addEntity(w);
			//System.out.println("Way add gave redirect " + redirect);
			if (redirect != (long) 0) {
				Way way = (Way) w;
				//System.out.println("Will do way redirect from id " + way.id
				//		   + " to id " + redirect);
				Long id = new Long (way.id);
				Long target = new Long (redirect);
				wayRedirect.put(id, target);
			}
		} else {
			names1.put(mn.getName(),mn);
		}
		if (!houseNumber) {
			//System.out.println("adding to wholename canon, !houseNumber: " + mn);
			if (! canons.add(mn)){
				//System.out.println("canon already there:" + mn);
				Name mnNext=new Name(w.getName()+"\0");
				mnNext.setCanon( mn.getCanon());
				try {
					SortedSet<Name> subSet=canons.tailSet(mnNext);
					Name mnExist=subSet.first();
					if (mnExist != null) {
//					System.out.println("mnExist:" + mnExist);
						mnExist.addEntity(w);
					}
				}
				catch (NoSuchElementException e) {
//					System.out.println("no such element exc. in canons.add");
				}
			}
		}
		// TODO: add whole word index, only add some entities (like housenumbers) to whole word idx
		// should add also stopwords 
		// add to word index; don't add housenumbers when housenumberindex element is used
		String [] words = mn.getName().split("[ ;,.()]");
		String [] housenumbers = words;
		if (allAddrTags && (w instanceof Node)) {
			Node n = (Node) w;
			if (n.hasHouseNumberTag()) {
				housenumbers = w.getAttribute("addr:housenumber").split("[ ;,.()]");
			}
		}
		if (!houseNumber) {
			for (String word : words) {
				if (word.length() == 0) {
					//System.out.println("Empty word");
					continue;
				}
				mn = new Name(word);
                                //System.out.println("adding word:" + mn);
				mn.addEntity(w);
				if (! wordCanons.add(mn)){
					//System.out.println("wordCanon already there:" + mn);
					Name mnNext=new Name(word+"\0");
					mnNext.setCanon( mn.getCanon());
					try {
						SortedSet<Name> subSet=wordCanons.tailSet(mnNext);
						Name mnExist=subSet.first();
						if (mnExist != null) {
							//						System.out.println("mnExist:" + mnExist);
							// Trouble? Adds to nameidx?
							mnExist.addEntity(w);
						}
					}
					catch (NoSuchElementException e) {
						//					System.out.println("no such element exc. in wordCanons.add");
					}
				}
			}
		}
		// add to housenumber index
		if (houseNumber || allAddrTags) {
			//System.out.println("Adding to housenumber index: Entity: " + w);
			for (String word : housenumbers) {
				//System.out.println("Word: " + word);
				if (word.length() == 0) {
					//System.out.println("Empty word");
					continue;
				}
				mn = new Name(word);
				//System.out.println("adding word:" + mn);
				mn.addEntity(w);
				if (! houseNumberCanons.add(mn)){
					//				System.out.println("wordCanon already there:" + mn);
					Name mnNext=new Name(word+"\0");
					mnNext.setCanon( mn.getCanon());
					try {
						SortedSet<Name> subSet=houseNumberCanons.tailSet(mnNext);
						Name mnExist=subSet.first();
						if (mnExist != null) {
							//						System.out.println("mnExist:" + mnExist);
							// Trouble? Adds to nameidx?
							mnExist.addEntity(w);
						}
					}
					catch (NoSuchElementException e) {
						//					System.out.println("no such element exc. in houseNumberCanons.add");
					}
				}
			}
		}
	}

	public TreeSet<Name> getCanons() {
		return canons;
	}

	public TreeSet<Name> getWordCanons() {
		return wordCanons;
	}

	public TreeSet<Name> getWholeWordCanons() {
		return wholeWordCanons;
	}

	public TreeSet<Name> getHouseNumberCanons() {
		return houseNumberCanons;
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

/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model.name;

import java.util.SortedSet;
import java.util.TreeSet;

import de.ueller.osmToGpsMid.model.Entity;


/**
 * @author hmueller
 *
 */
public class Names {
	TreeSet<Name> names1;
    TreeSet<Name> canons;
	public Names() {
		names1=new TreeSet<Name>(new NameComperator());
		canons=new TreeSet<Name>(new CaononComperator());
	}
	
	public void calcNameIndex(){
		int index=0;
		for (Name mapName : names1) {
			System.out.println(mapName+ " idx="+index);
			mapName.setIndex(index++);;
		}
	}
	public TreeSet<Name> getNames(){
		return names1;
	}

	public void addName(Entity w) {
		if (w.getName() == null )
			return;
		if (w.getName().trim().length() == 0){
			return;
		}
		Name mn =new Name(w);
		if (! names1.add(mn)){
			System.out.println("name already there:" + mn);
			Name mnNext=new Name(w.getName()+"\0");
			SortedSet<Name> subSet=names1.subSet(mn, mnNext);
			Name mnExist=subSet.first();
			mnExist.addEntity(w);
		}
		if (! canons.add(mn)){
			System.out.println("canon already there:" + mn);
			Name mnNext=new Name(w.getName()+"\0");
			SortedSet<Name> subSet=names1.subSet(mn, mnNext);
			Name mnExist=subSet.first();
			mnExist.addEntity(w);
		}
	}

	public TreeSet<Name> getCanons() {
		return canons;
	}

	/**
	 * @return
	 */
	public int getNameIdx(String name) {
		int index=0;
		for (Name mapName : names1) {
			if (mapName.getName().equals(name)){
				return mapName.getIndex();
			}
		}
		return -1;
	}

    
}

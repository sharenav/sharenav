package de.ueller.osmToGpsMid.model.name;


import java.util.ArrayList;

import de.ueller.osmToGpsMid.model.Entity;

/**
 * @author hmueller
 *
 */
public class Name {
	private String name;
	private String canon;
	private ArrayList<Entity> entitys = new ArrayList<Entity>(1);
	private int idx; 
	
	public Name(Entity e) {
		if (e.getName() == null){
			throw new IllegalArgumentException("null name not allowed");
		}
		setName(e.getName());
		addEntity(e);
	}
	/**
	 * construct a dummy element for subSet
	 * @param name
	 */
	public Name(String name){
		setName(name);
	}
	
	public void addEntity(Entity e){
		if(entitys.contains(e)){
			return;
		}
		entitys.add(e);
	}
	
//	private String normalizeName(){
//		
//	}
	private void setName(String name){
		this.name=name;
		String c=NumberCanon.canonial(name);
		if (c.length() < 21)
			this.canon=c;
		else
			this.canon=c.substring(0,20);
	}
	public String getName(){
		return name;
	}
	
	/**
	 * constuct the FileIndex in with this name will stored
	 * @return
	 */
	public String getCanonFileId(){
		if (canon.length() >= 2)
			return canon.substring(0, 2);
		else if (canon.length() >= 1)
			return canon.substring(0, 1) + "0";
		else return "00";
	}
	public String getCanonFileName(){
		try {
			return canon.substring(2);
		} catch (RuntimeException e) {
			return "";
		}
		
		
	}
	
	public String toString(){
		return "Name name='"+name+"("+canon+")'";
	}
	public String getCanon() {
		return canon;
	}
	/**
	 * @param i
	 */
	public void setIndex(int idx) {
		this.idx = idx;
	}
	public int getIndex() {
		return idx;
	}
	public ArrayList<Entity> getEntitys() {
		return entitys;
	}

}

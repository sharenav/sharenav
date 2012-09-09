package net.sharenav.osmToShareNav.model.name;


import java.util.ArrayList;

import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;

import net.sharenav.osmToShareNav.Configuration;


/**
 * @author hmueller
 *
 */
public class Name {
	private String name;
	private String canon;
	private ArrayList<Entity> entitys = new ArrayList<Entity>(1);
	private int idx;
	private boolean debug=false; 
	
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
	
	public long addEntity(Entity e){
		if(entitys.contains(e)){
			if (debug)
				System.out.println("dont add " +e + " because this element exists");
			return (long) 0;
		}
		if (name.equals(e.getName())){
			if (e instanceof Way) {
				for (Entity other :entitys ){
					if (other.nearBy == e.nearBy){
						if (debug)
							System.out.println("dont add " +e + " because simular element exists");
						if (Configuration.getConfiguration().useHouseNumbers) {
							// tell we need wayredirect
							Way o = (Way) other;
							//System.out.println("We need wayredirect, e.id: "
							//		   + ((Way)e).getId() + " o.id: " + o.getId());
							return o.getId();
						}
						return (long) 0;
					}
				}
			}
		}
		if (debug)
			System.out.println("add " +e);
		entitys.add(e);
		return (long) 0;
	}
	
//	private String normalizeName(){
//		
//	}
	private void setName(String name){
//		if ("Hauptstraße".equals(name)){
//			System.out.println("set name=Hauptstraße");
//			debug=true;
//		}
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
			return canon.substring(0, 1);
		else return "00";
	}
	public String getCanonFileName(String fileId){
		try {
			return canon.substring(fileId.length() == 1 ? 1 : 2);
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
	public void setCanon(String c){
		canon = c;
		return;
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

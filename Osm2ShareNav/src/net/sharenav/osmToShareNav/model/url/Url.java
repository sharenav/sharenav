package net.sharenav.osmToShareNav.model.url;


import java.util.ArrayList;

import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;

/**
 * @author hmueller
 *
 */
public class Url {
	private String url;
	private ArrayList<Entity> entitys = new ArrayList<Entity>(1);
	private int idx;
	private boolean debug=false; 
	
	public Url(Entity e) {
		if (e.getUrl() == null){
			throw new IllegalArgumentException("null url not allowed");
		}
		setUrl(e.getUrl());
		addEntity(e);
	}
	public Url(Entity e, String url) {
		if (url == null){
			throw new IllegalArgumentException("null url not allowed");
		}
		setUrl(url);
		addEntity(e);
	}
	/**
	 * construct a dummy element for subSet
	 * @param url
	 */
	public Url(String url){
		setUrl(url);
	}
	
	public void addEntity(Entity e){
		if(entitys.contains(e)){
			if (debug)
				System.out.println("dont add " +e + " because this element exists");
			return;
		}
		if (url.equals(e.getUrl())){
			for (Entity other :entitys ){
				if (other.nearBy == e.nearBy){
				//	if (debug)
				//	System.out.println("dont add " +e + " because simular element exists");
				//	return;
				}
			}
		}
		if (debug)
			System.out.println("add " +e);
		entitys.add(e);
	}
	
//	private String normalizeUrl(){
//		
//	}
	private void setUrl(String url){
//		if ("Hauptstraße".equals(url)){
//			System.out.println("set url=Hauptstraße");
//			debug=true;
//		}
		this.url=url;
	}
	public String getUrl(){
		return url;
	}
	
	/**
	 * constuct the FileIndex in with this url will stored
	 * @return
	 */
	public String toString(){
		return "Url url='"+url+"'";
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

/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model.url;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sharenav.osmToShareNav.model.Entity;


/**
 * @author hmueller
 *
 */
public class Urls {
	private TreeMap<String,Url> urls1;
	//private TreeSet<Url> canons;

	public Urls() {
		urls1=new TreeMap<String,Url>(String.CASE_INSENSITIVE_ORDER);
	}
	
	public void calcUrlIndex(){
		int index=0;
		for (Url mapUrl : urls1.values()) {
//			System.out.println(mapUrl+ " idx="+index);
			mapUrl.setIndex(index++);;
		}
	}
	public Collection<Url> getUrls(){
		return urls1.values();
	}

	public void addUrl(Entity w) {
		if (w.getUrl() == null )
			return;
		if (w.getUrl().trim().length() == 0){
			return;
		}
		Url mn =new Url(w);
		if (urls1.containsKey(mn.getUrl())){
//			System.out.println("url already there:" + mn);
			Url mnNext=new Url(w.getUrl()+"\0");
			SortedMap<String,Url> subSet=urls1.subMap(mn.getUrl(), mnNext.getUrl());
			Url mnExist=subSet.get(subSet.firstKey());
			mnExist.addEntity(w);
		} else {
			urls1.put(mn.getUrl(),mn);
		}
	}

	public void addPhone(Entity w) {
		if (w.getPhone() == null )
			return;
		if (w.getPhone().trim().length() == 0){
			return;
		}
		// set phone attribute as url
		Url mn =new Url(w, w.getPhone());
		if (urls1.containsKey(mn.getUrl())){
//			System.out.println("url for phone already there:" + mn);
			Url mnNext=new Url(w.getPhone()+"\0");
			SortedMap<String,Url> subSet=urls1.subMap(mn.getUrl(), mnNext.getUrl());
			Url mnExist=subSet.get(subSet.firstKey());
			mnExist.addEntity(w);
		} else {
			urls1.put(mn.getUrl(),mn);
		}
	}

	/**
	 * @return
	 */
	public int getUrlIdx(String url) {
		if (url == null) {
			return -1;
		}
		Url nm = urls1.get(url);
		if (nm != null) {
			return nm.getIndex();
		}
		System.out.println("ERROR: Did not find url in url idx: \"" + url + "\"");
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

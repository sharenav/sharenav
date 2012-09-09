/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2008  Kai Krueger
 * 
 */
package net.sharenav.osmToShareNav;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;
import net.sharenav.osmToShareNav.model.name.Name;
import net.sharenav.osmToShareNav.model.name.Names;
import net.sharenav.osmToShareNav.model.name.WayRedirect;
import net.sharenav.osmToShareNav.model.url.Url;
import net.sharenav.osmToShareNav.model.url.Urls;
import net.sharenav.osmToShareNav.tools.FileTools;

/**
 * @author hmueller
 *
 */
public class SearchList {
	Names names;
	Urls urls;
	WayRedirect wayRedirect;

	public static final int INDEX_NAME = 0;
	public static final int INDEX_WORD = 1;
	public static final int INDEX_WHOLEWORD = 2;
	public static final int INDEX_HOUSENUMBER = 3;
	public static final int INDEX_BIGNAME = 4;

	public static final int FLAG_NODE = 0x80;
	public static final int FLAG_URL = 0x40;
	public static final int FLAG_PHONE = 0x20;

	public SearchList(Names names, Urls urls, WayRedirect wayRedirect) {
		super();
		this.names = names;
		this.urls = urls;
		this.wayRedirect = wayRedirect;
	}

	public void createSearchList(String path, int listType){
		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			String lastStr = null;
			String lastFid = "";
			int curPos = 0;
			FileTools.createPath(new File(path + "/search"));
			for (Name mapName : ((listType == INDEX_NAME || listType == INDEX_BIGNAME) ? names.getCanons()
					     : (listType == INDEX_WORD ? names.getWordCanons()
						: (listType == INDEX_WHOLEWORD ? names.getWholeWordCanons() : names.getHouseNumberCanons())))) {
				String string=mapName.getCanonFileName(mapName.getCanonFileId());
				int eq=names.getEqualCount(string,lastStr);
				if (! lastFid.equals(mapName.getCanonFileId())) {
					if (ds != null) {
						ds.close();
					}
					lastFid = mapName.getCanonFileId();
					String fileName = path +
						(listType == INDEX_BIGNAME ? "/search/n" :
							(listType == INDEX_NAME ? "/search/s" : 
								(listType == INDEX_WORD ? "/search/w" :
									(listType == INDEX_WHOLEWORD ? "/search/ww" : "/search/h")))) +
						lastFid + ".d";
					//System.out.println("open " + fileName);
					fo = new FileOutputStream(fileName);
					ds = new DataOutputStream(fo);
					curPos = 0;
					eq = 0;
					lastStr = null;
				}
				String wrString = string.substring(eq);				
				
				/**
				 * Encoding of delta plus flags in bits:
				 * 10000000 Sign bit
				 * 01100000 long 
				 * 01000000 int
				 * 00100000 short
				 * 00000000 byte
				 * 000xxxxx delta 
				 */
				
				int delta = eq-curPos;
				if (Math.abs(delta) > 30) {
					System.out.println("Error: Overflow in Search cannon: " + mapName.getName());
				}
				if (delta <0){
					delta = -delta;
					delta += 0x80;
				}
				
					long l=0;
					if (wrString.length() > 0)
						l = Long.parseLong(wrString);
					if (l < Byte.MAX_VALUE){
//						System.out.println("byte   " + (delta)  + " " + (byte) delta + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta);
						ds.writeByte((int) l);
					} else if (l < Short.MAX_VALUE){
//						System.out.println("short  " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta | 0x20);
						ds.writeShort((int) l);
					} else if (l < Integer.MAX_VALUE){					
//						System.out.println("int    " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta | 0x40);
						ds.writeInt((int) l);
					} else {
//						System.out.println("long   " + (delta) + " " + (byte) delta  + " '" +string.substring(eq)+"' '"+mapName);
						ds.writeByte(delta| 0x60);
						ds.writeLong(l);
					}
				int nameIdx = mapName.getIndex();
				int primaryNameIdx = nameIdx;
				if (nameIdx >= Short.MAX_VALUE) {
					ds.writeInt(nameIdx | 0x80000000);
				} else {
					ds.writeShort(nameIdx);
				}				
				for (Entity e : mapName.getEntitys()){
					int isInFlags = 0;
					Node center=null;
					String url = null;
					String phone = null;
					String name = null;
					long idtowrite = 0;
					if (e instanceof Node) {
						idtowrite = ((Node) e).id;
					}
					if (e instanceof Way) {
						idtowrite = ((Way) e).id;
					}
					if (listType != INDEX_NAME && Configuration.getConfiguration().useHouseNumbers) {
						// write way id for matching housenumber to streetname
						//System.out.println ("listType == 3, testing node " + n);
						//System.out.println ("type was: " + n.getType(Configuration.getConfiguration()));
						String wayid = e.getAttribute("__wayid");
						if (wayid != null) {
							long way = Long.parseLong(wayid);
							//System.out.println ("Found housenumber node wayid:" + wayid + "(" + way + ")" );
							idtowrite = way;
						}
					}			   
					if (e instanceof Node) {
						Node n = (Node) e;						
						url = n.getUrl();
						phone = n.getPhone();
						name = n.getName();
						if (Configuration.getConfiguration().map66search) {
							isInFlags |= FLAG_NODE;
						}
						// polish.api.bigstyles
						if (Configuration.getConfiguration().bigStyles) {
							ds.writeShort(n.getType(Configuration.getConfiguration()));
						} else {
							if (Configuration.getConfiguration().map66search) {
								ds.writeByte(n.getType(Configuration.getConfiguration()));
							} else {
								ds.writeByte(-1*n.getType(Configuration.getConfiguration()));
							}
						}
						center=n;
						//System.out.println("entryType " + n.getNameType() + " idx=" + mapName.getIndex());
					}
					if (e instanceof Way) {
						Way w = (Way) e;
						url = w.getUrl();
						phone = w.getPhone();
						name = w.getName();
						//
						//ds.writeByte(w.getNameType());
						// polish.api.bigstyles
						if (Configuration.getConfiguration().bigStyles) {
							ds.writeShort(w.getType(Configuration.getConfiguration()));
						} else {
							ds.writeByte(w.getType(Configuration.getConfiguration()));
						}
//						System.out.println("entryType " + w.getNameType() + " idx=" + mapName.getIndex());
						center=w.getMidPoint();
					}
					int urlIdx = urls.getUrlIdx(url);
					int phoneIdx = urls.getUrlIdx(phone);
					if (urlIdx == -1) {
						urlIdx = 0;
					}
					if (phoneIdx == -1) {
						phoneIdx = 0;
					}
					if (Configuration.getConfiguration().map66search) {
						if (urlIdx != 0 && Configuration.getConfiguration().useUrlTags) {
							isInFlags |= FLAG_URL;
						}
						if (phoneIdx != 0 && Configuration.getConfiguration().usePhoneTags) {
							isInFlags |= FLAG_PHONE;
						}
					}
                                        // write id for housenumber or multi-word matching
					if (listType != INDEX_NAME) {
						Long idLong = new Long(idtowrite);
						// check if this is redirected to another way segment with the same name
						// due to space & search result conservation
						//System.out.println("Checking redirect for id " + idtowrite);
						Long targetLong = wayRedirect.get(idLong);
						if (targetLong != null) {
							long target = wayRedirect.get(idLong).longValue();
							//System.out.println("Doing redirect from id " + idtowrite + " to " + target);
							if (target != (long) 0) {
								idtowrite = target;
							}
						}
						ds.writeLong(idtowrite);
					}
					ArrayList<Entity> isIn=new ArrayList<Entity>();
					Entity nb=e.nearBy;
					while (nb != null && !isIn.contains(nb)){
						if (nb.getName() != null)
							isIn.add(nb);
						nb=nb.nearBy;
					}

					int isInSize = isIn.size();
					if (Configuration.getConfiguration().map66search) {
						isInSize |= isInFlags;
					}
					ds.writeByte(isInSize & 0xff);
					for (Entity e1 : isIn){
						int isinIdx = names.getNameIdx(e1.getName());
						if (isinIdx < 0) {
							System.out.println("Invalid isin (" + e1 + ") for Entety " + e + " with Name \"" + e1.getName() + "\"");
						}
						if (isinIdx >= Short.MAX_VALUE) {
							ds.writeInt(isinIdx | 0x80000000);
						} else {				  
							ds.writeShort(isinIdx);
						}
					}
					if (center == null){
						System.out.println("no center for searchList for "+e);
						center=new Node(0f,0f,-1);
					}
					ds.writeFloat(MyMath.degToRad(center.lat));
					ds.writeFloat(MyMath.degToRad(center.lon));
					int entityNameIdx = names.getNameIdx(name);
//					System.out.println("in entity, name: " + name);
//					System.out.println("in entity, entityNameIdx: " + entityNameIdx);
					if (entityNameIdx != -1) {
						nameIdx = entityNameIdx;
					} else {
						nameIdx = primaryNameIdx;
					}
					if (nameIdx >= Short.MAX_VALUE) {
						ds.writeInt(nameIdx | 0x80000000);
					} else {
						ds.writeShort(nameIdx);
					}				
					if (Configuration.getConfiguration().useUrlTags) {
						if (!Configuration.getConfiguration().map66search
						    || urlIdx != 0) {
							if (urlIdx >= Short.MAX_VALUE) {  // FIXME a flag somewhere to save space?
								ds.writeInt(urlIdx | 0x80000000);
							} else {
								ds.writeShort(urlIdx);
							}				
						}
					}
					if (Configuration.getConfiguration().usePhoneTags) {
						if (!Configuration.getConfiguration().map66search
						    || phoneIdx != 0) {
							if (phoneIdx >= Short.MAX_VALUE) {// FIXME a flag somewhere to save space?
								ds.writeInt(phoneIdx | 0x80000000);
							} else {
								ds.writeShort(phoneIdx);
							}				
						}
					}
				}
				// polish.api.bigstyles
				if (Configuration.getConfiguration().bigStyles) {
					ds.writeShort(0);
				} else {
					ds.writeByte(0);
				}
//				ds.writeUTF(string.substring(eq));
				curPos=eq;
				lastStr=string;
			}
			if (ds != null) ds.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createNameList(String path) {
//		names1 = getNames1();

		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			FileOutputStream foi = new FileOutputStream(path + "/dat/names-idx.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			String lastStr = null;
			fo = new FileOutputStream(path + "/dat/names-0.dat");
			ds = new DataOutputStream(fo);
			int curPos = 0;
			int idx = 0;
			short fnr = 1;
			short fcount = 0;
			for (Name mapName : names.getNames()) {
				String string = mapName.getName();
				int eq = names.getEqualCount(string, lastStr);				
				if (ds.size() > Configuration.getConfiguration().maxTileSize) {
					dsi.writeInt(idx);
					if (ds != null) {
						ds.close();
					}
					fo = new FileOutputStream(path + "/dat/names-" + fnr + ".dat");
					ds = new DataOutputStream(fo);
//					System.out.println("wrote names " + fnr + " with "+ fcount + " names");
					fnr++;
					curPos = 0;
					eq = 0;
					fcount = 0;
					lastStr = null;
				}
				ds.writeByte(eq - curPos);
				ds.writeUTF(string.substring(eq));
//				ds.writeShort(getWayNameIndex(mapName.getIs_in(), null));
//				System.out.println("" + (eq-curPos) + "'" +string.substring(eq) + "' '" + string);
				curPos = eq;
				lastStr = string;
				idx++;
				fcount++;
//				ds.writeUTF(string);
			}
			dsi.writeInt(idx);
			ds.close();
			dsi.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

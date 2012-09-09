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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;
import net.sharenav.osmToShareNav.model.name.Name;
import net.sharenav.osmToShareNav.model.name.Names;
import net.sharenav.osmToShareNav.model.url.Url;
import net.sharenav.osmToShareNav.model.url.Urls;

/**
 * @author hmueller
 *
 */
public class UrlList {
	Urls urls;

	public UrlList(Urls urls) {
		super();
		this.urls = urls;
	}

	public void createUrlList(String path) {
//		urls1 = getUrls1();

		try {
			FileOutputStream fo = null;
			DataOutputStream ds = null;
			FileOutputStream foi = new FileOutputStream(path + "/dat/urls-idx.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			String lastStr = null;
			fo = new FileOutputStream(path + "/dat/urls-0.dat");
			ds = new DataOutputStream(fo);
			int curPos = 0;
			int idx = 0;
			short fnr = 1;
			short fcount = 0;
			for (Url mapUrl : urls.getUrls()) {
				String string = mapUrl.getUrl();
				int eq = urls.getEqualCount(string, lastStr);				
				if (ds.size() > Configuration.getConfiguration().maxTileSize){
					dsi.writeInt(idx);
					if (ds != null) {
						ds.close();
					}
					fo = new FileOutputStream(path + "/dat/urls-" + fnr + ".dat");
					ds = new DataOutputStream(fo);
//					System.out.println("wrote urls " + fnr + " with "+ fcount + " urls");
					fnr++;
					curPos = 0;
					eq = 0;
					fcount = 0;
					lastStr = null;
				}
				ds.writeByte(eq - curPos);
				ds.writeUTF(string.substring(eq));
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

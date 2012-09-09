/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */
package net.sharenav.sharenav.ui;

import java.util.Vector;

import javax.microedition.lcdui.Image;

import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.names.NumberCanon;
import net.sharenav.midlet.ui.KeySelectMenuItem;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

class GuiPoiTypeSelectMenu extends KeySelectMenu implements KeySelectMenuListener {
	
	class PoiTypeSelectMenuItem implements KeySelectMenuItem {
		private Image img;
		private String  name;
		private short idx;
		private String canon;
		
		public PoiTypeSelectMenuItem(Image img, String name, short idx) {
			this.img = img;
			this.name = name;
			this.idx = idx;
			this.canon = NumberCanon.canonial(name);
		}

		public Image getImage() {
			return img;
		}

		public String getName() {
			return name;
		}
		
		public String getCanon() {
			return canon;
		}
		
		public short getIdx() {
			return idx;
		}
		
		public String toString() {
			return name + " [" + canon + "] (" + idx +")";
		}
		
	}

	private final static Logger logger = Logger.getInstance(GuiPoiTypeSelectMenu.class, Logger.DEBUG);
	
	private Vector  poiTypes;
	
	private KeySelectMenuReducedListener callbackReduced;
	
	public GuiPoiTypeSelectMenu(ShareNavDisplayable parent,
			KeySelectMenuReducedListener callback) throws Exception {
		super(parent);
		super.callback = this;
		callbackReduced = callback;
		setTitle(Locale.get("guipoitypeselectmenu.SelectPOIType")/*Select POI type*/);
		keySelectMenuResetMenu();
	}
	
	public void keySelectMenuResetMenu() {
		logger.info("Resetting POI type menu");
		this.removeAll();
		if (poiTypes == null) {
			poiTypes = new Vector();
			// FIXME select proper image for
			KeySelectMenuItem menuItem = new PoiTypeSelectMenuItem(Legend.getNodeSearchImage((short)0), 
					Locale.get("guipoitypeselectmenu.Everything")/*Everything*/, (short)0);
			poiTypes.addElement(menuItem);
			for (short i = 1; i < Legend.getMaxType(); i++) {
				menuItem = new PoiTypeSelectMenuItem(Legend.getNodeSearchImage(i), 
						Legend.getNodeTypeDesc(i), i);
				poiTypes.addElement(menuItem);
			}
		}
		this.addResult(poiTypes);
	}

	public void keySelectMenuSearchString(String searchString) {
		this.removeAll();
		Vector vec = new Vector();
		for (short i = 0; i < poiTypes.size(); i++) {
			PoiTypeSelectMenuItem poiType = (PoiTypeSelectMenuItem)poiTypes.elementAt(i); 
			if (poiType.getCanon().startsWith(searchString) || poiType.getName().toLowerCase().startsWith(searchString.toLowerCase())) {
				logger.info(poiType + " matches searchString " + searchString);
				vec.addElement(poiType);
			}
		}
		this.addResult(vec);
	}

	public void keySelectMenuCancel() {
		callbackReduced.keySelectMenuCancel();
	}

	public void keySelectMenuItemSelected(short poiType) {
		callbackReduced.keySelectMenuItemSelected(poiType);
		
	}


}
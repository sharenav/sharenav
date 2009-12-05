/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */
package de.ueller.midlet.gps;

import java.util.Vector;

import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Legend;
import de.ueller.midlet.gps.data.KeySelectMenuItem;
import de.ueller.midlet.gps.names.NumberCanon;

class GuiPOItypeSelectMenu extends KeySelectMenu implements KeySelectMenuListener {
	
	class POItypeSelectMenuItem implements KeySelectMenuItem {
		private Image img;
		private String  name;
		private byte idx;
		private String canon;
		
		public POItypeSelectMenuItem(Image img, String name, byte idx) {
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
		
		public byte getIdx() {
			return idx;
		}
		
		public String toString() {
			return name + " [" + canon + "] (" + idx +")";
		}
		
	}

	private final static Logger logger = Logger.getInstance(GuiPOItypeSelectMenu.class,Logger.DEBUG);
	
	private Vector  poiTypes;
	
	private KeySelectMenuReducedListener callbackReduced;
	
	public GuiPOItypeSelectMenu(GpsMidDisplayable parent,
			KeySelectMenuReducedListener callback) throws Exception {
		super(parent);
		super.callback = this;
		callbackReduced = callback;
		setTitle("Select POI type");
		keySelectMenuResetMenu();
	}
	
	public void keySelectMenuResetMenu() {
		logger.info("Resetting POI type menu");
		this.removeAll();
		if (poiTypes == null) {
			poiTypes = new Vector();
			for (byte i = 1; i < Legend.getMaxType(); i++) {
				KeySelectMenuItem menuItem = new POItypeSelectMenuItem(Legend.getNodeSearchImage(i),Legend.getNodeTypeDesc(i),i);
				poiTypes.addElement(menuItem);
			}
		}
		this.addResult(poiTypes);
	}

	public void keySelectMenuSearchString(String searchString) {
		this.removeAll();
		Vector vec = new Vector();
		for (byte i = 0; i < poiTypes.size(); i++) {
			POItypeSelectMenuItem poiType = (POItypeSelectMenuItem)poiTypes.elementAt(i); 
			if (poiType.getCanon().startsWith(searchString)) {
				logger.info(poiType + " matches searchString " + searchString);
				vec.addElement(poiType);
			}
		}
		this.addResult(vec);
	}

	public void keySelectMenuCancel() {
		callbackReduced.keySelectMenuCancel();
	}

	public void keySelectMenuItemSelected(KeySelectMenuItem item) {
		callbackReduced.keySelectMenuItemSelected(item);
		
	}


}
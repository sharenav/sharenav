/**
 * This file is part of OSM2ShareNav
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2011 
 */

package net.sharenav.osmToShareNav;

import java.util.ResourceBundle;
import java.util.MissingResourceException;

public class GetText {
	public static ResourceBundle myResources = null;
	private static boolean missingBundle = false;
	private static boolean inited = false;

	public static void init() {
		try {
			myResources = ResourceBundle.getBundle("Translations");
		} catch (MissingResourceException e) {
			missingBundle = true;
		}
	}
	public static String _(String s) {
		try {
			if (!inited) {
				init();
			}
			if (missingBundle) {
				return s;
			} else {
				return myResources.getString(s);
			}
		} catch (MissingResourceException e) {
			return s;
		}

	}
}

/*
 * ShareNav - Copyright (c) 2007-2011 Harald Mueller james22 at users dot sourceforge dot net and others
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Command;

// modify UI - on Android, store settings when "Back" key is pressed

public class ShareNavMenu {
//#if polish.android
	public static final byte OK = Command.BACK;
	public static final byte BACK = Command.CANCEL;
	public static final byte CANCEL = Command.CANCEL;
//#else
	public static final byte OK = Command.OK;
	public static final byte BACK = Command.BACK;
	public static final byte CANCEL = Command.CANCEL;
//#endif

}
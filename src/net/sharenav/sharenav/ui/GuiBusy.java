/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import de.enough.polish.util.Locale;

public class GuiBusy extends Canvas implements ShareNavDisplayable {

	protected void paint(Graphics g) {
		int height, width;
		width = getWidth();
		height = getHeight();
		g.setColor(255,0,0);
		g.fillRect((width - 120)/2, (height - 70)/2, 120, 70);
		g.setColor(255,255,255);
		g.fillRect((width - 100)/2, (height - 50)/2, 100, 50);
		g.setColor(0);
		g.drawRect((width - 100)/2, (height - 50)/2, 100, 50);		
		g.drawString(Locale.get("guibusy.Busy")/*Busy...*/, (width - g.getFont().stringWidth(Locale.get("guibusy.Busy")/*Busy...*/)) / 2, 
			(height - g.getFont().getHeight())/2, Graphics.TOP | Graphics.LEFT);
	}

	public void show() {
		ShareNav.getInstance().show(this);
	}

}

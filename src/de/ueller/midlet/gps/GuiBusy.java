package de.ueller.midlet.gps;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import de.ueller.midlet.gps.tile.C;

public class GuiBusy extends Canvas implements GpsMidDisplayable {

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
		g.drawString("Busy...", (width - g.getFont().stringWidth("Busy..."))/2, (height - g.getFont().getHeight())/2, Graphics.TOP
				| Graphics.LEFT);
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

}

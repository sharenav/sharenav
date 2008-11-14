/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
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
package de.ueller.midlet.gps;

import de.ueller.midlet.gps.tile.C;

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Splash extends Canvas implements CommandListener,Runnable{
	private Image splash;
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command("Accept", Command.OK, 2);
    private final Command EXIT_CMD = new Command("Decline", Command.EXIT, 1);
	private final GpsMid main;
	String[] txt={"Copyright:",
				  " Harald Mueller",
				  " Kai Krueger",
				  " sk750",
	              "Application:",
	              " licensed by GPL2",
	              " http://www.gnu.org/",
	              "Map data:",
	              " from OpenStreetMap",
	              " licensed by CC 2.0",
	              " http://creativecommons.org/",
	  "Thanks for source parts to:",
	  " Nikolay Klimchuk",
	  " Simon Turner",
	  " Will Perone",
      "Artwork:",
      " Tobias Mueller"};
	private Font f;
	int top=0;
	private Thread processorThread;
	private boolean shutdown=false;
	private int ssize;
	private int topStart=106;
	private int space;
	private double scale=1;
	private String strVersion; 


	public Splash(GpsMid main){
	this.main = main;
	try {
		splash=Image.createImage("/Gps-splash.png");

	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	if(splash.getWidth()<getWidth() ) {
		double scaleW=(double) getWidth()/ (double) splash.getWidth();
		double scaleH=(double) getHeight()/(double) splash.getHeight();
		scale=scaleH;
		if(scaleW<scaleH) {
			scale=scaleW;
		}
		splash=scaleImage(splash, (int)(scale*(double) splash.getWidth()), (int)(scale* (double) splash.getHeight()) );
		topStart*=scale;
	}

	f=Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
	space=getHeight()-topStart;
	ssize=f.getHeight()*txt.length+space;
	top=-space;
	strVersion = "V" + main.c.getAppVersion() + " (" + main.c.getBundleDate() + ")";
	show();
	addCommand(BACK_CMD);
	addCommand(EXIT_CMD);
	setCommandListener(this);
	processorThread = new Thread(this,"Splash");
	processorThread.setPriority(Thread.MIN_PRIORITY);
	processorThread.start();

	}

	protected void paint(Graphics g) {
		// cleans the screen
		g.setColor(150, 200, 250);
		g.setFont(f);
		g.fillRect(0, 0, getWidth(), getHeight());
		int sp=f.getHeight();
		int x = (int) (5*scale+ (getWidth()-splash.getWidth())/2);
		
		g.drawImage(splash,getWidth()/2, 0,Graphics.HCENTER|Graphics.TOP);

		g.setColor(0xFFFF99);
		g.drawString(strVersion, (getWidth() + splash.getWidth())/2 - 2 , 2, Graphics.TOP|Graphics.RIGHT);		
		
		g.setColor(255, 40, 40);
		int startLine=top/sp;
		int yc= topStart-top % sp;
		g.setClip(0,topStart, getWidth(), getHeight()-topStart);
		boolean visible=false;
		for (int i=startLine; i< txt.length;i++){
			visible=true;
			if (i >= 0){
				int w=f.stringWidth(txt[i]);
				g.drawString(txt[i], x, yc, 0);
			}
			yc+=sp;
			if (! visible){
				top=-space;
			}
		}
		
	}

	public void commandAction(Command c, Displayable d) {
        if (c == BACK_CMD) {
        	shutdown=true;
        	main.show();
        	return;
        }
        if (c == EXIT_CMD) {
        	shutdown=true;
        	main.exit();
        	return;
        }
	}
	public void show(){
		GpsMid.getInstance().show(this);
		//Display.getDisplay(main).setCurrent(this);
	}

	public void run() {
		while (! shutdown){
		synchronized (this) {
			try {
				wait(40);
			} catch (InterruptedException e) {

			}
			top++;
			if (top > (ssize)){
				top=-space;
			}
			repaint();
		}
		}
	}
	
	
	// based on Public Domain code (confirmed by E-Mail)
	// from http://willperone.net/Code/codescaling.php 
	public Image scaleImage(Image original, int newWidth, int newHeight)
    {        
    	// if we would not be able to allocate memory for
		// at least the memory for the original and the scaled image
		// plus 25% do not scale
		if(Runtime.getRuntime().freeMemory()<5*(original.getHeight() * original.getWidth() + newWidth*newHeight) ) {
    		scale=1;
    		return original;
    	}
        try {
			int[] rawInput = new int[original.getHeight() * original.getWidth()];
	        original.getRGB(rawInput, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
	        
	        int[] rawOutput = new int[newWidth*newHeight];        
	
	        // YD compensates for the x loop by subtracting the width back out
	        int YD = (original.getHeight() / newHeight) * original.getWidth() - original.getWidth(); 
	        int YR = original.getHeight() % newHeight;
	        int XD = original.getWidth() / newWidth;
	        int XR = original.getWidth() % newWidth;        
	        int outOffset= 0;
	        int inOffset=  0;
	        
	        for (int y= newHeight, YE= 0; y > 0; y--) {            
	            for (int x= newWidth, XE= 0; x > 0; x--) {
	                rawOutput[outOffset++]= rawInput[inOffset];
	                inOffset+=XD;
	                XE+=XR;
	                if (XE >= newWidth) {
	                    XE-= newWidth;
	                    inOffset++;
	                }
	            }            
	            inOffset+= YD;
	            YE+= YR;
	            if (YE >= newHeight) {
	                YE -= newHeight;     
	                inOffset+=original.getWidth();
	            }
	        }               
	        return Image.createRGBImage(rawOutput, newWidth, newHeight, false);
        } catch (Exception e) {
        	scale=1;
        	return original;
        }
    }
}

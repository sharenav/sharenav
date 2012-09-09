package net.sharenav.midlet.util;
/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Image;

import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class ImageTools  {
	private final static Logger logger = Logger.getInstance(ImageTools.class,Logger.DEBUG);
	
	public static Image getGreyImage(Image original) {
        try {
			int[] rawInput = new int[original.getHeight() * original.getWidth()];
	        original.getRGB(rawInput, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
	        
            for (int i = 0; i < rawInput.length; i++) {
                int rgb = rawInput[i];
                int alphaMask = rgb & 0xff000000;
                int red   = (rgb >> 16) & 0xff;
                int green = (rgb >>  8) & 0xff;
                int blue  =  rgb        & 0xff;
                int grey = (((red * 30) / 100) + ((green * 59) / 100) + ((blue * 11) / 100)) & 0xff;
                rawInput[i] = alphaMask | (grey << 16) | (grey << 8) | grey;
            } 
	        return Image.createRGBImage(rawInput, original.getWidth(), original.getHeight(), true);
        } catch (Exception e) {
		logger.exception(Locale.get("imagetools.ExceptionGreyingImage")/*Exception greying image*/, e);
        	return original;
        }
	}
	
	
	// based on Public Domain code (confirmed by E-Mail)
	// from http://willperone.net/Code/codescaling.php 
	public static Image scaleImage(Image original, int newWidth, int newHeight)
    {        
		if (newWidth < 2) {
			newWidth = 2;
		}
		if (newHeight < 2) {
			newHeight = 2;
		}
		try {
	        int originalWidth = original.getWidth();
	        int originalHeight = original.getHeight();
			int[] rawInput = new int[originalHeight * originalWidth];
	        original.getRGB(rawInput, 0, originalWidth, 0, 0, originalWidth, originalHeight);	        	        
	        int[] rawOutput = new int[newWidth*newHeight];        
	
	        // YD compensates for the x loop by subtracting the width back out
	        int YD = (originalHeight / newHeight) * originalWidth - originalWidth; 
	        int YR = originalHeight % newHeight;
	        int XD = originalWidth / newWidth;
	        int XR = originalWidth % newWidth;        
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
	                inOffset+=originalWidth;
	            }
	        }               
	        return Image.createRGBImage(rawOutput, newWidth, newHeight, true);
        } catch (Exception e) {
			logger.exception(Locale.get("imagetools.ExceptionScalingImage")/*Exception scaling image*/, e);
        	return original;
        }
    }	
	
	public static boolean isScaleMemAvailable(Image original, int newWidth, int newHeight) {
		return (Runtime.getRuntime().freeMemory() > 5*(original.getHeight() * original.getWidth() + newWidth * newHeight));
	}
}

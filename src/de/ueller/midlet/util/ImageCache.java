package de.ueller.midlet.util;
/*
 * GpsMid - Copyright (c) 2012 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.lang.Integer;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

import de.ueller.gps.location.GetCompass;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

class ImgId {
	short width;
	short height;
	
	public ImgId(int width, int height) {
		this.width = (short) width;
		this.height = (short) height;
	}
	
	public boolean equals(Object o){
		if (o == null || !(o instanceof ImgId) || o.getClass() != this.getClass()) {
			return false;			
		}
		ImgId other = (ImgId) o;
		if (other.width != this.width || other.height != this.height) {
			return false;
		}
		return true;
	}	
	
	public int hashCode() {
		return this.width + this.height;
	}
	
	public String toString() {
		return "(" + this.width + "x" + this.height + ")"; 		
	}
		
}

class ImgIdFile extends ImgId {
	String fileName;
	public ImgIdFile(String fileName, int width, int height) {
		super(width, height);
		this.fileName = fileName;
	}

	public boolean equals(Object o){
		if (super.equals(o)) {
			ImgIdFile other = (ImgIdFile) o;
			if (this.fileName.equalsIgnoreCase(other.fileName)) {
				return true;
			}
		}
		return false;
	}	
	
	public int hashCode() {
		return super.hashCode() + fileName.hashCode(); 		
	}

	public String toString() {
		return this.fileName + super.toString();
	}

}

class ImgIdOneColor extends ImgId {	
	int color;

	public ImgIdOneColor(int color, int width, int height) {
		super(width, height);
		this.color = color;
	}

	public boolean equals(Object o){
		if (super.equals(o)) {
			ImgIdOneColor other = (ImgIdOneColor) o;
			if (this.color == other.color) {
				return true;
			}
		}
		return false;
	}	
	
	public int hashCode() {
		return super.hashCode() + this.color; 		
	}

	public String toString() {
		return "oneColor" + super.toString();
	}
}

class ImgIdBaseImage extends ImgId {
	Image baseImage;
	
	public ImgIdBaseImage(Image baseImage, int width, int height) {
		super(width, height);
		this.baseImage = baseImage;
	}

	public boolean equals(Object o){
		if (super.equals(o)) {
			ImgIdBaseImage other = (ImgIdBaseImage) o;
			if (other.baseImage == this.baseImage) {
				return true;
			}
		}
		return false;
	}	
	
	public int hashCode() {
		return super.hashCode() + this.baseImage.hashCode(); 		
	}

	public String toString() {
		return baseImage + super.toString();
	}

}



class CacheEntry {
	Image img;
	long lastUsedTime;
	
	public CacheEntry(Image img) {
		this.img = img;
		this.lastUsedTime = System.currentTimeMillis();
	}	
}

public class ImageCache {

	private final static Logger logger = Logger.getInstance(ImageCache.class,Logger.DEBUG);
	private static Hashtable imageCache = new Hashtable();
	private static long lastCleanup = 0;

	
	/** returns an Image object of the given filename in its original width and height, either by loading it or taking it from the cache */
	public static Image getImageFromFile(String fileName) {
		return getImageFromFile(fileName, 0, 0);
	}
	
	/** returns an Image object of the given filename in the given width and height, either by loading it or taking it from the cache */
	public static Image getImageFromFile(String fileName, int width, int height) {
		ImgIdFile id = new ImgIdFile(fileName, width, height);
		CacheEntry cacheEntry = (CacheEntry) imageCache.get(id);
		if (cacheEntry != null) {
			return getCachedImage(cacheEntry);
		}
		Image img;
		// file system image
		try {
			img = Image.createImage(fileName);
			// System.out.println("Caching " + id.toString());				
		} catch (IOException ioe) {
			logger.error("Cannot load " + fileName);
			return null;
		}
		if (width != 0) {
			img = ImageTools.scaleImage(img, width, height);
		}
		return cacheImage(img, id);			
	}

	/**
	 * @param img
	 * @param id
	 */
	public static Image cacheImage(Image img, ImgId id) {
		if (img != null) {
			// System.out.println("Caching " + id.toString());				
			imageCache.put(id, new CacheEntry(img));
		}
		return img;
	}

	/** returns an Image object of the given base image in the given width and height, either by resizing it or taking it from the cache */
	public static Image getScaledImage(Image baseImage, int width, int height) {
		ImgIdBaseImage id = new ImgIdBaseImage(baseImage, width, height);
		CacheEntry cacheEntry = (CacheEntry) imageCache.get(id);
		if (cacheEntry != null) {
			return getCachedImage(cacheEntry);
		}
		Image img = null;
		if (width != 0) {
			img = ImageTools.scaleImage(baseImage, width, height);
		}
		return cacheImage(img, id);			
	}

	/** returns an Image object of the given one color in the given width and height, either by creating it or taking it from the cache */
	public static Image getOneColorImage(int color, int width, int height) {
		cleanup();
		ImgIdOneColor id = new ImgIdOneColor(color, width, height);
		CacheEntry cacheEntry = (CacheEntry) imageCache.get(id);
		if (cacheEntry != null) {
			return getCachedImage(cacheEntry);
		}
		Image img;
		int rawSize = width * height;
		int[] rawOutput = new int[rawSize];
        for (int outOffset = 0; outOffset < rawSize;) {
            rawOutput[outOffset++]= color;
        }
	    try {
	        img = Image.createRGBImage(rawOutput, width, height, true);
        } catch (Exception e) {
			logger.exception("Cannot create one color image", e);
			return null;
        }
		return cacheImage(img, id);	
	}
	
	public static Image getCachedImage(CacheEntry cacheEntry) {
		cacheEntry.lastUsedTime = System.currentTimeMillis();
		//System.out.println("Loaded from cache " + id.toString());
		return cacheEntry.img;
	}
	
	public static void cleanup() {
		long now = System.currentTimeMillis(); 
		if (Math.abs(now - lastCleanup) > 30000) {
			cleanup(30000);
		}	
	}
	
	/** uncache all cached images that have not been used longer than the given time */
	public static void cleanup(int minUnusedMillis) {
	    Vector remove = new Vector();
		long now = System.currentTimeMillis(); 
		for (Enumeration e = imageCache.keys() ; e.hasMoreElements() ;) {
			ImgId id = (ImgId) e.nextElement();
			CacheEntry cachedImage = (CacheEntry) imageCache.get(id);
			if (Math.abs(now - cachedImage.lastUsedTime) > minUnusedMillis) {
				remove.addElement(id);
				// System.out.println("Uncaching " + id.toString());
			}
	    }
		
		while (!remove.isEmpty()) {
			imageCache.remove(remove.firstElement());
			remove.removeElementAt(0);
		}
		lastCleanup = now;
	}

	public static void testImageCache() {
		Image img;
		System.out.println("Testing ImageCache");
		img = getImageFromFile("/bus.png", 20, 20);
		img = getImageFromFile("/church.png", 20, 20);
		img = getImageFromFile("/bus.png", 20, 20);
		cleanup(0);
		img = getImageFromFile("/bus.png", 20, 20);
		img = getImageFromFile("/church.png", 20, 20);
		img = getImageFromFile("/church.png", 20, 20);
	}
}

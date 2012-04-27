package de.ueller.midlet.util;
/*
 * GpsMid - Copyright (c) 2012 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

class ImgId {
	String fileName = null;
	Image baseImage = null;
	int oneColor = 0;
	short width;
	short height;
	
	public ImgId(String fileName, int width, int height) {
		this.width = (short) width;
		this.height = (short) height;
		this.fileName = fileName;
	}

	public ImgId(Image baseImage, int width, int height) {
		this.width = (short) width;
		this.height = (short) height;
		this.baseImage = baseImage;
	}

	public ImgId(int oneColor, int width, int height) {
		this.width = (short) width;
		this.height = (short) height;
		this.oneColor = oneColor;
	}
	
	public boolean equals(Object o){
		if (o == null || !(o instanceof ImgId) ) {
			return false;			
		}
		ImgId other = (ImgId) o;
		if (other.width != this.width || other.height != this.height || other.baseImage != this.baseImage || other.oneColor != this.oneColor
			||
			(other.fileName != null && this.fileName != null && !other.fileName.equalsIgnoreCase(this.fileName))
		) {
			return false;
		}
		return true;
	}	
	
	public int hashCode() {
		return this.width + this.height * 256 + ((this.fileName == null) ? (this.baseImage == null ? this.oneColor : baseImage.hashCode()) : this.fileName.hashCode()); 		
	}
	
	public String toString() {
		if (this.fileName == null && this.baseImage == null) {
			return "oneColor(" + this.width + "x" + this.height + ")";
		}
		return this.fileName == null ? super.toString() : this.fileName;
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
	public static Image getImage(String fileName) {
		return getImage(fileName, 0, 0);
	}
	
	/** returns an Image object of the given filename in the given width and height, either by loading it or taking it from the cache */
	public static Image getImage(String fileName, int width, int height) {
		return getImage(fileName, null, 0, width, height);
	}

	/** returns an Image object of the given base image in the given width and height, either by resizing it or taking it from the cache */
	public static Image getScaledImage(Image baseImg, int width, int height) {
		return getImage(null, baseImg, 0, width, height);
	}

	/** returns an Image object of the given one color in the given width and height, either by creating it or taking it from the cache */
	public static Image getOneColorImage(int oneColor, int width, int height) {
		return getImage(null, null, oneColor, width, height);
	}
	
	/** returns an Image object of the given parameters in the given width and height, either by loading / resizing / creating it or taking it from the cache */
	public static Image getImage(String fileName, Image baseImg, int oneColor, int width, int height) {
		long now = System.currentTimeMillis(); 
		if (Math.abs(now - lastCleanup) > 30000) {
			cleanup(30000);
		}
		
		ImgId id = (baseImg == null) ?
				(fileName == null ? new ImgId(oneColor, width, height) : new ImgId(fileName, width, height))
				:
				new ImgId(baseImg, width, height);
		Image img = null;
		CacheEntry cacheEntry = (CacheEntry) imageCache.get(id);
		if (cacheEntry == null) {
			if (baseImg == null) {
				if (fileName != null) {
					// file system image
					try {
						baseImg = Image.createImage(fileName);
						System.out.println("Loaded " + id.toString());				
					} catch (IOException ioe) {
						logger.error("Cannot load " + fileName);
					}
				}
			}
			// one color image
			if (fileName == null && baseImg == null) {
				int rawSize = width * height;
				int[] rawOutput = new int[rawSize];        
		        for (int outOffset = 0; outOffset < rawSize;) {
		            rawOutput[outOffset++]= oneColor;
		        }
			    try {
			        img = Image.createRGBImage(rawOutput, width, height, true);
					System.out.println("Created " + id.toString());
		        } catch (Exception e) {
					logger.exception("Cannot create one color image", e);
		        }
	        } else if (width != 0) {
				img = ImageTools.scaleImage(baseImg, width, height);
			}
			if (img != null) {
				imageCache.put(id, new CacheEntry(img));
			}
		}
		else {
			img = cacheEntry.img;
			cacheEntry.lastUsedTime = System.currentTimeMillis();
			//imageCache.put(id, cacheEntry);
			System.out.println("Loaded from cache " + id.toString());
		}
		
		return img;		
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
				System.out.println("Uncaching " + id.toString());
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
		img = getImage("/bus.png", 20, 20);
		img = getImage("/church.png", 20, 20);
		img = getImage("/bus.png", 20, 20);
		cleanup(0);
		img = getImage("/bus.png", 20, 20);
		img = getImage("/church.png", 20, 20);
		img = getImage("/church.png", 20, 20);
	}
}

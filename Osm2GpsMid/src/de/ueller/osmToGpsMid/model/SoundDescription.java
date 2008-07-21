/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

/**
 * @author sk750
 *
 */
public class SoundDescription {
	public String	name;
	public String   soundFile;
	
	public String toString() {
		return "Name: " + name + " SoundFile: " + soundFile;
	}
}

/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model.name;

import java.util.Comparator;

/**
 * @author hmueller
 *
 */
public class NameComperator implements Comparator<Name> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Name n1, Name n2) {
		return n1.getName().compareToIgnoreCase(n2.getName());
	}

}

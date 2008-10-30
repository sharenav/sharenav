/**
 * OSM2GpsMid 
 *  
 *
 */
package de.ueller.osmToGpsMid;

import de.ueller.osmToGpsMid.model.Bounds;

/**
 * 
 *
 */
public interface SelectionListener {
	
	public void regionSelected(Bounds bound);

}

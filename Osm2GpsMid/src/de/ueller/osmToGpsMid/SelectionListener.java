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
	
	public void regionMarked(Bounds bound);
	
	public void pointDoubleClicked(float lat, float lon);

}

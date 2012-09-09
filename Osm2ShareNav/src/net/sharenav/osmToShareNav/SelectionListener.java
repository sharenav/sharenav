/**
 * OSM2ShareNav 
 *  
 *
 */
package net.sharenav.osmToShareNav;

import org.openstreetmap.gui.jmapviewer.Coordinate;

import net.sharenav.osmToShareNav.model.Bounds;

/**
 * 
 *
 */
public interface SelectionListener {
	
	public void regionMarked(Bounds bound);
	
	public void pointDoubleClicked(float lat, float lon);

	/**
	 * @param clickPoint
	 */
	public void addRouteDestination(Coordinate clickPoint);

}

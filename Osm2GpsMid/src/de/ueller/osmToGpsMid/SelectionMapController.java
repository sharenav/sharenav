package de.ueller.osmToGpsMid;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import org.openstreetmap.gui.jmapviewer.JMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerRectangle;

import de.ueller.osmToGpsMid.model.Bounds;

/**
 * This code is adapted from the DefaultMapController of the
 * org.openstreetmap.gui.jmapviewer package
 * 
 */
public class SelectionMapController extends JMapController implements
		MouseListener, MouseMotionListener, MouseWheelListener {

	private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK
			| MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;

	

	private Point lastDragPoint;
	private Point2D.Double startSelPoint;
	private MapMarkerRectangle selRegion;

	private boolean isMoving = false;
	private boolean isSelecting = false;

	private boolean movementEnabled = true;

	private boolean wheelZoomEnabled = true;
	private boolean doubleClickZoomEnabled = true;
	private SelectionListener selListener;
	
	public SelectionMapController(JMapViewer map, SelectionListener selListener) {
		super(map);
		this.selListener = selListener;
	}

	public void mouseDragged(MouseEvent e) {
		if (movementEnabled && isMoving) {
			// Is only the selected mouse button pressed?
			if ((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
				Point p = e.getPoint();
				if (lastDragPoint != null) {
					int diffx = lastDragPoint.x - p.x;
					int diffy = lastDragPoint.y - p.y;
					map.moveMap(diffx, diffy);
				}
				lastDragPoint = p;
			}
		}

		if (isSelecting) {
			if (((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON3_DOWN_MASK) ||
					((((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON1_DOWN_MASK)) && ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) > 0))){
				Point p = e.getPoint();

				Point2D.Double endSelPoint = map.getPosition(e.getPoint());
				selRegion.setRectanlge(startSelPoint.x, startSelPoint.y,
						endSelPoint.x, endSelPoint.y);
				map.repaint();

			}
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (doubleClickZoomEnabled && e.getClickCount() == 2
				&& e.getButton() == MouseEvent.BUTTON1)
			map.zoomIn(e.getPoint());
	}

	public void mousePressed(MouseEvent e) {

		if ((e.getButton() == MouseEvent.BUTTON3) || 
				((e.getButton() == MouseEvent.BUTTON1) && ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) > 0))) {
			isSelecting = true;
			startSelPoint = map.getPosition(e.getPoint());
			selRegion = new MapMarkerRectangle(Color.BLACK, new Color(0x9fafafaf, true),startSelPoint.y,
					startSelPoint.x, startSelPoint.y, startSelPoint.x);
			map.addMapMarkerArea(selRegion);

		} else if (e.getButton() == MouseEvent.BUTTON1) {
			lastDragPoint = null;
			isMoving = true;
		}
	}

	public void mouseReleased(MouseEvent e) {

		lastDragPoint = null;
		isMoving = false;
		if (isSelecting) {
			isSelecting = false;
			Point p = e.getPoint();
			Point2D.Double endSelPoint = map.getPosition(e.getPoint());
			Bounds bound = new Bounds();
			bound.maxLat = Math.max((float)startSelPoint.x,(float)endSelPoint.x);
			bound.maxLon = Math.max((float)startSelPoint.y,(float)endSelPoint.y);;
			bound.minLat = Math.min((float)startSelPoint.x,(float)endSelPoint.x);
			bound.minLon = Math.min((float)startSelPoint.y,(float)endSelPoint.y);;
			selListener.regionSelected(bound);

			map.removeMapMarkerArea(selRegion);
		}

	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (wheelZoomEnabled)
			map.setZoom(map.getZoom() - e.getWheelRotation(), e.getPoint());
	}

	public boolean isMovementEnabled() {
		return movementEnabled;
	}

	/**
	 * Enables or disables that the map pane can be moved using the mouse.
	 * 
	 * @param movementEnabled
	 */
	public void setMovementEnabled(boolean movementEnabled) {
		this.movementEnabled = movementEnabled;
	}

	public boolean isWheelZoomEnabled() {
		return wheelZoomEnabled;
	}

	public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
		this.wheelZoomEnabled = wheelZoomEnabled;
	}

	public boolean isDoubleClickZoomEnabled() {
		return doubleClickZoomEnabled;
	}

	public void setDoubleClickZoomEnabled(boolean doubleClickZoomEnabled) {
		this.doubleClickZoomEnabled = doubleClickZoomEnabled;
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

}

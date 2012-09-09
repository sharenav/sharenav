package net.sharenav.osmToShareNav;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapArea;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;

import net.sharenav.osmToShareNav.model.Bounds;


/**
 * This code is adapted from the DefaultMapController of the
 * org.openstreetmap.gui.jmapviewer package.
 * It implements map moving by dragging with the left
 * mouse button, region marking by dragging with the right mouse button,
 * region selecting with double click and zooming by mouse wheel.
 */
public class SelectionMapController extends JMapController implements
		MouseListener, MouseMotionListener, MouseWheelListener {

	private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK
			| MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;

	private Point mLastDragPoint;
	private Coordinate mStartSelPoint;
	private MapArea mSelRegion;

	private boolean mIsMoving = false;
	private boolean mIsSelecting = false;

	private boolean mMovementEnabled = true;

	private boolean mWheelZoomEnabled = true;
	private SelectionListener mSelListener;
	
	
	public SelectionMapController(JMapViewer map, SelectionListener selListener) {
		super(map);
		mSelListener = selListener;
	}

	public void mouseDragged(MouseEvent e) {
		if (mMovementEnabled && mIsMoving) {
			// Is only the selected mouse button pressed?
			if ((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
				Point p = e.getPoint();
				if (mLastDragPoint != null) {
					int diffx = mLastDragPoint.x - p.x;
					int diffy = mLastDragPoint.y - p.y;
					map.moveMap(diffx, diffy);
				}
				mLastDragPoint = p;
			}
		}

		if (mIsSelecting) {
			if (((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON3_DOWN_MASK) ||
					((((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == MouseEvent.BUTTON1_DOWN_MASK)) 
							&& ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) > 0))) {

				Coordinate endSelPoint = map.getPosition(e.getPoint());
				mSelRegion.setRectangle(mStartSelPoint, endSelPoint);
				map.repaint();
			}
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			Coordinate clickPoint = map.getPosition(e.getPoint());
			mSelListener.pointDoubleClicked((float)clickPoint.getLat(), 
					(float)clickPoint.getLon());
		} else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1 && ((e.getModifiersEx() & (MouseEvent.ALT_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) > 0)){
			Coordinate clickPoint = map.getPosition(e.getPoint());
			mSelListener.addRouteDestination(clickPoint);
			map.addMapMarker(new MapMarkerDot(clickPoint.getLat(), clickPoint.getLon()));
			map.repaint();
		}
	}

	public void mousePressed(MouseEvent e) {
		if ((e.getButton() == MouseEvent.BUTTON3) || 
				((e.getButton() == MouseEvent.BUTTON1) 
						&& ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) > 0))) {
			mIsSelecting = true;
			mStartSelPoint = map.getPosition(e.getPoint());
			mSelRegion = new MapArea(Color.BLACK, new Color(0x9fafafaf, true), 
					mStartSelPoint, mStartSelPoint);
			map.addMapRectangle(mSelRegion);
		} else if (e.getButton() == MouseEvent.BUTTON1) {
			mLastDragPoint = null;
			mIsMoving = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		mLastDragPoint = null;
		mIsMoving = false;
		if (mIsSelecting) {
			mIsSelecting = false;
			Coordinate endSelPoint = map.getPosition(e.getPoint());
			Bounds bound = new Bounds();
			bound.maxLat = Math.max((float)mStartSelPoint.getLat(), (float)endSelPoint.getLat());
			bound.maxLon = Math.max((float)mStartSelPoint.getLon(), (float)endSelPoint.getLon());
			bound.minLat = Math.min((float)mStartSelPoint.getLat(), (float)endSelPoint.getLat());
			bound.minLon = Math.min((float)mStartSelPoint.getLon(), (float)endSelPoint.getLon());
			mSelListener.regionMarked(bound);

			map.removeMapRectangle(mSelRegion);
		}

	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (mWheelZoomEnabled) {
			map.setZoom(map.getZoom() - e.getWheelRotation(), e.getPoint());
		}
	}

	public boolean isMovementEnabled() {
		return mMovementEnabled;
	}

	/**
	 * Enables or disables that the map pane can be moved using the mouse.
	 * 
	 * @param movementEnabled
	 */
	public void setMovementEnabled(boolean movementEnabled) {
		mMovementEnabled = movementEnabled;
	}

	public boolean isWheelZoomEnabled() {
		return mWheelZoomEnabled;
	}

	public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
		mWheelZoomEnabled = wheelZoomEnabled;
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

}

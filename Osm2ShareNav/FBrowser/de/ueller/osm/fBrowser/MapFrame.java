/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osm.fBrowser;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.gui.jmapviewer.DefaultMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;

/**
* 
* Demonstrates the usage of {@link JMapViewer}
* 
* @author Jan Peter Stotz
* 
*/
public class MapFrame extends JInternalFrame {

	private static final long serialVersionUID = 1L;
	final JMapViewer map = new JMapViewer();
//	private MapMarkerRectangle	marker;
	private Tile	viewTile;
	private BWay	way;
	public MapFrame() {
		super("JMapViewer Demo",true, //resizable
		          true, //closable
		          true, //maximizable
		          true);//iconifiable
		setSize(400, 400);
		
		// final JMapViewer map = new JMapViewer(new MemoryTileCache(),4);
		// map.setTileLoader(new OsmFileCacheTileLoader(map));
		 new DefaultMapController(map);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		setExtendedState(JFrame.MAXIMIZED_BOTH);
		JPanel panel = new JPanel();
		JPanel helpPanel = new JPanel();
		add(panel, BorderLayout.NORTH);
		add(helpPanel, BorderLayout.SOUTH);
		JLabel helpLabel =
				new JLabel("Use right mouse button to move,\n "
						+ "left double click or mouse wheel to zoom.");
		helpPanel.add(helpLabel);
		JButton button = new JButton("setDisplayToFitMapMarkers");
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				map.setDisplayToFitMapMarkers();
			}
		});
		JComboBox tileSourceSelector =
				new JComboBox(new TileSource[] { new OsmTileSource.Mapnik(),
						new OsmTileSource.TilesAtHome(), new OsmTileSource.CycleMap() });
		tileSourceSelector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				map.setTileSource((TileSource) e.getItem());
			}
		});
		JComboBox tileLoaderSelector =
				new JComboBox(new TileLoader[] { new OsmFileCacheTileLoader(map),
						new OsmTileLoader(map) });
		tileLoaderSelector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				map.setTileLoader((TileLoader) e.getItem());
			}
		});
		map.setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
		panel.add(tileSourceSelector);
		panel.add(tileLoaderSelector);
		final JCheckBox showMapMarker = new JCheckBox("Map markers visible");
		showMapMarker.setSelected(map.getMapMarkersVisible());
		showMapMarker.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				map.setMapMarkerVisible(showMapMarker.isSelected());
			}
		});
		panel.add(showMapMarker);
		final JCheckBox showTileGrid = new JCheckBox("Tile grid visible");
		showTileGrid.setSelected(map.isTileGridVisible());
		showTileGrid.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				map.setTileGridVisible(showTileGrid.isSelected());
			}
		});
		panel.add(showTileGrid);
		final JCheckBox showZoomControls = new JCheckBox("Show zoom controls");
		showZoomControls.setSelected(map.getZoomContolsVisible());
		showZoomControls.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				map.setZoomContolsVisible(showZoomControls.isSelected());
			}
		});
		panel.add(showZoomControls);
		panel.add(button);
		add(map, BorderLayout.CENTER);



		// map.setDisplayPositionByLatLon(49.807, 8.6, 11);
		// map.setTileGridVisible(true);
	}

	/**
	 * @param tile
	 */
	public synchronized void setSelected(Tile tile) {
		try {
//			setMapView(tile);
			map.setIgnoreRepaint(true);
			if (tile instanceof SingleTile){
				((SingleTile) tile).setSelected(null);
			}
//		System.out.println("setMarker " + tile);
			double f=180d/Math.PI;
			map.setMapRectangleList(new ArrayList<MapRectangle>());
			map.addMapRectangle(tile);
			setIgnoreRepaint(false);
		} catch (Exception e) {
			System.err.println("pro while setSel");
			e.printStackTrace();
		}
	}
	/**
	 * @param sel
	 */
	public void setSelected(BWay sel) {
		this.way = sel;
		sel.getTile().setSelected(sel);
		map.addMapRectangle(sel.getTile());
		System.out.println("set selected Way");
	}

	void setMapView(Tile tile){
		double f=180d/Math.PI;
		int mapZoomMax =  JMapViewer.MAX_ZOOM;
	int newZoom = mapZoomMax;
	int x_min=OsmMercator.LonToX(tile.minLon*f, mapZoomMax);
	int y_min=OsmMercator.LatToY(tile.maxLat*f, mapZoomMax);
	int x_max=OsmMercator.LonToX(tile.maxLon*f, mapZoomMax);
	int y_max=OsmMercator.LatToY(tile.minLat*f, mapZoomMax);
	int height = Math.max(0, map.getHeight());
	int width = Math.max(0, map.getWidth());
	int x = x_max - x_min;
	int y = y_max - y_min;
	while (x > width || y > height) {
		// System.out.println("zoom: " + zoom + " -> " + x + " " + y);
		newZoom--;
		x >>= 1;
		y >>= 1;
	}
	x = x_min + (x_max - x_min) / 2;
	y = y_min + (y_max - y_min) / 2;
	int z = 1 << (mapZoomMax - newZoom);
	x /= z;
	y /= z;
	map.setDisplayPosition(x, y, newZoom);
	}

    public Point getMapPosition(double lat, double lon, boolean checkOutside) {
    	return map.getMapPosition(lat,lon, checkOutside);
    }


}

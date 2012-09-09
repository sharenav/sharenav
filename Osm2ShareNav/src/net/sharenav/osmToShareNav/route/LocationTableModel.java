/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.route;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;
;

/**
 * @author hmu
 *
 */
public class LocationTableModel extends AbstractTableModel {
		final Vector<Location> routeList;
		  /**
		 * @param routeList
		 */
		public LocationTableModel(Vector<Location> routeList) {
			super();
			this.routeList = routeList;
		}

		public Object getValueAt(int row, int column) {
		    if (row < routeList.size()){
		    	Location l=routeList.get(row);
		    	switch (column){
//		    	case 0: return row+1;
		    	case 0: if (l.getCity()!=null){
		    		return l.getCountry()+"-"+l.getZip()+" "+l.getCity()+"/"+l.getStreet();
		    	}
		    		return l.getNode().lat+":"+l.getNode().lon;
//		    	case 2: return l.getNode().lon;
		    	}
		    }
		    return null;
		  }

		  public int getColumnCount() {
		    return 1;
		  }

		  public int getRowCount() {
		    return routeList.size();
		  }
		}
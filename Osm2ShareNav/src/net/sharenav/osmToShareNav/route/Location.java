/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.route;

import net.sharenav.osmToShareNav.model.Node;

/**
 * @author hmu
 *
 */
public class Location {
	private Node node;
	private String search;
	private String city;
	private String zip;
	private String country;
	private String street;
	/**
	 * @return the street
	 */
	public String getStreet() {
		return street;
	}
	/**
	 * @return the node
	 */
	public Node getNode() {
		return node;
	}
	/**
	 * @param node
	 */
	public Location(Node node) {
		super();
		this.node = node;
	}
	/**
	 * @param node
	 */
	public Location(float lat,float lon) {
		this.node = new Node(lat,lon,-1);
	}

	/**
	 * @return the search
	 */
	public String getSearch() {
		return search;
	}
	/**
	 * @param search the search to set
	 */
	public void setSearch(String search) {
		this.search = search;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the zip
	 */
	public String getZip() {
		return zip;
	}
	/**
	 * @param zip the zip to set
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}
	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @param group
	 */
	public void setStreet(String str) {
		this.street=str;
	}

	public String toPropertyString(int locationNr) {
		return	"routeDest." + locationNr + ".lat = " + node.lat + "\r\n" +
				"routeDest." + locationNr + ".lon = " + node.lon + "\r\n";
	}

}

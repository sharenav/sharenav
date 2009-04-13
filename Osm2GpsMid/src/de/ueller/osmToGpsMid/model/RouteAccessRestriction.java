package de.ueller.osmToGpsMid.model;


public class RouteAccessRestriction {
	public String restrictionFor;
	public String key;
	public String values;

	public RouteAccessRestriction(String restrictionFor, String key, String values) {
		this.restrictionFor = restrictionFor;
		this.key = key;
		this.values = values;
	}

	public String toString() {
		return restrictionFor + ": " + "No access for " + key + "=" + values.substring(0, values.length()-2);
	}
}

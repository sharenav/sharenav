package de.ueller.osmToGpsMid.model;


public class RouteAccessRestriction {
	public String restrictionFor;
	public String key;
	public String values;
	public boolean permitted;

	public RouteAccessRestriction(String restrictionFor, String key, String values, boolean permitted) {
		this.restrictionFor = restrictionFor;
		this.key = key;
		this.values = values;
		this.permitted = permitted;
	}

	public String toString() {
		return restrictionFor + ": " + "Ways tagged with " + key + "=" + values.substring(0, values.length()-1) + " become " + (permitted?"permitted":"forbidden") + " for routing";
	}
}

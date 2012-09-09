package net.sharenav.osmToShareNav.model;


public class RouteAccessRestriction {
	public String key;
	public String values;
	public boolean permitted;

	public RouteAccessRestriction(String key, String values, boolean permitted) {
		this.key = key;
		this.values = values;
		this.permitted = permitted;
	}

	public String toString() {
		return "Ways tagged with " + key + "=" + values.substring(0, values.length()-1) + " become " + (permitted?"permitted":"forbidden") + " for routing";
	}
}

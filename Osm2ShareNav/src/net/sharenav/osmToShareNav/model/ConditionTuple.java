package net.sharenav.osmToShareNav.model;


public class ConditionTuple {
	public String key;
	public String value;
	public boolean exclude;
	public boolean regexp;
	public boolean properties;
	
	public String toString() {
		return "Specialisation " + key + "=" + value + " excluded:" + exclude +
		    " regexp: " + regexp + " properties: "+ properties;
	}
}

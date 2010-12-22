package de.ueller.osmToGpsMid.model;


public class ConditionTuple {
	public String key;
	public String value;
	public boolean exclude;
	
	public String toString() {
		return "Specialisation " + key + "=" + value + " excluded:" + exclude;
	}
}

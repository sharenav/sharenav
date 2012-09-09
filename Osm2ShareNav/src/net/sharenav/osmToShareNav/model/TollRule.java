package net.sharenav.osmToShareNav.model;


public class TollRule {
	public String key;
	public String values;
	public boolean enableToll;
	public boolean debugTollRule;

	public TollRule(String key, String values, boolean enableToll, boolean debugTollRule) {
		this.key = key;
		this.values = values;
		this.enableToll = enableToll;
		this.debugTollRule = debugTollRule;
	}

	public String toString() {
		return "Ways tagged with " + key + "=" + values.substring(0, values.length()-1) + " become" + (enableToll?"":" no") + " toll roads (last match on way is relevant)";
	}
}

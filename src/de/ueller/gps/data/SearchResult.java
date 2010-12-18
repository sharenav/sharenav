package de.ueller.gps.data;

public class SearchResult {
	public byte type;
	public int nameIdx = -1;
	public int urlIdx = -1;
	public int phoneIdx = -1;
	public int[] nearBy;
	public float lat;
	public float lon;
	public float dist = -1.0f;
}

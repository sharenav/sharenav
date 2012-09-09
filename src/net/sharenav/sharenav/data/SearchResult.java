package net.sharenav.sharenav.data;

public class SearchResult {
	// use negative types for nodes in this structure, though not in index data since map format 66
	public short type;
	//#if polish.api.bigsearch
	public byte source;
	public long resultid = 0;
	public long osmID = 0;
	public int preMatchIdx = 0;
	//public int postMatchIdx = 0;
        //#endif
	public int nameIdx = -1;
	public int urlIdx = -1;
	public int phoneIdx = -1;
	public int[] nearBy;
	public float lat;
	public float lon;
	public float dist = -1.0f;
}

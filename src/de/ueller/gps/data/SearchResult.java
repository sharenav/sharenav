package de.ueller.gps.data;

public class SearchResult {
	//#if polish.api.bigstyles
	public short type;
	//#else
	public byte type;
	//#endif
	//#if polish.api.bigsearch
	public byte source;
	public long resultid = 0;
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

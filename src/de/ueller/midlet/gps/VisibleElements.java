package de.ueller.midlet.gps;


import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.PaintContext;

public class VisibleElements {
	
	private final static int MAX_AREA=100;
	private final static int MAX_PATH=500;
	private final static int MAX_POI=100;
	private int id;
	
	int areasCount=0;
	Way[] areas = new Way[MAX_AREA];
	Node[][] areaNodes = new Node[MAX_AREA][];
	int pathsCount=0;
	Way[] paths = new Way[MAX_PATH];
	Node[][] pathNodes = new Node[MAX_PATH][];
	int poisCount=0;
	Node[] poi = new Node[MAX_POI];
	
	public VisibleElements(int id) {
		super();
		this.id = id;
	}

	public void cleanAll(){
		areasCount=0;
		pathsCount=0;
		poisCount=0;
	}
	
	public void paint(PaintContext pc){
		int i;
		for (i=0;i<areasCount;i++){
			areas[i].setColor(pc);
			areas[i].paintAsArea(pc, areaNodes[i]);
		}
		for (i=0;i<pathsCount;i++){
			paths[i].setColor(pc);
			paths[i].paintAsPath(pc, pathNodes[i]);
		}
		for (i=0;i<poisCount;i++){
			poi[i].paint(pc);
		}
	}
	
	public void addArea(Way w,Node[] nodes){
		if (areasCount < MAX_AREA){
			areas[areasCount]=w;
			areaNodes[areasCount]=nodes;
			areasCount++;
		} else {
			System.out.println("no more areas");
		}
	}
	public void addPath(Way w,Node[] nodes){
		if (pathsCount < MAX_PATH){
			paths[pathsCount]=w;
			pathNodes[pathsCount]=nodes;
			pathsCount++;
		} else {
			System.out.println("no more paths");
		}
	}
	public void addPoi(Node n){
		if (poisCount < MAX_POI){
			poi[poisCount]=n;
			poisCount++;
		} else {
			System.out.println("no more POIs");
		}
	}

	public int getId() {
		return id;
	}
}

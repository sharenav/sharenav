package net.sharenav.osmToShareNav.model;


public class Line extends Entity {

	public Node	from;
	public Node	to;
	public Long id;

	public Line(Node from, Node to, long id) {
		this.from = from;
		this.to = to;
		this.id = id;
	}

	public Line(long id) {
		this.id=id;
		from=to=null;
	}
	
	public boolean isValid(){
		if (from==null) return false;
		if (to==null) return false;
		return true;
	}
	public String toString(){
		return "Line("+id+") from " + from + " to " + to;  
	}

}

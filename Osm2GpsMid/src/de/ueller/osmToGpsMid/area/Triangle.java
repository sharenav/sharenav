package de.ueller.osmToGpsMid.area;

import de.ueller.osmToGpsMid.model.Bounds;



public class Triangle {
	private Vertex[] vert=new Vertex[3];
	public boolean opt=false;
	
	public Triangle(Vertex n1,Vertex n2, Vertex n3) {
		getVert()[0]=n1;
		getVert()[1]=n2;
		getVert()[2]=n3;
	}
	
	public boolean isVertexInside(Vertex n){
		if (n==getVert()[0] && n==getVert()[1] && n==getVert()[2]){
			return false;
		}
		float n1=getVert()[1].minus(getVert()[0]).cross(n.minus(getVert()[0]));
		float n2=getVert()[2].minus(getVert()[1]).cross(n.minus(getVert()[1]));
		float n3=getVert()[0].minus(getVert()[2]).cross(n.minus(getVert()[2]));
		 if(n1*n2 > 0.0 && n1*n3 > 0.0 && n2*n3 > 0.0){
			 return true;
		 }		 
		 return false;
		 

	}
	
	@Override
	public String toString() {
		return new String("tri " + getVert()[0] + getVert()[1] + getVert()[2]);
	}
	
	public Vertex getMidpoint(){
		float lat=0f;
		float lon=0f;
		for (int i=0;i<3;i++){
			lat+=getVert()[i].getLat();
			lon+=getVert()[i].getLon();
		}
		return new Vertex(lat/3,lon/3,0l);
	}
	
	public Bounds extendBound(Bounds b){
		if (b==null) b=new Bounds();
		for (int i=0;i<3;i++){
			b.extend(getVert()[i].getLat(), getVert()[i].getLon());
		}
		return b;
	}

//	/**
//	 * @param vert the vert to set
//	 */
//	private void setVert(Vertex[] vert) {
//		this.vert = vert;
//	}

	/**
	 * @return the vert
	 */
	public Vertex[] getVert() {
		return vert;
	}
	
	public int equalVert(Triangle other){
		int ret=0;
		for (int i=0;i<3;i++){
			for (int j=0;j<3;j++){
				if (getVert()[i].getNode() == other.getVert()[j].getNode()) ret++;
			}
		}
		return ret;
	}

}

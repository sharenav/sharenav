/**
 * @author hmueller
 * 
 */
package de.ueller.midlet.gps.tile;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;


public class PaintContext {
	public final static byte DRAW_AREAS_NO=0;
	public final static byte DRAW_AREAS_OUTLINE=1;
	public final static byte DRAW_AREAS_FILL=2;
	public Graphics g;
	public Projection p;
	public int xSize;
	public int ySize;
	public Node screenRU=new Node();
	public Node screenLD=new Node();
	IntPoint swapLineP=new IntPoint(0,0);
	IntPoint lineP1=null;
	IntPoint lineP2=new IntPoint(0,0);
	public float scale=15000f;
	byte viewId=1;
	public final Image IMG_PARKING=Image.createImage("/images/parking.png");

	public byte drawAreas=DRAW_AREAS_NO;
	public boolean showTileOutline=false;
	public Trace trace;
	public PaintContext() throws Exception{
		super();
		// TODO Auto-generated constructor stub
	}

	

}

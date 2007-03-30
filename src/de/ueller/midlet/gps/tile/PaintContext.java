/**
 * @author hmueller
 * 
 */
package de.ueller.midlet.gps.tile;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Projection;


public class PaintContext extends ScreenContext {
	public final static byte DRAW_AREAS_NO=0;
	public final static byte DRAW_AREAS_OUTLINE=1;
	public final static byte DRAW_AREAS_FILL=2;
	public Graphics g;
	public Projection p;
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * a valid object after method exit 
	 */
	public IntPoint swapLineP=new IntPoint(0,0);
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * null after method exit. Point will used as startpoint of a line to
	 * indicate the fact that there is no startpoint at the begin of painting,
	 * this points to null 
	 */
	public IntPoint lineP1=null;
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * a valid Object after method exit. Point will used as endpoint of a line.
	 * the calculation go directly to the literals insid the object.
	 */
	public IntPoint lineP2=new IntPoint(0,0);
	public final Image IMG_PARKING=Image.createImage("/images/parking.png");
	public final Image IMG_FUEL=Image.createImage("/images/fuel.png");
	public final Image IMG_SCHOOL=Image.createImage("/images/school.png");
	public final Image IMG_TELEPHONE=Image.createImage("/images/telephone.png");

	public byte drawAreas=DRAW_AREAS_NO;
	public boolean showTileOutline=false;
	public Trace trace;
	public PaintContext() throws Exception{
		super();
		// TODO Auto-generated constructor stub
	}
	

}

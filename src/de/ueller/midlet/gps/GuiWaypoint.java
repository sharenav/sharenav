package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;


public class GuiWaypoint extends List implements CommandListener,
		GpsMidDisplayable, UploadListener {

	private final static Logger logger=Logger.getInstance(GuiWaypoint.class,Logger.DEBUG);
	
	private final Command SEND_ALL_CMD = new Command("Send All", Command.ITEM, 1);	
	private final Command LOAD_CMD = new Command("Load Gpx", Command.ITEM, 2);
	private final Command SEND_CMD = new Command("Send", Command.ITEM, 4);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);	
	private final Command SALL_CMD = new Command("Select All", Command.ITEM, 2);
	private final Command DSALL_CMD = new Command("Deselect All", Command.ITEM, 2);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command GOTO_CMD = new Command("Display", Command.OK,6);

	private PositionMark[] waypoints;
	private final Trace parent;	
	
	public GuiWaypoint(Trace parent) throws Exception {
		super("Waypoints", List.MULTIPLE);
		this.parent = parent;
		setCommandListener(this);
		initWaypoints();
		
		//addCommand(SEND_CMD);
		addCommand(SEND_ALL_CMD);
		addCommand(LOAD_CMD);
		addCommand(DEL_CMD);		
		addCommand(SALL_CMD);		
		addCommand(DSALL_CMD);
		addCommand(BACK_CMD);		
		addCommand(GOTO_CMD);
		
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void initWaypoints() {
		this.deleteAll();		
		waypoints = parent.gpx.listWayPt();
		for (int i = 0; i < waypoints.length; i++) {
			this.append(waypoints[i].displayName,null);
		}
	}

	public void commandAction(Command c, Displayable d) {
		logger.debug("got Command " + c);
		if (c == SEND_CMD) {			
			/* TODO */			
			return;
		}
		if (c == DEL_CMD) {
			boolean[] sel = new boolean[waypoints.length];
			this.getSelectedFlags(sel);			
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					parent.gpx.deleteWayPt(waypoints[i]);
				}
			}			
			initWaypoints();
			return;
		}
		if ((c == SALL_CMD) || (c == DSALL_CMD)) {
			boolean select = (c == SALL_CMD);
			boolean[] sel = new boolean[waypoints.length];
			for (int i = 0; i < waypoints.length; i++)
				sel[i] = select;
			this.setSelectedFlags(sel);
			return;
		}		
		
		if (c == BACK_CMD) {			
			parent.show();
			return;
		}
		
		if (c == SEND_ALL_CMD) {
			parent.gpx.sendWayPt(parent.getConfig().getGpxUrl(), this);			
			return;
			
		}
		if (c == LOAD_CMD) {			
			GuiGpxLoad ggl = new GuiGpxLoad(this);
			ggl.show();
			return;
			
		}
		
		if (c == GOTO_CMD) {
			float w = 0, e = 0, n = 0, s = 0; 
			int idx = -1;
			boolean[] sel = new boolean[waypoints.length];
			this.getSelectedFlags(sel);			
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					if (idx == -1) {
						idx = i;
						w =  waypoints[i].lon;
						e =  waypoints[i].lon;
						n = waypoints[i].lat;
						s = waypoints[i].lat;
					} else {
						idx = -2;
						if (waypoints[i].lon < w)
							w = waypoints[i].lon;
						if (waypoints[i].lon > e)
							e = waypoints[i].lon;
						if (waypoints[i].lat < s)
							s = waypoints[i].lat;
						if (waypoints[i].lat > n)
							n = waypoints[i].lat;
					}					
				}
			}
			if (idx > -1) {
				parent.setTarget(waypoints[idx]);				
			} else {
				IntPoint intPoint1 = new IntPoint(10,10);
				IntPoint intPoint2 = new IntPoint(getWidth() - 10,getHeight() - 10);				
				Node n1 = new Node(n*MoreMath.FAC_RADTODEC,w*MoreMath.FAC_RADTODEC);
				Node n2 = new Node(s*MoreMath.FAC_RADTODEC,e*MoreMath.FAC_RADTODEC);
				float scale = parent.projection.getScale(n1, n2, intPoint1, intPoint2);				
				parent.receivePosItion((n-s)/2 + s, (e-w)/2 + w, scale*1.2f);				
			}
			parent.show();
			return;
		}

	}
	
	public void completedUpload() {
		Alert alert = new Alert("Information");
		alert.setString("Completed GPX upload");
		Display.getDisplay(parent.getParent()).setCurrent(alert);
	}

	public void show() {
		Display.getDisplay(parent.getParent()).setCurrent(this);
	}
}

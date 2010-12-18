/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.tests;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class NMEAsimScreen extends Canvas implements CommandListener {
	
	private Command exitCmd;
	// list of available message to display
	private Vector msgs = new Vector();
	private int midx;
	private Font f;
	private int w;
	private int h;
	private int fh;
	private NMEAsimMidlet mid;

	public NMEAsimScreen(NMEAsimMidlet mid) {
		super();
		exitCmd = new Command("Exit", Command.EXIT, 1);
		addCommand(exitCmd);
		setCommandListener(this);
	}

	protected void paint(Graphics g) {
		
		g.setColor( 255, 255, 255 );
	    g.fillRect( 0, 0, w, h );
	    g.setColor( 0, 0, 0 );
	    g.setFont( f );
		
		int y = 15;
		midx = 0;
		
		if ( f == null )
		{
			// cache the font and width,height value
			// when it is used the first time
			f = Font.getFont(  Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL );
			w = this.getWidth();
			h = this.getHeight();
			fh = f.getHeight();
		}

		// render the messages on screen
		for ( int i= midx; i< msgs.size(); i++ )
		{
			String s = (String)msgs.elementAt(i);
			g.drawString( s, 0, y, Graphics.BASELINE | Graphics.LEFT );
			y += fh;			
		}		
	}

	public void commandAction(Command cmd, Displayable disp) {
		if (cmd == exitCmd) {
			addMsg("Closing application");
			mid.quitApp();
		}
	}
	
	public void addMsg(String msg) {
		System.out.println("INFO: " + msg);
		msgs.addElement(msg);
		repaint();
	}

}

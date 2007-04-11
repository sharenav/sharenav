package de.ueller.midlet.gps;

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Splash extends Canvas implements CommandListener,Runnable{
	private Image splash;
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command("Accept", Command.BACK, 1);
    private final Command EXIT_CMD = new Command("Decline", Command.EXIT, 2);
	private final GpsMid main;
	String[] txt={"Copyright:",
				  " Harald Müller",
				  "Thanks for source parts to:",
				  " Nikolay Klimchuk",
	              "Artwork:",
	              " Tobias Müller",
	              "Applicaton:",
	              " licensd by GPL2",
	              " http://www.gnu.org/",
	              "Map data:",
	              " from OpenStreetMap",
	              " licensed by CC 2.0",
	              " http://creativecommons.org/"};
	private Font f;
	int top=0;
	private Thread processorThread;
	private boolean shutdown=false;
	private int ssize;
	private int topStart=106;
	private int space;



	public Splash(GpsMid main){
	this.main = main;
	try {
		splash=Image.createImage("/images/Gps-splash.png");

	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	f=Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
	space=getHeight()-topStart;
	ssize=f.getHeight()*txt.length+space;
	top=-space;
	show();
	addCommand(BACK_CMD);
	addCommand(EXIT_CMD);
	setCommandListener(this);
	processorThread = new Thread(this,"Splash");
	processorThread.setPriority(Thread.MIN_PRIORITY);
	processorThread.start();

	}

	protected void paint(Graphics g) {
		// cleans the screen
		g.setColor(180, 180, 255);
		g.setFont(f);
		g.fillRect(0, 0, getWidth(), getHeight());
		int sp=f.getHeight();
		g.drawImage(splash,getWidth()/2, 0,Graphics.HCENTER|Graphics.TOP);

		g.setColor(255, 40, 40);
		int startLine=top/sp;
		int yc= topStart-top % sp;
		g.setClip(0,topStart, getWidth(), getHeight()-topStart);
		boolean visible=false;
		for (int i=startLine; i< txt.length;i++){
			visible=true;
			if (i >= 0){
			int w=f.stringWidth(txt[i]);
			if (w > (getWidth()-10)){
				System.out.println("to long");
			}
			g.drawString(txt[i], 5, yc, 0);
			}
			yc+=sp;
			if (! visible){
				top=-space;
			}
		}
		
	}

	public void commandAction(Command c, Displayable d) {
        if (c == BACK_CMD) {
        	main.show();
        	return;
        }
        if (c == EXIT_CMD) {
        	shutdown=true;
        	main.exit();
        	return;
        }
	}
	public void show(){
	Display.getDisplay(main).setCurrent(this);
	}

	public void run() {
		while (! shutdown){
		synchronized (this) {
			try {
				wait(40);
			} catch (InterruptedException e) {

			}
			top++;
			if (top > (ssize)){
				top=-space;
			}
			repaint();
		}
		}
	}
}

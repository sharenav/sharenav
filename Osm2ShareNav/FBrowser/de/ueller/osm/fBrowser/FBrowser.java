/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osm.fBrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.JMapViewer;


/**
 * @author hmueller
 *
 */
public class FBrowser extends JFrame implements ActionListener {
    /**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	JDesktopPane desktop;
	private String root="D:/java/Workspace/ShareNav-Release/Osm2ShareNav/testdata";
	
	public String getRoot() {
		return root;
	}
	
	public MapFrame getMap() {
		return map;
	}



	private MapFrame	map;

    
    /**
	 * 
	 */
	public FBrowser() {
        super("GPSMid File Browser");

        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                  screenSize.width  - inset*2,
                  screenSize.height - inset*2);

        //Set up the GUI.
        desktop = new JDesktopPane(); //a specialized layered pane
//        createFrame(); //create first "window"
        setContentPane(desktop);
        setJMenuBar(createMenuBar());

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
	}
	
	
	protected JMenuBar createMenuBar() {
	        JMenuBar menuBar = new JMenuBar();

	        //Set up the lone menu.
	        JMenu menu = new JMenu("Open");
	        menu.setMnemonic(KeyEvent.VK_O);
	        menuBar.add(menu);
	        //Set up the lone menu.
	        JMenuItem cmenu = new JMenuItem("Config");
	        cmenu.setMnemonic(KeyEvent.VK_C);
	        cmenu.setAccelerator(KeyStroke.getKeyStroke(
	                KeyEvent.VK_C, ActionEvent.ALT_MASK));
	        cmenu.setActionCommand("config");
	        cmenu.addActionListener(this);
	        menuBar.add(cmenu);

	        //setup Dictionary
	        for (int i=0 ; i<=3 ; i++){
	        	menuEntry(i, menu);
	        }
	        //Set up the map menu item.
	        JMenuItem menuItem = new JMenuItem("Map");
	        menuItem.setMnemonic(KeyEvent.VK_M);
	        menuItem.setAccelerator(KeyStroke.getKeyStroke(
	                KeyEvent.VK_M, ActionEvent.ALT_MASK));
	        menuItem.setActionCommand("map");
	        menuItem.addActionListener(this);
	        menu.add(menuItem);

	        //Set up the second menu item.
	        menuItem = new JMenuItem("Quit");
	        menuItem.setMnemonic(KeyEvent.VK_Q);
	        menuItem.setAccelerator(KeyStroke.getKeyStroke(
	                KeyEvent.VK_Q, ActionEvent.ALT_MASK));
	        menuItem.setActionCommand("quit");
	        menuItem.addActionListener(this);
	        menu.add(menuItem);

	        return menuBar;
	    }
	 
	 private void menuEntry(int level,JMenu menu){
	        JMenuItem menuItem = new JMenuItem("Level " + level);
	        menuItem.setMnemonic(KeyEvent.VK_0 + level);
	        menuItem.setAccelerator(KeyStroke.getKeyStroke(
	        		KeyEvent.VK_0 + level, ActionEvent.ALT_MASK));
	        menuItem.setActionCommand("level" + level);
	        menuItem.addActionListener(this);
	        menu.add(menuItem);

	 }


	 @Override
	 public void actionPerformed(ActionEvent e) {
		 String cmd=e.getActionCommand();
		 System.out.println("call " + cmd);
		 if (cmd.startsWith("level")) { 
			 createFrame(cmd);
		 } else if ("config".equals(cmd)) {
			 askDataDir();
		 } else if ("map".equals(cmd)){
			 openMap();
		 } else {
			 quit();
		 }
	 }

	 
	/** Create a new map internal frame.
	 */
	protected void openMap() {
		map = new MapFrame();
		map.setVisible(true); //necessary as of 1.3
		desktop.add(map);
		try {
			map.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {
		}
	}


    /** Create a new internal Tree frame which shows the tile structure.
     */
    protected void createFrame(String cmd) {
        DictTreeFrame frame = new DictTreeFrame(cmd,this);
        frame.setVisible(true); //necessary as of 1.3
        desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {}
    }

    /** Quit the application.
     */
    protected void quit() {
        System.exit(0);
    }

	private void askDataDir() {
		JFileChooser chooser = new JFileChooser(root);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select map subdirectory");
		
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return "Data Dir";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			root = chooser.getSelectedFile().getAbsolutePath();
		}

		createFrame("level0");
		createFrame("level1");
		createFrame("level2");
		createFrame("level3");
		openMap();
	}

	
    /**
     * Create the GUI and show it. For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        FBrowser frame = new FBrowser();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display the window.
        frame.setVisible(true);
    }


    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    
	/**
	 * @param tile
	 */
	public void setSelected(Tile tile) {
		if (map != null){
			map.setSelected(tile);
			tile.setMap(map);
		}
	}
	
	
	/**
	 * @param sel
	 */
	public void setSelected(BWay sel) {
		if (map != null){
			map.setSelected(sel);
			sel.getTile().setMap(map);
		}
	}
}

/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osm.fBrowser;


import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import sun.reflect.generics.tree.Tree;


/**
 * @author hmueller
 *
 */
public class DictTreeFrame extends JInternalFrame implements TreeSelectionListener {
	private static final long serialVersionUID = 1L;
	static final int xOffset = 30
	static final int yOffset = 30;
	static int openFrameCount = 0;
	private Tile dict;
	private int	level;
	private final String	root;
	private final FBrowser	fBrowser;

	/**
	 * 
	 */
	public DictTreeFrame(String slevel, FBrowser fBrowser) {
		super("Dict Tree " + slevel,
		          true, //resizable
		          true, //closable
		          true, //maximizable
		          true);//iconifiable
		this.fBrowser = fBrowser;
		this.root = fBrowser.getRoot();
		openFrameCount++;
		level = Integer.parseInt(slevel.substring(5, 6));
		setLocation(xOffset * openFrameCount, yOffset * openFrameCount);
		try {
			JTree tree = new JTree(readDict((byte)level));
			tree.addTreeSelectionListener(this);
			add(new JScrollPane(tree));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pack();
	}

	/**
	 * @param tree
	 */
	private Tile readDict(byte zl) throws IOException {
		InputStream is = new FileInputStream(root + "/dat/dict-" + zl + ".dat");
		DataInputStream ds = new DataInputStream(is);
		if (! "DictMid".equals(ds.readUTF())){
			throw new IOException("not a DictMid-file");
		}
		byte type = ds.readByte();
		switch (type) {
			case Tile.TYPE_MAP:
				dict = new SingleTile(ds, 1, zl, root);
				break;
			case Tile.TYPE_CONTAINER:
				dict = new ContainerTile(ds, 1, zl, root);
				break;
			case Tile.TYPE_EMPTY:
				// empty tile;
				break;
			case Tile.TYPE_FILETILE:
				dict = new FileTile(ds, 1, zl, root);
				break;

			default:
				break;
			}
			
			ds.close();
			return dict;
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath path = e.getPath();
		JTree tree = (JTree) e.getSource();
		Object sel = tree.getLastSelectedPathComponent();
		if (sel instanceof Tile){
			fBrowser.setSelected((Tile) sel);
		} 
		if (sel instanceof BWay){
			fBrowser.setSelected((BWay) sel);
		}
		
	}


}

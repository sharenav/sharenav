package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Harald Mueller apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.OutputStream;

//#if polish.api.fileConnection
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
//#if polish.api.mmapi
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
//#if polish.api.advancedmultimedia
import javax.microedition.amms.control.ImageFormatControl;
import javax.microedition.amms.control.camera.CameraControl;
import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.amms.control.camera.SnapshotControl;
//#endif
//#endif
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;

/**
 * 
 * This class provides a camera capture form based on jsr-135 and jsr-234
 * It allows the user to capture pictures with a builtin camera
 * directly from with in GpsMid.
 * 
 * The idea is to directly be able to geotag images from within GpsMid
 * for example when survaying for OSM.
 *
 */
public class GuiCamera extends Canvas implements CommandListener, GuiCameraInterface, SelectionListener, GpsMidDisplayable {

	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command CAPTURE_CMD = new Command("Capture", Command.OK, 5);
	private final Command STORE_CMD = new Command("Select directory", Command.ITEM, 5);

	private final static Logger logger = Logger.getInstance(GuiCamera.class,
			Logger.DEBUG);
	//#if polish.api.mmapi	
	private Player mPlayer;
	private VideoControl video;
	//#if polish.api.advancedmultimedia
	private FocusControl focus;	
	//#endif
	//#endif
	private Trace parent;
	private String basedirectory;

	public void init(Trace parent, Configuration config) {
		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(CAPTURE_CMD);
		addCommand(STORE_CMD);
		setCommandListener(this);
		setUpCamera();
	}

	/*
	 * This sets up the basic parameters of the camera and initialises
	 * the view finder of the camera
	 */
	private void setUpCamera() {
		//#if polish.api.mmapi 		
		try {
			basedirectory = GpsMid.getInstance().getConfig().getPhotoUrl();
			logger.info("Storing photos at " + basedirectory);			
			try {
				/**
				 * Nokia seems to have used a non standard locator to specify
				 * the Player for capturing photos
				 */
				mPlayer = Manager.createPlayer("capture://image");
			} catch (MediaException me) {
				/**
				 * This is the standard locator.
				 * As Nokia also supports the capture://video, but does not
				 * allow to take snapshots with it, need to use this ordering
				 */
				mPlayer = Manager.createPlayer("capture://video");
			}
			if (mPlayer == null) {
				logger.error("Couldn't initialize camera player");
				return;
			}
			mPlayer.realize();

			video = (VideoControl) mPlayer
					.getControl("VideoControl");
			video.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
			video.setDisplayFullScreen(true);
			video.setVisible(true);

			mPlayer.start();
			//#if polish.api.advancedmultimedia
			CameraControl camera = (CameraControl) mPlayer
					.getControl("CameraControl");
			logger.info("Initialised Camera: " + camera);

			if (camera != null) {
				int[] res = camera.getSupportedStillResolutions();
				for (int i = 0; i < res.length; i++) {
					logger.info("res: " + res[i]);
				}
				camera.setStillResolution(res.length / 2 - 1);
				logger.info("Resolution: " + camera.getStillResolution());
			} else {
				logger.error("Can't get access to camera properties");
			}

			ImageFormatControl format = (ImageFormatControl) mPlayer
					.getControl("ImageFormatControl");
			if (format != null) {
				logger.info("Format: " + format.getFormat());
				logger.info("MeteDataSupportMode: "
						+ format.getMetadataSupportMode());

				String[] formats = format.getSupportedFormats();
				for (int i = 0; i < formats.length; i++) {
					logger.info("Supported format: " + formats[i]);
				}

				String[] metaData = format.getSupportedMetadataKeys();
				if (metaData != null) {
					for (int i = 0; i < metaData.length; i++) {
						logger.info("Supported metadata: " + metaData[i]);
					}
				} else {
					logger.info("MetaData not supported! Won't be able to geotag from within GpsMid");
				}
				
			} else {
				logger.info("Device doesn't support ImageFormatControl");
			}
			//#endif
		} catch (SecurityException se) {
			logger.exception("Security Exception: ", se);
		} catch (IOException e) {
			logger.exception("IOexception", e);
		} catch (MediaException e) {
			logger.exception("MediaExcpetion", e);
		}
		//#else
		logger.error("Camera control is not supported by this device");
		//#endif
	}
	
	private void takePicture135() throws SecurityException {
		logger.info("Captureing photo with jsr 135");
		//#if polish.api.mmapi
		if (mPlayer == null || video == null) {
			logger.error("mPlayer is not initialised, couldn't capture photo");
			return;
		}
		
		try {
			int idx = 0; 
			byte [] photo = video.getSnapshot("encoding=jpeg&width=1600&height=1200");
			logger.info("Captured photo of size : " + photo.length);
			
			FileConnection fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
			while (fc.exists()) {
				fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
				idx++;
			}
			fc.create();			
			OutputStream fos = fc.openOutputStream();			
			fos.write(photo, 0, photo.length);
			fos.close();
		} catch (MediaException e) {
			logger.exception("Couldn't take picture", e);			
		} catch (IOException e) {
			logger.exception("IOException capturing the photo", e);
		}		
		//#endif
 
	}

	private void takePicture234() throws SecurityException {
		logger.info("Captureing photo with jsr 234");
		//#if polish.api.mmapi && polish.api.advancedmultimedia		
		if (mPlayer == null) {
			logger.error("mPlayer is not initialised, couldn't capture photo");
			return;
		}

		try {
			if (focus == null) {
				focus = (FocusControl) mPlayer.getControl("FocusControl");
			}
			if (focus != null) {
				if (focus.isAutoFocusSupported()) {
					focus.setFocus(FocusControl.AUTO);
				} else {
					// Otherwise, try the "mountain" or infinity setting. Find out what was actually set.
					int focusSet = focus.setFocus(Integer.MAX_VALUE);
				}
			} else {
				logger.info("Couldn't get focus control");
			}
			
			logger.info("Finished focusing");

			SnapshotControl snapshot = (SnapshotControl) mPlayer
					.getControl("SnapshotControl");
			if (snapshot == null) {
				logger.info("Couldn't aquire SnapshotControl, falling back to jsr135");
				takePicture135();
				return;
			}
			
			String basedir = basedirectory.substring(8,basedirectory.length());
			try {
				snapshot.setDirectory(basedir);
				snapshot.setFilePrefix("GpsMid-"
						+ HelperRoutines.formatSimpleDateSecondNow() + "-");
				snapshot.setFileSuffix(".jpg");
			} catch (Exception e) {
				logger.exception("Failed to set directory", e);
			}
			
			logger.info("About to capture to file " + snapshot.getDirectory() + snapshot.getFilePrefix());

			// Take one picture and allow the user to keep or discard it
			snapshot.start(SnapshotControl.FREEZE);
		} catch (MediaException e) {
			logger.exception("Couldn't capture photo", e);
		}
		//#endif

	}
	
	private void takePicture() {
		try {
			//#if polish.api.mmapi && polish.api.advancedmultimedia
			takePicture234();
			//#else
			takePicture135();			
			//#endif
		} catch (SecurityException se) {
			logger.error("Permission denied to take a photo");
		}
	}

	protected void paint(Graphics arg0) {
		//Don't have to do anything here, as the video Player will
		//do the painting for us.
	}

	public void keyPressed(int keyCode) {
		
		if (keyCode == Configuration.KEYCODE_CAMERA_CAPTURE) {
			takePicture();
		}
		if (keyCode == Configuration.KEYCODE_CAMERA_COVER_CLOSE) {
			//#if polish.api.mmapi && polish.api.advancedmultimedia
			if (mPlayer != null)
				mPlayer.close();
			//#endif
			parent.show();
		}
		
	}

	public void commandAction(Command c, Displayable disp) {
		if (c == BACK_CMD) {
			//#if polish.api.mmapi && polish.api.advancedmultimedia
			if (mPlayer != null)
				mPlayer.close();
			//#endif
			parent.show();
		}
		if (c == CAPTURE_CMD) {
			if ((basedirectory == null) ||(!basedirectory.startsWith("file:///"))) {
				logger.error("You need to select a directory where to save first");
				return;
			}
			takePicture();
		}
		if (c == STORE_CMD) {
			//#if polish.api.fileConnection
			new FsDiscover(this,this,basedirectory,true,null,"Directory to store photos");
			//#endif
		}

	}

	public void show() {
		GpsMid.getInstance().show(this);
		//Display.getDisplay(parent.getParent()).setCurrent(this);
	}

	public void selectedFile(String url) {
		logger.info("Setting picture directory to " + url);
		GpsMid.getInstance().getConfig().setPhotoUrl(url);
		basedirectory = url;
		
	}

}

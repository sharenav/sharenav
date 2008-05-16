package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Harald Mueller apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
//#if polish.api.mmapi && polish.api.advancedmultimedia
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import javax.microedition.amms.control.ImageFormatControl;
import javax.microedition.amms.control.camera.CameraControl;
import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.amms.control.camera.SnapshotControl;
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
public class GuiCamera extends Canvas implements CommandListener, GuiCameraInterface {

	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command CAPTURE_CMD = new Command("Capture", Command.OK, 5);

	private final static Logger logger = Logger.getInstance(GuiCamera.class,
			Logger.DEBUG);
	//#if polish.api.mmapi && polish.api.advancedmultimedia
	private Player mPlayer;
	private FocusControl focus;
	SnapshotControl snapshot;
	//#endif
	private Trace parent;

	public void init(Trace parent, Configuration config) {
		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(CAPTURE_CMD);
		setCommandListener(this);
		setUpCamera();
	}

	/*
	 * This sets up the basic parameters of the camera and initialises
	 * the view finder of the camera
	 */
	private void setUpCamera() {
		//#if polish.api.mmapi && polish.api.advancedmultimedia
		logger.info("Audio capture: " + System.getProperty("supports.audio.capture"));
		logger.info("Audio encodings: " + System.getProperty("audio.encodings"));
		try {
			mPlayer = Manager.createPlayer("capture://video");
			if (mPlayer == null) {
				logger.error("Couldn't initialize camera player");
				return;
			}
			mPlayer.realize();

			VideoControl mVideoControl = (VideoControl) mPlayer
					.getControl("VideoControl");
			mVideoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
			mVideoControl.setDisplayFullScreen(true);
			mVideoControl.setVisible(true);

			mPlayer.start();

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

	private void takePicture() {
		logger.info("Captureing photo");
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

			SnapshotControl snapshot = (SnapshotControl) mPlayer
					.getControl("SnapshotControl");
			if (snapshot == null) {
				logger.error("Couldn't aquire SnapshotControl");
				return;
			}

			snapshot.setDirectory("e:/other/");
			snapshot.setFilePrefix("GpsMid-"
					+ HelperRoutines.formatSimpleDateNow() + "-");
			snapshot.setFileSuffix(".jpg");

			// Take one picture and allow the user to keep or discard it
			snapshot.start(SnapshotControl.FREEZE);

		} catch (MediaException e) {
			logger.exception("Couldn't capture photo", e);
		}
		//#endif

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
			takePicture();
		}

	}

	public void show() {
		GpsMid.getInstance().show(this);
		//Display.getDisplay(parent.getParent()).setCurrent(this);
	}

}

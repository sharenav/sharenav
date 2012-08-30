package de.ueller.gpsmid.ui;

/*
 * GpsMid - Copyright (c) 2008 Harald Mueller apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

//#if polish.api.fileConnection
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;
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
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.ui.GpsMidMenu;
import de.ueller.midlet.ui.SelectionListener;
import de.ueller.util.HelperRoutines;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.StringTokenizer;

import de.enough.polish.util.Locale;

//#if polish.android
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import de.enough.polish.android.midlet.MidletBridge;
//#endif

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
public class GuiCamera extends Canvas implements CommandListener, ItemCommandListener, GuiCameraInterface, SelectionListener, GpsMidDisplayable
//#if polish.android
, Camera.PictureCallback
//#endif
 {

	private final Command CANCEL_CMD = new Command(Locale.get("generic.Cancel")/*Cancel*/, GpsMidMenu.BACK, 5);
	private final Command OK_CMD = new Command(Locale.get("generic.OK")/*Ok*/, GpsMidMenu.OK, 5);
	private final Command OK2_CMD = new Command(Locale.get("generic.OK")/*Ok*/, GpsMidMenu.OK, 5);
	private final Command CAPTURE_CMD = new Command(Locale.get("guicamera.Capture")/*Capture*/, Command.OK, 5);
	private final Command STORE_CMD = new Command(Locale.get("guicamera.SelectDir")/*Select directory*/, Command.ITEM, 5);
	public final Command SETUP_CMD = new Command(Locale.get("guicamera.Setup")/*Setup*/, Command.ITEM, 6);
	
	private volatile static GuiCamera instance;

	private final static Logger logger = Logger.getInstance(GuiCamera.class,
			Logger.DEBUG);
	//#if polish.api.mmapi	
	//#if polish.android
	private Camera camera;
	private Player mPlayer;
	private VideoControl video;
	//#else
	private Player mPlayer;
	private VideoControl video;
	//#endif
	//#if polish.api.advancedmultimedia
	private FocusControl focus;	
	//#endif
	//#endif
	private String encoding;
	// if parent == null, called from GuiDiscover
	// FIXME Maybe can be simplifies with setting up a simple setup/init GpsMidDiscoverable instead of separate
	// Trace and GuiDiscover variables
	private Trace parent;
	private GuiDiscover setupParent;
	private String basedirectory;
	
	private ChoiceGroup selectJsrCG;
	private ChoiceGroup selectExifCG;
	private TextField   encodingTF;
	private ChoiceGroup encodingCG;

	private byte[] photo;

        //#if polish.android
	private SurfaceHolder surfaceHolder;
	private SurfaceView surfaceView = null;
	private View uiView = null;
	//#endif

	public void init(Trace parent) {
		this.parent = parent;
		addCommand(CANCEL_CMD);
		addCommand(CAPTURE_CMD);
		//addCommand(STORE_CMD);
		//#if polish.android
		//#else
		addCommand(SETUP_CMD);
		//#endif
		setCommandListener(this);
		setUpCamera();
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		instance = this;
	}

	public static GuiCamera getInstance() {
		return instance;
	}

	//#if polish.android
	SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
		}
    
		public void surfaceChanged(SurfaceHolder holder,
					   int format, int width,
					   int height) {
			startPreview();
			surfaceView.setFocusable(true);
			surfaceView.setFocusableInTouchMode(true);
			surfaceView.requestFocus();
			surfaceView.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
						keyReleased(keyCode);
						return true;
					}
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						return true;
					}
					return false;
				}
			});
		}
    
		public void surfaceDestroyed(SurfaceHolder holder) {
		}
	};
	public void startPreview() {
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException ioe) {
			//
		}
		surfaceView.requestFocus();
		camera.startPreview();
	}
	//#endif
	/*
	 * This sets up the basic parameters of the camera and initialises
	 * the view finder of the camera
	 */
	private void setUpCamera() {
		//#if polish.api.mmapi 		
		try {
			basedirectory = Configuration.getPhotoUrl();
			//#debug debug
			logger.debug("Storing photos at " + basedirectory);
			//#if polish.android
			// open the first camera
			// between 0 and getNumberOfCameras()-1.
			try {
				camera = Camera.open(0);
			} catch (RuntimeException re) {
				logger.error(Locale.get("guicamera.CouldntInitializeCameraPlayer")/*Could not initialize camera player*/);
				return;
			}
			uiView = MidletBridge.instance.getWindow().getCurrentFocus();
			surfaceView = new SurfaceView(MidletBridge.instance.getWindow().getContext());

			Camera.Parameters params = camera.getParameters();
			surfaceHolder = surfaceView.getHolder();
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			surfaceHolder.addCallback(surfaceCallback);
			MidletBridge.instance.getWindow().setContentView(surfaceView);
			//#else
			try {
				/**
				 * Nokia seems to have used a non standard locator to specify
				 * the Player for capturing photos
				 */
				//#if polish.android
				// Android doesn't work currently (2012-03-29) at all, but if it will be added to J2MEPolish,
				// it probably won't be the non-Standard Nokia way
				mPlayer = Manager.createPlayer("capture://video");
				//#else
				mPlayer = Manager.createPlayer("capture://image");
				//#endif
			} catch (MediaException me) {
				/**
				 * This is the standard locator.
				 * As Nokia also supports the capture://video, but does not
				 * allow to take snapshots with it, need to use this ordering
				 */
				mPlayer = Manager.createPlayer("capture://video");
			}
			if (mPlayer == null) {
				logger.error(Locale.get("guicamera.CouldntInitializeCameraPlayer")/*Could not initialize camera player*/);
				return;
			}
			// Then again Samsung (Xcover 271) doesn't complain on creating capture://image, but barfs later, so we'll try to catch that one here
			try {
				mPlayer.realize();
				video = (VideoControl) mPlayer
					.getControl("VideoControl");
				video.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
				video.setDisplayFullScreen(true);
				video.setVisible(true);

				mPlayer.start();
			} catch (MediaException me) {
				mPlayer = Manager.createPlayer("capture://video");
				mPlayer.realize();
				video = (VideoControl) mPlayer
					.getControl("VideoControl");
				video.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
				video.setDisplayFullScreen(true);
				video.setVisible(true);

				mPlayer.start();
			}
			if (mPlayer == null) {
				logger.error(Locale.get("guicamera.CouldntInitializeCameraPlayer")/*Could not initialize camera player*/);
				return;
			}

			//#endif
			//#if polish.api.advancedmultimedia
			CameraControl camera = (CameraControl) mPlayer
					.getControl("CameraControl");
			//#debug trace
			logger.trace("Initialised Camera: " + camera);

			if (camera != null) {
				int[] res = camera.getSupportedStillResolutions();
				for (int i = 0; i < res.length; i++) {
					//#debug debug
					logger.debug("res: " + res[i]);
				}
				camera.setStillResolution(res.length / 2 - 1);
				//#debug debug
				logger.debug("Resolution: " + camera.getStillResolution());
			} else {
				logger.error(Locale.get("guicamera.CantGetAccessToCameraProp")/*Can not get access to camera properties*/);
			}

			ImageFormatControl format = (ImageFormatControl) mPlayer
					.getControl("ImageFormatControl");
			if (format != null) {
				//#mdebug
				logger.debug("Format: " + format.getFormat());
				logger.debug("MeteDataSupportMode: "
						+ format.getMetadataSupportMode());

				String[] formats = format.getSupportedFormats();
				
				for (int i = 0; i < formats.length; i++) {
					logger.debug("Supported format: " + formats[i]);
				}

				String[] metaData = format.getSupportedMetadataKeys();
				if (metaData != null) {
					for (int i = 0; i < metaData.length; i++) {
						logger.debug("Supported metadata: " + metaData[i]);
					}
				}
				//#enddebug
				
			} else {
				logger.debug("Device does not support ImageFormatControl");
			}
			//#endif
			
			
			
		} catch (SecurityException se) {
			logger.exception(Locale.get("guicamera.SecurityException")/*Security Exception: */, se);
			//#if polish.android
			//#else
			mPlayer = null;
			video = null;
			//#endif
		//#if polish.android
		//#else
		} catch (IOException e) {
			logger.exception(Locale.get("guicamera.IOexception")/*IOexception*/, e);
			//#if polish.android
			//#else
			mPlayer = null;
			video = null;
			//#endif
		} catch (MediaException e) {
			logger.exception(Locale.get("guicamera.MediaException")/*MediaException*/, e);
			//#if polish.android
			//#else
			mPlayer = null;
			video = null;
			//#endif
		logger.error(Locale.get("guicamera.CameraControlNotSupported")/*Camera control is not supported by this device*/);
		//#endif polish.android
		}
		//#endif
	}
	
	//#if polish.android
	private void exitPreview() {
		if (uiView != null) {
			surfaceView.setVisibility(View.GONE);
			if (uiView.getParent() != null) {
				((ViewGroup)uiView.getParent()).removeView(uiView);
			}
			if (surfaceView != null && surfaceView.getParent() != null) {
				((ViewGroup)surfaceView.getParent()).removeView(surfaceView);
			}
			MidletBridge.instance.getWindow().setContentView(uiView);
			uiView = null;
		}

	}

	public void onPictureTaken(byte[] data, Camera camera) {
		photo = data;
		addCommand(OK2_CMD);
		//GpsMid.getInstance().show(this);
		try {
			int idx = 0; 
			//repaint();
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ADD_EXIF)) {
				photo = addExifEncoding(photo);
			}
			//#debug debug
			logger.debug("Captured photo of size : " + photo.length);
			
			FileConnection fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
			while (fc.exists()) {
				fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
				idx++;
			}
			fc.create();
			OutputStream fos = fc.openOutputStream();
			fos.write(photo, 0, photo.length);
			fos.close();
			//video.setVisible(true);
		} catch (IOException e) {
			logger.exception(Locale.get("guicamera.IOExceptionCapturingPhoto")/*IOException capturing the photo*/, e);
		} catch (NullPointerException npe) {
			logger.exception(Locale.get("guicamera.FailedToTakePicture")/*Failed to take a picture*/, npe);
		}
		cameraOff(null);
		parent.show();
	}
	//#endif
	private void takePicture135() throws SecurityException {
		logger.info("Captureing photo with jsr 135");
		//#if polish.api.mmapi
		//#if polish.android
		//#else
		if (mPlayer == null || video == null) {
			logger.error(Locale.get("guicamera.mPlayerNotInitedCaptureFail")/*mPlayer is not initialised, could not capture photo*/);
			return;
		}
		//#endif
		
		try {
			//#if polish.android
			camera.takePicture(null, null, null, this);
		} finally {
		}
			//#else
			int idx = 0; 
			byte [] photo = video.getSnapshot(Configuration.getPhotoEncoding());
			repaint();
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ADD_EXIF)) {
				photo = addExifEncoding(photo);
			}
			//#debug debug
			logger.debug("Captured photo of size : " + photo.length);
			
			FileConnection fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
			while (fc.exists()) {
				fc = (FileConnection)Connector.open(basedirectory + "GpsMid-" + HelperRoutines.formatInt2(idx) + "-" + HelperRoutines.formatSimpleDateSecondNow() + ".jpg");
				idx++;
			}
			fc.create();
			OutputStream fos = fc.openOutputStream();
			fos.write(photo, 0, photo.length);
			fos.close();
			video.setVisible(true);
		} catch (MediaException e) {
			logger.exception(Locale.get("guicamera.CouldntTakePicture")/*Could not take picture*/, e);
		} catch (IOException e) {
			logger.exception(Locale.get("guicamera.IOExceptionCapturingPhoto")/*IOException capturing the photo*/, e);
		} catch (NullPointerException npe) {
			logger.exception(Locale.get("guicamera.FailedToTakePicture")/*Failed to take a picture*/, npe);
		}
			//#endif
		//#endif
 
	}

	private void takePicture234() throws SecurityException {
		logger.info("Captureing photo with jsr 234");
		//#if polish.api.mmapi && polish.api.advancedmultimedia		
		if (mPlayer == null) {
			logger.error(Locale.get("guicamera.mPlayerNotInitedCaptureFail")/*mPlayer is not initialised, could not capture photo*/);
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
				//#debug debug
				logger.debug("Could not get focus control");
			}
			
			//#debug trace
			logger.trace("Finished focusing");

			SnapshotControl snapshot = (SnapshotControl) mPlayer
					.getControl("SnapshotControl");
			if (snapshot == null) {
				logger.info("Could not aquire SnapshotControl, falling back to jsr135");
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
				logger.exception(Locale.get("guicamera.FailedSettingDirectory")/*Failed to set directory*/, e);
			}
			
			//#debug
			logger.debug("About to capture to file " + snapshot.getDirectory() + snapshot.getFilePrefix());

			// Take one picture and allow the user to keep or discard it
			snapshot.start(SnapshotControl.FREEZE);
			
			//#if polish.api.pdaapi
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ADD_EXIF)) {
				try {
					Thread.sleep(3000);
					FileConnection fcDir = (FileConnection)Connector.open(basedirectory);
					Enumeration filelist = fcDir.list();
					while (filelist.hasMoreElements()) {
						String fileName = (String) filelist.nextElement();
						logger.debug("File in camera dir: " + fileName);
						if (fileName.indexOf(snapshot.getFilePrefix()) >= 0) {
							logger.debug("Found photo, adding Exif: " + fileName);
							FileConnection fcIn = (FileConnection)Connector.open(basedirectory + fileName);
							InputStream is = fcIn.openInputStream();
							byte [] image = new byte[(int)fcIn.fileSize()];
							is.read(image);
							FileConnection fcOut = (FileConnection)Connector.open(basedirectory + fileName + "-exif.jpg");
							fcOut.create();
							OutputStream os = fcOut.openOutputStream();
							os.write(addExifEncoding(image));
							os.close();
							is.close();
							return;
						}
					}
					logger.info("Could not find the image file to add exif information");
				} catch (IOException ioe) {
					logger.exception(Locale.get("guicamera.TryingToReadFileAfterPicture")/*Trying to read file after picture*/, ioe);
				} catch (InterruptedException ie) {
					logger.info("Sleep was interupted");
				}
			}
			//#endif
			
			
		} catch (MediaException e) {
			logger.exception(Locale.get("guicamera.CouldntCapturePhoto")/*Could not capture photo*/, e);
		}
		//#else
		logger.error(Locale.get("guicamera.JSR234SupportNotAvailable")/*JSR-234 support is not compiled into this MIDlet. Please use JSR-135*/);
		//#endif

	}
	
	private void takePicture() {
		try {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_USE_JSR_234)) {
				takePicture234();
			} else {
				takePicture135();
			}			
		} catch (SecurityException se) {
			logger.error(Locale.get("guicamera.PermissionDeniedPhoto")/*Permission denied to take a photo*/);
		}
	}

	protected void paint(Graphics g) {
		//It looks like it is necessary to do something
		//in this method, otherwise the video display gets
		//confused after taking a picture. So display a simple
		//"Progress Display"
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(255,0,0);
		g.drawRect(5, 5, getWidth() - 10, getHeight() - 10);
		g.drawRect(6, 6, getWidth() - 12, getHeight() - 12);
		g.drawRect(7, 7, getWidth() - 14, getHeight() - 14);
		g.setColor(0,255,0);
		//#if polish.android
		g.drawString(Locale.get("guicamera.SavingPhotoAndroid")/*Saving photo...OK to continue*/, getWidth()/2, getHeight()/2, Graphics.BASELINE | Graphics.HCENTER);
		//#else
		g.drawString(Locale.get("guicamera.SavingPhoto")/*Saving photo...*/, getWidth()/2, getHeight()/2, Graphics.BASELINE | Graphics.HCENTER);
		//#endif
	}

	public void keyPressed(int keyCode) {
		logger.info("Pressed key code " + keyCode + " in Camera GUI");
		
		if ((getGameAction(keyCode) == FIRE) || (keyCode == Configuration.KEYCODE_CAMERA_CAPTURE)) {
			commandAction(CAPTURE_CMD, (Displayable) null);
		}
		if (keyCode == Configuration.KEYCODE_CAMERA_COVER_CLOSE) {
			//#if polish.api.mmapi
			if (mPlayer != null) {
				mPlayer.close();
			}
			//#endif
			parent.show();
		}
		
	}
	
	//#if polish.android
	// See http://developer.android.com/sdk/android-2.0.html
	// for a possible Native Android problem & workaround
	public void keyReleased(int keyCode) {
		logger.info("Released key code " + keyCode + " in Camera GUI");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cameraOff(null);
			parent.show();
		}
	}
	//#endif

	public void cameraOff(Displayable disp) {
		//#if polish.android
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		exitPreview();
		//#endif
		if (disp == this || disp == null) {
			//#if polish.api.mmapi
			if (mPlayer != null) {
				mPlayer.close();
			}
			//#endif
		}
	}


	public void commandAction(Command c, Item i) {
//		 forward item command action to form
		commandAction(c, (Displayable) null);
		
	}
	
	public void commandAction(Command c, Displayable disp) {
		if (c == CANCEL_CMD || c == OK2_CMD) {
			cameraOff(disp);
			if (disp == this) {
				parent.show();
			} else {
				if (parent == null) {
					// return to setup
					setupParent.show();
				} else {
					this.show();
				}
			}
		}
		//#if polish.api.mmapi
		if (c == CAPTURE_CMD) {
			if ((basedirectory == null) ||(!basedirectory.startsWith("file:///"))) {
				logger.error(Locale.get("guicamera.SelectDirFirst")/*You need to select a directory where to save first*/);
				return;
			}
			if (parent != null) {
				takePicture();
			}
		}
		if (c == STORE_CMD) {
			//#if polish.api.fileConnection
				if (parent == null) {
					// return to setup
					setupParent.show();
				} else {
					this.show();
				}
				new FsDiscover(parent == null ? (GpsMidDisplayable) setupParent : (GpsMidDisplayable) this,
					       this, basedirectory,FsDiscover.CHOOSE_DIRONLY,null,"Media Store Directory");
			//#endif
		}
		if (c == SETUP_CMD) {
			logger.info("Starting Setup dialog");
			
			if ((mPlayer != null) && (video != null)) {
				try {
					mPlayer.stop();
					video.setVisible(false);
				} catch (MediaException e) {
					logger.exception(Locale.get("guicamera.CouldNotStopCameraViewer")/*Could not stop camera viewer*/, e);
				}
			}
			basedirectory = Configuration.getPhotoUrl();
			Form setupDialog = new Form(Locale.get("guicamera.Setup")/*Setup*/);
			setupDialog.addCommand(CANCEL_CMD);
			setupDialog.addCommand(OK_CMD);
			setupDialog.addCommand(STORE_CMD);
			setupDialog.setCommandListener(this);
			
			/**
			 * Setup JSR selector
			 */
			String [] selectJsr = {"JSR-135", "JSR-234"};
			selectJsrCG = new ChoiceGroup(Locale.get("guicamera.PicturesVia")/*Pictures via...*/, Choice.EXCLUSIVE, selectJsr ,null);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_USE_JSR_234)) {
				selectJsrCG.setSelectedIndex(1, true);
			} else {
				selectJsrCG.setSelectedIndex(0, true);
			}
			
			/**
			 * Setup Exif selector
			 */
			String [] selectExif = {Locale.get("guicamera.AddExif")/*Add exif*/};
			selectExifCG = new ChoiceGroup(Locale.get("guicamera.Geocoding")/*Geocoding*/, Choice.MULTIPLE,selectExif,null);
			boolean [] selExif = new boolean[1];
			selExif[0] = Configuration.getCfgBitState(Configuration.CFGBIT_ADD_EXIF);
			selectExifCG.setSelectedFlags(selExif);
			
			/**
			 * Setup Encoding
			 */
			encodingTF = new TextField(Locale.get("guicamera.EncodingString")/*Encoding string: */, Configuration.getPhotoEncoding() , 100 ,TextField.ANY);
			String encodings = null;
			try {
				 encodings = System.getProperty("video.snapshot.encodings");
				logger.debug("Encodings: " + encodings); 
			} catch (Exception e) {
				logger.info("Device does not support the encoding property");
			}
			String [] encStrings = new String[0];
			String setEnc = Configuration.getPhotoEncoding();
			if (setEnc == null) {
				setEnc = "";
			}
			int encodingSel = -1;
			if (encodings != null) {
				encStrings = StringTokenizer.getArray(encodings, " ");
				for (int i = 0; i < encStrings.length; i++) {
					logger.debug("Enc: " + encStrings[i]);
					if (setEnc.equalsIgnoreCase(encStrings[i])) {
						encodingSel = i;
						logger.debug("Enc Sel: " + encStrings[i]);
					}
				}
			} else {
				encStrings = new String [0];
			}
			if (encodingSel == -1) {
				encodingSel = encStrings.length;
			}
			String [] tmp = new String[encStrings.length + 1];
			System.arraycopy(encStrings, 0, tmp, 0, encStrings.length);
			tmp[encStrings.length] = Locale.get("guicamera.Custom")/*Custom*/;
			encStrings = tmp;
			encodingCG = new ChoiceGroup(Locale.get("guicamera.SelectEncoding")/*Select encoding: */, Choice.EXCLUSIVE, encStrings, null);
			encodingCG.setSelectedIndex(encodingSel, true);
					     
					     /**
					     * Setup custom encoding text field
					     */
					     TextField storageDir = new TextField(Locale.get("guicamera.store")/*store: */, basedirectory, 100, TextField.UNEDITABLE);
					     storageDir.setDefaultCommand(STORE_CMD);
					     storageDir.setItemCommandListener(this);
					     
					     setupDialog.append(selectJsrCG);
					     setupDialog.append(selectExifCG);
					     setupDialog.append(encodingCG);
					     setupDialog.append(encodingTF);
					     setupDialog.append(storageDir);
					     GpsMid.getInstance().show(setupDialog);
			//#debug trace
			logger.trace("Showing Setup dialog");
			
		}
		/**
		 * OK command for the setup dialog
		 */
		if (c == OK_CMD) {
			if (selectJsrCG.getSelectedIndex() == 1) {
				Configuration.setCfgBitState(Configuration.CFGBIT_USE_JSR_234, true, true);
			} else {
				Configuration.setCfgBitState(Configuration.CFGBIT_USE_JSR_234, false, true);
			}
			
			boolean [] selExif = new boolean[1];
			selectExifCG.getSelectedFlags(selExif);
			if (selExif[0]) {
				Configuration.setCfgBitState(Configuration.CFGBIT_ADD_EXIF, true, true);
			} else {
				Configuration.setCfgBitState(Configuration.CFGBIT_ADD_EXIF, false, true);
			}
			
			String encType = encodingCG.getString(encodingCG.getSelectedIndex());
			if (encType.equals(Locale.get("guicamera.Custom")/*Custom*/)) {
				encType = encodingTF.getString();
			}
			Configuration.setPhotoEncoding(encType);
			
			if (parent == null) {
				// return to setup
				setupParent.show();
			} else {
				this.show();
			}
		}
		//#endif
	}
	
	/**
	 * Add a EXIF header to a JPEG byte array containing information about the current
	 * GPS position and height. This is currently not particularly smart, as it does
	 * not try and parse any information, but simply copies in a binary blob containing
	 * the exif header to the start of the jpg file. It does not check if the data
	 * already contains an exif header. 
	 * @param jpgImage
	 * @return
	 */
	private byte [] addExifEncoding(byte [] jpgImage) {
		byte [] newImage = new byte[jpgImage.length + 201];
		/**
		 * Exif binary blob:
		 * byte  value      comment
		 * 0 - 1 0xff 0xd8: Start of image marker. Must be the first 2 bytes in a jpeg file
		 * 2 - 3 0xff 0xe1: App1 (exif) marker
		 * 4 - 5          : length of the exif block including these two bytes (199 in the case of this block) 
		 */
		byte [] tmp = {
				(byte) 0xff, (byte) 0xd8, (byte)0xff,(byte)0xe1,0x00,(byte)0xc7,0x45,0x78,0x69,0x66,0x00,0x00,0x49,0x49,
				0x2a, 0x00, 0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x25, (byte)0x88, 0x04, 0x00, 0x01, 0x00, 0x00, 0x00, 
				0x1a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x01, 0x00, 0x04, 0x00, 
				0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x4e, 0x00, 
				0x00, 0x00, 0x02, 0x00, 0x05, 0x00, 0x03, 0x00, 0x00, 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x03, 0x00, 
				0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x45, 0x00, 0x00, 0x00, 0x04, 0x00, 0x05, 0x00, 0x03, 0x00, 
				0x00, 0x00, (byte)0x98, 0x00, 0x00, 0x00, 0x05, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
				0x00, 0x00, 0x06, 0x00, 0x05, 0x00, 0x01, 0x00, 0x00, 0x00, (byte)0xb0, 0x00, 0x00, 0x00, 0x12, 0x00, 
				0x02, 0x00, 0x07, 0x00, 0x00, 0x00, (byte)0xb8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2e, 0x00, 
				0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte)0xa9, 0x31, 0x3b, 0x00, 0x40, 0x42, 0x0f, 0x00, 0x00, 0x00, 
				0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x07, 0x78, 
				0x72, 0x00, 0x40, 0x42, 0x0f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte)0xd2, 0x04, 
				0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x57, 0x47, 0x53, 0x2d, 0x38, 0x34, 0x00};
		
		/**
		 * The exif header must be directly after the Start of image marker. and is 201 bytes long 
		 */
		System.arraycopy(jpgImage, 2, newImage, 203, jpgImage.length - 2);
		System.arraycopy(tmp,0,newImage,0,203);
		
		Position pos = parent.getCurrentPosition();
		int altitude = (int)pos.altitude;
		
		HelperRoutines.copyInt2ByteArray(newImage, 188, altitude);
		
		float lat = pos.latitude;
		float lon = pos.longitude;

		

		if (lat > 0) {
			newImage[60] = 0x4e; //N orth
		} else {
			newImage[60] = 0x53; //S outh
			lat *= -1;
		}
		if (lon > 0) {
			newImage[84] = 0x45; //E ast
		} else {
			newImage[84] = 0x57; //W est
			lon *= -1;
		}
		
		/**
		 * The exif encoding for positions are three "Rationals"
		 * one for degree, one for minutes and one for seconds.
		 * Each rational is 8bytes long. The first 4 are the numerator
		 * the last are the denominator
		 * 
		 * We only use Degrees and Minutes and set Seconds to 0.
		 * Instead the Minutes are devided by 1000000, to get 6 decimal
		 * places.
		 */
		
		HelperRoutines.copyInt2ByteArray(newImage, 140, (int)Math.abs(lat));
		float lat_min = (float)(lat - Math.floor(lat))  * 60000000.0f;
		HelperRoutines.copyInt2ByteArray(newImage, 148, (int)lat_min);
		
		
		HelperRoutines.copyInt2ByteArray(newImage, 164, (int)Math.abs(lon));
		float lon_min = (float)(lon - Math.floor(lon))  * 60000000.0f;
		HelperRoutines.copyInt2ByteArray(newImage, 172, (int)lon_min);
		
		
		return newImage;
	}

	public void setup(GuiDiscover parent) {
		GpsMid.getInstance().show(this);
		this.parent = null;
		setupParent = parent;
		commandAction(SETUP_CMD, (Displayable) this);
	}

	public void show() {
		GpsMid.getInstance().show(this);
		//#if polish.api.mmapi
		if ((mPlayer != null) && (video != null)) {
			try {
				mPlayer.start();
				video.setVisible(true);
			} catch (MediaException e) {
				logger.exception(Locale.get("guicamera.CouldNotShowCameraViewer")/*Could not show camera viewer*/, e);
			}
		}
		//#endif
		//Display.getDisplay(parent.getParent()).setCurrent(this);
	}

	public void selectionCanceled() {
		/**
		 * Nothing to do at the moment
		 */
	}
	
	public void selectedFile(String url) {
		logger.info("Setting picture directory to " + url);
		Configuration.setPhotoUrl(url);
		basedirectory = url;
	}
}

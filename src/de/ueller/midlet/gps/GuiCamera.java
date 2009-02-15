package de.ueller.midlet.gps;

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
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.midlet.gps.data.MoreMath;

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
public class GuiCamera extends Canvas implements CommandListener, ItemCommandListener, GuiCameraInterface, SelectionListener, GpsMidDisplayable {

	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command OK_CMD = new Command("Ok", Command.OK, 5);
	private final Command CAPTURE_CMD = new Command("Capture", Command.OK, 5);
	private final Command STORE_CMD = new Command("Select directory", Command.ITEM, 5);
	private final Command SETUP_CMD = new Command("Setup", Command.ITEM, 6);

	private final static Logger logger = Logger.getInstance(GuiCamera.class,
			Logger.DEBUG);
	//#if polish.api.mmapi	
	private Player mPlayer;
	private VideoControl video;
	//#if polish.api.advancedmultimedia
	private FocusControl focus;	
	//#endif
	//#endif
	private String encoding;
	private Trace parent;
	private String basedirectory;
	
	private ChoiceGroup selectJsrCG;
	private ChoiceGroup selectExifCG;
	private TextField   encodingTF;
	private ChoiceGroup encodingCG;

	public void init(Trace parent) {
		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(CAPTURE_CMD);
		//addCommand(STORE_CMD);
		addCommand(SETUP_CMD);
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
			basedirectory = Configuration.getPhotoUrl();
			//#debug debug
			logger.debug("Storing photos at " + basedirectory);
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
				logger.error("Can't get access to camera properties");
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
				logger.debug("Device doesn't support ImageFormatControl");
			}
			//#endif
			
			
			
		} catch (SecurityException se) {
			logger.exception("Security Exception: ", se);
			mPlayer = null;
			video = null;
		} catch (IOException e) {
			logger.exception("IOexception", e);
			mPlayer = null;
			video = null;
		} catch (MediaException e) {
			logger.exception("MediaExcpetion", e);
			mPlayer = null;
			video = null;
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
			byte [] photo = video.getSnapshot(Configuration.getPhotoEncoding());
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
		} catch (MediaException e) {
			logger.exception("Couldn't take picture", e);
		} catch (IOException e) {
			logger.exception("IOException capturing the photo", e);
		} catch (NullPointerException npe) {
			logger.exception("Failed to take a picture", npe);
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
				//#debug debug
				logger.debug("Couldn't get focus control");
			}
			
			//#debug trace
			logger.trace("Finished focusing");

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
					logger.exception("Trying to read file after picture", ioe);
				} catch (InterruptedException ie) {
					logger.info("Sleep was interupted");
				}
			}
			//#endif
			
			
		} catch (MediaException e) {
			logger.exception("Couldn't capture photo", e);
		}
		//#else
		logger.error("JSR-234 support is not compiled into this MIDlet. Please use JSR-135");
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
			logger.error("Permission denied to take a photo");
		}
	}

	protected void paint(Graphics arg0) {
		//Don't have to do anything here, as the video Player will
		//do the painting for us.
	}

	public void keyPressed(int keyCode) {
		logger.info("Pressed key code " + keyCode + " in Camera GUI");
		
		if ((getGameAction(keyCode) == FIRE) || (keyCode == Configuration.KEYCODE_CAMERA_CAPTURE)) {
			takePicture();
		}
		if (keyCode == Configuration.KEYCODE_CAMERA_COVER_CLOSE) {
			//#if polish.api.mmapi
			if (mPlayer != null)
				mPlayer.close();
			//#endif
			parent.show();
		}
		
	}
	
	public void commandAction(Command c, Item i) {
//		 forward item command action to form
		commandAction(c, (Displayable) null);
		
	}
	
	public void commandAction(Command c, Displayable disp) {
		if (c == BACK_CMD) {
			if (disp == this) {
				//#if polish.api.mmapi
				if (mPlayer != null)
					mPlayer.close();
				//#endif
				parent.show();
			} else {
				this.show();
			}
		}
		//#if polish.api.mmapi
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
		if (c == SETUP_CMD) {
			logger.info("Starting Setup dialog");
			
			if ((mPlayer != null) && (video != null)) {
				try {
					mPlayer.stop();
					video.setVisible(false);
				} catch (MediaException e) {
					logger.exception("Could not stop camera viewer", e);
				}
			}
			Form setupDialog = new Form("Setup");
			setupDialog.addCommand(BACK_CMD);
			setupDialog.addCommand(OK_CMD);
			setupDialog.addCommand(STORE_CMD);
			setupDialog.setCommandListener(this);
			
			/**
			 * Setup JSR selector
			 */
			String [] selectJsr = {"JSR-135", "JSR-234"};
			selectJsrCG = new ChoiceGroup("Pictures via...", Choice.EXCLUSIVE, selectJsr ,null);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_USE_JSR_234)) {
				selectJsrCG.setSelectedIndex(1, true);
			} else {
				selectJsrCG.setSelectedIndex(0, true);
			}
			
			/**
			 * Setup Exif selector
			 */
			String [] selectExif = {"Add exif"};
			selectExifCG = new ChoiceGroup("Geocoding", Choice.MULTIPLE,selectExif,null);
			boolean [] selExif = new boolean[1];
			selExif[0] = Configuration.getCfgBitState(Configuration.CFGBIT_ADD_EXIF);
			selectExifCG.setSelectedFlags(selExif);
			
			/**
			 * Setup Encoding
			 */
			encodingTF = new TextField("Encoding string: ", Configuration.getPhotoEncoding() , 100 ,TextField.ANY);
			String encodings = null;
			try {
				 encodings = System.getProperty("video.snapshot.encodings");
				logger.debug("Encodings: " + encodings); 
			} catch (Exception e) {
				logger.info("Device does not support the encoding property");
			}
			String [] encStrings = new String[0];
			String setEnc = Configuration.getPhotoEncoding();
			if (setEnc == null) setEnc = "";
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
			if (encodingSel == -1)
					encodingSel = encStrings.length;
			String [] tmp = new String[encStrings.length + 1];
			System.arraycopy(encStrings, 0, tmp, 0, encStrings.length);
			tmp[encStrings.length] = "Custom";
			encStrings = tmp;
			encodingCG = new ChoiceGroup("Select encoding: ", Choice.EXCLUSIVE, encStrings, null);
			encodingCG.setSelectedIndex(encodingSel, true);
			
			/**
			 * Setup custom encoding text field
			 */
			TextField storageDir = new TextField("store: ", basedirectory, 100, TextField.UNEDITABLE);
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
			if (encType.equals("Custom"))
				encType = encodingTF.getString();
			Configuration.setPhotoEncoding(encType);
			
			this.show();
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

		

		if (lat > 0)
			newImage[60] = 0x4e; //N orth
		else {
			newImage[60] = 0x53; //S outh
			lat *= -1;
		}
		if (lon > 0)
			newImage[84] = 0x45; //E ast
		else {
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

	public void show() {
		GpsMid.getInstance().show(this);
		//#if polish.api.mmapi
		if ((mPlayer != null) && (video != null)) {
			try {
				mPlayer.start();
				video.setVisible(true);
			} catch (MediaException e) {
				logger.exception("Could not show camera viewer", e);
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

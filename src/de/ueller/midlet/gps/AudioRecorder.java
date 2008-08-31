package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

//#if polish.api.mmapi
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;
//#endif


import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;

public class AudioRecorder  implements SelectionListener{
	private final static Logger logger = Logger.getInstance(AudioRecorder.class, Logger.DEBUG);
	
	//#if polish.api.mmapi	
	private Player mPlayer;
	private RecordControl record;	
	//#endif
	private String basedirectory;
	
	public boolean startRecorder() {
		//#if polish.api.mmapi
		try{
			String supportRecording = System.getProperty("supports.audio.capture");
			if ((supportRecording == null) || (!supportRecording.equalsIgnoreCase("true"))) {
				logger.error("Phone does not support recording");
			}
			logger.info("Supported audio encodings: " + System.getProperty("audio.encodings"));
			logger.info("Starting audio recording");
			mPlayer = Manager.createPlayer("capture://audio");
			if (mPlayer == null) {
				logger.error("Couldn't initialize camera player");
				return false;
			}
			mPlayer.realize();			
			record = (RecordControl) mPlayer.getControl("RecordControl");
			if (record == null) {
				logger.error("Failed to get RecordControl");
				return false;
			}
			basedirectory = GpsMid.getInstance().getConfig().getPhotoUrl();
			if (basedirectory == null) {
				logger.error("Don't know where to save the recording, please specify a directory and try again");
				//#if polish.api.fileConnection				
				new FsDiscover(Trace.getInstance(),this,basedirectory,true,null,"Directory to store photos and audio");
				//#endif
				record = null;
				return false;
			}
			String fileSubPart = "GpsMid-" + HelperRoutines.formatSimpleDateSecondNow();
			String fileName = basedirectory + fileSubPart +".amr";
			logger.info("Saving audio stream to " + fileName);
			record.setRecordLocation(fileName);
			record.startRecord();
			mPlayer.start();
			
			/**
			 * Add a waypoint marker at the current position in order to later
			 * on be able to synchronize the audio track
			 */
			Trace tr = Trace.getInstance();
			Position pos = tr.getCurrentPosition();
			PositionMark here = new PositionMark(pos.latitude *MoreMath.FAC_DECTORAD, pos.longitude *MoreMath.FAC_DECTORAD);
			here.displayName = "AudioMarker-" + fileSubPart;
			Trace.getInstance().gpx.addWayPt(here);
		} catch (SecurityException se) {
			record = null;
			logger.error("Permision denied to record audio");
		} catch (Exception me) {
			record = null;
			logger.exception("Failed to start recording", me);
		}
		//#endif

		return true;
	}
	
	public void stopRecord() {
		//#if polish.api.mmapi
		if (record == null)
			return;
		try{
			record.stopRecord();
			record.commit();
			record = null;
			mPlayer.stop();
			mPlayer.close();			
		} catch (IOException ioe) {
			logger.exception("Failed to save audio recording", ioe);
		} catch (MediaException e) {
			logger.exception("Failed to close audio recording", e);
		}
		//#endif
 
	}
	
	public boolean isRecording() {
		boolean res = false;
		//#if polish.api.mmapi
		res = (record != null);		
		//#endif
		return res;
	}

	public void selectedFile(String url) {
		logger.info("Setting picture directory to " + url);
		GpsMid.getInstance().getConfig().setPhotoUrl(url);
		basedirectory = url;
	}

}

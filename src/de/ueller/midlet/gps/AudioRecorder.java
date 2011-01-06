package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

//#if polish.api.fileConnection
import java.io.IOException;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif
//#if polish.api.mmapi
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

import de.enough.polish.util.Locale;

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
				logger.error(Locale.get("audiorecorder.PhoneNoRecordSupp")/*Phone does not support recording*/);
			}
			logger.info("Supported audio encodings: " + System.getProperty("audio.encodings"));
			logger.info("Starting audio recording");
			mPlayer = Manager.createPlayer("capture://audio");
			if (mPlayer == null) {
				logger.error(Locale.get("audiorecorder.InitializeAudioRecorderFail")/*Could not initialize audio recorder*/);
				return false;
			}
			mPlayer.realize();			
			record = (RecordControl) mPlayer.getControl("RecordControl");
			if (record == null) {
				logger.error(Locale.get("audiorecorder.FailedGettingRecordControl")/*Failed to get RecordControl*/);
				return false;
			}
			basedirectory = Configuration.getPhotoUrl();
			if (basedirectory == null) {
				logger.error(Locale.get("audiorecorder.SpecifyDir")/*Dont know where to save the recording, please specify a directory and try again*/);
				//#if polish.api.fileConnection				
				new FsDiscover(Trace.getInstance(),this,basedirectory,FsDiscover.CHOOSE_DIRONLY,null,"Media Store Directory");
				//#endif
				record = null;
				return false;
			}
			String fileSubPart = "GpsMid-" + HelperRoutines.formatSimpleDateSecondNow();
			String fileName = basedirectory + fileSubPart +".amr";
			logger.info("Saving audio stream to " + fileName);
			// Some JVMs seem to require the file to already exist before they can record to it
			//#if polish.api.fileConnection
			Connection c = Connector.open( fileName, Connector.READ_WRITE);
			FileConnection fc = (FileConnection) c;
			if (!fc.exists()) {
				fc.create();
			}
			//#endif
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
			tr.alert("Audio recording", "Recording audio to " + fileName, 1500);
		} catch (SecurityException se) {
			record = null;
			logger.error(Locale.get("audiorecorder.PermisionDeniedRecordingAudio")/*Permision denied to record audio*/);
		} catch (Exception me) {
			record = null;
			logger.exception(Locale.get("audiorecorder.FailedStartingRecording")/*Failed to start recording*/, me);
			// offer a chance to fix a possibly improper URL
			//#if polish.api.fileConnection				
			new FsDiscover(Trace.getInstance(),this,basedirectory,FsDiscover.CHOOSE_DIRONLY,null,"Media Store Directory");
			//#endif
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
			Trace tr = Trace.getInstance();
			tr.alert("Audio recording", "Stopped audio recording", 750);
		} catch (IOException ioe) {
			logger.exception(Locale.get("audiorecorder.FailedSavingAudioRecording")/*Failed to save audio recording*/, ioe);
		} catch (MediaException e) {
			logger.exception(Locale.get("audiorecorder.FailedClosingAudioRecording")/*Failed to close audio recording*/, e);
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
	
	public void selectionCanceled() {
		/**
		 * Nothing to do at the moment
		 */
	}

	public void selectedFile(String url) {
		logger.info("Setting media store to " + url);
		Configuration.setPhotoUrl(url);
		basedirectory = url;
	}

}

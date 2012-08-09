package de.ueller.gpsmid.ui;
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
//#if polish.android
import android.media.MediaRecorder;
//#endif


import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.midlet.ui.SelectionListener;
import de.ueller.util.HelperRoutines;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class AudioRecorder  implements SelectionListener{
	private final static Logger logger = Logger.getInstance(AudioRecorder.class, Logger.DEBUG);
	
	//#if polish.api.mmapi	
	//#if polish.android
	private MediaRecorder recorder;
	//#else
	private Player mPlayer;
	private RecordControl record;	
	//#endif
	//#endif
	private String basedirectory;
	
	public boolean startRecorder() {
		//#if polish.api.mmapi
		try{
			String supportRecording = System.getProperty("supports.audio.capture");
			//#if polish.api.mmapi
			//#if polish.android
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			//#else
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
			//#endif polish.android
			basedirectory = Configuration.getPhotoUrl();
			//#if polish.android
			//#else
			if (basedirectory == null) {
				logger.error(Locale.get("audiorecorder.SpecifyDir")/*Dont know where to save the recording, please specify a directory and try again*/);
				//#if polish.api.fileConnection				
				new FsDiscover(Trace.getInstance(),this,basedirectory,FsDiscover.CHOOSE_DIRONLY,null,"Media Store Directory");
				//#endif
				record = null;
				return false;
			}
			//#endif
			String fileSubPart = "GpsMid-" + HelperRoutines.formatSimpleDateSecondNow();
			//#if polish.android
			// with the normal line tries to open /file:/sdcard/GpsMid-201.. and fails
			//
			// skip /file: prefix
			String fileName = basedirectory.substring(6) + fileSubPart +".amr";
			//#else
			String fileName = basedirectory + fileSubPart +".amr";
			//#endif
			logger.info("Saving audio stream to " + fileName);
			// Some JVMs seem to require the file to already exist before they can record to it
			//#if polish.api.fileConnection
			//#if polish.android
			// with READ_WRITE there's an exception:
			// W/System.err(11824): java.lang.IllegalArgumentException: Unknown connection type:-1
			// W/System.err(11824):    at de.enough.polish.android.io.Connector.createConnection(Connector.java:78)
			// W/System.err(11824):    at de.enough.polish.android.io.Connector.open(Connector.java:53)
			// W/System.err(11824):    at de.ueller.gpsmid.ui.AudioRecorder.startRecorder(AudioRecorder.java:95)
			//#else
			Connection c = Connector.open( fileName, Connector.READ_WRITE);
			FileConnection fc = (FileConnection) c;
			if (!fc.exists()) {
				fc.create();
			}
			//#endif
			//#endif
			//#if polish.android
			recorder.setOutputFile(fileName);
			recorder.prepare();
			recorder.start();   // Recording is now started			
			//#else
			record.setRecordLocation(fileName);
			record.startRecord();
			mPlayer.start();
			//#endif
			
			/**
			 * Add a waypoint marker at the current position in order to later
			 * on be able to synchronize the audio track
			 */
			Trace tr = Trace.getInstance();
			Position pos = tr.getCurrentPosition();
			PositionMark here = new PositionMark(pos.latitude *MoreMath.FAC_DECTORAD, pos.longitude *MoreMath.FAC_DECTORAD);
			here.displayName = "AudioMarker-" + fileSubPart;
			Trace.getInstance().gpx.addWayPt(here);
			tr.alert(Locale.get("audiorecorder.audRecording")/*Audio recording*/, 
				 Locale.get("audiorecorder.recTo")/*Recording audio to*/ + " " + fileName, 1500);
		} catch (SecurityException se) {
			//#if polish.android
			recorder = null;
			//#else
			record = null;
			//#endif
			logger.error(Locale.get("audiorecorder.PermisionDeniedRecordingAudio")/*Permision denied to record audio*/);
		} catch (Exception me) {
			//#if polish.android
			recorder = null;
			//#else
			record = null;
			//#endif
			logger.exception(Locale.get("audiorecorder.FailedStartingRecording")/*Failed to start recording*/, me);
			// offer a chance to fix a possibly improper URL
			//#if polish.api.fileConnection				
			new FsDiscover(Trace.getInstance(),this,basedirectory,FsDiscover.CHOOSE_DIRONLY,null,"Media Store Directory");
			//#endif
		}
		//#endif
		//#endif

		return true;
	}
	
	public void stopRecord() {
		//#if polish.api.mmapi
		//#if polish.android
		if (recorder == null)
			return;
		//#else
		if (record == null)
			return;
		//#endif
		//#if polish.android
		//#else
		try{
		//#endif
			//#if polish.android
			recorder.stop();
			recorder.release();
			recorder = null;
			//#else
			record.stopRecord();
			record.commit();
			record = null;
			mPlayer.stop();
			mPlayer.close();
			//#endif
			Trace tr = Trace.getInstance();
			tr.alert(Locale.get("audiorecorder.audRecording")/*Audio recording*/, 
				 Locale.get("audiorecorder.stopped")/*Stopped audio recording*/, 750);
		//#if polish.android
		//#else
		} catch (IOException ioe) {
			logger.exception(Locale.get("audiorecorder.FailedSavingAudioRecording")/*Failed to save audio recording*/, ioe);
		} catch (MediaException e) {
			logger.exception(Locale.get("audiorecorder.FailedClosingAudioRecording")/*Failed to close audio recording*/, e);
		}
		//#endif
		//#endif
 
	}
	
	public boolean isRecording() {
		boolean res = false;
		//#if polish.api.mmapi
		//#if polish.android
		res = (recorder != null);		
		//#else
		res = (record != null);		
		//#endif
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

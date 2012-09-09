/*
 * ShareNav - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 *          Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See Copying 
 */

package net.sharenav.sharenav.ui;

import java.io.IOException;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.routing.RouteInstructions;
import net.sharenav.sharenav.routing.RouteSyntax;
import net.sharenav.util.Logger;

import net.sourceforge.util.zip.ZipFile;
import net.sourceforge.util.zip.ZipEntry;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
//#if polish.api.mmapi	
//#ifndef polish.android
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.Manager;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;
import de.enough.polish.multimedia.AudioPlayer;
//#else
import android.media.AudioManager;
import android.media.MediaPlayer;
import de.enough.polish.android.midlet.MidletBridge;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import java.io.FileDescriptor;
import java.io.FileInputStream;
//#endif
//#endif

/**
 * Helper class to create sounds for different state changes of the Midlet.
 */
public class NoiseMaker
//#if polish.api.mmapi	
//#ifndef polish.android
	implements PlayerListener
//#else
	implements MediaPlayer.OnCompletionListener
//#endif
//#endif

{
	
	// private final byte[] mPosFixSequence;
	
	// private final byte[] mNoFixSequence;
	
	private final static Logger mLogger = Logger.getInstance(NoiseMaker.class, Logger.DEBUG);
	
	private static volatile String mPlayingNames = "";
	private static volatile int mPlayingNameIndex=0;

	private static volatile boolean updatesMissing = false;
	private static volatile boolean updatesTested = false;

//#if polish.api.mmapi			
//#if polish.android
	private static volatile MediaPlayer sPlayer = null;
//#else
	private static volatile Player sPlayer = null; 
	private static volatile AudioPlayer aPlayer = null; 
	private static byte[] mConnOpenedSequence;	
	private static byte[] mConnLostSequence;
//#endif			
//#endif
	
	private static volatile long mOldMsTime = 0;
	private static volatile String mOldPlayingNames = "";
	private static volatile byte mTimesToPlay = 0;
	private static volatile String mPlayingSoundName = "";
	private static String lastSuccessfulSuffix = null;
	
	public NoiseMaker()
	{
//#if polish.api.mmapi	
//#ifndef polish.android
	    final byte tempo = 30; // set tempo to 120 bpm 
	    final byte e = 8;  // eighth-note
	    final byte q = 16; // quarter-note

	    final byte C4 = ToneControl.C4; 
	    final byte D4 = (byte)(C4 + 2); // a whole step 
	    final byte E4 = (byte)(C4 + 4); // a major third 
	    final byte F4 = (byte)(C4 + 5); // a fourth
	    final byte G4 = (byte)(C4 + 7); // a fifth
	    final byte A4 = (byte)(C4 + 9);
	    final byte B4 = (byte)(C4 + 10);
	    final byte H4 = (byte)(C4 + 11);
	    final byte C5 = (byte)(C4 + 12);

	    mConnOpenedSequence = new byte[] {
	        ToneControl.VERSION, 1,
	        ToneControl.TEMPO, tempo,
	        ToneControl.SET_VOLUME, 100,
	        F4,e, A4,q
	    };
	    
	    mConnLostSequence = new byte[] {
	        ToneControl.VERSION, 1,
	        ToneControl.TEMPO, tempo,
	        ToneControl.SET_VOLUME, 100,
	        A4,e, F4,q
	    };

//	   String mixing = System.getProperty("supports.mixing");
//	   deviceSupportsMixing = (mixing != null && mixing.equals("true"));
	   //deviceSupportsMixing = false;
	    
		//#mdebug debug
	    mLogger.debug("Supported content types:");
	    String[] contentTypes = Manager.getSupportedContentTypes(null) ;       
	    for (int i = 0  ; i < contentTypes.length  ; i++) { 
	    	mLogger.debug(i + ". " + contentTypes[i]);
	    }
  		//#enddebug
	    
//#endif
//#endif
	}

//#if polish.api.mmapi
//#if polish.android
	// using MediaPlayer
	private synchronized boolean preparePlayer(String soundFile, String mediaType, String suffix) {
		String soundFileWithSuffix = soundFile;
		if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
			//#debug debug
			mLogger.debug("Preparing to play sound " + soundFile);
		} else {
			if (Configuration.getMapUrl().endsWith("/")) {
				soundFileWithSuffix = Configuration.getMapUrl() + soundFile;
				//#debug debug
				mLogger.debug("Preparing to play url " + soundFileWithSuffix);
			} else {
				//#debug debug
				mLogger.debug("Preparing to play zip sound " + soundFileWithSuffix);
			}
		}
		try {
			if (sPlayer == null)
			{
				sPlayer = new MediaPlayer();
				sPlayer.setOnCompletionListener(this);
				sPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			}
		} catch (Exception ex) {
			sPlayer = null;
			mLogger.exception("Failed to create resource player for " + soundFileWithSuffix, ex);
		}
		if (sPlayer != null) {	
			//#debug debug
			mLogger.debug("created player for " + soundFileWithSuffix);
			try {
				if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
					AssetFileDescriptor afd = MidletBridge.instance.getResources().getAssets().openFd(soundFile);
					sPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
					afd.close();
				} else {
					if (Configuration.getMapUrl().endsWith("/")) {
						sPlayer.setDataSource(soundFileWithSuffix.substring("file://".length()));
					} else {
						if (Configuration.mapZipFile == null) {
							//#debug debug
							Configuration.mapZipFile = new ZipFile(Configuration.getMapUrl(), -1);
						}
						// access the zip to find out at which position the uncompressed sound is at and what is the length
						String prefix = "";
						if (Configuration.zipFileIsApk) {
							prefix = "assets/";
						}

						ZipEntry ze = Configuration.mapZipFile.getEntry(prefix + soundFileWithSuffix);
						// then open the zip file and position media player there for playing
						FileInputStream fis = new FileInputStream(Configuration.getMapUrl().substring("file://".length()));
						sPlayer.setDataSource(fis.getFD(), ze.getOffset(), ze.getSize());
					}
				}
				sPlayer.prepare();
			} catch (Exception ex) {
				//#debug debug
				mLogger.debug("RESOURCE NOT FOUND: " + soundFileWithSuffix);
				return false;
			}
			lastSuccessfulSuffix = suffix;
			return true;
		}
		return false;
	}

	private synchronized void cleanPlayer() {
		if (sPlayer != null) {
			sPlayer.release();
		}
		sPlayer = null;
	}

	public void onCompletion(MediaPlayer mp) {
		cleanPlayer();
		playNextSoundFile();
	}
//#else
	// using MMAPI for J2ME
	private synchronized boolean preparePlayer(String soundFile, String mediaType, String suffix) {
		// read from bundle
		String soundFileWithSuffix = null;
		if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
			soundFileWithSuffix = "/" + soundFile;
		} else {
			if (Configuration.getMapUrl().endsWith("/")) {
				soundFileWithSuffix = Configuration.getMapUrl() + soundFile;
			} else {
				soundFileWithSuffix = soundFile;
			}
		}

		InputStream is = null;
		if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
			// mapdir map
			//#if polish.android
			// for builtin maps, open as asset from bundle
			// not really used currently as the android player is above in another #if section
			is = MidletBridge.instance.getResources().getAssets().open(soundFileWithSuffix);
			//#else
			// for builtin maps, open from bundle
			is = getClass().getResourceAsStream(soundFileWithSuffix);
			//#endif
		} else {
			try {
				if (Configuration.getMapUrl().endsWith("/")) {
					FileConnection fc = (FileConnection) Connector.open(soundFileWithSuffix, Connector.READ);
					is = fc.openInputStream();
				} else {
					if (Configuration.mapZipFile == null) {
						//#debug debug
						Configuration.mapZipFile = new ZipFile(Configuration.getMapUrl(), -1);
					}
					is = Configuration.mapZipFile.getInputStream(Configuration.mapZipFile.getEntry(soundFileWithSuffix));
				}
			} catch (Exception ex) {

				mLogger.silentexception("Error opening external sound file, map url: " + Configuration.getMapUrl() + " file: " + soundFileWithSuffix, ex);
				return false;
			}
			// zip map
		}
		if (is != null) {
			//#debug debug
			mLogger.debug("Got Inputstream for " + soundFileWithSuffix);
			try {
				if (updatesMissing) {
					if (aPlayer == null) {
						aPlayer = new AudioPlayer(true, mediaType);
					}
				} else {
					sPlayer = Manager.createPlayer(is, mediaType);
				}
			} catch (Exception ex) {
				sPlayer = null;
				aPlayer = null;
				mLogger.exception("Failed to create resource player for " + soundFileWithSuffix, ex);
			}
		} else {
			mLogger.debug("RESOURCE NOT FOUND: " + soundFileWithSuffix);
		}
		if (sPlayer != null || aPlayer != null) {
			//#debug debug
			mLogger.debug("created player for " + soundFileWithSuffix);
			try {
				if (!updatesMissing) {
					sPlayer.realize();
				}
			} catch (Exception ex) {
				mLogger.exception("Failed to realize player for " + soundFileWithSuffix, ex);
			}
			//#debug debug
			mLogger.debug("realized player for " + soundFileWithSuffix);
			if (updatesMissing) {
				try {
					// FIXME should start a new thread to play so ShareNav
					// doesn't block
					is = getClass().getResourceAsStream( soundFileWithSuffix);
					aPlayer.play(is);
				} catch (Exception ex) {
					//#debug debug
					mLogger.debug("RESOURCE NOT FOUND: " + soundFileWithSuffix + ex);
					return false;
				}
			} else {
				sPlayer.addPlayerListener( this );
				VolumeControl volCtrl = (VolumeControl) sPlayer.getControl("VolumeControl");
				if (volCtrl != null) {
					volCtrl.setLevel(100);
				}
			}
			lastSuccessfulSuffix = suffix;
			if (updatesMissing) {
				//#debug debug
				mLogger.debug("starting sleep after " + soundFileWithSuffix);
				try {
					Thread.sleep(500);
				} catch (Exception ex) {}
				playNextSoundFile();
			}
			return true;
		}
		return false;
	}
			
	private synchronized void cleanPlayer() {
		if (updatesMissing) {
			aPlayer.cleanUpPlayer();
			aPlayer = null;
		} else {
			sPlayer.close();
		}
	}
	
	public synchronized void playerUpdate( Player player, String event, Object eventData )
	{
		//#debug debug
		mLogger.debug("playerUpdate got " + event);

		if (event == PlayerListener.STARTED) {
			updatesMissing = false;
			updatesTested = true;
		}
		// Release resources used by player when it's finished.
		if (event == PlayerListener.END_OF_MEDIA)
		{
			cleanPlayer();
			playNextSoundFile();
		}
	}
//#endif

	
	private synchronized void startPlayer(String nextSoundName) {
		try {
			if (!updatesMissing) {
				boolean  testUpdates = !updatesTested;
				sPlayer.start();
				//#debug debug
				mLogger.debug("player for " + nextSoundName + " started");
				// phoneME on WinCE lacks playerupdate callbacks, test for this
				//#ifndef polish.android
				// playing should start in a short while
				if (testUpdates) {
					// FIXME perhaps - can another check
					// be added, is half a second too short on
					// some platforms for starting sound,
					// will we get false positives?
					//#debug debug
					mLogger.debug("starting playerupdate test");
					try {
						Thread.sleep(500);
					} catch (Exception ex) {}
				}
				// if updatePlayer wasn't called, mark that we have no updates
				if (!updatesTested) {
					//#debug debug
					mLogger.debug("playerupdate test failed");
					updatesTested = true;
					updatesMissing = true;
				}
				//#endif
			}
		} catch (Exception ex) {
			mLogger.exception("Failed to play sound", ex);
		}
	}

//#ifndef polish.android
	private static byte [] getToneSequence( String name ) {
    	byte sequence[] = null;
		if (name.equals("CONNECT")) {
			/**
			 * Sound for the situation that the connection to the
			 * GPS device has been (re)established.
			 */
			sequence = mConnOpenedSequence;	
		} else if (name.equals("DISCONNECT")) {
			/**
			 * Sound for the situation that the connection to the
			 * GPS device has been lost.
			 */
			sequence = mConnLostSequence;				
		} else if (name.equals("FIX")) {
			/**
			 * Sound for the situation that a valid position was received
			 */
			//sequence = mPosFixSequence;				
		} else if (name.equals("NOFIX")) {
			/**
			 * Sound for the situation that the position has become unknown.
			 */
			//sequence = mNoFixSequence;				
		}
		return sequence;
	}
	
	
	private void playSequence( String name )
	{
	    try { 
	    	byte sequence[] = getToneSequence(name);
	    	if (sequence != null) {
		    	// As a player cannot return to a state before PREFETCHED
		    	// once it has played, the tone sequence cannot be changed.
		    	// So we have to create a new player every time.
		        Player player = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
		        player.addPlayerListener( this );
		        player.realize(); 
		    	ToneControl toneCtrl = (ToneControl)player.getControl( "ToneControl" );
		    	if (toneCtrl != null) {
		    		toneCtrl.setSequence( sequence ); 
		    		mPlayingNames = name;
		    		player.start();
		    	}
	    	}
	    } catch (Exception ex) {		
	    	mLogger.exception("Failed to play sound", ex);
	    }
	}
//#else
	private void playSequence( String name ) {
	}
//#endif
//#endif

	public void immediateSound(String name) {
		synchronized (NoiseMaker.class) {			
			if (mPlayingNameIndex < mPlayingNames.length()) {
				mPlayingNameIndex = 0;
				mPlayingNames = name + ";" + mPlayingNames;
				mLogger.debug("inserted sound " + name + 
						" giving new sequence: " + mPlayingNames);
				return;
			}
		}
		resetSoundRepeatTimes();
		playSound (name);
		RouteInstructions.reallowInInstruction();
	}
	
	public void playSound( String names )
	{
		playSound(names, (byte) 0, (byte) 1 );
	}

	/* names can contain multiple sound names separated by ;
	 * the contained sound parts will be played after each other
	 * */
	public void playSound( String names, byte minSecsBeforeRepeat, byte maxTimesToPlay )
	{
//#if polish.api.mmapi                         

		// do not repeat same sound before minSecsBeforeRepeat
		long msTime = System.currentTimeMillis();			
		if (mOldPlayingNames.equals(names) &&
				(Math.abs(msTime - mOldMsTime) < 1000L * minSecsBeforeRepeat
			  || mTimesToPlay <= 0 )
		) {
			return;
		}
		//#debug debug
		mLogger.debug(msTime - mOldMsTime + " " + names + mOldPlayingNames + mTimesToPlay);
		mOldMsTime = System.currentTimeMillis();
		if (! mOldPlayingNames.equals(names) ) {
			mTimesToPlay = maxTimesToPlay;
			// if we would skip a speed limit alert by the new sound chain,
			// put the alert sound at the beginning of the new sound chain to play
			if (mPlayingNameIndex < mPlayingNames.length() ) {
				if(
					mPlayingSoundName.equals(RouteSyntax.getSpeedLimitSound())
					|| mPlayingNames.indexOf(RouteSyntax.getSpeedLimitSound(), mPlayingNameIndex) != -1
				) {
					names = RouteSyntax.getSpeedLimitSound() + ";" + names;
				}
			}
			//#debug debug
			mLogger.debug("new sound " + names + " timestoplay: " + mTimesToPlay + 
					"   old sound: " + mOldPlayingNames );
			mOldPlayingNames = names;
		}		
		//#debug debug
		mLogger.debug("play " + names);
		synchronized(NoiseMaker.class) {
			mPlayingNameIndex = 0;
			mPlayingNames=names;
		}
		if (determineNextSoundName() != null) {
			mPlayingNameIndex = 0;
			playNextSoundFile();
		} else {
//#ifndef polish.android
			playSequence(names);
//#endif
		}
		mTimesToPlay--;
		//#debug debug
		mLogger.debug("mTimesToPlay--:" + mTimesToPlay );

//#endif
	}
	
	// allow to play same sound again
	public static void resetSoundRepeatTimes() {
		mOldPlayingNames = "";
		mLogger.debug("reset sound repeat");
	}

//#if polish.api.mmapi				
	// determine next sound name to be played from playingNames
	private static String determineNextSoundName() {
		synchronized (NoiseMaker.class) {
			// end of names to play?
			if (mPlayingNameIndex > mPlayingNames.length() ) {
				return null;
			}
			
			int iEnd = mPlayingNames.indexOf(';', mPlayingNameIndex);
			if (iEnd == -1 ) {
				iEnd = mPlayingNames.length();
			}
			String nextSoundName = mPlayingNames.substring(mPlayingNameIndex, iEnd);
			//#debug debug
			mLogger.debug("Determined sound part: " + nextSoundName + "/" + 
					mPlayingNames + "/" + mPlayingNameIndex + "/" + iEnd);
			mPlayingNameIndex = iEnd + 1;
			mPlayingSoundName = nextSoundName;
			return nextSoundName;
		}
	}
	
	private synchronized boolean createResourcePlayer(String soundName) {
		//#debug debug
		mLogger.debug("createResourcePlayer for " + soundName);
		stopPlayer();
		
		String trySuffix;
		boolean fileFound = false;
		for (int i = -1; i < Legend.soundFormats.length; i++) {
			if (i == -1) {
				// if there's no successful suffix yet, continue with next (first) sound format
				if (lastSuccessfulSuffix == null) {
					continue;
				}
				// try last successful suffix next
				trySuffix = lastSuccessfulSuffix;
			} else {
				// try suffix list last
				trySuffix = Legend.soundFormats[i];
//				System.out.println("****************** try " + trySuffix);
			}
			String mediaType = null;
			if (trySuffix.equals("amr") ) {
				mediaType = "audio/amr";
			} else if (trySuffix.equals("mp3") ) {
				mediaType = "audio/mpeg";
			} else if (trySuffix.equals("wav") ) {
				mediaType = "audio/x-wav";
			} else if (trySuffix.equals("ogg") ) {
				mediaType = "audio/x-ogg";
			}
			//#debug debug
			mLogger.debug("Preparing to play sound " + soundName.toLowerCase());
			if (preparePlayer(Configuration.getSoundDirectory() + "/" + soundName.toLowerCase() + "." + trySuffix, mediaType, trySuffix)) {
				fileFound = true;
				break;
			} else {
				//#mdebug debug
				mLogger.debug("Could NOT CREATE PLAYER for " + mediaType);
				//#enddebug
			}
		}
		return fileFound;
	}
	
	private synchronized void playNextSoundFile() {
		String nextSoundName = determineNextSoundName();
		if (nextSoundName != null) {
			if (
			//#if polish.android
				false
			//#else
				Configuration.getCfgBitState(Configuration.CFGBIT_SND_TONE_SEQUENCES_PREFERRED) && getToneSequence(nextSoundName) != null
			//#endif
			    ) {
				playSequence(nextSoundName);
			} else if (createResourcePlayer(nextSoundName)) {
				if (sPlayer != null) {
					startPlayer(nextSoundName);
				}
			}
		}
	}
//#endif

	public static synchronized void stopPlayer() {
	//#if polish.api.mmapi	
	if (sPlayer != null) {
		//#debug debug
		mLogger.debug("Closing old player");
		//#if polish.android
		if (sPlayer != null) {
			sPlayer.release();
		}
		//#else
		if (!updatesMissing) {
			sPlayer.close();
		}
		//#endif
		if (!updatesMissing) {
			sPlayer = null;
		}
	}
	//#endif
	}
}

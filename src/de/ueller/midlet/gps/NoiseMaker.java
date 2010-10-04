/*
 * GpsMid - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 *          Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See Copying 
 */

package de.ueller.midlet.gps;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Legend;

import net.sourceforge.util.zip.ZipFile;

import java.io.InputStream;
//#if polish.api.mmapi	
//#ifndef polish.android
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.Manager;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;
//#else
import android.media.AudioManager;
import android.media.MediaPlayer;
import de.enough.polish.android.midlet.MidletBridge;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import java.io.FileDescriptor;
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

	private static ZipFile mapZipFile;

//#if polish.api.mmapi			
//#if polish.android
	private static volatile MediaPlayer sPlayer = null;
//#else
	private static volatile Player sPlayer = null; 
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
	        C4,e, E4,e, G4,e, C5,q
	    };
	    
	    mConnLostSequence = new byte[] {
	        ToneControl.VERSION, 1,
	        ToneControl.TEMPO, tempo,
	        ToneControl.SET_VOLUME, 100,
	        A4,e, A4,e, A4,e, F4,q
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
		// if it doesn't end with /, it's a zip file: use sounds from android bundle for now
		// FIXME add code to play sound from zipfile if possible
		if (Configuration.usingBuiltinMap() || !Configuration.getMapUrl().endsWith("/")) {
			//#debug debug
			mLogger.debug("Preparing to play sound " + soundFile);
		} else {
			soundFileWithSuffix = Configuration.getMapUrl() + soundFile;
			//#debug debug
			mLogger.debug("Preparing to play url " + soundFileWithSuffix);
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
				if (Configuration.usingBuiltinMap() || !Configuration.getMapUrl().endsWith("/")) {
					AssetFileDescriptor afd = MidletBridge.instance.getResources().getAssets().openFd(soundFile);
					sPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
					afd.close();
				} else {
					sPlayer.setDataSource(soundFileWithSuffix.substring("file://".length()));
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
		if (sPlayer != null) sPlayer.release();
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
		if (Configuration.usingBuiltinMap()) {
			soundFileWithSuffix = "/" + soundFile;
		} else {
			if (Configuration.getMapUrl().endsWith("/")) {
				soundFileWithSuffix = soundFile;
			} else {
				soundFileWithSuffix = Configuration.getMapUrl() + soundFile;
			}
		}

		InputStream is = null;
		if (Configuration.usingBuiltinMap()) {
			if (Configuration.getMapUrl().endsWith("/")) {
				// mapdir map
				is = getClass().getResourceAsStream(soundFileWithSuffix);
			} else {
				try {
					if (mapZipFile == null) {
						//#debug debug
						mapZipFile = new ZipFile(Configuration.getMapUrl(), -1);
					}
					is = mapZipFile.getInputStream(mapZipFile.getEntry(soundFileWithSuffix));
				} catch (Exception ex) {
					mLogger.exception("Error opening zip file: " + Configuration.getMapUrl(), ex);
					return false;
				}
				// zip map
			}
		}
		if (is != null) {
			//#debug debug
			mLogger.debug("Got Inputstream for " + soundFileWithSuffix);
			try {
				sPlayer = Manager.createPlayer(is, mediaType);
			} catch (Exception ex) {
				sPlayer = null;
				mLogger.exception("Failed to create resource player for " + soundFileWithSuffix, ex);
			}
		}
		//#debug debug
		else mLogger.debug("RESOURCE NOT FOUND: " + soundFileWithSuffix);
		if (sPlayer != null) {
			//#debug debug
			mLogger.debug("created player for " + soundFileWithSuffix);
			try {
				sPlayer.realize();
			} catch (Exception ex) {
				mLogger.exception("Failed to realize player for " + soundFileWithSuffix, ex);
			}
			//#debug debug
			mLogger.debug("realized player for " + soundFileWithSuffix);
			sPlayer.addPlayerListener( this );
			VolumeControl volCtrl = (VolumeControl) sPlayer.getControl("VolumeControl");
			if (volCtrl != null) {
				volCtrl.setLevel(100);
			}
			lastSuccessfulSuffix = suffix;
			return true;
		}
		return false;
	}
			
	private synchronized void cleanPlayer() {
		sPlayer.close();
	}
	
	public synchronized void playerUpdate( Player player, String event, Object eventData )
	{
		//#debug debug
		mLogger.debug("playerUpdate got " + event);
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
			sPlayer.start();
			//#debug debug
			mLogger.debug("player for " + nextSoundName + " started");
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
		if (sPlayer != null) sPlayer.release();
		//#else
		sPlayer.close();
		//#endif
		sPlayer = null;
	}
	//#endif
	}
}

/*
 * GpsMid - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 *          Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See Copying 
 */

package de.ueller.midlet.gps;

import de.ueller.gps.data.Legend;
import de.ueller.midlet.gps.data.Proj2DMoveUp;
import de.ueller.midlet.gps.tile.SoundDescription;

import java.io.InputStream;
//#if polish.api.mmapi	
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;
//#endif

/**
 * Helper class to create sounds for different state changes of the Midlet.
 */
public class NoiseMaker
//#if polish.api.mmapi	
		implements PlayerListener
//#endif
{
	
	// private final byte[] mPosFixSequence;
	
	// private final byte[] mNoFixSequence;

	
	private final static Logger mLogger = Logger.getInstance(NoiseMaker.class, Logger.DEBUG);
	
	private static volatile String mPlayingNames = "";
	private static volatile int mPlayingNameIndex=0;

//#if polish.api.mmapi			
	private static volatile Player mPlayer = null; 
	private static byte[] mConnOpenedSequence;	
	private static byte[] mConnLostSequence;
//#endif			
	
	private static volatile long mOldMsTime = 0;
	private static volatile String mOldPlayingNames = "";
	private static volatile byte mTimesToPlay = 0;
	private static volatile String mNextSoundFile = null;
	private static volatile String mPlayingSoundName = "";
	
	public NoiseMaker()
	{
//#if polish.api.mmapi	
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
	}

//#if polish.api.mmapi	
	public synchronized void playerUpdate( Player player, String event, Object eventData )
	{
		//#debug debug
		mLogger.debug("playerUpdate got " + event);
		// Release resources used by player when it's finished.
		if (event == PlayerListener.END_OF_MEDIA)
		{
			mPlayer.close();
			playNextSoundFile();
		}
	}
	
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
					mPlayingSoundName.equals("SPEED_LIMIT")
					|| mPlayingNames.indexOf("SPEED_LIMIT", mPlayingNameIndex) != -1
				) {
					names = "SPEED_LIMIT;" + names;
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
		if (determineNextSoundFile()) {
			mPlayingNameIndex = 0;
			playNextSoundFile();
		} else {
			playSequence(names);
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
	
	// determine name of next sound part
	private static boolean determineNextSoundFile() {
		String nextSoundName = determineNextSoundName();
		if (nextSoundName == null) {
			mNextSoundFile = null;
			return false;
		}
		
		SoundDescription sDes = Legend.getSoundDescription(nextSoundName);
		if (sDes != null) {
			mNextSoundFile = sDes.soundFile;
			//#debug debug
			mLogger.debug("using soundfile " + mNextSoundFile + " from description for " +
					nextSoundName);
		}
		//#debug debug
		else mLogger.debug("no description found for " + nextSoundName);
		if (sDes == null || mNextSoundFile == null) {
			if (getToneSequence(nextSoundName) != null) {
				return false;
			}
			mNextSoundFile = "/" + nextSoundName.toLowerCase() + ".amr";
			//#debug debug
			mLogger.debug("using soundname + extension as sound file: " + mNextSoundFile);
		}
		return true;
	}
	
	private synchronized void createResourcePlayer(String soundFile) {
		//#debug debug
		mLogger.debug("createResourcePlayer for " + soundFile);
		if (mPlayer != null) {
			//#debug debug
			mLogger.debug("Closing old player");
			mPlayer.close();
			mPlayer = null;
		}	
		try {
			InputStream is = getClass().getResourceAsStream(soundFile);
			if (is != null) {
				//#debug debug
				mLogger.debug("Got Inputstream for " + soundFile);
				String mediaType = null;
				if (soundFile.toLowerCase().endsWith(".amr") ) {
					mediaType = "audio/amr";
				} else if (soundFile.toLowerCase().endsWith(".wav") ) {
					mediaType = "audio/x-wav";
				} else if (soundFile.toLowerCase().endsWith(".mp3") ) {
					mediaType = "audio/mpeg";
				} else if (soundFile.toLowerCase().endsWith(".ogg") ) {
	            	mediaType = "audio/x-ogg";
				}
				mPlayer = Manager.createPlayer(is, mediaType);
				if (mPlayer!=null) {
					//#debug debug
					mLogger.debug("created player for " + soundFile);
					mPlayer.realize();
					//#debug debug
					mLogger.debug("realized player for " + soundFile);
					mPlayer.addPlayerListener( this );
					VolumeControl volCtrl = (VolumeControl) mPlayer.getControl("VolumeControl");
					if (volCtrl != null) {
						volCtrl.setLevel(100);
					}
				}
                //#debug debug
                else mLogger.debug("Could NOT CREATE PLAYER for " + mediaType);
			}
			//#debug debug
			else mLogger.debug("RESOURCE NOT FOUND: " + soundFile);
		} catch (Exception ex) {
	    	mLogger.exception("Failed to create resource player for " + soundFile, ex);
			mPlayer = null;
		}
	}
	
		
	private synchronized void playNextSoundFile() {
		determineNextSoundFile();
		if (mNextSoundFile != null) {
			createResourcePlayer(mNextSoundFile);
			if (mPlayer != null) {
				try {
					mPlayer.start();
					//#debug debug
					mLogger.debug("player for " + mNextSoundFile + " started");
				} catch (Exception ex) {
			    	mLogger.exception("Failed to play sound", ex);
				}
			}
		}
	}
//#endif
}

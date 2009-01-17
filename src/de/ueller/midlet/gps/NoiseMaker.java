/*
 * GpsMid - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 *          Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See Copying 
 */

package de.ueller.midlet.gps;

import de.ueller.midlet.gps.data.Proj2DMoveUp;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.SoundDescription;

import java.io.InputStream;
//#if polish.api.mmapi	
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
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
	
	private static volatile String playingNames = "";
	private static volatile int playingNameIndex=0;

//#if polish.api.mmapi			
	private static volatile Player player[] = {null, null, null}; 
	private final byte[] mConnOpenedSequence;	
	private final byte[] mConnLostSequence;
//#endif			

	private static volatile byte prefetchPlayerNr=1; 
	
	private static volatile long oldMsTime = 0;
	private static volatile String oldPlayingNames = "";
	private static volatile byte timesToPlay = 0;
	private static volatile String nextSoundFile = null;

	private static boolean deviceSupportsMixing;
	
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

	   String mixing = System.getProperty("supports.mixing");
	   deviceSupportsMixing = (mixing != null && mixing.equals("true"));
	   //deviceSupportsMixing = false;
//#endif
	}

//#if polish.api.mmapi	
	public synchronized void playerUpdate( Player player, String event, Object eventData )
	{
		// Release resources used by player when it's finished.
		if (event == PlayerListener.END_OF_MEDIA)
		{
			//System.out.println("Playing stopped");
			player.close();
			// mark player as free if it's one of the prefetch players
			for (int i=0; i<3; i++) {
				if (this.player[i] != null && this.player[i] == player) {
					this.player[i] = null;
					break;
				}
			}
			playPrefetched();
		}
	}
	
	private byte [] getToneSequence( String name ) {
    	byte sequence[] = null;
		if(name.equals("CONNECT")) {
			/**
			 * Sound for the situation that the connection to the
			 * GPS device has been (re)established.
			 */
			sequence= mConnOpenedSequence;	
		} else if (name.equals("DISCONNECT")) {
			/**
			 * Sound for the situation that the connection to the
			 * GPS device has been (re)established.
			 */
			sequence= mConnLostSequence;				
		} else if (name.equals("FIX")) {
			/**
			 * Sound for the situation that a valid position was received
			 */
			//sequence= mPosFixSequence;				
		} else if (name.equals("NOFIX")) {
			/**
			 * Sound for the situation that the position has become unknown.
			 */
			//sequence= mNoFixSequence;				
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
		        toneCtrl.setSequence( sequence ); 
				playingNames = name;
		        player.start();
	    	}
	    } catch (Exception ex) {		
	    	mLogger.exception("Failed to play sound", ex);
	    }
	}
//#endif
	

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
		if (oldPlayingNames.equals(names) &&
				(Math.abs(msTime-oldMsTime) < 1000L * minSecsBeforeRepeat
			  || timesToPlay <= 0 )
		) {
			return;
		}
		//#debug debug
		mLogger.debug(msTime-oldMsTime + " " + names + oldPlayingNames + timesToPlay);
		oldMsTime = System.currentTimeMillis();
		if (! oldPlayingNames.equals(names) ) {
			timesToPlay = maxTimesToPlay;
			//#debug debug
			mLogger.debug("new sound " + names + " timestoplay: " + timesToPlay + "   old sound: " + oldPlayingNames );
			oldPlayingNames = names;
		}		
		//#debug debug
		mLogger.debug("play " + names);
		playingNameIndex = 0;
		playingNames=names;
		if (prefetchNextSound()) {
			playPrefetched();
		} else {
			playSequence(names);
		}
		timesToPlay--;
		//#debug debug
		mLogger.debug("timestoplay--:" + timesToPlay );
//#endif
	}
	
	// allow to play same sound again
	public void resetSoundRepeatTimes() {
		oldPlayingNames = "";
		mLogger.debug("reset sound repeat");
	}

//#if polish.api.mmapi				

	
	// determine next sound name to be played from playingNames
	private String determineNextSoundName() {	
		// end of names to play?
		if (playingNameIndex>playingNames.length() ) {
			return null;
		}
		
		int iEnd = playingNames.indexOf(';', playingNameIndex);
		if (iEnd == -1 ) {
			iEnd = playingNames.length();
		}
		String nextSoundName = playingNames.substring(playingNameIndex, iEnd);
		//#debug debug
		mLogger.debug("Determined sound part: " + name + "/" + playingNames + "/" + playingNameIndex + "/" + iEnd + " to player " + prefetchPlayerNr);
		playingNameIndex = iEnd + 1;
		return nextSoundName;
	}
	
	// prefetches next sound part
	private synchronized boolean prefetchNextSound() {
		String nextSoundName = determineNextSoundName();
		if (nextSoundName == null) {
			nextSoundFile = null;
			return false;
		}
		
		SoundDescription sDes = C.getSoundDescription(nextSoundName);
		if (sDes != null) {
			nextSoundFile = sDes.soundFile;
		}
		if (sDes == null || nextSoundFile == null) {
			if (getToneSequence(nextSoundName) != null) {
				return false;
			}
			nextSoundFile = nextSoundName.toLowerCase() + ".amr";
		}
		

		// use next prefetch player
		prefetchPlayerNr++;
		if (prefetchPlayerNr > 2) {
			prefetchPlayerNr = 0;
		}
		
		if (deviceSupportsMixing) {
			if (createResourcePlayer(nextSoundFile)) {
				try {
					player[prefetchPlayerNr].prefetch();
				} catch (MediaException mpe) {
					try {
						player[prefetchPlayerNr].realize();					
					} catch (MediaException mpe2) {
						//#debug debug
						mLogger.debug("realizing player failed");
					}
				}
			}
		}
		return true;
	}
	
	private boolean createResourcePlayer(String soundFile) {
		// if next prefetch player is not free, free it
		if (player[prefetchPlayerNr]!=null) {
			player[prefetchPlayerNr].close();
			player[prefetchPlayerNr]=null;
		}	
		try {
			InputStream is = getClass().getResourceAsStream(soundFile);
			if (is != null) {
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
				player[prefetchPlayerNr] = Manager.createPlayer(is, mediaType);
				if (player[prefetchPlayerNr]!=null) {
					player[prefetchPlayerNr].realize();
					player[prefetchPlayerNr].addPlayerListener( this );
					VolumeControl volCtrl = (VolumeControl) player[prefetchPlayerNr].getControl("VolumeControl");
					volCtrl.setLevel(100);
				}
			}
		} catch (Exception ex) {
	    	mLogger.exception("Failed to create resource player for " + soundFile, ex);
			return false;
		}
		return true;
	}
	
		
	private synchronized void playPrefetched() {
		if (!deviceSupportsMixing && nextSoundFile != null) {
			createResourcePlayer(nextSoundFile);
		}
		Player pl = player[prefetchPlayerNr];
		try {
			//#debug debug
			mLogger.debug("Playing Player " + prefetchPlayerNr);
			if (pl != null) {
				pl.start();
			}
		} catch (Exception ex) {
	    	mLogger.exception("Failed to play prefetched sound", ex);
		}
		prefetchNextSound();
	}
//#endif
}
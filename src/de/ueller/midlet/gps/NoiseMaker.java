/*
 * GpsMid - Copyright (c) 2008 mbaeuerle at users dot sourceforge dot net 
 *          Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 */

package de.ueller.midlet.gps;

import de.ueller.midlet.gps.tile.C;
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
	
	private byte[] mPosFixSequence;
	
	private byte[] mNoFixSequence;

	private byte[] mConnOpenedSequence;
	
	private byte[] mConnLostSequence;
	
	private final static Logger mLogger = Logger.getInstance(NoiseMaker.class, Logger.DEBUG);
	
	private static volatile String playingNames = "";
	private static volatile int playingNameIndex=0;
//#if polish.api.mmapi			
	private static volatile Player [] player = new Player[3]; 
//#endif			
	private static volatile byte currentPlayerNr=0; 
	private static volatile byte prefetchPlayerNr=1; 
	private static volatile boolean [] prefetched={false, false, false}; 
	
	
	private static long oldMsTime = 0;
	private static String oldplayingNames = "";

	
	public NoiseMaker()
	{
//#if polish.api.mmapi	
	    byte tempo = 30; // set tempo to 120 bpm 
	    byte e = 8;  // eighth-note
	    byte q = 16; // quarter-note

	    byte C4 = ToneControl.C4; 
	    byte D4 = (byte)(C4 + 2); // a whole step 
	    byte E4 = (byte)(C4 + 4); // a major third 
	    byte F4 = (byte)(C4 + 5); // a fourth
	    byte G4 = (byte)(C4 + 7); // a fifth
	    byte A4 = (byte)(C4 + 9);
	    byte B4 = (byte)(C4 + 10);
	    byte H4 = (byte)(C4 + 11);
	    byte C5 = (byte)(C4 + 12);

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
//#endif
	}

//#if polish.api.mmapi	
	public void playerUpdate( Player player, String event, Object eventData )
	{
		// Release resources used by player when it's finished.
		if (event == PlayerListener.END_OF_MEDIA)
		{
			//System.out.println("Playing stopped");
			player.close();
			if (prefetched[prefetchPlayerNr]) {
				playPrefetched();
			} else {
				playingNames = "";
				playingNameIndex=0;
				oldMsTime = System.currentTimeMillis();
			}
		}
	}
//#endif
	
	private void playSequence( String name )
	{
//#if polish.api.mmapi	
	    try { 
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
//#endif
	}
	

	public void playSound( String names )
	{
		playSound(names, (byte) 0);
	}

	/* names can contain multiple sound names separated by ;
	 * the contained sound parts will be played after each other
	 * */
	public void playSound( String names, byte minSecsBeforeRepeat )
	{
		if (playingNames.equals(names)) {			
			//System.out.println("Already playing");
			return;
		}

		// do not repeat same sound before minSecsBeforeRepeat
		long msTime = System.currentTimeMillis();
		if (oldplayingNames.equals(names) && Math.abs(msTime-oldMsTime) < minSecsBeforeRepeat*1000) {
			//System.out.println(msTime-oldMsTime + " " + name + oldplayingNames);
			return;
		}
		oldplayingNames = names;

		playingNameIndex = 0;
		playingNames=names;
		if (prefetchNextSound()) {
			playPrefetched();
		} else {
			playSequence(names);
		}
	}
	
	
	// prefetches next sound part
	private synchronized boolean prefetchNextSound() {
//#if polish.api.mmapi			
		// ignore request if we've got no free player
		if (prefetched[prefetchPlayerNr]==true) {
			return false;
		}
		
		// use next prefetch player
		prefetchPlayerNr++;
		if (prefetchPlayerNr > 2) {
			prefetchPlayerNr = 0;
		}

		// end of names to play?
		if (playingNameIndex>playingNames.length() ) {
			return false;
		}
		
		int iEnd = playingNames.indexOf(';', playingNameIndex);
		if (iEnd == -1 ) {
			iEnd = playingNames.length();
		}
		String name = playingNames.substring(playingNameIndex, iEnd);
		//System.out.println("Prefetching sound part: " + name + "/" + playingNames + "/" + playingNameIndex + "/" + iEnd + " to player " + prefetchPlayerNr);
		playingNameIndex = iEnd + 1;
		
		String soundFile = null;
		SoundDescription sDes = C.getSoundDescription(name);
		if (sDes != null) {
			soundFile = sDes.soundFile;
		}
		if (sDes == null || soundFile == null) {
			soundFile = name.toLowerCase() + ".amr";
		}
		
		try {
			InputStream is = getClass().getResourceAsStream(soundFile);
			if (is != null) {
				String mediaType = null;
				if (soundFile.toLowerCase().endsWith(".mp3") ) {
					mediaType = "audio/mpeg";
				} else if (soundFile.toLowerCase().endsWith(".wav") ) {
					mediaType = "audio/x-wav";
				} else if (soundFile.toLowerCase().endsWith(".amr") ) {
					mediaType = "audio/amr";
				}
				player[prefetchPlayerNr] = Manager.createPlayer(is, mediaType);
				player[prefetchPlayerNr].addPlayerListener( this );
				player[prefetchPlayerNr].prefetch();
				VolumeControl volCtrl = (VolumeControl) player[prefetchPlayerNr].getControl("VolumeControl");
				volCtrl.setLevel(100);
				prefetched[prefetchPlayerNr]=true;
			} else {
				return false;
			}
		} catch (Exception ex) {
	    	mLogger.exception("Failed to play sound from resource", ex);
			return false;
		}
//#endif
		return true;
	}
	
	
	private synchronized void playPrefetched() {
//#if polish.api.mmapi			
		try {
			//System.out.println("Playing Player " + prefetchPlayerNr);
			if (player[prefetchPlayerNr].getState() == Player.PREFETCHED) {
				player[prefetchPlayerNr].start();
			}
		} catch (Exception ex) {
	    	mLogger.exception("Failed to play prefetched sound", ex);
		}
		prefetched[prefetchPlayerNr] = false;
		prefetchNextSound();
//#endif
	}
}
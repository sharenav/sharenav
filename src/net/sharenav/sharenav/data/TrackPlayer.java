package net.sharenav.sharenav.data;

import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

import de.enough.polish.util.Locale;

/*
 * ShareNav - Copyright (c) 200 sk750 at users dot sourceforge dot net 
 * this class replays a GPX track
 */


public class TrackPlayer implements Runnable {
	private final static Logger logger = Logger.getInstance(TrackPlayer.class, 
			Logger.TRACE);

	private volatile boolean shutdown = false;
	private Thread processorThread;
	private float trkPtLat[];
	private float trkPtLon[];
	private long trkPtTime[];
	private short trkPtSpeed[];
	private int iReplaying = 0;
	private int step = 0;
	private final int STEPS_BETWEEN_TRACKPOINTS = 4;

	public static boolean isPlaying = false;
	public static TrackPlayer trackPlayer;
	public static int trackPtsPerSecond = 1;
	
	public TrackPlayer() {
		super();
	}

	public void playTrack(float trkPtLat[], float trkPtLon[], long trkPtTime[], short trkPtSpeed[]) {
		stop();
		shutdown = false;
		trackPtsPerSecond = 1;
		this.trkPtLat = trkPtLat;
		this.trkPtLon = trkPtLon;
		this.trkPtTime = trkPtTime;
		this.trkPtSpeed = trkPtSpeed;
		
		iReplaying = 0;
		processorThread = new Thread(this, "TrackPlayer");
		//processorThread.setPriority(Thread.NORMAL_PRIORITY);
		processorThread.start();
	}

	
	public void run() {
		isPlaying = true;
		Trace tr = Trace.getInstance();
		Position pos = null;
		do {
			if (iReplaying < trkPtLat.length) {
//				System.out.println("replay trkPt " + iReplaying );

				// we have no course in the tracks, therefore we need to calculate it ourselves
				int courseToGo = 0;
				if (iReplaying > 1) {
					courseToGo = - (int) (MoreMath.bearing_start(
							trkPtLat[iReplaying - 1],
							trkPtLon[iReplaying - 1],
							trkPtLat[iReplaying],
							trkPtLon[iReplaying]
					) * 2);
					courseToGo %= 360;
					if (courseToGo < 0) {
						courseToGo += 360;
					}
//					System.out.println(" course " +  courseToGo);
				}
				short speed = trkPtSpeed[iReplaying];
				if (speed == 0  && iReplaying > 0) {
					/* we have no speed in the track, therefore we need to calculate it ourselves
					 * assume we got 1 trackpoint per second, therefore the speed is same as meters moved
					 */
					speed = (short) MoreMath.dist(
									trkPtLat[iReplaying - 1] * MoreMath.FAC_DECTORAD,
									trkPtLon[iReplaying - 1] * MoreMath.FAC_DECTORAD,
									trkPtLat[iReplaying] * MoreMath.FAC_DECTORAD,
									trkPtLon[iReplaying] * MoreMath.FAC_DECTORAD
					);
					speed *= trackPtsPerSecond;
				}
//				System.out.println(" speed " +  speed);

				// calculate extra points for more smooth moving
				float deltaLat = 0;
				float deltaLon = 0;
				if (iReplaying < trkPtLat.length - 1) {
					deltaLat = trkPtLat[iReplaying + 1] - trkPtLat[iReplaying];
					deltaLon = trkPtLon[iReplaying + 1] - trkPtLon[iReplaying];
					deltaLat = (deltaLat * step) / STEPS_BETWEEN_TRACKPOINTS;
					deltaLon = (deltaLon * step) / STEPS_BETWEEN_TRACKPOINTS;
				}
				
				// send the position to Trace
				pos = new Position(
					trkPtLat[iReplaying] + deltaLat, 
					trkPtLon[iReplaying] + deltaLon,
					0,
					speed,
					courseToGo,
					0,
					trkPtTime[iReplaying]
				);
				step++;
				if (step == STEPS_BETWEEN_TRACKPOINTS ) {
					step = 0;
					iReplaying++;
				}
				tr.receivePosition(pos);
				try {
					Thread.sleep(1000 / trackPtsPerSecond / STEPS_BETWEEN_TRACKPOINTS);
				} catch (InterruptedException e) {
					;
				}
			} else {
				shutdown = true;
			}
		} while (!shutdown);
		// receive last position again with no speed and altitude
		if (pos != null) {
			pos.speed = 0;
			pos.altitude = 0;
			tr.receivePosition(pos);		
		}
		tr.receiveMessage(Locale.get("trackplayer.ReplayDone")/*Replay done*/);
		processorThread = null;
		this.trkPtLat = null;
		this.trkPtLon = null;
		this.trkPtTime = null;
		this.trkPtSpeed = null;
		
		synchronized (this) {
			isPlaying = false;
			notifyAll();
		}
	}
		
	public synchronized void stop() {
		shutdown = true;
		notifyAll();
		try {
			while ((processorThread != null) && (processorThread.isAlive())) {
				wait(1000);
			}
		} catch (InterruptedException e) {
			//Nothing to do
		}
	}
	
	public static void faster() {
		addToReplaySpeed(1);
	}

	public static void slower() {
		addToReplaySpeed(-1);
	}
	
	public static void addToReplaySpeed(int diff) {
		if ( (trackPtsPerSecond + diff) <= 50 &&  (trackPtsPerSecond + diff) >= 1) {
			trackPtsPerSecond += diff;
		}
		Trace.getInstance().alert(Locale.get("trackplayer.Player")/*Player: */ + getInstance().iReplaying + "/" + getInstance().trkPtLat.length + Locale.get("trackplayer.trackpoints")/* trackpoints*/, Locale.get("trackplayer.ReplayingAt")/*Replaying at */ + trackPtsPerSecond + Locale.get("trackplayer.trackpointss")/* trackpoints/s*/, 1000);
	}
	
	/**
	 * Returns the instance of the TrackPlayer. If non exists yet,
	 * start a new instance.
	 * @return
	 */
	public static synchronized TrackPlayer getInstance() {
		if (trackPlayer == null) {
			try {
				trackPlayer = new TrackPlayer();
			} catch (Exception e) {
				logger.exception(Locale.get("trackplayer.FailedToInitTrackPlayer")/*Failed to initialise TrackPlayer*/, e);
			}
		}
		return trackPlayer;
	}
	
}

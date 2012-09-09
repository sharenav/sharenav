/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * 			Copyright (c) 2009 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Integer;
import java.lang.Math;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.importexport.ExportSession;
import net.sharenav.sharenav.importexport.GpxImportHandler;
import net.sharenav.sharenav.importexport.GpxParser;
import net.sharenav.sharenav.tile.GpxTile;
import net.sharenav.sharenav.tile.Tile;
import net.sharenav.sharenav.tile.WaypointsTile;
import net.sharenav.sharenav.ui.GuiNameEnter;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.midlet.ui.InputListener;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.DateTimeTools;
import net.sharenav.util.HelperRoutines;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

import de.enough.polish.util.Locale;

/**
 * Handles pretty much everything that has to do with tracks and waypoints:
 * <ul>
 * <li>Recording tracks,</li>
 * <li>Adding waypoints,</li>
 * <li>Deleting, Renaming,</li>
 * <li>Storing in RecordStores,</li>
 * <li>Exporting and Importing in GPX format.</li>
 * </ul>
 */
public class Gpx extends Tile implements Runnable, InputListener {
	
	// statics for user-defined rules for track recording
	private static long oldMsTime;
	private static float oldlat;
	private static float oldlon;
	private static float oldheight;
	
	private final static Logger logger = Logger.getInstance(Gpx.class, Logger.DEBUG);
	
	private RecordStore trackDatabase = null;
	private RecordStore wayptDatabase = null;
	private int trackDatabaseRecordId = -1;
	/** Counts how many track points were recorded so far. */
	private int mTrkRecorded = 0;
	/** Counts how many tracke segments were recorded so far - subtracted from user-visible point count. */
	private int mTrkSegments = 0;
	public int delay = 0;
	
	private float trkOdo;
	private float trkVertSpd;
	private float trkVmax;
	private int   trkTimeTot;
	
	private Thread processorThread = null;
	private String url = null;
	private String waypointsSaveFileName = null;
	
	/** Value for mJobState: No job */
	private static final int JOB_IDLE = 0;
	/** Value for mJobState: Reloading waypoints */
	private static final int JOB_RELOAD_WPTS = 1;
	/** Value for mJobState: Exporting tracks to GPX */
	private static final int JOB_EXPORT_TRKS = 2;
	/** Value for mJobState: Exporting waypoints to GPX */
	private static final int JOB_EXPORT_WPTS = 3;
	/** Value for mJobState: Importing GPX data */
	private static final int JOB_IMPORT_GPX = 4;
	/** Value for mJobState: Deleting tracks */
	private static final int JOB_DELETE_TRKS = 5;
	/** Value for mJobState: Deleting waypoints */
	private static final int JOB_DELETE_WPTS = 6;
	/** Value for mJobState: Saving track to RecordStore */
	private static final int JOB_SAVE_TRK = 7;
	
	/** State of job processing in run(). */
	private int mJobState;
	
	/** Flag whether user is currently being asked for name of track at start of recording. */
	private boolean mEnteringGpxNameStart = false;

	/** Flag whether user is currently being asked for name of track at end of recording. */
	private boolean mEnteringGpxNameStop = false;
	
	private boolean applyRecordingRules = true;
	private float maxDistance;
	
	/** Actual name of the track to be saved */
	private String trackName;

	/** Original name of the track to be saved */
	private String origTrackName;

	/** Vector of tracks to be exported */
	private Vector exportTracks;

	/** Vector of tracks to be deleted */
	private Vector mTrksToDelete;

	/** Track that is currently being exported */
	private PersistEntity currentTrk;
	
	/** Vector of way point IDs to be deleted */
	private Vector mWayPtIdsToDelete;
	
	private UploadListener feedbackListener;
	
	private String importExportMessage;
	
	/** Stream used for importing GPX data */
	private InputStream mImportStream;

	/** Primary stream used to hold the track points that are recorded. */
	private DataOutputStream mTrkOutStream;

	/** The stream dos forwards his data to this stream. */
	private ByteArrayOutputStream mTrkByteOutStream;
	
	/** Flag whether track recording is currently suspended. */
	private boolean trkRecordingSuspended;
	
	/** Holds the track that is currently recorded. */
	private final GpxTile trackTile;

	/** Holds tracks that are loaded to be displayed but not altered by recording more trackpoints. */
	private final GpxTile	loadedTracksTile;

	/** Holds all waypoints to display them on the map. */
	private final WaypointsTile wayPtTile;

	/** Default constructor of this class.
	 * It creates new tiles for waypoints, the recording track and loaded tracks.
	 * It also triggers loading of the waypoints from the RecordStore.
	 */
	public Gpx() {
		trackTile = new GpxTile();
		loadedTracksTile = new GpxTile(true);
		wayPtTile = new WaypointsTile();
		reloadWayPts();
	}
	
	public void displayWaypoints(boolean displayWpt) {
		
	}
	
	/**
	 * Loads one given track and displays it on the map-screen
	 * @param trk The track to be displayed
	 */
	public void displayTrk(PersistEntity trk) {
		if (trk == null) {
			//TODO:
		} else {
			try {
				trackTile.dropTrk();
				openTrackDatabase();
				DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(trk.id)));
				trackName = dis1.readUTF();
				mTrkRecorded = dis1.readInt();
				mTrkSegments = 0;
				int trackSize = dis1.readInt();
				byte[] trackArray = new byte[trackSize];
				dis1.read(trackArray);
				DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
				for (int i = 0; i < mTrkRecorded; i++) {
					float lat = trackIS.readFloat();
					float lon = trackIS.readFloat();
					if (i == 0) {
						Trace tr = Trace.getInstance();
						tr.receivePosition(lat * MoreMath.FAC_DECTORAD,
								lon * MoreMath.FAC_DECTORAD, tr.scale);
					}
					trackIS.readShort(); //altitude
					long time = trackIS.readLong();	//Time
					trackIS.readByte(); //Speed
					if (time > Long.MIN_VALUE + 10) { 
						// We use some special markers in the Time to indicate
						// data other than trackpoints, so ignore these.
						trackTile.addTrkPt(lat, lon, false);
					}
				}
				dis1.close();
				dis1 = null;
				trackDatabase.closeRecordStore();
				trackDatabase = null;
			} catch (IOException e) {
				logger.exception(Locale.get("gpx.IOExceptionDisplayingTrack")/*IOException displaying track*/, e);
			} catch (RecordStoreNotOpenException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrackDBNotOpen")/*Exception displaying track (database not open)*/, e);
			} catch (InvalidRecordIDException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrackIDInvalid")/*Exception displaying track (ID invalid)*/, e);
			} catch (RecordStoreException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrack")/*Exception displaying track*/, e);
			}
		}
	}
	
	/**
	 * Loads the given tracks to display them on the map-screen
	 * @param trks Vector of tracks to be displayed
	 */
	public void displayTrk(Vector trks) {
		if (trks == null) {
			//TODO:
		} else {
			try {
				loadedTracksTile.dropTrk();
				openTrackDatabase();
				for (int j = 0; j< trks.size(); j++) {
					PersistEntity track = (PersistEntity)trks.elementAt(j);
					DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(track.id)));
					trackName = dis1.readUTF();
					mTrkRecorded = dis1.readInt();
					mTrkSegments = 0;
					int trackSize = dis1.readInt();
					byte[] trackArray = new byte[trackSize];
					dis1.read(trackArray);
					DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
					for (int i = 0; i < mTrkRecorded; i++) {
						float lat = trackIS.readFloat();
						float lon = trackIS.readFloat();
						//center map on track start
						if (i == 0 && j == 0) {
							Trace tr = Trace.getInstance();
							tr.receivePosition(lat * MoreMath.FAC_DECTORAD,
									lon * MoreMath.FAC_DECTORAD, tr.scale);
						}
						trackIS.readShort(); //altitude
						long time = trackIS.readLong();	//Time
						trackIS.readByte(); //Speed
						if (time > Long.MIN_VALUE + 10) { //We use some special markers in the Time to indicate
										//Data other than trackpoints, so ignore these.
							loadedTracksTile.addTrkPt(lat, lon, false);
						}
					}
					dis1.close();
					dis1 = null;
				}
				
				trackDatabase.closeRecordStore();
				trackDatabase = null;
				
			} catch (OutOfMemoryError oome) {
				loadedTracksTile.dropTrk();
				try {
					trackDatabase.closeRecordStore();
				} catch (RecordStoreException e) {
					logger.exception(Locale.get("gpx.ExceptionClosingRecordstoreAfterOutOfMemoryErrorDisplayingTracks")/*Exception closing Recordstore after OutOfMemoryError displaying tracks*/, e);
				}
				trackDatabase = null;
				System.gc();
			
			} catch (IOException e) {
				logger.exception(Locale.get("gpx.IOExceptionDisplayingTrack")/*IOException displaying track*/, e);
			} catch (RecordStoreNotOpenException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrackDBNotOpen")/*Exception displaying track (database not open)*/, e);
			} catch (InvalidRecordIDException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrackIDInvalid")/*Exception displaying track (ID invalid)*/, e);
			} catch (RecordStoreException e) {
				logger.exception(Locale.get("gpx.ExceptionDisplayingTrack")/*Exception displaying track*/, e);
			}
		}
		
	}

	
	/**
	 * Loads the first given track to replay it on the map-screen
	 * @param trks Vector of tracks to be replayed
	 */
	public void replayTrk(Vector trks) {
		if (trks == null) {
			//TODO:
		} else {
			try {
				openTrackDatabase();
				PersistEntity track = (PersistEntity)trks.firstElement();
				DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(track.id)));
				trackName = dis1.readUTF();
				mTrkRecorded = dis1.readInt();
				mTrkSegments = 0;
				//#debug debug
				logger.debug("Replay track with " + mTrkRecorded + " points");
				int trackSize = dis1.readInt();
				byte[] trackArray = new byte[trackSize];
				dis1.read(trackArray);
				DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
				float trkPtLat[] = new float[mTrkRecorded];
				float trkPtLon[] = new float[mTrkRecorded];
				long trkPtTime[] = new long[mTrkRecorded];
				short trkPtSpeed[] = new short[mTrkRecorded];
				for (int i = 0; i < mTrkRecorded; i++) {
					trkPtLat[i] = trackIS.readFloat();
					trkPtLon[i] = trackIS.readFloat();
					trackIS.readShort(); //altitude
					trkPtTime[i] = trackIS.readLong();	//Time
					trkPtSpeed[i] = trackIS.readByte(); //Speed
				}
				TrackPlayer.getInstance().playTrack(trkPtLat, trkPtLon, trkPtTime, trkPtSpeed);
				dis1.close();
				dis1 = null;
				
				trackDatabase.closeRecordStore();
				trackDatabase = null;
				
			} catch (OutOfMemoryError oome) {
				try {
					trackDatabase.closeRecordStore();
				} catch (RecordStoreException e) {
					logger.exception(Locale.get("gpx.ExceptionClosingRecordstoreOOM")/*Exception closing Recordstore after OutOfMemoryError replaying tracks*/, e);
				}
				trackDatabase = null;
				System.gc();
			
			} catch (IOException e) {
				logger.exception(Locale.get("gpx.IOExceptionReplayingTrack")/*IOException replaying track*/, e);
			} catch (RecordStoreNotOpenException e) {
				logger.exception(Locale.get("gpx.ExceptionReplayingTrackDBNotOpen")/*Exception replaying track (database not open)*/, e);
			} catch (InvalidRecordIDException e) {
				logger.exception(Locale.get("gpx.ExceptionReplayingTrackIDInvalid")/*Exception replaying track (ID invalid)*/, e);
			} catch (RecordStoreException e) {
				logger.exception(Locale.get("gpx.ExceptionReplayingTrack")/*Exception replaying track*/, e);
			}
		}
	}

	
	/**
	 * Removes all loaded tracks from the screen
	 */
	public void undispLoadedTracks() {
		loadedTracksTile.dropTrk();
	}
	
	/** Adds a waypoint to the RecordStore.
	 * Will also write a marker with the ID of this waypoint to the recording track
	 * if this is enabled in the configuration (CFGBIT_WPTS_IN_TRACK).
	 * @param waypt The waypoint to add
	 */
	public void addWayPt(PositionMark waypt) {
		byte[] buf = waypt.toByte();
		try {
			openWayPtDatabase();
			int id = wayptDatabase.addRecord(buf, 0, buf.length);
			waypt.id = id;
			wayptDatabase.closeRecordStore();
			wayptDatabase = null;
			if (   isRecordingTrk()
				&& Configuration.getCfgBitState(Configuration.CFGBIT_WPTS_IN_TRACK)) {
				// store waypoint in GPX track
				//#debug info
				logger.info("Adding waypoint in GPX track: " + waypt);
				/**
				 * Add a marker to the recording for the waypoint
				 */
				mTrkOutStream.writeFloat(0.0f);
				mTrkOutStream.writeFloat(0.0f);
				mTrkOutStream.writeShort(id);
				mTrkOutStream.writeLong(Long.MIN_VALUE + 1);
				mTrkOutStream.writeByte(0);
				mTrkRecorded++;
			}
			
		} catch (IOException ioe) {
			logger.exception(Locale.get("gpx.FailedWritingWaypointIntoTrack")/*Failed to write waypoint into track*/, ioe);
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.ExceptionStoringWaypointDBNotOpen")/*Exception storing waypoint (database not open)*/, e);
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.RecordStoreFull")/*Record store is full, could not store waypoint*/, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.ExceptionStoringWaypoint")/*Exception storing waypoint*/, e);
		}
		wayPtTile.addWayPt(waypt);
	}

	/** Updates a waypoint in the RecordStore. That is, the waypoint's ID is
	 * searched and its data is replaced.
	 * @param waypt New "version" of the waypoint
	 */
	public void updateWayPt(PositionMark waypt) {
		byte[] buf = waypt.toByte();
		try {
			openWayPtDatabase();
			wayptDatabase.setRecord(waypt.id, buf, 0, buf.length);
			wayptDatabase.closeRecordStore();
			wayptDatabase = null;
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.ExceptionUpdatingWaypointDBNotOpen")/*Exception updating  waypoint (database not open)*/, e);
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.RecordStoreFull")/*Record store is full, could not update waypoint*/, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.ExceptionUpdatingWaypoint")/*Exception updating waypoint*/, e);
		}
	}
	
	/** Checks if a waypoint exists in the waypoint tile(s).
	 * 
	 * @param wayPt Waypoint to search
	 * @return True if exists, false if it doesn't
	 */
	public boolean existsWayPt(PositionMark wayPt) {
		if (wayPtTile != null) {
			return wayPtTile.existsWayPt(wayPt);
		}
		return false;
	}
		
	/** Adds a trackpoint to the track's output stream and the trackTile.
	 * This is only done if the recording rules apply and recording is not suspended.
	 * Will also update the odometer data (max speed, distance travelled etc etc)
	 * 
	 * @param trkpt Trackpoint to be added
	 */
	public void addTrkPt(Position trkpt) {
		if (trkRecordingSuspended) {
			return;
		}
		
		//#debug debug
		logger.debug("Adding trackpoint: " + trkpt);
		
		long msTime = trkpt.timeMillis;
		float lat = trkpt.latitude * MoreMath.FAC_DECTORAD;
		float lon = trkpt.longitude * MoreMath.FAC_DECTORAD;
		float distance = 0.0f;
		boolean doRecord=false;

		try {
			// always record when i.e. receiving or loading tracklogs
			// or when starting to record
			if (!applyRecordingRules || mTrkRecorded==0) {
				doRecord=true;
				distance = ProjMath.getDistance(lat, lon, oldlat, oldlon);
			}
			// check which record rules to apply
			else if (Configuration.getGpxRecordRuleMode() == Configuration.GPX_RECORD_ADAPTIVE) {
				/** adaptive recording
				 * 
				 * When saving tracklogs and adaptive recording is enabled,
				 * we reduce the frequency of saved samples if the speed drops
				 * to less than a certain amount. This should increase storage
				 * efficiency if one doesn't need to repeatedly
				 * store positions if the device is not moving
				 * 
				 * Chose the following arbitrary sampling frequency:
				 * Greater 8km/h (2.22 m/s): every sample
				 * Greater 4km/h (1.11 m/s): every second sample
				 * Greater 2km/h (0.55 m/s): every fourth sample
				 * Below 2km/h (0.55 m/s): every tenth sample
				 */
				if ( (trkpt.speed > 2.222f) || ((trkpt.speed > 1.111f) && (delay > 0 )) ||
						((trkpt.speed > 0.556) && delay > 3 ) || (delay > 10)) {
					doRecord = true;
					distance = ProjMath.getDistance(lat, lon, oldlat, oldlon);
					delay = 0;
				} else {
					delay++;
				}
			} else {
				/*
				 * User-specified recording rules
				 */
				distance = ProjMath.getDistance(lat, lon, oldlat, oldlon);
				if (
						// is not always record distance not set
						// or always record distance reached
						(
							Configuration.getGpxRecordAlwaysDistanceCentimeters() !=0 &&
							100 * distance >= Configuration.getGpxRecordAlwaysDistanceCentimeters()
						)
						||
						(
							(
								// is minimum time interval not set
								// or interval at least minimum interval?
									Configuration.getGpxRecordMinMilliseconds() == 0 ||
								Math.abs(msTime - oldMsTime) >= Configuration.getGpxRecordMinMilliseconds()
							)
							&&
							(
							// is minimum distance not set
							// or distance at least minimum distance?
							Configuration.getGpxRecordMinDistanceCentimeters() == 0 ||
							100 * distance >= Configuration.getGpxRecordMinDistanceCentimeters()
							)
						)
				) {
					doRecord = true;
				}
			}
			if (doRecord) {
				// Add point to the recording track's stream
				mTrkOutStream.writeFloat(trkpt.latitude);
				mTrkOutStream.writeFloat(trkpt.longitude);
				mTrkOutStream.writeShort((short)trkpt.altitude);
				mTrkOutStream.writeLong(trkpt.timeMillis);
				mTrkOutStream.writeByte((byte)(trkpt.speed * 3.6f)); //Convert to km/h
				mTrkRecorded++;
				// Add point to the recording track's tile
				trackTile.addTrkPt(trkpt.latitude, trkpt.longitude, false);
				// we need to redraw
				Trace.getInstance().newDataReady();
				// Update odometer data (max speed, distance travelled etc etc)
				if ((oldlat != 0.0f) || (oldlon != 0.0f)) {
					trkOdo += distance;
					long timeDelta = msTime - oldMsTime;
					float deltaV = trkpt.altitude - oldheight;
					trkTimeTot += timeDelta;
					if (timeDelta > 300000) {
						trkVertSpd = deltaV / timeDelta * 1000.0f;
					} else {
						//TODO: This formula is not consistent and needs improvement!!
						float alpha = (300000 - timeDelta) / 300000.0f;
//						System.out.println("trkVertSpeed: " + trkVertSpd +
//								" timeDelta: " + timeDelta + " Alpha: " + alpha +
//								" deltaV " + deltaV + " instVertSpeed: " +
//								(deltaV / timeDelta * 1000.0f));
						trkVertSpd = (trkVertSpd * alpha) + ((1 - alpha) *
								(deltaV / timeDelta * 1000.0f));
					}
					
					if (trkVmax < trkpt.speed) {
						trkVmax = trkpt.speed;
					}
				}
				oldMsTime = msTime;
				oldlat = lat;
				oldlon = lon;
				oldheight = trkpt.altitude;
				// Write track to the RecordStore every 255 points to reduce the
				// data loss in case the application crashes.
				// TODO: This is done synchronously. Couldn't this be a problem if
				// this operation is very slow (device busy, many points to write)?
				if ((mTrkRecorded & 0xff) == 0xff) {
					storeTrk();
				}
			}
		} catch (OutOfMemoryError oome) {
			try {
				Trace.getInstance().dropCache();
				logger.info("Was out of memory, but we might have recovered");
			}catch (OutOfMemoryError oome2) {
				logger.fatal(Locale.get("gpx.OOMCantAddTrackpoint")/*Out of memory, can not add trackpoint*/);
			}
		} catch (IOException e) {
			logger.exception(Locale.get("gpx.CouldNotAddTrackpoint")/*Could not add trackpoint*/, e);
		}
	}

	/** Triggers deletion of waypoints from the RecordStore.
	 * 
	 * @param wayptIds Vector of waypoint IDs to be deleted
	 * @param ul Listener for progress updates
	 */
	public void deleteWayPts(Vector wayptIds, UploadListener ul) {
		mWayPtIdsToDelete = wayptIds;
		feedbackListener = ul;
		startProcessorThread(JOB_DELETE_WPTS);
	}

	/** Actually deletes waypoints from the RecordStore.
	 * Uses the IDs in mWayPtIdsToDelete for this
	 */
	public boolean doDeleteWayPts() {
		try {
			openWayPtDatabase();
			for (int i = 0; i < mWayPtIdsToDelete.size(); i++) {
				wayptDatabase.deleteRecord(((Integer)mWayPtIdsToDelete.elementAt(i)).intValue());
			}
			wayptDatabase.closeRecordStore();
			wayptDatabase = null;
			mWayPtIdsToDelete = null;
			importExportMessage = Locale.get("gpx.Success")/*Success!*/;
			return true;
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingWaypoint")/*Exception deleting waypoint (database not open)*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingWaypoint")/*Exception deleting waypoint (database not open):*/ + " " +
				e.getMessage();
			return false;
		} catch (InvalidRecordIDException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingWaypointIDInvalid")/*Exception deleting waypoint (ID invalid)*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingWaypointIDInvalid")/*Exception deleting waypoint (ID invalid): */ +
				e.getMessage();
			return false;
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingWaypoint2")/*Exception deleting waypoint*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingWaypoint2")/*Exception deleting waypoint: */ +
				e.getMessage();
			return false;
		}
	}

	/** Signals to reload the waypoints.
	 */
	public void reloadWayPts() {
		if (processorThread != null && processorThread.isAlive()) {
			logger.info("ProcessorThread busy, not triggering JOB_RELOAD_WPTS");
			/* Already reloading, nothing to do */
			/* TODO: This is not correct, the thread might be busy with another
			 * operation and then the reload will not be done. */
			return;
		}
		startProcessorThread(JOB_RELOAD_WPTS);
	}
	
	/** Starts the recording of a new track.
	 */
	public void doNewTrk() {
		mTrkByteOutStream = new ByteArrayOutputStream();
		mTrkOutStream = new DataOutputStream(mTrkByteOutStream);
		trackDatabaseRecordId = -1;
		oldlat = 0.0f;
		oldlon = 0.0f;
		trkOdo = 0.0f;
		trkVmax = 0.0f;
		trkVertSpd = 0.0f;
		trkTimeTot = 0;
		mTrkRecorded = 0;
		mTrkSegments = 0;
		trkRecordingSuspended = false;
		origTrackName = new String(trackName);
	}

	/** Starts the recording of a new track. An intermediate step may be to ask
	 * the user for the name of the track.
	 * @param dontAskName If true, the user will not be asked for the track name,
	 *   no matter what the configuration says.
	 */
	public void newTrk(boolean dontAskName) {
		newTrk(null, dontAskName);
	}

	/** Starts the recording of a new track. An intermediate step may be to ask
	 * the user for the name of the track.
	 * @param newTrackName Name of track
	 * @param dontAskName If true, the user will not be asked for the track name,
	 *   no matter what the configuration says.
	 */
	public void newTrk(String newTrackName, boolean dontAskName) {
		logger.debug("Starting a new track recording");
		trackTile.dropTrk();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		
		//Construct a track name from the current time
		if (newTrackName == null) {
			StringBuffer trkName = new StringBuffer();
			// TODO: Should we change the format?
			trkName.append(cal.get(Calendar.YEAR)).append("-").append(DateTimeTools.formatInt2(cal.get(Calendar.MONTH) + 1));
			trkName.append("-").append(DateTimeTools.formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_");
			trkName.append(DateTimeTools.formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(DateTimeTools.formatInt2(cal.get(Calendar.MINUTE)));
			trackName = trkName	.toString();
		} else {
			// TODO: what to do if track with this name exists?
			// TODO: Limit to Configuration.MAX_TRACKNAME_LENGTH
			trackName = new String(newTrackName);
		}
		origTrackName = new String(trackName);
		
		if ((!dontAskName) && Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_START)) {
			mEnteringGpxNameStart = true;
			GuiNameEnter gne = new GuiNameEnter(this, Locale.get("gpx.StartingRecording")/*Starting recording*/, trackName, Configuration.MAX_TRACKNAME_LENGTH);
			doNewTrk();
			gne.show();
		} else {
			doNewTrk();
		}
	}
	
	/** Actually writes the track that was written into mTrkByteOutStream
	 * to the RecordStore for tracks.
	 */
	private void storeTrk() {
		try {
			if (mTrkOutStream == null) {
				logger.debug("Not recording, so no track to save");
				return;
			}
			mTrkOutStream.flush();
			//#debug debug
			logger.debug("storing track with " + mTrkRecorded + " points");
			ByteArrayOutputStream baosDb = new ByteArrayOutputStream();
			DataOutputStream dosDb = new DataOutputStream(baosDb);
			dosDb.writeUTF(trackName);
			dosDb.writeInt(mTrkRecorded);
			dosDb.writeInt(mTrkByteOutStream.size());
			// This is the actual writing of the points' data
			dosDb.write(mTrkByteOutStream.toByteArray());
			dosDb.flush();
			openTrackDatabase();
			// The stream in which the points were written is now saved to the RecordStore
			if (trackDatabaseRecordId < 0) {
				trackDatabaseRecordId = trackDatabase.addRecord(baosDb.toByteArray(), 0, baosDb.size());
			} else {
				trackDatabase.setRecord(trackDatabaseRecordId, baosDb.toByteArray(), 0, baosDb.size());
			}
			trackDatabase.closeRecordStore();
			trackDatabase = null;
			
		} catch (IOException e) {
			logger.exception(Locale.get("gpx.IOExceptionSavingTrack")/*IOException saving track: */, e);
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.ExceptionSavingTrackDBNotOpen")/*Exception saving track (database not open): */, e);
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.ExceptionSavingTrackDBFull")/*Exception saving track (database full): */, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.ExceptionSavingTrack")/*Exception saving track: */, e);
		} catch (OutOfMemoryError oome) {
			logger.fatal(Locale.get("gpx.OOMCantSaveTracklog")/*Out of memory, can not save tracklog*/);
		}
	}

	/** Triggers the saving of the track to the RecordStore (storeTrk()).
	 * Afterwards, no matter if the save was successful(!), clears the streams
	 * where the track points were stored and the trackTile which displayed them.
	 */
	public boolean doSaveTrk() {
		storeTrk();
		try {
			//#debug debug
			logger.debug("Closing track with " + mTrkRecorded + " points");
			mTrkOutStream.close();
			mTrkByteOutStream.close();
		} catch (IOException e) {
			logger.exception(Locale.get("gpx.FailedClosingTrackrecording")/*Failed to close trackrecording*/, e);
		}
		mTrkOutStream = null;
		mTrkByteOutStream = null;
		trackTile.dropTrk();
		return true;
	}

	/** Starts the saving of the track. An intermediate step may be to ask
	 * the user for the name of the track.
	 * @param dontAskName If true, the user will not be asked for the track name,
	 *   no matter what the configuration says.
	 */
	public void saveTrk(boolean dontAskName) {
		if (mTrkOutStream == null) {
			logger.debug("Not recording, so no track to save");
			return;
		}
		if ((!dontAskName) && Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_STOP)) {
			mEnteringGpxNameStop = true;
			GuiNameEnter gne = new GuiNameEnter(this, Locale.get("gpx.StoppingRecording")/*Stopping recording*/, trackName, Configuration.MAX_TRACKNAME_LENGTH);
			gne.show();
		} else {
			startProcessorThread(JOB_SAVE_TRK);
		}
	}
	
	/** Suspends track recording. No more points will be recorded until
	 * resumeTrk() is called. The track will be written to the RecordStore
	 * to reduce the track loss if the application should crash later.
	 */
	public void suspendTrk() {
		trkRecordingSuspended = true;
		try {
			if (mTrkOutStream != null) {
				/**
				 * Add a marker to the recording to be able to break up
				 * the GPX file into separate track segments after each suspend.
				 */
				mTrkOutStream.writeFloat(0.0f);
				mTrkOutStream.writeFloat(0.0f);
				mTrkOutStream.writeShort(0);
				mTrkOutStream.writeLong(Long.MIN_VALUE);
				mTrkOutStream.writeByte(0);
				mTrkRecorded++;
				mTrkSegments++;
				oldlat = 0.0f;
				oldlon = 0.0f;
				storeTrk();
			}
		} catch (IOException ioe) {
			logger.exception(Locale.get("gpx.FailedWritingTrackSegmentationMarker")/*Failed to write track segmentation marker*/, ioe);
		}
	}
	
	/** Must be called after suspendTrk() to resume track recording.
	 */
	public void resumeTrk() {
		trkRecordingSuspended = false;
	}

	/** Triggers deleting of the specified tracks from the RecordStore.
	 * 
	 * @param tracks Vector of tracks to delete
	 */
	public void deleteTracks(Vector tracks, UploadListener ul) {
		mTrksToDelete = tracks;
		feedbackListener = ul;
		startProcessorThread(JOB_DELETE_TRKS);
	}

	/** Actually deletes the tracks in mTrksToDelete from the RecordStore.
	 */
	private boolean doDeleteTracks() {
		try {
			openTrackDatabase();
			for (int i = 0; i < mTrksToDelete.size(); i++)
			{
				trackDatabase.deleteRecord(((PersistEntity)mTrksToDelete.elementAt(i)).id);
			}
			trackDatabase.closeRecordStore();
			trackDatabase = null;
			trackTile.dropTrk();
			mTrksToDelete = null;
			importExportMessage = Locale.get("gpx.Finished")/*Finished!*/;
			return true;
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingTrackDBNotOpen")/*Exception deleting track (database not open)*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingTrackDBNotOpen")/*Exception deleting track (database not open): */ +
				e.getMessage();
			return false;
		} catch (InvalidRecordIDException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingTrackIDInvalid")/*Exception deleting track (ID invalid)*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingTrackIDInvalid")/*Exception deleting track (ID invalid): */ +
				e.getMessage();
			return false;
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.ExceptionDeletingTrack")/*Exception deleting track*/, e);
			importExportMessage = Locale.get("gpx.ExceptionDeletingTrack")/*Exception deleting track: */ +
				e.getMessage();
			return false;
		}
	}
	
	/** Changes the name of a track.
	 * The track is searched in the RecordStore by its ID and is rewritten
	 * with the new name.
	 * 
	 * @param trk The track to rename
	 */
	public void updateTrackName(PersistEntity trk) {
		String action = " " + Locale.get("gpx.ReadingForUpdatingTrackname")/*reading for updating trackname*/;
		try {
			openTrackDatabase();
			DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(
					trackDatabase.getRecord(trk.id)));
			String trackName = dis1.readUTF();
			int recorded = dis1.readInt();
			int trackSize = dis1.readInt();
			byte[] trackArray = new byte[trackSize];
			dis1.read(trackArray);

			action = " " + Locale.get("gpx.PreparingUpdateOf")/*preparing update of*/ + " ";
			ByteArrayOutputStream baosDb = new ByteArrayOutputStream();
			DataOutputStream dosDb = new DataOutputStream(baosDb);
			// check if renaming the track being currently recorded
			if (isRecordingTrk() && trk.id == trackDatabaseRecordId) {
				this.trackName = trk.displayName;
			}
			dosDb.writeUTF(trk.displayName);
			dosDb.writeInt(recorded);
			dosDb.writeInt(trackSize);
			dosDb.write(trackArray);
			dosDb.flush();
			
			action = " " + Locale.get("gpx.UpdatingOf")/*updating of*/ + " ";
			trackDatabase.setRecord(trk.id, baosDb.toByteArray(), 0, baosDb.size());
			
			trackDatabase.closeRecordStore();
			trackDatabase = null;

		} catch (IOException e) {
			logger.exception(Locale.get("gpx.IOException")/*IOException*/ + action, e);
		} catch (RecordStoreNotOpenException e) {
			logger.exception(Locale.get("gpx.Exception")/*Exception*/ + action + Locale.get("gpx.DatabaseNotOpen")/* (database not open)*/, e);
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.Exception")/*Exception*/ + action + Locale.get("gpx.DatabaseFull")/* (database full)*/, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.Exception")/*Exception*/ + action, e);
		} catch (OutOfMemoryError oome) {
			logger.fatal(Locale.get("gpx.OOMCantDo")/*Out of memory, can not do*/ + action);
		}
	}
	
	/** Starts the receiving of GPX data.
	 * 
	 * @param ins Stream from which to read the GPX data
	 * @param ul Listener for progress information
	 * @param maxDist Maximum distance (in kilometers) for filtering waypoints
	 */
	public void receiveGpx(InputStream ins, UploadListener ul, float maxDist) {
		if (ins == null) {
			logger.error(Locale.get("gpx.CouldNotOpenInputStreamToGpxFile")/*Could not open input stream to gpx file*/);
		}
		if ((processorThread != null) && (processorThread.isAlive())) {
			logger.error(Locale.get("gpx.StillProcessingAnotherGpxFile")/*Still processing another gpx file*/);
		}
		maxDistance = maxDist;
		mImportStream = ins;
		feedbackListener = ul;
		startProcessorThread(JOB_IMPORT_GPX);
	}
	
	/** Starts the export of tracks in GPX format.
	 * 
	 * @param url URL to export the tracks to
	 * @param ul Listener for progress information
	 * @param tracks Vector of tracks to export
	 */
	public void exportTracks(String url, UploadListener ul, Vector tracks) {
		logger.debug("Exporting tracks to " + url);
		feedbackListener = ul;
		this.url = url;
		trackTile.dropTrk();
		exportTracks = tracks;
		startProcessorThread(JOB_EXPORT_TRKS);
	}

	/** Starts the export of all waypoints in GPX format.
	 * First, the user is asked for the name of the object which will contain
	 * the waypoints.
	 * 
	 * @param url URL to export the waypoints to
	 * @param ul Listener for progress information
	 */
	public void exportWayPts(String url, String filename, UploadListener ul) {
		this.url = url;
		feedbackListener = ul;
		waypointsSaveFileName = filename;
		startProcessorThread(JOB_EXPORT_WPTS);
	}
	
	/** Returns a list of all way points, sorted by the current search criterion.
	 * 
	 * @return Vector of waypoints
	 */
	public Vector listWayPoints() {
		return listWayPoints(false);
	}

	/** Returns a list of all way points, sorted by the current search criterion.
	 * 
	 * @param favorites flag if only favorites wanted
	 * @return Vector of waypoints
	 */
	public Vector listWayPoints(boolean favorites) {
		int sortmode = Configuration.getWaypointSortMode();
		Node centerPos = Trace.getInstance().center;
		Vector source = wayPtTile.listWayPt();
		Vector sorted = new Vector(source.size());
		PositionMark insert;
		PositionMark compare;
		for (int i = 0; i < source.size(); i++) {
			if (favorites) {
				PositionMark waypt = (PositionMark)source.elementAt(i);
				if (! waypt.displayName.endsWith("*") ) {
					continue;
				}

			}
			// We want to insert source[i] at the right position into sorted
			insert = (PositionMark)source.elementAt(i);
			float distInsert = ProjMath.getDistance(centerPos.radlat, centerPos.radlon,
					insert.lat, insert.lon);
			int j = 0;
			while (j < sorted.size()) {
				compare = (PositionMark)sorted.elementAt(j);
				if (sortmode == Configuration.WAYPT_SORT_MODE_NEW_FIRST) {
    				// Newest waypoints first
    				if (insert.timeMillis > compare.timeMillis) {
    					break;
    				}
				} else if (sortmode == Configuration.WAYPT_SORT_MODE_OLD_FIRST) {
    				// Oldest waypoints first
    				if (insert.timeMillis <= compare.timeMillis) {
    					break;
    				}
				} else if (sortmode == Configuration.WAYPT_SORT_MODE_ALPHABET) {
    				// Alphabetically
    				if (insert.displayName.compareTo(compare.displayName) < 0) {
    					break;
    				}
				} else if (sortmode == Configuration.WAYPT_SORT_MODE_DISTANCE) {
    				// By distance from map center
					float distCompare = ProjMath.getDistance(
							centerPos.radlat, centerPos.radlon, compare.lat, compare.lon);
    				if (distInsert <= distCompare) {
    					break;
    				}
				}
				j++;
			}
			sorted.insertElementAt(insert, j);
		}
		return sorted;
		//return wayPts;
	}
	
	/** Returns the number of waypoints.
	 * 
	 * @return Number of waypoints
	 */
	public int getNumberWaypoints() {
		int noWpt = wayPtTile.getNumberWaypoints();
		//#debug debug
		logger.debug("WaypointsTile returns: No of WP = " + noWpt);
		return noWpt;
	}
	
	/** Returns whether waypoints are being loaded
	 * @return True if still loading
	 */
	public boolean isLoadingWaypoints() {
		return (mJobState == JOB_RELOAD_WPTS);
	}
	
	/**
	 * Read tracks from the RecordStore to display the names in the list on screen.
	 */
	public PersistEntity[] listTrks() {
		PersistEntity[] trks;
		byte [] record = new byte[16000];
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(record));
		try {
			openTrackDatabase();
			logger.info("GPX database has " + trackDatabase.getNumRecords()
					+ " entries and a size of " + trackDatabase.getSize());
			trks = new PersistEntity[trackDatabase.getNumRecords()];
			
			RecordEnumeration p = trackDatabase.enumerateRecords(null, null, false);
			logger.info("Enumerating tracks: " + p.numRecords());
			int i = 0;
			boolean bExceptionOccurred = false;
			while (p.hasNextElement()) {
				int idx = p.nextRecordId();
				PersistEntity trk = new PersistEntity();

				try {
					while (trackDatabase.getRecordSize(idx) > record.length) {
						record = new byte[record.length + 16000];
						dis = new DataInputStream(new ByteArrayInputStream(record));
					}

					trackDatabase.getRecord(idx, record, 0);
					dis.reset();
					
					String trackName = dis.readUTF();
					int noTrackPoints = dis.readInt();
					logger.debug("Found track " + trackName + " with " + noTrackPoints + " TrkPoints");
					trk.displayName = trackName + " (" + noTrackPoints + ")";
					trk.setTrackSize(noTrackPoints);
				} catch (RecordStoreFullException e) {
					trk.displayName = Locale.get("gpx.ErrorRecordStoreFullException")/*Error (RecordStoreFullException)*/;
					logger.error(Locale.get("gpx.RecordStoreFullList")/*Record Store is full, can not load list */ + 
							i + Locale.get("gpx.WithIndex")/* with index */ + idx + ":" + e.getMessage());
				} catch (RecordStoreNotFoundException e) {
					trk.displayName = Locale.get("gpx.ErrorRecordStoreNotFoundException")/*Error (RecordStoreNotFoundException)*/;
					logger.error(Locale.get("gpx.RecordStoreNotFoundList")/*Record Store not found, can not load list */ + 
							i + Locale.get("gpx.WithIndex")/* with index */ + idx + ": " + e.getMessage());
				} catch (RecordStoreException e) {
					trk.displayName = Locale.get("gpx.ErrorRecordStoreException")/*Error (RecordStoreException)*/;
					logger.error(Locale.get("gpx.RecordStoreExceptionTrack")/*Record Store exception, can not load track */ + 
							i + Locale.get("gpx.WithIndex")/* with index */ + idx + ": " + e.getMessage());
					logger.error( e.toString());
				} catch (Exception e) {
					System.out.println("Exception: " + e.toString());
					bExceptionOccurred = true;
				}
				trk.id = idx;
				trks[i++] = trk;
			}
			logger.info("Enumerated tracks");
			trackDatabase.closeRecordStore();
			trackDatabase = null;
			
			if (bExceptionOccurred) {
				logger.error(Locale.get("gpx.ExceptionReadingTracks")/*At least one of...*/);
			}
			
			return trks;
		} catch (RecordStoreFullException e) {
			logger.error(Locale.get("gpx.RecordStoreFullList2")/*Record Store is full, can not load list: */ + e.getMessage());
		} catch (RecordStoreNotFoundException e) {
			logger.error(Locale.get("gpx.RecordStoreNotFoundList2")/*Record Store not found, can not load list: */ + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error(Locale.get("gpx.RecordStoreExceptionList2")/*Record Store exception, can not load list: */ + e.getMessage());
		}
		return null;
	}
	
	/** Called when an out of memory condition is detected.
	 * Will stop track recording and save the track in the RecordStore.
	 */
	public void dropCache() {
		trackTile.dropTrk();
		// Can't drop waypoints. The only way to get them back is to restart ShareNav.
		// So if we do this we might as well quit the app altogether.
		// Plus it shocks the user as he will think they were deleted.
		//wayPtTile.dropWayPt();
		System.gc();
		if (isRecordingTrk()) {
			saveTrk(true);
		}
	}

	/** Currently without function.
	 * TODO: Should it do anything?
	 */
	public boolean cleanup(int level) {
		return false;
	}

	/** Currently without function.
	 * TODO: Should it do anything?
	 */
	public void walk(PaintContext pc, int opt) {
	}
	public int getNameIdx(float lat, float lon, short type) {
		// only interesting for SingleTile	
		return -1;
	}

	/**
	 * Renders the waypoints, the recording track and the loaded tracks on the screen.
	 */
	public void paint(PaintContext pc, byte layer) {
		// Rendering the tracks in reverse order...
		// Loaded tracks on the bottom,
		loadedTracksTile.paint(pc, layer);
		// then the other layers on top.
		trackTile.paint(pc, layer);
		wayPtTile.paint(pc, layer);
	}
	
	/** Returns how many actual track points have been recorded so far (segments are not counted for this user-visible string).
	 * 
	 * @return Number of track points
	 */
	public int getTrkPointCount() {
		return mTrkRecorded - mTrkSegments;
	}
	
	/** Returns whether a track is currently recorded.
	 * 
	 * @return True if recording is on, false if not.
	 */
	public boolean isRecordingTrk() {
		return (mTrkOutStream != null);
	}
	
	/** Returns whether track recording is currently suspended
	 * 
	 * @return True if recording is suspended, false if not.
	 */
	public boolean isRecordingTrkSuspended() {
		return trkRecordingSuspended;
	}
	
	/**
	 * @return current GPX track length in m
	 */
	public float currentTrkLength() {
		return trkOdo;
	}
	
	/**
	 * @return current GPX track's average speed in m/s
	 */
	public float currentTrkAvgSpd() {
		return (1000.0f*trkOdo) / trkTimeTot;
	}
	
	/**
	 * @return current GPX tracks time duration in ms
	 */
	public long currentTrkDuration() {
		return trkTimeTot;
	}
	
	/**
	 * @return current GPX track's maximum speed in m/s
	 */
	public float maxTrkSpeed() {
		return trkVmax;
	}
	
	/**
	 * @return current GPX track's exponentially averaged vertical speed in m/s
	 */
	public float deltaAltTrkSpeed() {
		return trkVertSpd;
	}
	
	/** Start processor thread with the specified job.
	 * @param job Job to perform, use JOB_XX for this.
	 */
	private void startProcessorThread(int job) {
		if (mJobState == JOB_IDLE) {
    		if (job > JOB_IDLE && job <= JOB_SAVE_TRK) {
    			mJobState = job;
    			processorThread = new Thread(this, "GpxProcessor");
    			processorThread.setPriority(Thread.MIN_PRIORITY);
    			processorThread.start();
    		} else {
    			logger.error(Locale.get("gpx.BadParameterJob")/*Bad parameter job*/ + "=" + job + Locale.get("gpx.OfGpxStartProcessorThread")/* of Gpx.startProcessorThread*/ + "()");
    		}
		} else {
			logger.error("Gpx.startProcessorThread(): " + Locale.get("gpx.NotIdleCantStart")/*Not idle, can not start processorThread!*/);
			// TODO: Should pass this on (return true/false) to let the user know.
		}
	}
	
	/** Processor thread which will perform one of several jobs.
	 * Which job is determined by mJobState.
	 */
	public void run() {
		logger.info("GPX processing thread started");
		try {
			boolean success = false;
			if (mJobState == JOB_RELOAD_WPTS) {
				loadWaypointsFromDatabase();
				if (feedbackListener != null) {
					feedbackListener.uploadAborted();
					feedbackListener = null;
				}
			} else if (mJobState == JOB_EXPORT_TRKS) {
				// Export GPX tracks
				for (int i = 0; i < exportTracks.size(); i++) {
					currentTrk = (PersistEntity)exportTracks.elementAt(i);
					if (feedbackListener != null) {
						feedbackListener.updateProgress(Locale.get("gpx.Exporting")/*Exporting */ + currentTrk.displayName + "\n");
					}
					success = sendGpx();
					if (success == false) {
						logger.error(Locale.get("gpx.FailedToExportTrack")/*Failed to export track */ + currentTrk);
					}
				}
			} else if (mJobState == JOB_EXPORT_WPTS) {
				// Export GPX waypoints
				success = sendGpx();
			} else if (mJobState == JOB_IMPORT_GPX) {
				// Import GPX
				if (mImportStream != null) {
					success = doReceiveGpx();
					mImportStream = null;
				} else {
					logger.error(Locale.get("gpx.GPXImportRequestedButInputStream")/*GPX import requested but InputStream was null*/);
				}
			} else if (mJobState == JOB_DELETE_TRKS) {
				success = doDeleteTracks();
			} else if (mJobState == JOB_DELETE_WPTS) {
				success = doDeleteWayPts();
				loadWaypointsFromDatabase();
			} else if (mJobState == JOB_SAVE_TRK) {
				success = doSaveTrk();
			} else {
				logger.error(Locale.get("gpx.DidNotKnowWhatToDoWithGpx")/*Did not know what to do in*/+ " Gpx.run()");
				importExportMessage = Locale.get("gpx.DidNotKnowWhatToDo")/*Did not know what to do.*/;
				success = false;
			}
			// Must be called *before* changing the job state, else somebody might
			// trigger (in the context of this thread btw) another action = thread
			// before this one has completely finished.
			if (feedbackListener != null) {
				feedbackListener.completedUpload(success, importExportMessage);
				feedbackListener = null;
			}
		} catch (Exception e) {
			logger.exception(Locale.get("gpx.AnErrorOccuredDuringGPXJob")/*An error occured during GPX job*/, e);
		} catch (OutOfMemoryError oome) {
			Trace.getInstance().dropCache();
			logger.error(Locale.get("gpx.OOMDuringGPXjobTryingToRecover")/*Out of memory during GPX job, trying to recover*/);
		}
		mJobState = JOB_IDLE;
	}

	/** Convenience method to open the waypoint RecordStore.
	 */
	private void openWayPtDatabase() {
		try {
			if (wayptDatabase == null)
			{
				wayptDatabase = RecordStore.openRecordStore("waypoints", true);
			}
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.RecordstoreFullOpenWP")/*Recordstore full while trying to open waypoints*/, e);
		} catch (RecordStoreNotFoundException e) {
			logger.exception(Locale.get("gpx.WaypointRecordstoreNotFound")/*Waypoint recordstore not found*/, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.RecordStoreExceptionOpeningWaypoints")/*RecordStoreException opening waypoints*/, e);
		} catch (OutOfMemoryError oome) {
			logger.error(Locale.get("gpx.OOMOpeningWaypoints")/*Out of memory opening waypoints*/);
		}
	}

	/**
	 * Read waypoints from the RecordStore and put them in the wayPtTile for displaying.
	 */
	private void loadWaypointsFromDatabase() {
		try {
			wayPtTile.dropWayPt();
			RecordEnumeration renum;
			
			logger.info("Loading waypoints into tile");
			openWayPtDatabase();
			renum = wayptDatabase.enumerateRecords(null, null, false);
			while (renum.hasNextElement()) {
				int id;
				id = renum.nextRecordId();
				PositionMark waypt = new PositionMark(id, wayptDatabase.getRecord(id));
				wayPtTile.addWayPt(waypt);
			}
			wayptDatabase.closeRecordStore();
			wayptDatabase = null;
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.RecordStoreExceptionLoadingWaypoints")/*RecordStoreException loading waypoints*/, e);
		}  catch (OutOfMemoryError oome) {
			logger.error(Locale.get("gpx.OOMLoadingWaypoints")/*Out of memory loading waypoints*/);
		}
	}

	/** Convenience method to open the tracks RecordStore.
	 */
	private void openTrackDatabase() {
		try {
			if (trackDatabase == null) {
				logger.info("Opening track database");
				trackDatabase = RecordStore.openRecordStore("tracks", true);
			}
		} catch (RecordStoreFullException e) {
			logger.exception(Locale.get("gpx.RecordstoreFullOpenTracks")/*Recordstore is full while trying to open tracks*/, e);
		} catch (RecordStoreNotFoundException e) {
			logger.exception(Locale.get("gpx.TracksRecordstoreNotFound")/*Tracks recordstore not found*/, e);
		} catch (RecordStoreException e) {
			logger.exception(Locale.get("gpx.RecordStoreExceptionOpeningTracks")/*RecordStoreException opening tracks*/, e);
		} catch (OutOfMemoryError oome) {
			logger.error(Locale.get("gpx.OOMOpeningTracks")/*Out of memory opening tracks*/);
		}
	}

	/** Actually writes the track points of the current track in GPX format.
	 * 
	 * @param oS The stream to which the track points are written
	 */
	private void streamTracks(OutputStream oS) throws IOException,
			RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		float lat, lon;
		short ele;
		long time;
//		Date date = new Date();
		
		openTrackDatabase();
		DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(
				trackDatabase.getRecord(currentTrk.id)));
		trackName = dis1.readUTF();
		mTrkRecorded = dis1.readInt();
		mTrkSegments = 0;
		int trackSize = dis1.readInt();
		byte[] trackArray = new byte[trackSize];
		dis1.read(trackArray);
		DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
		oS.write("<trk>\r\n<trkseg>\r\n".getBytes());
		StringBuffer sb = new StringBuffer(128);
		
		// Calculate interval for progressbar update - progressbar is updated in 2% steps
		int progUpdtIntervall = 1;
		if (mTrkRecorded >= 50) {
			progUpdtIntervall = mTrkRecorded / 50;
		}
			
		for (int i = 1; i <= mTrkRecorded; i++) {
			lat = trackIS.readFloat(); lon = trackIS.readFloat();
			ele = trackIS.readShort();
			time = trackIS.readLong();
			// Read extra bytes in the buffer, that are currently not written to the GPX file.
			// Will add these at a later time.
			trackIS.readByte(); //Speed
			if (time == Long.MIN_VALUE) {
				oS.write("</trkseg>\r\n".getBytes());
				oS.write("<trkseg>\r\n".getBytes());
			} else if (time == Long.MIN_VALUE + 1) {
				PositionMark waypt = null;
				try {
					int id;
					id = ele;

					openWayPtDatabase();
					waypt = new PositionMark(id, wayptDatabase.getRecord(id));
					wayptDatabase.closeRecordStore();
					wayptDatabase = null;
					if (waypt != null) {
						// TODO: check if this copes with the case when
						// waypoint has been removed before converting
						// track to GPX
						// Stream waypoint to a separate bytearray to write it out
						// at the end of the track.
						streamWayPt(baos, waypt);
					}
				} catch (RecordStoreException e) {
					logger.info("RecordStoreException (" + e.getMessage() + ") loading track embeded waypoint. Has it been deleted?");
				} catch (OutOfMemoryError oome) {
					logger.error(Locale.get("gpx.OOMLoadingWaypoints")/*Out of memory loading waypoints*/);
				}
			} else {
				sb.setLength(0);
				sb.append("<trkpt lat='").append(lat).append("' lon='").append(lon).append("'>\r\n");
				sb.append("<ele>").append(ele).append("</ele>\r\n");
//				date.setTime(time);
//				sb.append("<time>").append(formatUTC(date)).append("</time>\r\n");
				sb.append("<time>").append(DateTimeTools.getUTCDateTime(time)).append("</time>\r\n");
				//System.out.println(DateTimeTools.getUTCDateTime(time) + " / " + formatUTC(date));
				sb.append("</trkpt>\r\n");
				writeUTF(oS, sb);
			}
			/**
			 * Increment the progress bar when progress has increased 2%
			 * Don't update on every point as an optimisation.
			 */
			if (((i % progUpdtIntervall) == 0) && (feedbackListener != null)) {
				// Update the progress bar in GuiGpx
				feedbackListener.updateProgressValue(progUpdtIntervall);
			}
		} // for
		oS.write("</trkseg>\r\n</trk>\r\n".getBytes());
		oS.write(baos.toByteArray());
		trackDatabase.closeRecordStore();
		trackDatabase = null;
		/** Update the progress bar by the remaining part */
		feedbackListener.updateProgressValue(mTrkRecorded % progUpdtIntervall);
	}
	
	/** Writes a string in UTF-8
	 * 
	 * @param oS The stream to which to write the string
	 * @param sb StringBuffer with the string
	 */
	private void writeUTF(OutputStream oS, StringBuffer sb) {
		try {
			oS.write(sb.toString().getBytes(Configuration.getUtf8Encoding()));
		} catch (IOException e) {
			logger.exception(Locale.get("gpx.IOExceptionInWriteUTF")/*IOException in writeUTF*/ + "()", e);
		}
	}
	
	/** Writes one waypoint in GPX format.
	 * 
	 * @param oS The stream to which the waypoints are written
	 * @param wayPt The waypoint to write
	 */
	private void streamWayPt (OutputStream oS, PositionMark wayPt)
				throws IOException {
		StringBuffer sb = new StringBuffer(128);
		//if (wayPt.lat > 90) wayPt.lat = wayPt.lat * MoreMath.FAC_DECTORAD * MoreMath.FAC_DECTORAD;
		//if (wayPt.lon > 180) wayPt.lon = wayPt.lon * MoreMath.FAC_DECTORAD * MoreMath.FAC_DECTORAD;
		sb.append("<wpt lat='").append(wayPt.lat * MoreMath.FAC_RADTODEC)
		  .append("' lon='").append(wayPt.lon * MoreMath.FAC_RADTODEC).append("'>\r\n")
		  .append("<name>").append(HelperRoutines.utf2xml(wayPt.displayName)).append("</name>\r\n");
		if (wayPt.ele != PositionMark.INVALID_ELEVATION)
		{
			sb.append("<ele>").append(wayPt.ele).append("</ele>\r\n");
		}
		// TODO: explain When will timeMillis of a wayPt be 0 ?
		if (wayPt.timeMillis != 0)
		{
			//dateStreamWayPt.setTime(wayPt.timeMillis);
			//sb.append("<time>").append(formatUTC(dateStreamWayPt)).append("</time>\r\n");
			sb.append("<time>").append(DateTimeTools.getUTCDateTime(wayPt.timeMillis)).append("</time>\r\n");
		}
		// fix and sats are not filled yet so we don't export them either.
		// sym and type are not exported yet but they could be mapped to strings.
		sb.append("</wpt>\r\n");
		writeUTF(oS, sb);
	}

	/** Actually writes the waypoints from wayPtTile in GPX format.
	 * 
	 * @param oS The stream to which the waypoints are written
	 */
	private void streamWayPts (OutputStream oS) throws IOException {
		Vector waypts = listWayPoints();
		PositionMark wayPt = null;
		
		for (int i = 0; i < waypts.size(); i++) {
			wayPt = (PositionMark)waypts.elementAt(i);
			streamWayPt(oS, wayPt);
		}
	}
	
	/** This method exports a track or waypoints in GPX format.
	 * 
	 * @return True if successful, false if not. Details can be found in the string importExportMessage.
	 */
	private boolean sendGpx() {
		try {
			String name = null;
			
			logger.trace("Starting to send a GPX file, about to open a connection to" + url);
			
			if (mJobState == JOB_EXPORT_TRKS)
			{
				name = Configuration.getValidFileName(currentTrk.displayName);
			}
			else if (mJobState == JOB_EXPORT_WPTS)
			{
				if (waypointsSaveFileName == null) {
					importExportMessage = Locale.get("gpx.NoWilenameWpSendingAborted")/*No filename, way points sending aborted*/;
					return false;
				}
				name = Configuration.getValidFileName(waypointsSaveFileName);
			}
			
			if (url == null) {
				importExportMessage = Locale.get("gpx.NoGPXreceiver")/*No GPX receiver specified. Please select a GPX receiver in the setup menu*/;
				return false;
			}
			
			OutputStream outStream = null;
			ExportSession session = null;
			try {
				/**
				 * We jump through hoops here (Class.forName) in order to decouple
				 * the implementation of JSRs. The problem is, that not all phones have all
				 * of the JSRs, and if we simply called the Class directly, it would
				 * cause the whole app to crash. With this method, we can hopefully catch
				 * the missing JSRs and gracefully report an error to the user that the operation
				 * is not available on this phone.
				 */
				/**
				 * The Class.forName and the instantiation of the class must be separate
				 * statements, as otherwise this confuses the proguard obfuscator when
				 * rewriting the flattened renamed classes.
				 */
				Class tmp = null;
				if (url.startsWith("file:")) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.FileExportSession");
				} else if (url.startsWith("comm:")) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.CommExportSession");
				} else if (url.startsWith("btgoep:")) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.ObexExportSession");
				} else if (url.startsWith("http:")) {
					tmp = Class.forName("net.sharenav.midlet.sharenav.ui.GuiGpxOsmUpload");
				}
				if (tmp != null) {
					logger.info("Got class: " + tmp);
					Object objTmp = tmp.newInstance();
					if (objTmp instanceof ExportSession) {
						session = (ExportSession)(objTmp);
					} else {
						logger.info("objTmp: " + objTmp + "is not part of " + ExportSession.class.getName());
					}
				}
			} catch (ClassNotFoundException cnfe) {
				importExportMessage = Locale.get("gpx.UnsuportedExport")/*Your phone does not support this form of exporting, please choose a different one*/;
				session = null;
				return false;
			} catch (ClassCastException cce) {
				logger.exception(Locale.get("gpx.CouldNotCastTheClass")/*Could not cast the class*/, cce);				
			}
			if (session == null) {
				importExportMessage = Locale.get("gpx.UnsuportedExport")/*Your phone does not support this form of exporting, please choose a different one*/;
				return false;
			}
			outStream = session.openSession(url, name);
			if (outStream == null) {
				importExportMessage = Locale.get("gpx.CouldNotObtainValidConn")/*Could not obtain a valid connection to*/ + " " + url;
				return false;
			}
			outStream.write("<?xml version='1.0' encoding='UTF-8'?>\r\n".getBytes());
			outStream.write("<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n".getBytes());
			
			if (mJobState == JOB_EXPORT_WPTS)
			{
				streamWayPts(outStream);
			}
			else if (mJobState == JOB_EXPORT_TRKS)
			{
				streamTracks(outStream);
			}
			outStream.write("</gpx>\r\n".getBytes());

			outStream.flush();
			outStream.close();
			session.closeSession();
			importExportMessage = Locale.get("gpx.success")/*success*/;
			return true;
		} catch (IOException e) {			
			logger.error(Locale.get("gpx.IOExceptionCantTransmitTracklog")/*IOException, can not transmit tracklog: */ + e);
			importExportMessage = e.getMessage();
		} catch (OutOfMemoryError oome) {
			importExportMessage = Locale.get("gpx.OOMCantTransmitTracklog")/*Out of memory, can not transmit tracklog*/;
			logger.fatal(importExportMessage);
		} catch (Exception ee) {			
			logger.error(Locale.get("gpx.ErrorWhileSendingTracklogs")/*Error while sending tracklogs: */ + ee);
			importExportMessage = ee.getMessage();
		}
		return false;
	}
	
	/** Reads GPX data from mInputStream.
	 * 
	 * @return True if reading was successful, false if not
	 */
	private boolean doReceiveGpx() {
		try {
			// Determine parser that can be used on this system
			boolean success;
			String jsr172Version = null;
			Class parserClass;
			Object parserObject;
			GpxParser parser;
			GpxImportHandler importHandler = new GpxImportHandler(maxDistance, this, feedbackListener);
			try {
				jsr172Version = System.getProperty("xml.jaxp.subset.version");
			} catch (Exception e) {
				/**
				 * Some phones throw exceptions if trying to access properties that don't
				 * exist, so we have to catch these and just ignore them.
				 */
			}
			if ((jsr172Version != null) &&  (jsr172Version.length() > 0)) {
				logger.info("Using builtin jsr 172 XML parser");
				parserClass = Class.forName("net.sharenav.sharenav.importexport.Jsr172GpxParser");
			} else {
				logger.info("Using QDXMLParser");
				parserClass = Class.forName("net.sharenav.sharenav.importexport.QDGpxParser");
			}
			parserObject = parserClass.newInstance();
			parser = (GpxParser) parserObject;
			
			// Trigger parsing of GPX data
			// As addTrkPt() is used for importing tracks, recording rules have
			// to be disabled temporarily.
			applyRecordingRules = false;
			success = parser.parse(mImportStream, importHandler);
			applyRecordingRules = true;
			
			mImportStream.close();
			importExportMessage = importHandler.getMessage();
			return success;
		} catch (ClassNotFoundException cnfe) {
			importExportMessage = Locale.get("gpx.NoXMLParseSupport")/*Your phone does not support XML parsing*/;
		} catch (Exception e) {
			importExportMessage = Locale.get("gpx.ImportGPXError")/*Something went wrong while importing GPX*/ + ": " + e;
		}
		return false;
	}
	
//	/**
//	 * Formats an integer to 2 digits, as used for example in time.
//	 * I.e. 3 gets printed as 03.
//	 **/
//	private static final String formatInt2(int n) {
//		if (n < 10) {
//			return "0" + n;
//		} else {
//			return Integer.toString(n);
//		}
//	}
//
//	/**
//	 * Date-Time formatter that corresponds to the standard UTC time as used in XML
//	 * @param time Time to be formatted
//	 * @return String containing the formatted time
//	 */
//	private static final String formatUTC(Date time) {
//		// TODO: This function needs optimising. It has a too high object churn.
//		Calendar c = null;
//		if (c == null) {
//			c = Calendar.getInstance();
//		}
//		c.setTime(time);
//		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" +
//		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
//		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";
//
//	}

	/** Called when the user has entered a name.
	 * Will continue with the actual operation, i.e. saving the track or the waypoints.
	 * @param strResult The name entered
	 */
	public void inputCompleted(String strResult) {
		if (mEnteringGpxNameStart || mEnteringGpxNameStop) {
			trackName = strResult;
			if (mEnteringGpxNameStart) {
				mEnteringGpxNameStart = false;
				if (trackName != null) {
					origTrackName = new String(trackName);
				} else {
					// cancel start of recording
					// old behaviour: cancel rename: trackName = new String(origTrackName);
					try {
						Trace tr = Trace.getInstance();
						//#debug debug
						logger.debug("Closing track with " + mTrkRecorded + " points due to user cancel");
						mTrkOutStream.flush();
						mTrkOutStream.close();
						mTrkByteOutStream.close();
						tr.alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.Cancelled")/*Cancelled*/, 1250);
					} catch (IOException e) {
						logger.exception(Locale.get("gpx.FailedClosingTrackrecording")/*Failed to close trackrecording*/, e);
					}
					mTrkOutStream = null;
					mTrkByteOutStream = null;
					trackTile.dropTrk();
				}
			}
			if (mEnteringGpxNameStop) {
				mEnteringGpxNameStop = false;
				if (trackName == null) {
					// old behaviour: cancel rename: trackName = origTrackName;
					Trace tr = Trace.getInstance();
					// don't stop recording
					tr.alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.Continuing")/*Continuing*/, 1250);
					trackName = new String(origTrackName);

				} else {
					startProcessorThread(JOB_SAVE_TRK);
				}
			}
			Trace.getInstance().show();
			return;
		}
	}
}

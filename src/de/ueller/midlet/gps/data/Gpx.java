package de.ueller.midlet.gps.data;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Math;
import java.util.Calendar;
import java.util.Date;
import javax.microedition.io.CommConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#if polish.api.pdaapi
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
//#if polish.api.btapi
//#if polish.api.obex
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
//#endif
//#endif
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
//#if polish.api.webservice
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;
//#else
import uk.co.wilson.xml.MinML2;
//#endif
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gpsMid.mapData.GpxTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.ImageCollector;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.tile.PaintContext;

public class Gpx extends Tile implements Runnable {
	private float maxDistance;
	private int importedWpts;
	private int tooFarWpts;
	private int duplicateWpts;
	
	// statics for user-defined rules for record trackpoint
	private static long oldMsTime;
	private static float oldlat;
	private static float oldlon;
	
	//#if polish.api.webservice
	class GpxParserW extends DefaultHandler {
	//#if polish.api.thisjustisnttrue 
	//this is just to keep an editor happy that doesn't know the preprocessor	
	}
	//#endif
	//#else
	class GpxParser extends MinML2 {
	//#endif	
		PositionMark wayPt;
		Position p = new Position(0,0,0,0,0,0,new Date());
		boolean name = false;
		boolean ele = false;
		boolean time = false;
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
			if (qName.equalsIgnoreCase("wpt")) {
				/**
				 * Gpx files have coordinates in degrees. We store them internally as radians.
				 * So we need to convert these.
				 */
				float node_lat = Float.parseFloat(atts.getValue("lat"))*MoreMath.FAC_DECTORAD;
				float node_lon = Float.parseFloat(atts.getValue("lon"))*MoreMath.FAC_DECTORAD;
				float distance=0;
				
				boolean inRadius=true;
				if (maxDistance!=0) {
					distance = ProjMath.getDistance(node_lat, node_lon, ImageCollector.mapCenter.radlat, ImageCollector.mapCenter.radlon); 
					distance=(int)(distance/100.0f)/10.0f;
					if (distance>maxDistance) {
						inRadius=false;
						tooFarWpts++;
					}
				}
				//System.out.println("MaxDist: " + maxDistance + " Distance: " + distance + " inRadius: " + inRadius);
				if (inRadius) {
					wayPt = new PositionMark(node_lat,node_lon);
				}
			} else if (qName.equalsIgnoreCase("name")) {
				name = true;				
			} else if (qName.equalsIgnoreCase("trk")) {
				newTrk();
			} else if (qName.equalsIgnoreCase("trkseg")) {
				
			} else if (qName.equalsIgnoreCase("trkpt")) {
				/**
				 * Positions seem to be handeled in degree rather than radians
				 * as all the other coordinates.
				 * Be careful with the conversions!
				 */
				p.latitude = Float.parseFloat(atts.getValue("lat"));
				p.longitude = Float.parseFloat(atts.getValue("lon"));
				p.altitude = 0;
				p.course = 0;
				p.speed = 0;				
			} else if (qName.equalsIgnoreCase("ele")) {
				ele = true;
			} else if (qName.equalsIgnoreCase("time")) {
				time = true;
			}
		}
		public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equalsIgnoreCase("wpt")) {
				if (wayPt != null) {
					logger.info("Received waypoint: " + wayPt);
					if (!existsWayPt(wayPt)) {
						addWayPt(wayPt);
						importedWpts++;
					}
					else {
						duplicateWpts++;
					}
					wayPt = null;
				}
								
			} else if (qName.equalsIgnoreCase("name")) {
				name = false;
			} else if (qName.equalsIgnoreCase("trk")) {
				saveTrk();				
			} else if (qName.equalsIgnoreCase("trkseg")) {
				
			} else if (qName.equalsIgnoreCase("trkpt")) {
				addTrkPt(p);
			} else if (qName.equalsIgnoreCase("ele")) {
				ele = false;
			} else if (qName.equalsIgnoreCase("time")) {
				time = false;
			}
		}
		public void startDocument() {
			logger.debug("Started parsing XML document");
		}
		public void endDocument() {
			logger.debug("Finished parsing XML document");
		}
		public void characters(char[] ch, int start, int length) {
			if (wayPt != null) {
				if (name) {
					if (wayPt.displayName == null) {
						wayPt.displayName = new String(ch,start,length);
					} else {
						wayPt.displayName += new String(ch,start,length);
					}
				}
			} else if (p != null) {
				if (ele) {
					p.altitude = Float.parseFloat(new String(ch,start,length));
				} else if (time) {					
				}				
			}
		}
	}	
	
	private final static Logger logger=Logger.getInstance(Gpx.class,Logger.DEBUG);
	
	private RecordStore trackDatabase;
	private RecordStore wayptDatabase;
	public int recorded=0;
	public int delay = 0;
	
	
	private Thread processorThread=null;
	private String url=null;
	
	private boolean sendWpt;
	private boolean sendTrk;
	private boolean reloadWpt;
	
	private boolean applyRecordingRules = true;
	
	private String trackName;
	private PersistEntity currentTrk;
	
	private UploadListener feedbackListener;
	
	/**
	 * Variables used for transmitting GPX data:
	 */
	
	private InputStream in;
	
	private ByteArrayOutputStream baos;
	private DataOutputStream dos;
	private Connection session = null;
	
	//#if polish.api.btapi
	//#if polish.api.obex
	private Operation operation = null;
	//#endif
	//#endif
	
	private GpxTile tile;
	
	public Gpx() {
		tile = new GpxTile();		
		loadTrksFromDatabase();
		reloadWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();		
	}
	
	public void displayWaypoints(boolean displayWpt) {
		
	}
	
	public void displayTrk(PersistEntity trk) {
		if (trk == null) {
			//TODO:
		} else {
			try {
			tile.dropTrk();
			DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(trk.id)));
			trackName = dis1.readUTF();
			recorded = dis1.readInt();
			int trackSize = dis1.readInt();
			byte[] trackArray = new byte[trackSize];
			dis1.read(trackArray);
			DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
			for (int i = 0; i < recorded; i++) {
				tile.addTrkPt(trackIS.readFloat(), trackIS.readFloat(), false);
				trackIS.readShort(); //altitude
				trackIS.readLong();	//Time			
				trackIS.readByte(); //Speed				
			}
			dis1.close();
			dis1 = null;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RecordStoreNotOpenException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidRecordIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RecordStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void addWayPt(PositionMark waypt) {
		byte[] buf = waypt.toByte();
		try {
			int id = wayptDatabase.addRecord(buf, 0, buf.length);
			waypt.id = id;
		} catch (RecordStoreNotOpenException e) {			
			logger.exception("Exception storing waypoint (database not open)", e);
		} catch (RecordStoreFullException e) {
			logger.exception("Record store is full, could not store waypoint", e);			
		} catch (RecordStoreException e) {
			logger.exception("Exception storing waypoint", e);
		}
		tile.addWayPt(waypt);		
	}
	
	private boolean existsWayPt(PositionMark newWayPt) {
		if (tile != null) {
			return tile.existsWayPt(newWayPt);
		}
		return false;		
	}
		
	
	public void addTrkPt(Position trkpt) {
		Configuration config=GpsMid.getInstance().getConfig();
		logger.info("Adding trackpoint: " + trkpt);
		boolean doRecord=false;
		try {
			// always record when i.e. receiving or loading tracklogs
			// or when starting to record
			if (!applyRecordingRules || recorded==0) {
				doRecord=true;
			}
			// check which record rules to apply
			else if (config.getGpxRecordRuleMode()==Configuration.GPX_RECORD_ADAPTIVE) {
				/** adaptive recording
				 * 
				 * When saving tracklogs and adaptive recording is enabled,
				 * we reduce the frequency of saved samples if the speed drops
				 * to less than a certain amount. This should increase storage
				 * efficiency if one doesn't need if one doesn't need to repeatedly
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
					doRecord=true;
					delay = 0;
				} else {
					delay++;
				}			
			} else {
				/*
				 user-specified recording rules
				*/
				long msTime=System.currentTimeMillis();
				float lat=trkpt.latitude*MoreMath.FAC_DECTORAD;
				float lon=trkpt.longitude*MoreMath.FAC_DECTORAD;
				float distance = 100*ProjMath.getDistance(lat, lon, oldlat, oldlon); 
				if ( 
						// is not always record distance not set
						// or always record distance reached
						(
							config.getGpxRecordAlwaysDistanceCentimeters()!=0 &&
							distance >= config.getGpxRecordAlwaysDistanceCentimeters()
						)
						||
						(  
							(
								// is minimum time interval not set
								// or interval at least minimum interval?
								config.getGpxRecordMinMilliseconds() == 0 ||
								Math.abs(msTime-oldMsTime) >= config.getGpxRecordMinMilliseconds()
							)
							&&
							(
							// is minimum distance not set
							// or distance at least minimum distance?
							config.getGpxRecordMinDistanceCentimeters()==0 ||
							distance >= config.getGpxRecordMinDistanceCentimeters()
							)
						)
				) {
					doRecord=true;
					oldMsTime=msTime;
					oldlat=lat;
					oldlon=lon;
				}
			}
			if(doRecord) {
				dos.writeFloat(trkpt.latitude);
				dos.writeFloat(trkpt.longitude);
				dos.writeShort((short)trkpt.altitude);
				dos.writeLong(trkpt.date.getTime());
				dos.writeByte((byte)(trkpt.speed*3.6f)); //Convert to km/h				
				recorded++;
				tile.addTrkPt(trkpt.latitude, trkpt.longitude, false);
			}
		} catch (OutOfMemoryError oome) {
			try {				
				Trace.getInstance().dropCache();
				logger.info("Was out of memory, but we might have recovered");
			}catch (OutOfMemoryError oome2) {
				logger.fatal("Out of memory, can't add trackpoint");				
			}			
		} catch (IOException e) {
			logger.exception("Could not add trackpoint", e);
		}
	}
	
	public void deleteWayPt(PositionMark waypt) {
		deleteWayPt(waypt, null);
	}
	public void deleteWayPt(PositionMark waypt, UploadListener ul) {
		this.feedbackListener = ul;
		try {
			wayptDatabase.deleteRecord(waypt.id);
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reloadWayPts() {
		if (processorThread != null && processorThread.isAlive()) {
			/* Already reloading, nothing to do */
			return;
		}
		reloadWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void newTrk() {
		logger.debug("Starting a new track recording");
		tile.dropTrk();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		
		//Construct a track name from the current time
		StringBuffer trkName = new StringBuffer();
		trkName.append(cal.get(Calendar.YEAR)).append("-").append(formatInt2(cal.get(Calendar.MONTH) + 1));
		trkName.append("-").append(formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_");
		trkName.append(formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(formatInt2(cal.get(Calendar.MINUTE)));
		trackName = trkName	.toString();
		
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		recorded = 0;		
	}
	
	public void saveTrk() {
		try {
			logger.debug("Finishing track with " + recorded + " points");
			if (dos == null) {
				logger.debug("Not currently recording, so can't save the track");
				return;
			}
			dos.flush();			
			ByteArrayOutputStream baosDb = new ByteArrayOutputStream();
			DataOutputStream dosDb = new DataOutputStream(baosDb);
			dosDb.writeUTF(trackName);
			dosDb.writeInt(recorded);
			dosDb.writeInt(baos.size());
			dosDb.write(baos.toByteArray());
			dosDb.flush();
			trackDatabase.addRecord(baosDb.toByteArray(), 0, baosDb.size());
			dos.close();
			dos = null;
			baos = null;
			tile.dropTrk();
		} catch (IOException e) {
			logger.error("IOE: " + e.getMessage());
		} catch (RecordStoreNotOpenException e) {
			logger.error("RSNOE: " + e.getMessage());
		} catch (RecordStoreFullException e) {
			logger.error("RSFE: " + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("RSE: " + e.getMessage());
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't save tracklog");			
		}
		
	}
	
	public void deleteTrk(PersistEntity trk) {
		try {
			trackDatabase.deleteRecord(trk.id);
			tile.dropTrk();
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void receiveGpx(InputStream in, UploadListener ul, float maxDistance) {
		this.maxDistance=maxDistance;
		this.in = in;
		this.feedbackListener = ul;
		importedWpts=0;
		tooFarWpts=0;
		duplicateWpts=0;
		if (in == null) {
			logger.error("Could not open input stream to gpx file");
		}
		if ((processorThread != null) && (processorThread.isAlive())) {
			logger.error("Still processing another gpx file");
		}
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void sendTrk(String url, UploadListener ul, PersistEntity trk) {
		logger.debug("Sending " + trk + " to " + url);
		feedbackListener = ul;
		this.url = url;
		sendTrk = true;
		tile.dropTrk();
		currentTrk = trk;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void sendWayPt(String url, UploadListener ul) {
		this.url = url;
		feedbackListener = ul;
		sendWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public PositionMark [] listWayPt() {
		return tile.listWayPt();
	}
	
	public PersistEntity[] listTrks() {
		PersistEntity[] trks;
		byte [] record = new byte[16000];		
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(record));		
		try {
			if (trackDatabase == null)
				trackDatabase = RecordStore.openRecordStore("tracks", false);
			logger.info("GPX database has " +trackDatabase.getNumRecords() + " entries and a size of " + trackDatabase.getSize());
			trks = new PersistEntity[trackDatabase.getNumRecords()];
			
			RecordEnumeration p = trackDatabase.enumerateRecords(null, null, false);			
			logger.info("Enumerating tracks: " + p.numRecords());
			int i = 0;
			while (p.hasNextElement()) {
				int idx = p.nextRecordId();
				while (trackDatabase.getRecordSize(idx) > record.length) {
					record = new byte[record.length + 16000];
					dis = new DataInputStream(new ByteArrayInputStream(record));
				}
				trackDatabase.getRecord(idx, record, 0);
				dis.reset();
				
				String trackName = dis.readUTF();
				int noTrackPoints = dis.readInt();
				logger.debug("Found track " + trackName + " with " + noTrackPoints + "TrkPoints");
				PersistEntity trk = new PersistEntity();
				trk.id = idx;
				trk.displayName = trackName + " (" + noTrackPoints + ")";
				trks[i++] = trk;
			}
			logger.info("Enumerated tracks");
			return trks;
		} catch (RecordStoreFullException e) {
			logger.error("Record Store is full, can't load list" + e.getMessage());
		} catch (RecordStoreNotFoundException e) {
			logger.error("Record Store not found, can't load list" + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("Record Store exception, can't load list" + e.getMessage());
		} catch (IOException e) {
			logger.error("IO exception, can't load list" + e.getMessage());
		}
		return null;
	}
	
	public void dropCache() {
		tile.dropTrk();
		tile.dropWayPt();
		System.gc();
		if (isRecordingTrk())
			saveTrk();		
	}
	
	public boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public void paint(PaintContext pc, byte layer) {
		tile.paint(pc, layer);
		
	}
	
	public boolean isRecordingTrk() {
		return (dos != null);
	}
	
	public void run() {
		boolean success = false;
		if (reloadWpt) {			
			try {
				/**
				 * Sleep for a while to limit the number of reloads happening
				 */
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loadWaypointsFromDatabase();
			reloadWpt = false;
			if (feedbackListener != null) {
				feedbackListener.uploadAborted();
				feedbackListener = null;
			}
			return;
		} else if (sendTrk || sendWpt) {
			success = sendGpx();
		} else if (in != null) {
			success = receiveGpx();
		} else {
			logger.error("Did not know whether to send or receive");
		}
		if (feedbackListener != null) {
			StringBuffer sb = new StringBuffer();
			// create statistics only for import 
			if (in != null) {
				if(maxDistance!=0) {
					sb.append("\n(max. distance: " + maxDistance + " km)");
				}
				sb.append("\n\n" + importedWpts + " waypoints imported");
				if(tooFarWpts!=0 || duplicateWpts!=0) {
					sb.append("\n\nSkipped waypoints:");
					if(maxDistance!=0) {
						sb.append("\n" + tooFarWpts + " too far away");
					}
					if(duplicateWpts!=0) {
						sb.append("\n" + duplicateWpts + " already existing");				
					}
				}
			}
			feedbackListener.completedUpload(success, sb.toString());
		}
		feedbackListener = null;
		sendTrk = false;
		sendWpt = false;
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void loadWaypointsFromDatabase() {		
		try {
			tile.dropWayPt();
			RecordEnumeration renum;
			
			logger.info("Loading waypoints into tile");
			wayptDatabase = RecordStore.openRecordStore("waypoints", true);			
			renum = wayptDatabase.enumerateRecords(null, null, false);			
			while (renum.hasNextElement()) {
				int id;			
				id = renum.nextRecordId();			
				PositionMark waypt = new PositionMark(id,wayptDatabase.getRecord(id));
				tile.addWayPt(waypt);						
			}
		} catch (RecordStoreFullException e) {
			logger.error("Recordstore is full while trying to open waypoints");
		} catch (RecordStoreNotFoundException e) {
			logger.error("Waypoints recordstore not found");
		} catch (RecordStoreException e) {
			logger.exception("RecordStoreException", e);
		}  catch (OutOfMemoryError oome) {
			logger.error("Out of memory loading waypoints");
		}
	}
	
	private void loadTrksFromDatabase() {
		try {			
			logger.info("Opening track database");
			trackDatabase = RecordStore.openRecordStore("tracks", true);			
		} catch (RecordStoreFullException e) {
			logger.error("Recordstore is full while trying to open waypoints");
		} catch (RecordStoreNotFoundException e) {
			logger.error("Waypoints recordstore not found");
		} catch (RecordStoreException e) {
			logger.exception("RecordStoreException", e);
		} catch (OutOfMemoryError oome) {
			logger.error("Out of memory opening Tracks");
		}
	}
	
	
	/**
	 * The following routines are used to output a gpx file
	 */
	
	private OutputStream obtainFileSession(String url, String name) {		
		OutputStream oS = null;
		//#if polish.api.pdaapi
		try {
			url += name + ".gpx";
			logger.info("Opening file " + url);
			session = Connector.open(url);
			FileConnection fileCon = (FileConnection) session;
			if (fileCon == null)
				throw new IOException("Couldn't open url " + url);
			if (!fileCon.exists())
				fileCon.create();
			
			oS = fileCon.openOutputStream();
		} catch (IOException e) {
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#endif
		return oS;		
	}
	
	private OutputStream obtainCommSession(String url) {		
		OutputStream oS = null;		
		try {			
			session = Connector.open(url);			
			CommConnection commCon = (CommConnection) session;			
			oS = commCon.openOutputStream();			
		} catch (IOException e) {
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}		
		return oS;		
	}
	
	private OutputStream obtainBluetoothObexSession(String url, String name) {		
		OutputStream oS = null;
		//#if polish.api.btapi
		//#if polish.api.obex
		try {			
			session = Connector.open(url);
			ClientSession csession = (ClientSession) session; 
			HeaderSet headers = csession.createHeaderSet();	        
			csession.connect(headers);
			logger.debug("Connected");
			headers.setHeader(HeaderSet.NAME, name + ".gpx");
			headers.setHeader(HeaderSet.TYPE, "text");
			
			operation = csession.put(headers);			
			oS = operation.openOutputStream();
		} catch (IOException e) {
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#else
		logger.fatal("This version does not support OBEX over bluetooth, so we can't send files");
		//#endif
		//#endif
		return oS;		
	}
	
	private void closeBluetoothObexSession() {
		//#if polish.api.btapi
		//#if polish.api.obex
		try {
			session.close();
			int code = operation.getResponseCode();
			if (code == ResponseCodes.OBEX_HTTP_OK) {				
				logger.info("Successfully transfered file");				
			} else {
				logger.error("Unsuccessful return code in Opex push: " + code);
			}
		} catch (IOException e) {
			logger.error("Failed to close connection after transmitting GPX");
			e.printStackTrace();
		}
		//#endif
		//#endif
	}
	
	private void closeFileSession() {
		try {
			session.close();			
		} catch (IOException e) {
			logger.error("Failed to close connection after storing to file");
			e.printStackTrace();
		}
	}
	
	private void streamTracks (OutputStream oS) throws IOException, RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException{		
		DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(currentTrk.id)));
		trackName = dis1.readUTF();
		recorded = dis1.readInt();
		int trackSize = dis1.readInt();
		byte[] trackArray = new byte[trackSize];
		dis1.read(trackArray);
		DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
				
		oS.write("<trk>\r\n<trkseg>\r\n".getBytes());						
		
		for (int i = 1; i <= recorded; i++) {
			StringBuffer sb = new StringBuffer(128);
			sb.append("<trkpt lat='").append(trackIS.readFloat()).append("' lon='").append(trackIS.readFloat()).append("' >\r\n");
			sb.append("<ele>").append(trackIS.readShort()).append("</ele>\r\n");
			sb.append("<time>").append(formatUTC(new Date(trackIS.readLong()))).append("</time>\r\n");
			sb.append("</trkpt>\r\n");				
			// Read extra bytes in the buffer, that are currently not written to the GPX file.
			// Will add these at a later time.
			trackIS.readByte(); //Speed			
			oS.write(sb.toString().getBytes());
		}
		oS.write("</trkseg>\r\n</trk>\r\n".getBytes());
		
	}
	
	private void streamWayPts (OutputStream oS) throws IOException{		
		PositionMark[] waypts = tile.listWayPt();
		PositionMark wayPt = null;
		
		for (int i = 0; i < waypts.length; i++) {
			wayPt = waypts[i];			
			StringBuffer sb = new StringBuffer(128);
			sb.append("<wpt lat='").append(wayPt.lat*MoreMath.FAC_RADTODEC).append("' lon='").append(wayPt.lon*MoreMath.FAC_RADTODEC).append("' >\r\n");
			sb.append("<name>").append(wayPt.displayName).append("</name>\r\n");
			sb.append("</wpt>\r\n");
			oS.write(sb.toString().getBytes());
		}
	}
	
	private boolean sendGpx() {
		try {
			String name = null;
			
			logger.trace("Starting to send a GPX file, about to open a connection to" + url);
			
			if (sendTrk) {
				name = currentTrk.displayName;
			} else if (sendWpt)
				name = "Waypoints";
			
			if (url == null) {
				logger.error("No GPX receiver specified. Please select a GPX receiver in the setup menu");
				return false;
			}
			
			OutputStream oS = null;
			if (url.startsWith("file:")) {
				oS = obtainFileSession(url, name);
			} else if (url.startsWith("comm:")) {
				oS = obtainCommSession(url);
			} else {
				oS = obtainBluetoothObexSession(url, name);
			}
			if (oS == null) {
				logger.error("Could not obtain a valid connection to " + url);
				return false;
			}
			oS.write("<?xml version='1.0' encoding='UTF-8'?>\r\n".getBytes());
			oS.write("<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n".getBytes());
			
			if (sendWpt)
				streamWayPts(oS);
			if (sendTrk)
				streamTracks(oS);
			
			oS.write("</gpx>\r\n\r\n".getBytes());
						
			oS.flush();
			oS.close();
			if (url.startsWith("file:")) {
				closeFileSession();
			} else if (url.startsWith("comm:")) {
				closeFileSession();			
			} else{			
				closeBluetoothObexSession();
			}			
			return true;
		} catch (IOException e) {			
			logger.error("IOE:" + e);	
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't transmit tracklogs");
		} catch (Exception ee) {			
			logger.error("Error while sending tracklogs: " + ee);
		}
		return false;
	}
	
	private boolean receiveGpx() {
		//#if polish.api.webservice
		SAXParserFactory factory = SAXParserFactory.newInstance();
		// Parse the input
        SAXParser saxParser;
        //#endif
		try {
			applyRecordingRules = false;
			//#if polish.api.webservice
			GpxParserW parserw = new GpxParserW();			
			saxParser = factory.newSAXParser();						
			saxParser.parse( in, parserw);
			//#else
			GpxParser parser = new GpxParser();
			parser.parse(new InputStreamReader(in));
			//#endif
			applyRecordingRules = true;
			in.close();
			return true;
		}
		//#if polish.api.webservice
		catch (ParserConfigurationException e) {
			logger.error("XML Parser configuration error: " + e.getMessage());
			e.printStackTrace();
		}
		//#endif
		catch (SAXException e) {
			logger.error("Invalid XML, parsing error: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("IO error, could not read the data: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00. 
	 **/
	private static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}
			
	}	
	/**
	 * Date-Time formater that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	private static final String formatUTC(Date time) {
		// This function needs optimising. It has a too high object churn.
		Calendar c = null;
		if (c == null)
			c = Calendar.getInstance();
		c.setTime(time);
		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" +
		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";		 
		
	}


	public void walk(PaintContext pc, int opt) {
		// TODO Auto-generated method stub
		
	}
}

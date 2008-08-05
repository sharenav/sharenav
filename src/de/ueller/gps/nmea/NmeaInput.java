/**
 * SirfDecoder
 * 
 * takes an InputStream and interpret layer 3 and layer 4. Than make
 * callbacks to the receiver witch ahas to implement SirfMsgReceiver 
 *
 * @autor Harald Mueller james22 at users dot sourceforge dot net
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.gps.nmea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;



public class NmeaInput implements Runnable, LocationMsgProducer{

	protected static final Logger logger = Logger.getInstance(NmeaInput.class,Logger.TRACE);
	private NmeaMessage smsg;
	private InputStream ins;
	private OutputStream rawDataLogger;
	private Thread					processorThread;
	private LocationMsgReceiver	receiver;
	private boolean closed=false;
	private byte connectQuality=100;
	int bytesReceived=0;
	private int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	private String message;
	
	private byte [] buf1 = new byte[512]; //Buffer used to read data from GPS-receiver
	private byte [] buf2 = new byte[128]; //Buffer used to recombine data into NMEA sentences
	
	public void init(InputStream ins,LocationMsgReceiver receiver) {
		this.ins = ins;
		this.receiver = receiver;
		processorThread = new Thread(this,"NMEA Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();
		smsg=new NmeaMessage(receiver);
		//logger.error("Starting NMEA");		
	}

	public void run(){
		receiver.receiveMessage("Start NMEA");
		// eat the buffer content
		try {
		try {
			byte [] buf = new byte[512]; 
			while (ins.available() > 0){
				int recieved = ins.read(buf);
				bytesReceived += recieved;
				if (rawDataLogger != null) {
					rawDataLogger.write(buf, 0, recieved);					
					rawDataLogger.flush();					
				}
			}
			receiver.receiveMessage("Erasing " + bytesReceived +" bytes");
			bytesReceived=100;			
		} catch (IOException e1) {
			receiver.receiveMessage("Closing: " + e1.getMessage());
			close("Closing: " + e1.getMessage());
		}
		
		byte timeCounter=41;
		while (!closed){
			timeCounter++;
			if (timeCounter > 40){
				timeCounter = 0;
				if(connectQuality > 100) {
					connectQuality=100;
				}
				if(connectQuality < 0) {
					connectQuality=0;
				}
				receiver.receiveStatistics(connectError,connectQuality);
//				watchdog if no bytes received in 10 sec then exit thread
				if (bytesReceived == 0){
					close("No Data from NMEA");
				} else {
					bytesReceived=0;
				}
			}
			
			process();
			if (! closed)
			try {
				synchronized (this) {
					connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
					wait(250);
				}				
			} catch (InterruptedException e) {
				//Nothing to do in this case
			}
			
		}
		if (message == null){
			receiver.locationDecoderEnd();
		} else {
			receiver.locationDecoderEnd(message);
		}
		} catch (OutOfMemoryError oome) {
			logger.fatal("NmeaInput thread crashed as out of memory: " + oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			logger.fatal("NmeaInput thread crashed unexpectedly: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.ueller.gps.nmea.LocationMsgProducer#close()
	 */
	public synchronized void close() {
		disableRawLogging();
		closed=true;
	}
	public synchronized void close(String message) {
		disableRawLogging();
		closed=true;
		this.message=message;
	}
	
	public void process(){		
		//position markers in buf1 and buf2, length of useful data in buf1
		int p1 = 0, p2 = 0, len1 = 0;
		char c;
		//Start marker in buf1 from where to copy
		int start1 = 0; 
		//Indicate if the start and or the end of a NMEA sentence has been read
		boolean found_start = false, found_end = false; 
		//t1 = System.currentTimeMillis();
		
		try {
			
			while ((ins.available() > 0 || p1 > 0) && ! closed) {				
				if (p1 == 0) {					
					len1 = ins.read(buf1);
					bytesReceived += len1;
					if (rawDataLogger != null) {
						rawDataLogger.write(buf1, 0, len1);						
						rawDataLogger.flush();						
					}
				}
				
				//if we have already seen the start of a sentence, copy buf1 from the start
				if (found_start) start1 = 0; 
				
				//Scan through buf1 to check for the beginning and end of a sentence
				while (p1 < len1 && !found_end) {
					c = (char)buf1[p1];
					switch (c){					
					case '\n':
						if (found_start) found_end = true;							
						break;
					case '$':
						start1 = p1;
						found_start = true;						
						break;
					}
					p1++;
				}				
				//If we haven't seen the start of the sentence, this data is useless so discard it
				if (!found_start) {
					p1 = 0; p2 = 0; found_end = false;
					continue; 
				}
				//Check to make sure we don't overrun the buffer
				if (p2 + p1 - start1 > 128) {
					System.out.println("Error: NMEA string was longer than 128 char, but max should be 82");
					p1 = 0; p2 = 0; found_start = false; found_end = false;
					continue;
				}								
				System.arraycopy(buf1, start1, buf2, p2, p1 - start1);				
				p2 += p1 - start1;				
				if (p1 == len1) p1 = 0; //consumed all of buf1, begin at the start again
				if (!found_end)	continue;
				//We have a complete NMEA sentence in buf2, so we can now decode it.
				//First check the checksum and ignore incorrect data
				if (isChecksumCorrect(buf2, p2)) {
					//Throw away the first 3 characters ($GP) and the last 5 (checksum and \r\n)
					String nmea_sentence = new String(buf2,3,p2-8);					
					smsg.decodeMessage(nmea_sentence);
				}
				
				//Reset buf2 for the next sentence
				p2 = 0; found_start = false; found_end = false;				
			} 
		} catch (IOException e) {
			receiver.receiveMessage("Closing: " + e.getMessage());
			close("Closed: " + e.getMessage());
		}
	}
	
	/**
	 * Calculate the NMEA checksum. This is a byte wise XOR of the NMEA string between (excluding)
	 * the $ and the * 
	 * @param buf the entire NMEA sentence including $ and \r\n
	 * @param len
	 * @return if the checksum is correctly computed
	 */
	private boolean isChecksumCorrect(byte[] buf, int len) {
		if (len < 7)
			return false;
		byte checksum = buf[1]; //ignore the first character, that is the $ sign
		for (int i = 2; i < len - 5; i++) { // ignore the * checksum and \r\n			
			checksum = (byte)(checksum ^ buf[i]);
		}
		byte targetChecksum;
		try {
			targetChecksum = (Integer.valueOf(new String(buf,len-4,2), 16).byteValue());
		} catch (NumberFormatException nfe) {
			return false;
		}
		return (targetChecksum == checksum);
	}
	
	public void enableRawLogging(OutputStream os) {
		rawDataLogger = os;		
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception("Couldn't close raw gps logger", e);
			}
			rawDataLogger = null;
		}
	}

	public void addLocationMsgReceiver(LocationMsgReceiver receiver) {
		// TODO Auto-generated method stub
		
	}
}

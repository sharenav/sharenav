/**
 * SirfDecoder
 * 
 * takes an InputStream and interpret layer 3 and layer 4. Than make
 * callbacks to the receiver witch ahas to implement SirfMsgReceiver 
 *
 * @version $Revision$$ ($Name$)
 * @autor Harald Mueller james22 at users dot sourceforge dot net
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.gps.nmea;

import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;



public class NmeaInput implements Runnable, LocationMsgProducer{

	protected static final Logger logger = Logger.getInstance(NmeaInput.class,Logger.TRACE);
	private int	start;
	private int	checksum;
	private NmeaMessage smsg;
	private InputStream ins;
	private Thread					processorThread;
	private final LocationMsgReceiver	receiver;
	private boolean closed=false;
	private byte connectQuality=100;
	int bytesReceived=0;
	private int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	private String message;
	private static final int STATE_EXPECT_START_1=0;
	private static final int STATE_EXPECT_PREFIX=1;
	private static final int STATE_EXPECT_BODY=2;
	private static final int STATE_EXPECT_CHECKSUM=3;
	
	private byte [] buf1 = new byte[512]; //Buffer used to read data from GPS-receiver
	private byte [] buf2 = new byte[128]; //Buffer used to recombine data into NMEA sentences
	
	public NmeaInput(InputStream ins,LocationMsgReceiver receiver) {
		super();
		this.ins = ins;
		this.receiver = receiver;
		processorThread = new Thread(this,"NMEA Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();
		smsg=new NmeaMessage(receiver);

	}
	public NmeaInput(boolean test,InputStream ins,LocationMsgReceiver receiver) {
		super();
		this.ins = ins;
		this.receiver = receiver;
		smsg=new NmeaMessage(receiver);

	}

	public void run(){
		receiver.receiveMessage("start NMEA");
		// eat the buffe content
		try {
		try {
			byte [] buf = new byte[512]; 
			while (ins.available() > 0){
				bytesReceived += ins.read(buf);				
			}
			receiver.receiveMessage("erase " + bytesReceived +" bytes");
			bytesReceived=100;			
		} catch (IOException e1) {
			receiver.receiveMessage("closing " + e1.getMessage());
			close("closing " + e1.getMessage());
		}
		
		byte timeCounter=21;
		while (!closed){
			timeCounter++;
			if (timeCounter > 20){
				timeCounter = 0;
				if(connectQuality > 100) {
					connectQuality=100;
				}
				if(connectQuality < 0) {
					connectQuality=0;
				}
				receiver.receiveStatistics(connectError,connectQuality);
//				watchdog if no bytes received in 5 sec then exit thread
				if (bytesReceived == 0){
					close("no Data form NMEA");
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
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			logger.fatal("NmeaInput thread crashed unexpectadly: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.ueller.gps.nmea.LocationMsgProducer#close()
	 */
	public synchronized void close() {
		closed=true;
	}
	public synchronized void close(String message) {
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
				//Throw away the first 3 characters ($GP) and the last 7 (checksum and \r\n)
				String nmea_sentence = new String(buf2,3,p2-7);
				smsg.decodeMessage(nmea_sentence);
				
				//Reset buf2 for the next sentence
				p2 = 0; found_start = false; found_end = false;				
			} 
		} catch (IOException e) {
			receiver.receiveMessage("closing " + e.getMessage());
			close("closed " + e.getMessage());
		}
	}
	
	public void processOld() {
		StringBuffer readBuffer = smsg.getBuffer();
		try {
			while (ins.available() > 0) {
			    switch (start) {
				case STATE_EXPECT_START_1:
					char c = (char)ins.read();
//					System.out.println("got char:" + c);
					if (c == '$') {
							start = STATE_EXPECT_PREFIX;
						} else {
							connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN1]++;
							connectQuality--;
							break;
						}
				case STATE_EXPECT_PREFIX:
					if (ins.available() < 2) // expect GP
						break;
					if (ins.read() != 'G'){
						start=STATE_EXPECT_START_1;
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN2]++;
						connectQuality--;
						break;
					}
					if (ins.read() != 'P'){
						start=0;
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN2]++;
						connectQuality--;
						break;
					}
					// buffer prepare for body
					readBuffer.setLength(0);
				case STATE_EXPECT_BODY:
					if (readBody(readBuffer)){
						break;
					}
				case STATE_EXPECT_CHECKSUM: // checksum
					if (ins.available() < 4) {
						break;
					}
					checksum = Character.digit((char) ins.read(),16);
					checksum = checksum * 16 + Character.digit((char) ins.read(),16);
//					System.out.println("checksum "+checksum+" "+calcChecksum(smsg));
					if (ins.read() != '\r'){
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_END_SIGN1]++;						
						start=STATE_EXPECT_START_1;
						break;
					}
					if (ins.read() != '\n'){
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_END_SIGN2]++;						
						connectQuality--;
					}
					smsg.decodeMessage();
					start=STATE_EXPECT_START_1;
					break;
				} // switch
			} // while
		} catch (IOException e) {
			receiver.receiveMessage("Fehler: " + e.getMessage());
			close();
		}

	}

	/**
	 * read body til end or til checksum
	 * @param readBuffer
	 * @return true if not found * for checksum
	 * @throws IOException
	 */
	private boolean readBody(StringBuffer readBuffer) throws IOException{
		while (ins.available() > 0){
			int b=ins.read();
			switch (b){
			case '$':
				start=STATE_EXPECT_PREFIX;
				connectQuality--;
				System.out.println("Error got $ in Messagebody");
			case '\r':
				connectQuality++;
				break;
			case '\n':
				connectQuality++;
				start=0;
				try {
					smsg.decodeMessage();
					connectQuality++;
				} catch (RuntimeException e) {
					receiver.receiveMessage(e.toString());
				}
				break;
			case '*':
				start=3;
				connectQuality++;
				return false;
			default:
				readBuffer.append((char) b);
			if (readBuffer.length() > 80){
				start=0;
				connectQuality--;
				connectError[LocationMsgReceiver.SIRF_FAIL_MSG_TO_LONG]++;
				return true;
			}
			}
		}
		return true;

	}
	
	public long calcChecksum(NmeaMessage s) {
		long mesChecksum;
		mesChecksum = '$'+'G'+'P';
		for (int i = 0; i < s.buffer.length(); i++) {
			mesChecksum += s.buffer.charAt(i);
		}
		mesChecksum &= 255;
		return mesChecksum;
	}
}

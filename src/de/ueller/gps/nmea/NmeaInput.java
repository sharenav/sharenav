/**
 * SirfDecoder
 * 
 * takes an InputStream and interpret layer 3 and layer 4. Than make
 * callbacks to the receiver witch ahas to implement SirfMsgReceiver 
 *
 * @version $Revision$$ ($Name$)
 * @autor Harald Mueller james22 at users dot sourceforge dot net
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.gps.nmea;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.ueller.midlet.gps.LocationMsgReceiver;



public class NmeaInput implements Runnable{

	
	private int	start;
	private int	checksum;
	private int	length;
	private NmeaMessage smsg;
	private InputStream ins;
	private Thread					processorThread;
	private final LocationMsgReceiver	receiver;
	private boolean closed=false;
	private byte connectQuality=100;
	private int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	private static final int STATE_EXPECT_START_1=0;
	private static final int STATE_EXPECT_PREFIX=1;
	private static final int STATE_EXPECT_BODY=2;
	private static final int STATE_EXPECT_CHECKSUM=3;
	
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
		receiver.receiveMessage("start Tread");
		byte timeCounter=21;
		while (!closed){
			timeCounter++;
			if (timeCounter > 4){
				timeCounter = 0;
				if(connectQuality > 100) {
					connectQuality=100;
				}
				if(connectQuality < 0) {
					connectQuality=0;
				}
				receiver.receiveStatistics(connectError,connectQuality);
			}
			
			process();
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
		receiver.sirfDecoderEnd();
	}
	
	public synchronized void close() {
		closed=true;
	}
	
	public void process(){
		StringBuffer readBuffer = smsg.getBuffer();
		char c;
		try {
			while (ins.available() > 0) {
				c=(char)ins.read();
				switch (c){
				case '\r':
					break;
				case '\n':
					smsg.decodeMessage();
					break;
				case '$':
					readBuffer.setLength(0);
					ins.read();
					ins.read();
					break;
				default:
					readBuffer.append(c);
				}
			} 
		} catch (Exception e) {
			// TODO: handle exception
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

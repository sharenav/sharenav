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
package de.ueller.gps.sirf;

import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;



public class SirfInput implements Runnable, LocationMsgProducer{

	
	private int	start;
	private int	checksum;
	private int	length;
	private SirfMessage smsg;
	private InputStream ins;
	private Thread					processorThread;
	private final LocationMsgReceiver	receiver;
	private boolean closed=false;
	private byte connectQuality=100;
	//#debug info
	private int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	private int msgsReceived;
	//#debug error
	private final static Logger logger = Logger.getInstance(SirfInput.class,Logger.DEBUG);

	
	public SirfInput(InputStream ins,LocationMsgReceiver receiver) {
		super();
		//#debug
		logger.debug("init SirfInput");
		this.ins = ins;
		this.receiver = receiver;
		processorThread = new Thread(this,"Sirf Decoder");
		processorThread.setPriority(7);
		processorThread.start();
		smsg=new SirfMessage(receiver);

	}

	public void run(){
		receiver.receiveMessage("start SIRF receiver");
		//#debug debug
		logger.debug("start SIRF receiver");
		try {
			receiver.receiveMessage("eat up " + ins.available() + "bytes");
			//#debug debug
			logger.debug("addr of ins:" + ins);
			while (ins.available() > 1022)
				ins.read(smsg.readBuffer,0,1023);
			while (ins.available() > 0)
				ins.read();
		} catch (IOException e1) {
			//#debug error
			logger.error(e1.getMessage());
			receiver.receiveMessage("closing " + e1.getMessage());
			closed=true;
		}
		msgsReceived=1;
		byte timeCounter=21;
		while (!closed){
			//#debug debug
			logger.debug("addr of ins:" + ins);
			timeCounter++;
			if (timeCounter > 4){
				timeCounter = 0;
				if(connectQuality > 100) {
					connectQuality=100;
				}
				if(connectQuality < 0) {
					connectQuality=0;
				}
				//#debug info
				receiver.receiveStatistics(connectError,connectQuality);
//				watchdog if no bytes received in 5 sec then exit thread
				if (msgsReceived == 0){
					closed=true;
					receiver.receiveMessage("SIRF timeout no msg");
				} else {
					msgsReceived=0;
				}

			}
			process();
			try {
				synchronized (this) {
					wait(250);
				}
				
			} catch (InterruptedException e) {
				//#debug
				logger.debug(e.getMessage());
			}
			
		}
		receiver.locationDecoderEnd();
	}
	
	public synchronized void close() {
		closed=true;
	}
	public synchronized void close(String msg) {
		closed=true;
		
	}
	
	public void process() {
		byte[] readBuffer = smsg.readBuffer;
		try {
			//#debug debug
			logger.debug("loop avail :" + ins.available() );
			while (ins.available() > 0) {
				long mesChecksum;
				switch (start) {
				case 0:
					// leave on messages begin if close is requested
					if (closed) {
						return;
					}
					if (ins.available() < 2) {
						break;
					}
					if (ins.read() == 0xA0) {
						if (ins.read() == 0xA2) {
							start = 2;
							msgsReceived++;
						} else {
							//#debug info
							connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN2]++;
							connectQuality--;
							break;
						}
					} else {
						//#debug info
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN1]++;
						connectQuality--;
						break;
					}
				case 2:
					if (ins.available() < 2) {
						break;
					}
					length = ins.read() * 256;
					length += ins.read();
					if (length >= 1023) {
						//#debug info
						connectError[LocationMsgReceiver.SIRF_FAIL_MSG_TO_LONG]++;
						connectQuality--;
						start = 0;
						break;
					}
					start = 3;
					smsg.length = length;
				case 3:
					if (ins.available() <= length) {
						break;
					}
					if (ins.read(readBuffer, 0, length) != length) {
						//#debug info
						connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
						connectQuality-=2;
						start = 0;
						break;
					}
					start = 4;
				case 4:
					if (ins.available() < 2) {
						break;
					}
					checksum = ins.read();
					checksum = checksum * 256 + ins.read();
					mesChecksum = calcChecksum(smsg);
					if (mesChecksum != checksum) {
						connectQuality-=10;
						//#debug info
						connectError[LocationMsgReceiver.SIRF_FAIL_MSG_CHECKSUM_ERROR]++;
						start = 0;
					}
					start = 5;
				case 5:
					if (ins.available() < 2) {
						break;
					}
					if (ins.read() == 0xB0) {
						if (ins.read() == 0xB3) {
							connectQuality++;
							smsg.decodeMsg(smsg);
							start = 0;
						} else {
							start = 0;
							connectQuality--;
							//#debug info
							connectError[LocationMsgReceiver.SIRF_FAIL_NO_END_SIGN2]++;
						}
					} else {
						start = 0;
						connectQuality--;
						//#debug info
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_END_SIGN1]++;
					}
					break;
				} // switch
			} // while
		} catch (IOException e) {
			receiver.receiveMessage("Fehler: " + e.getMessage());
			close();
		}

	}
	
	public long calcChecksum(SirfMessage s) {
		long mesChecksum;
		mesChecksum = 0;
		for (int i = 0; i < s.length; i++) {
			mesChecksum += s.getByte(i);
		}
		mesChecksum &= 32767l;
		return mesChecksum;
	}
}

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
package net.sharenav.gps.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.Logger;



public class SirfInput extends BtReceiverInput {

	private final static Logger logger = Logger.getInstance(SirfInput.class,Logger.DEBUG);
	private int	start;
	private int	checksum;
	private int	length;
	private SirfMessage smsg;
	private int msgsReceived;

	public boolean init(LocationMsgReceiver receiver) {
		//#debug
		logger.debug("Starting Sirf Decoder");
		smsg=new SirfMessage(this.receiverList);
		return super.init(receiver);
	}

	/*	public void run(){
		receiver.receiveMessage("start SIRF receiver");
		//#debug debug
		logger.debug("start SIRF receiver");
//		try {
//			receiver.receiveMessage("eat up " + btGpsInputStream.available() + "bytes");
//			//#debug debug
//			logger.debug("addr of btGpsInputStream:" + btGpsInputStream);
//			while (btGpsInputStream.available() > 1022)
//				btGpsInputStream.read(smsg.readBuffer,0,1023);
//			while (btGpsInputStream.available() > 0)
//				btGpsInputStream.read();
//		} catch (IOException e1) {
//			//#debug error
//			logger.error(e1.getMessage());
//			receiver.receiveMessage("closing " + e1.getMessage());
//			closed=true;
//		}
		msgsReceived=1;
		byte timeCounter=21;
		while (!closed){
			//#debug debug
			logger.debug("addr of btGpsInputStream:" + btGpsInputStream);
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
	 */	
	public void process() throws IOException {
		byte[] readBuffer = smsg.readBuffer;
		//#debug debug
		logger.debug("loop avail :" + btGpsInputStream.available() );
		while (btGpsInputStream.available() > 0) {
			long mesChecksum;
			switch (start) {
			case 0:
				// leave on messages begin if close is requested
				if (closed) {
					return;
				}
				if (btGpsInputStream.available() < 2) {
					break;
				}
				if (btGpsInputStream.read() == 0xA0) {
					if (btGpsInputStream.read() == 0xA2) {
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
				if (btGpsInputStream.available() < 2) {
					break;
				}
				length = btGpsInputStream.read() * 256;
				length += btGpsInputStream.read();
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
				if (btGpsInputStream.available() <= length) {
					break;
				}
				if (btGpsInputStream.read(readBuffer, 0, length) != length) {
					//#debug info
					connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
					connectQuality-=2;
					start = 0;
					break;
				}
				start = 4;
			case 4:
				if (btGpsInputStream.available() < 2) {
					break;
				}
				checksum = btGpsInputStream.read();
				checksum = checksum * 256 + btGpsInputStream.read();
				mesChecksum = calcChecksum(smsg);
				if (mesChecksum != checksum) {
					connectQuality-=10;
					//#debug info
					connectError[LocationMsgReceiver.SIRF_FAIL_MSG_CHECKSUM_ERROR]++;
					start = 0;
				}
				start = 5;
			case 5:
				if (btGpsInputStream.available() < 2) {
					break;
				}
				if (btGpsInputStream.read() == 0xB0) {
					if (btGpsInputStream.read() == 0xB3) {
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
		//This is put here temporarilly while merging SirfInput and NmeaInput
		if (msgsReceived > 0) bytesReceived = 100; 

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

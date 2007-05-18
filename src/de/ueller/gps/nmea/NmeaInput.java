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
	
	public NmeaInput(InputStream ins,LocationMsgReceiver receiver) {
		super();
		this.ins = ins;
		this.receiver = receiver;
		processorThread = new Thread(this,"Sirf Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();
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
	
	public void process() {
		StringBuffer readBuffer = smsg.getBuffer();
		try {
			err: while (ins.available() > 0) {
				switch (start) {
				case 0:
					if (ins.read() == '$') {
							start = 1;
						} else {
							connectError[LocationMsgReceiver.SIRF_FAIL_NO_START_SIGN1]++;
							connectQuality--;
							break;
						}
				case 1:
					if (ins.available() < 6) // expect GP
						break;
					if (ins.read() != 'G'){
						start=0;
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
				case 2:
					while (ins.available() > 0){
						int b=ins.read();
						if (b != '\n'){
							readBuffer.append((char) b);
						} else if (b == '*'){ 
							start=3;
							break; // while
						} else {
							start=1;
							smsg.decodeMessage();
						}
					}
					if (start != 3){
						break;
					}
				case 3: // checksum
					if (ins.available() < 2) {
						break;
					}
					checksum = ins.read()-'A';
					checksum = checksum * 256 + (ins.read()-'A');
					System.out.println(checksum);
					start=4;
				case 4:
					if (ins.available() == 0) {
						break;
					}
					if (ins.read() != '\n'){
						connectError[LocationMsgReceiver.SIRF_FAIL_NO_END_SIGN1]++;
					} else {
						smsg.decodeMessage();
						start=1;
					}
				} // switch
			} // while
		} catch (IOException e) {
			receiver.receiveMessage("Fehler: " + e.getMessage());
			close();
		}

	}
	
	public long calcChecksum(NmeaMessage s) {
		long mesChecksum;
		mesChecksum = 0;
		for (int i = 0; i < s.buffer.length(); i++) {
			mesChecksum += s.buffer.charAt(i);
		}
		mesChecksum &= 32767l;
		return mesChecksum;
	}
}

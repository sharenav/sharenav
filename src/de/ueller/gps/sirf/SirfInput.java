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


public class SirfInput implements Runnable{

	
	private int	start;
	private StringBuffer	ausgabe;
	private int	checksum;
	private int	length;
	private SirfMessage smsg;
	private InputStream ins;
	private Thread					processorThread;
	private boolean closed=false;
	
	public SirfInput(InputStream ins,SirfMsgReceiver receiver) {
		super();
		this.ins = ins;
		processorThread = new Thread(this,"Sirf Decoder");
		processorThread.start();
		smsg=new SirfMessage(receiver);

	}

	public void run(){
		while (!closed){
			process();
			try {
				synchronized (this) {
					wait(100);
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void close() {
		closed=true;
	}
	
	public void process() {
		byte[] readBuffer = smsg.readBuffer;
		try {
			while (ins.available() > 0) {
				long mesChecksum;
				switch (start) {
				case 0:
					if (ins.available() < 2)
						break;
					if (ins.read() == 0xA0) {
						if (ins.read() == 0xA2) {
							start = 2;
						} else {
							if (ausgabe != null)
							ausgabe.append("not the 2th start sign\n");
							break;
						}
					} else {
						if (ausgabe != null)
						ausgabe.append("not the start sign\n");
						break;
					}
				case 2:
					if (ins.available() < 2)
						break;
					length = ins.read() * 256;
					length += ins.read();
					if (length >= 1023) {
						if (ausgabe != null)
						ausgabe.append("message is to long: " + length
								+ "\n");
						start = 0;
						break;
					}
					start = 3;
					smsg.length = length;
				case 3:
					if (ins.available() <= length)
						break;
					if (ins.read(readBuffer, 0, length) != length) {
						if (ausgabe != null)
						ausgabe.append("message interupted\n");
						start = 0;
						break;
					}
					start = 4;
				case 4:
					if (ins.available() < 2)
						break;
					checksum = ins.read();
					checksum = checksum * 256 + ins.read();
					mesChecksum = calcChecksum(smsg);
					if (mesChecksum != checksum) {
						if (ausgabe != null)
						ausgabe.append("checksum error " + checksum + "!="
								+ mesChecksum + "\n");
						start = 0;
					}
					start = 5;
				case 5:
					if (ins.available() < 2)
						break;
					if (ins.read() == 0xB0) {
						if (ins.read() == 0xB3) {
							String nachricht = smsg.decodeMsg(smsg);
							if (nachricht != null)
								ausgabe.append(nachricht + "\n");
							start = 0;
						} else {
							start = 0;
							if (ausgabe != null)
							ausgabe.append("missing 2th endsign\n");
						}
					} else {
						start = 0;
						if (ausgabe != null)
						ausgabe.append("missing 1th endsign\n");
					}
					break;
				} // switch
			} // while
		} catch (IOException e) {
			System.out.println("Fehler: " + e);
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

	
	public StringBuffer getAusgabe() {
		return ausgabe;
	}

	
	public void setAusgabe(StringBuffer ausgabe) {
		this.ausgabe = ausgabe;
	}


}

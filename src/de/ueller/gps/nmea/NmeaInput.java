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

import de.ueller.gps.BtReceiverInput;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;



public class NmeaInput extends BtReceiverInput {

	protected static final Logger logger = Logger.getInstance(NmeaInput.class,Logger.TRACE);
	private NmeaMessage smsg;
	
	private byte [] buf1 = new byte[512]; //Buffer used to read data from GPS-receiver
	private byte [] buf2 = new byte[128]; //Buffer used to recombine data into NMEA sentences
	
	public void init(InputStream ins, OutputStream outs, LocationMsgReceiver receiver) {
		//#debug
		logger.debug("Starting NMEA");
		smsg=new NmeaMessage(receiver);		
		super.init(ins, outs, receiver);
	}

	public void process() throws IOException{		
		//position markers in buf1 and buf2, length of useful data in buf1
		int p1 = 0, p2 = 0, len1 = 0;
		char c;
		//Start marker in buf1 from where to copy
		int start1 = 0; 
		//Indicate if the start and or the end of a NMEA sentence has been read
		boolean found_start = false, found_end = false; 

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
				logger.info("Error: NMEA string was longer than 128 char, but max should be 82");
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
			} else {
				logger.info("NMEA sentence has incorrect checksum, discarding: " + new String(buf2));
			}

			//Reset buf2 for the next sentence
			p2 = 0; found_start = false; found_end = false;				
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
			logger.info("Target checksum not recognised: " + new String(buf,len-4,2));
			return false;
		}
		return (targetChecksum == checksum);
	}
}

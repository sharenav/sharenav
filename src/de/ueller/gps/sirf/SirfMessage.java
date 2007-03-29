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

import java.util.Calendar;

import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;

public class SirfMessage {

	public byte[]			readBuffer	= new byte[1023];


	Position				pold;

	public int				length		= 0;

	private final SirfMsgReceiver	receiver;

	public SirfMessage(SirfMsgReceiver receiver) {
		this.receiver = receiver;}


	public SirfMessage(byte[] readBuffer,SirfMsgReceiver receiver) {
		this.receiver=receiver;
		this.readBuffer = readBuffer;
		length = readBuffer.length;
	}

	public short getByte(int i) {
		short buf = readBuffer[i];
		if (buf < 0) buf = (short) (buf + 256);
		return buf;
	}

	public int getMessageId() {
		return getByte(0);
	}

	public short get2Byte(int i) {
		return (short) (getByte(i) * 256 + getByte(i + 1));
	}

	public int get2ByteUnsigned(int i) {
		return (getByte(i) * 256 + getByte(i + 1));
	}

	public int get4Byte(int i) {
		return (getByte(i) * 16777216 + getByte(i + 1) * 65536 + getByte(i + 2) * 256 + getByte(i + 3));
	}

	public String decodeMsg(SirfMessage smsg) {
		int type = smsg.getMessageId();
		switch (type) {
			case 2:
				return decodeMeasureNavigation();
			case 4:
				return decodeMeasuredTrackerDataOut();
			case 6:
				return decodeSoftwareVersion();
			case 9:
				return decodeThroughput();
			case 11:
				return decodeCommandAck();
			case 12:
				return decodeCommandNack();
			case 41:
				return decodeGeodeticNavigationData();
		}
		return message("msg " + type + " not implemented");
		// return null;
	}

	private String decodeMeasuredTrackerDataOut() {
		int anz=getByte(7);
		int i=8;
		Satelit s[]=new Satelit[anz];
		for (int l = 0; l < anz; l++){
			s[l]=decode1sMeasuredTrackerDataOut(8+l*15);			
		}
//		pcs.firePropertyChange("SatelitData",null,s);
		receiver.receiveStatelit(s);
		return message("MeasuredTrackerDataOut " + anz);

	}

	private Satelit decode1sMeasuredTrackerDataOut(int i) {
		Satelit s=new Satelit();
		s.id=getByte(i++);
		s.azimut=getByte(i++)/2f*3f;
		s.elev=getByte(i++)/2f;
		s.state=get2ByteUnsigned(i);
		i+=2;
		for (int l=0;l<10;l++){
			s.signal[l]=getByte(i++);
		}
		if (s.id != 0)
		message("Satelit " + s.id + " Aq s:"+s.isAcquisitionSucessfully()
                + " PH:"+s.isCharrierPhaseValid()
                + " BS:"+s.isBitSync()
                + " SS:"+s.isSubframeSync()
                + " CP:"+s.isCarrierPullin()
                + " LK:"+s.isLocked()
                + " Aq f:"+s.isAcquisitionFaild()
                + " EP:"+s.isEphemeris()
                + " SI:"+s.signal[0]
				+ " "+s.azimut + " " + s.elev
				                    );
		return s;
	}

	private String decodeThroughput() {
		return null;
		// return "Throughput SegStatMax:"+(1d*get2ByteUnsigned(1)/186d)+" last ms: "+(1d*get2ByteUnsigned(7)/186d);
	}

	private String decodeSoftwareVersion() {
		return message(new String(readBuffer));
	}

	private String decodeCommandNack() {

		return message("Not OK for Message " + getByte(1));
	}

	private String decodeCommandAck() {
		return message("OK for Message " + getByte(1));
	}

	private String decodeGeodeticNavigationData() {
		short valid = get2Byte(1);
		// short type = get2Byte(3);
		// short eWeekNr = get2Byte(5);
		// int two = get4Byte(7);
		short year = get2Byte(11);
		short month = getByte(13);
		short day = getByte(14);
		short hour = getByte(15);
		short min = getByte(16);
		double second = 1d * get2ByteUnsigned(17) / 1000d;
		// int satIdList = get4Byte(19);
		double lat = getDeg(23);
		double lon = getDeg(27);
		// double altEl = getMeter(31);
		double altMSL = getMeter(35);
		// short mapDatum = getByte(39);
		double sog = 0.01d * get2ByteUnsigned(40);
		double course = 0.01d * get2ByteUnsigned(42);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, (int) second);
		cal.add(Calendar.HOUR, 2);
		Position p = new Position((float) lat, (float) lon, (float) altMSL, (float) sog, (float) course, valid, cal.getTime());
//		pcs.firePropertyChange("GpsPosition", pold, p);
		receiver.receivePosItion(p);
		pold = p;
		return message("" + day + "." + month + "." + year + " " + hour + ":" + min + ":" + second + " Lat=" + lat + " lon=" + lon + " h=" + altMSL + " sog=" + sog);
	}

	private double getMeter(int i) {
		return (0.01d * get4Byte(i));
	}

	private double getDeg(int i) {
		return (get4Byte(i) * 1.0d / 10000000d);
	}

	private String decodeMeasureNavigation() {
		short byte1 = getByte(14);
		boolean dgps=false;
		String msg="UK";
		if ((byte1 & 0x80) > 0){
			dgps=true;
		}
		byte1=(short) (byte1 & 0x0f);
		switch (byte1){
			case 0: msg="No"; break;
			case 1: msg="1S"; break;
			case 2: msg="2S"; break;
			case 3: msg="2D"; break;
			case 4: msg="3D"; break;
			case 5: msg="2DP"; break;
			case 6: msg="3DP"; break;
			case 7: msg="DR"; break;

		}
		String ret= ((dgps)?"DGPS " : "" )+ msg;
		receiver.receiveMessage("got MeasureNavigation " + ret);
		receiver.receiveSolution(ret);
		return null;
	}

	private String message(String msg) {
		receiver.receiveMessage(msg);
		return msg;
	}
}

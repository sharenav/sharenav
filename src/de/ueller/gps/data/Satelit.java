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
package de.ueller.gps.data;

import java.util.Date;


public class Satelit {
	
	private static final int STATUS_ACQUISITION_SUCESSFULLY=0x1;
	private static final int STATUS_CARRIER_PHASE_VALID=0x2;
	private static final int STATUS_BIT_SYNC=0x4;
	private static final int STATUS_SUBFRAME_SYNC=0x8;
	private static final int STATUS_CARRIER_PULLIN=0x10;
	private static final int STATUS_CODE_LOCKED=0x20;
	private static final int STATUS_ACQUISITION_FAILD=0x40;
	private static final int STATUS_EMPEMERIS=0x80;
	
	public int id;
	public float azimut;
	public float elev;
	public int state;
	public int signal[];
	/**
	 * Signal to noise ratio in arbitrary units
	 */
	public int snr;
	public Date lastUpdate;
	public Satelit() {
		signal=new int[10];
	}
	
	private boolean isState(int mask){
		return ((state & mask) > 0);
	}
	
	public boolean isAcquisitionSucessfully(){
		return isState(STATUS_ACQUISITION_SUCESSFULLY);
	}
	public boolean isCharrierPhaseValid(){
		return isState(STATUS_CARRIER_PHASE_VALID);
	}
	public boolean isBitSync(){
		return isState(STATUS_BIT_SYNC);
	}
	public boolean isSubframeSync(){
		return isState(STATUS_SUBFRAME_SYNC);
	}
	public boolean isCarrierPullin(){
		return isState(STATUS_CARRIER_PULLIN);
	}
	public boolean isLocked(){
		return isState(STATUS_CODE_LOCKED);
	}
	
	public void isLocked(boolean locked){
		if (locked == true)
			state |= STATUS_CODE_LOCKED;
		else
			state &= ~STATUS_CODE_LOCKED;
	}
	public boolean isAcquisitionFaild(){
		return isState(STATUS_ACQUISITION_FAILD);
	}
	public boolean isEphemeris(){
		return isState(STATUS_EMPEMERIS);
	}

	
	public Date getLastUpdate() {
		return lastUpdate;
	}

	
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}

/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See file COPYING.
 */

package net.sharenav.gps;

import java.util.Date;

/** Container for the data of a GPS satellite.
 */
public class Satellite {
	private static final int STATUS_ACQUISITION_SUCCESSFUL = 0x1;
	private static final int STATUS_CARRIER_PHASE_VALID = 0x2;
	private static final int STATUS_BIT_SYNC = 0x4;
	private static final int STATUS_SUBFRAME_SYNC = 0x8;
	private static final int STATUS_CARRIER_PULL_IN = 0x10;
	private static final int STATUS_CODE_LOCKED = 0x20;
	private static final int STATUS_ACQUISITION_FAILED = 0x40;
	private static final int STATUS_EPHEMERIS = 0x80;
	
	public int id;
	public float azimut;
	public float elev;
	/** State of this satellite, see the STATUS_* constants for possible values */
	public int state;
	/** Signal to noise ratio in arbitrary units */
	public int snr;
	public Date lastUpdate;

	public Satellite() {
	}
	
	private boolean isState(int mask) {
		return ((state & mask) > 0);
	}
	
	public boolean isAcquisitionSuccessful() {
		return isState(STATUS_ACQUISITION_SUCCESSFUL);
	}

	public boolean isCharrierPhaseValid() {
		return isState(STATUS_CARRIER_PHASE_VALID);
	}

	public boolean isBitSync() {
		return isState(STATUS_BIT_SYNC);
	}

	public boolean isSubframeSync() {
		return isState(STATUS_SUBFRAME_SYNC);
	}

	public boolean isCarrierPullIn() {
		return isState(STATUS_CARRIER_PULL_IN);
	}

	public boolean isLocked() {
		return isState(STATUS_CODE_LOCKED);
	}
	
	public void isLocked(boolean locked) {
		if (locked == true) {
			state |= STATUS_CODE_LOCKED;
		} else {
			state &= ~STATUS_CODE_LOCKED;
		}
	}

	public boolean isAcquisitionFailed() {
		return isState(STATUS_ACQUISITION_FAILED);
	}

	public boolean isEphemeris() {
		return isState(STATUS_EPHEMERIS);
	}
	
	public Date getLastUpdate() {
		return lastUpdate;
	}
	
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}

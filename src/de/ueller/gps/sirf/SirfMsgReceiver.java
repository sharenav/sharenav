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

import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;


public interface SirfMsgReceiver {

	public static final byte SIRF_FAIL_NO_START_SIGN1=0;
	public static final byte SIRF_FAIL_NO_START_SIGN2=1;
	public static final byte SIRF_FAIL_MSG_TO_LONG=2;
	public static final byte SIRF_FAIL_MSG_INTERUPTED=3;
	public static final byte SIRF_FAIL_MSG_CHECKSUM_ERROR=4;
	public static final byte SIRF_FAIL_NO_END_SIGN1=5;
	public static final byte SIRF_FAIL_NO_END_SIGN2=6;
	public static final byte SIRF_FAIL_COUNT=7;

	public void receivePosItion(Position pos);
	public void receiveStatelit(Satelit[] sat);
	public void receiveMessage(String s);
	public void receiveSolution(String s);
}

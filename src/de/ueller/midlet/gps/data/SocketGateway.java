/*
 * GpsMid - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * See COPYING
 */
package de.ueller.midlet.gps.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.CellIdProvider;

//#if polish.android
import java.util.List;
import android.content.Context;
import de.enough.polish.android.midlet.MidletBridge;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorListener;
//#endif


public class SocketGateway {
	final static int PROTO_REQ_COMPASS = 6574724;
	final static int PROTO_REQ_CELLID = 6574723;
	
	private static CompassProvider singelton;
	
	private static final Logger logger = Logger.getInstance(CompassProvider.class,
			Logger.TRACE);
	
	private static SocketConnection clientSock = null;
	private static DataInputStream clientIS = null;
	private static DataOutputStream clientOS = null;
	
	final static int TYPE_COMPASS = 1;
	final static int TYPE_CELLID = 2;

	final static int RETURN_OK = 1;
	final static int RETURN_IOE = 2;
	final static int RETURN_FAIL = 3;

	static GSMCell cell = null;
	
	static Compass compass = null;
	
	public static Compass getCompass() {
		return compass;
	}
	public static GSMCell getCell() {
		return cell;
	}
	public synchronized static int getSocketData(int dataType) {
		if (clientSock == null) {
			try {
				logger.info("Connecting to socket://127.0.0.1:59721");
				clientSock = (SocketConnection) Connector.open("socket://127.0.0.1:59721");
				clientSock.setSocketOption(SocketConnection.KEEPALIVE, 0);
				clientOS = new DataOutputStream(clientSock.openOutputStream());
				clientIS = new DataInputStream(clientSock.openInputStream());
				logger.info("Connected to socket");
				
			} catch (SecurityException se) {
				logger.exception("Sorry, you declined to try and connect to a local helper deamon", se);
				clientSock = null;
				return RETURN_FAIL;
			} catch (ConnectionNotFoundException cnfe) {
				//This is quite common, so silently ignore this;
				logger.silentexception("Could not open a connection to local helper deamon", cnfe);
				clientSock = null;
				return RETURN_FAIL;
			} catch (IOException ioe) {
				logger.exception("Failed to open connection to a local helper deamon", ioe);
				clientSock = null;
				return RETURN_IOE;
			}
		}
		
		if (dataType == TYPE_COMPASS) {
			try {
				byte [] buf = new byte[4096];
				logger.info("Requesting next Compass");
				int noAvail = clientIS.available();
				while (noAvail > 0) {
					if (noAvail > 4096) {
						noAvail = 4096;
					}
					//#debug debug
					logger.debug("Emptying Buffer of length " + noAvail);
					clientIS.read(buf,0,noAvail);
					noAvail = clientIS.available();
				}
				clientOS.writeInt(PROTO_REQ_COMPASS);
				clientOS.flush();
				//debug trace
				logger.trace("Wrote Compass request");
				if (clientIS.available() < 4) {
					//#debug debug
					logger.debug("Not Enough Data wait 50");
					Thread.sleep(50);
				}
				if (clientIS.available() < 4) {
					//#debug debug
					logger.debug("Not Enough Data wait 500");
					Thread.sleep(500);
				}
				if (clientIS.available() > 3) {
					//#debug debug
					logger.debug("Reading");
					compass.direction = clientIS.readInt();
					logger.info("Read Compass: " + compass);
					return RETURN_OK;
				}
				logger.info("Not enough data available from socket, can't retrieve Compass: " + clientIS.available());
			} catch (IOException ioe) {
				logger.silentexception("Failed to read compass", ioe);
				clientSock = null;
				return RETURN_IOE;
			} catch (InterruptedException ie) {
				return RETURN_FAIL;
			}
		} else if (dataType == TYPE_CELLID) {
			try {
				byte [] buf = new byte[4096];
				logger.info("Requesting next CellID");
				int noAvail = clientIS.available();
				while (noAvail > 0) {
					if (noAvail > 4096) {
						noAvail = 4096;
					}
					//#debug debug
					logger.debug("Emptying Buffer of length " + noAvail);
					clientIS.read(buf,0,noAvail);
					noAvail = clientIS.available();
				}
				clientOS.writeInt(PROTO_REQ_CELLID);
				clientOS.flush();
				//debug trace
				logger.trace("Wrote Cell request");
				if (clientIS.available() < 18) {
					//#debug debug
					logger.debug("Not Enough Data wait 50");
					Thread.sleep(50);
				}
				if (clientIS.available() < 18) {
					//#debug debug
					logger.debug("Not Enough Data wait 500");
					Thread.sleep(500);
				}
				if (clientIS.available() > 17) {
					//#debug debug
					logger.debug("Reading");
					cell.mcc = (short)clientIS.readInt();
					cell.mnc = (short)clientIS.readInt();
					cell.lac = clientIS.readInt();
					cell.cellID = clientIS.readInt();
					short signal = clientIS.readShort();
					logger.info("Read Cell: " + cell);
					return RETURN_OK;
				}
				logger.info("Not enough data available from socket, can't retrieve Cell: " + clientIS.available());
			} catch (IOException ioe) {
				logger.silentexception("Failed to read cellid", ioe);
				clientSock = null;
				return RETURN_IOE;
			} catch (InterruptedException ie) {
				return RETURN_FAIL;
			}
		}
		return RETURN_FAIL;
	}
}

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.tests;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;


public class NMEAsimMidlet extends MIDlet implements Runnable {

	//
	  // major service class as SERVICE_TELEPHONY
	  private final static int SERVICE_TELEPHONY = 0x400000;
	// Bluetooth singleton object
	private LocalDevice device;
	private DiscoveryAgent agent;
	public final static UUID uuid = new UUID("102030405060708090A0B0C0D0E0F010", false);

	private NMEAsimScreen screen;

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// TODO Auto-generated method stub

	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
		addMsg("Starting up NMEAsimulator");
		Display display = Display.getDisplay(this);
		screen = new NMEAsimScreen(this);
		display.setCurrent(screen);
		Thread t = new Thread(this);
		addMsg("Starting up NMEAsimulator");
		t.start();
	}

	public void run() {
		StreamConnectionNotifier server;
		boolean done = false;
		// human friendly name of this service
		String appName = "NMEA_Emulator";


		// connection to remote device
		StreamConnection c = null;
		try
		{
			// initialize the JABWT stack
		      device = LocalDevice.getLocalDevice(); // obtain reference to singleton
		      device.setDiscoverable(DiscoveryAgent.GIAC); // set Discover mode to LIAC
		      
			String url = "btspp://localhost:" + uuid.toString() +";name="+ appName;
			
			addMsg("Server url: " + url);

			// Create a server connection object, using a
			// Serial Port Profile URL syntax and our specific UUID
			// and set the service name to BlueChatApp
			server =  (StreamConnectionNotifier)Connector.open(url);
			
			System.out.println(server);
			addMsg(server.toString());

			// Retrieve the service record template
			ServiceRecord rec = device.getRecord( server );

			// set ServiceRecrod ServiceAvailability (0x0008) attribute to indicate our service is available
			// 0xFF indicate fully available status
			// This operation is optional
			rec.setAttributeValue( 0x0008, new DataElement( DataElement.U_INT_1, 0xFF ) );

			// Set the Major Service Classes flag in Bluetooth stack.
			// We choose Telephony Service
			rec.setDeviceServiceClasses( SERVICE_TELEPHONY  );
		} catch (Exception e)
		{
			addMsg(e.toString());
			e.printStackTrace();
			addMsg("Failed to create a connection: " + e.getMessage());
			return;
		}

		while( !done)
		{
			try {
				
				//
				// start accepting client connection.
				// This method will block until a client
				// connected
				addMsg("Waiting for client to connect");
				c = server.acceptAndOpen();
				addMsg("Client connected: " + c.toString());		
				OutputStream os = c.openOutputStream();
				
				InputStream is = getClass().getResourceAsStream("/GPS-NMEA-Test.txt");
				
				if (is == null) {
					addMsg("NMEA file not found");
					break;
				}
				InputStreamReader isr = new InputStreamReader(is);
				
				while(1==1) {
					if (is.available() == 0)
						isr.reset();
					StringBuffer sb = new StringBuffer(80);				
					char tmp = 'x';
					while (tmp != '\n') {					
						tmp = (char)isr.read();
						sb.append(tmp);
					}
					sb.append("\r\n");
					String nmeaMessage = sb.toString();
					addMsg(nmeaMessage);
					if (nmeaMessage.startsWith("$GPGGA")) {
						synchronized(this) {
							wait(1000);
						}
					}
					os.write(nmeaMessage.getBytes());
						
				}
				
				

			} catch (Exception e) {
				addMsg("Exception: " + e);
			}
		}
	}
	
	public void addMsg(String msg) {
		if (screen != null)
			screen.addMsg(msg);
	}
	
	public void quitApp() {
		try {
			this.destroyApp(true);
		} catch (MIDletStateChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.util.Vector;

//#if polish.api.btapi
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
//#endif
import de.ueller.gps.tools.StringTokenizer;

public class DiscoverGps
	//#if polish.api.btapi
	implements Runnable, DiscoveryListener
	//#endif
	{
	
	private final static Logger logger=Logger.getInstance(DiscoverGps.class,Logger.DEBUG);
	
	//#if polish.api.btapi
	/** Shows the engine is ready to work. */
	private static final int		READY						= 0;

	/** Shows the engine is searching bluetooth devices. */
	public static final int		DEVICE_SEARCH				= 1;

	/** Shows the engine is ready with searching bluetooth devices. */
	public static final int		DEVICE_READY				= 2;

	/** Shows the engine is searching bluetooth services. */
	public static final int		SERVICE_SEARCH				= 3;

	/** the engine is wating for a serviceselection */
	public static final int		SERVICE_SELECT				= 4;
	/** the engine is wating for a serviceselection */
	public static final int		NODEVICE				= 5;

	private static final String[]	stateText					= { "ready",
			"device search", "device select", "service search","select service","No Device in range"};

	private final GuiDiscover		parent;

	/** Process the search/download requests. */
	private Thread					processorThread;

	/** Collects the remote devices found during a search. */
	private Vector					/* RemoteDevice */devices	= new Vector();

	/** Collects the services found during a search. */
	private Vector					/* ServiceRecord */records	= new Vector();

	/** Keeps the device discovery return code. */
	private int						discType					= -1;

	/** Keeps the services search IDs (just to be able to cancel them). */
	private int[]					searchIDs;
	/** Optimization: keeps service search pattern. */
	private UUID[]					uuidSet;

	/** Optimization: keeps attributes list to be retrieved. */
	private int[]					attrSet;

	/** Keeps the current state of engine. */
	private int						state						= READY;

	/** Keeps the discovery agent reference. */
	private DiscoveryAgent			discoveryAgent;

	private boolean					isClosed;

	/** Keeps the device index for witch a Service discover is requested */
	private int	selectedDevice = -1;
	public static final long UUDI_SERIAL=0x1101;
	public static final long UUDI_FILE=0x1105;

	private final long searchType;
	
	public DiscoverGps(GuiDiscover parent,long searchType) {
		this.parent = parent;
		this.searchType = searchType;
		// we have to initialize a system in different thread...
		processorThread = new Thread(this);
		processorThread.start();

	}

	private synchronized void cancelDeviceSearch(){
		if (state == DEVICE_SEARCH) {
			discoveryAgent.cancelInquiry(this);
		}
	}

	/** Cancel's the devices/services search. */
	public void cancelSearch() {
		cancelDeviceSearch();
		cancelServiceSearch();
	}

	private synchronized void cancelServiceSearch(){
		if (state == SERVICE_SEARCH) {
			for (int i = 0; i < searchIDs.length; i++) {
				discoveryAgent.cancelServiceSearch(searchIDs[i]);
			}
		}
	}

	/**
	 * Destroy a work with bluetooth - exits the accepting thread and close notifier.
	 */
	void destroy() {
		synchronized (this) {
			parent.addDevice("shutdown discover");
			cancelSearch();
			isClosed = true;

			notify();

			// FIXME: implement me
		}

		// wait for acceptor thread is done
		try {
			processorThread.join();
		} catch (InterruptedException e) {} // ignore
		parent.addDevice("distroyed");
		parent.show();
	}

	/**
	 * Invoked by system when a new remote device is found - remember the found device.
	 */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		// same device may found several times during single search
		parent.addDevice("found "+btDevice.getBluetoothAddress());		
		if (devices.indexOf(btDevice) == -1) {
			devices.addElement(btDevice);
		}
	}

	public int getState() {
		return state;
	}

	/**
	 * Invoked by system when device discovery is done.
	 * <p>
	 * Use a trick here - just remember the discType and process its evaluation in another thread.
	 */
	public void inquiryCompleted(int discType) {
		this.discType = discType;
		// parent.showState("search complete");
		parent.addDevice("inquiry Complete");
		synchronized (this) {
			notify();
		}
	}

	/** Sets the request to search the devices/services. */
	void requestSearch() {
		synchronized (this) {
			notify();
		}
	}
	
	

	public void run() {
		try {
			//Probe Commports:
			try {
				String commports = System.getProperty("microedition.commports");			
				String[] commport = StringTokenizer.getArray(commports, ",");
				for (int i = 0; i < commport.length; i++) {				
					parent.addDevice("comm:" + commport[i] + ";baudrate=19200",
						commport[i]);
				}
			} catch (RuntimeException re) {
				logger.error("Comm ports are not supported on this device: " + re.getMessage());
			} catch (Exception e) {
				logger.error("Comm ports are not supported on this device: " + e.getMessage());
			}
			
//			System.out.println("Start Thread Discover Gps");
			// initialize bluetooth first
			parent.addDevice("init BT");
			boolean isBTReady = false;

			try {
				// create/get a local device and discovery agent
				LocalDevice localDevice = LocalDevice.getLocalDevice();
				discoveryAgent = localDevice.getDiscoveryAgent();

				// remember we've reached this point.
				isBTReady = true;
			} catch (Exception e) {
				System.err.println("Can't initialize bluetooth: " + e);
				parent.addDevice("Can't init bluetooth");
			}

			parent.completeInitialization(isBTReady);

			// nothing to do if no bluetooth available
			if (!isBTReady) {
				parent.addDevice("no Blutooth");
				return;
			}

			// initialize some optimization variables
			uuidSet = new UUID[1];

			// ok, we are interesting in btspp or File services,
			// which one at the moment is specified by searchType
			uuidSet[0] = new UUID(searchType);			
			selectService();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parent.addDevice("Thread end");
		parent.btDiscoverReady();			

	}

	private void searchDevice() {
		try {		
			setState(DEVICE_SEARCH);
			discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (waitUntilNotify()) {
			return;
		}
		switch (discType) {
			case INQUIRY_ERROR:
				parent.addDevice("Device discovering error...");
				return;
			case INQUIRY_TERMINATED:
				// make sure no garbage in found devices list
				parent.addDevice("Device search canceled");

				// nothing to report - go to next request
				break;

			case INQUIRY_COMPLETED:
				if (devices.size() == 0) {
//					parent.addDevice("No devices in range");
//					parent.addDevice("btspp://000DB5315C50:1;authenticate=false;encrypt=false;master=false","Dummy for emulator");
					setState(NODEVICE);
				} else {
				   setState(DEVICE_READY);
				   break;
				}
				break;
			default:
				// what kind of system you are?... :(
				parent.addDevice("unknown Return from Discover");
				System.err.println("system error:"
						+ " unexpected device discovery code: " + discType);
//					destroy();

		//		return;
		}
//			if (waitUntilNotify()) 
//				return;
	}

	private void searchService() {
		synchronized (this) {			
			searchIDs = new int[devices.size()];
			int i = 0; int retries = 0;
			while (i < searchIDs.length) {				
				if (retries > 4) {
					//This device discovery failed.
					//Set searchIDs[i] to -1 to indicate it has failed,
					//as serviceSerchComplete uses this to check if all searches
					//have completed
					searchIDs[i] = -1;
					i++; retries = 0;
					continue;
				}
				try {
					RemoteDevice rd = (RemoteDevice) devices.elementAt(i);
					searchIDs[i] = discoveryAgent.searchServices(attrSet, uuidSet,
							rd, this);
				} catch (BluetoothStateException e) {				
					//This exception is most likely due to the fact
					//that the device is not able to handle concurrent
					//searchServices() calls. So wait a while and try again					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						//Nothing to do in that case						
					}
					retries++;
					continue;
				}
				i++;
				retries = 0;
			}			
			parent.addDevice("wait for Discovery end");
		}
	}

	public synchronized void selectDevice(int idx){
		selectedDevice=idx;
		state=SERVICE_SEARCH;
		notify();
	}

	private void selectService() {
//		while (!isClosed) {
		// suche die devices
		    parent.addDevice("search devices");
			searchDevice();
			if (devices.size() == 0){
				parent.addDevice("no Device found");
			    return;
			}
			// durchsuche alle devices nach services
			parent.addDevice("search services");
			searchService();
			if (getState() != SERVICE_SELECT)			
				waitUntilNotify();			
//			parent.clear();
			if (devices.size() == 0){
				parent.addDevice("no Service found");
			    return;
			}
			for (int i=0; i<records.size();i++){
				ServiceRecord service=(ServiceRecord) records.elementAt(i);
				parent.addDevice(service.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
			}
			// if no Services found, try with the discovered BT devices
			// this is because RAZER V3i is after firmware update not able
			// to discover services
			parent.addDevice("constuct "+devices.size()+" services");
			if (records.size()==0 && devices.size() > 0){
				for (int dl=0; dl < devices.size(); dl++){
					RemoteDevice rd = (RemoteDevice) devices.elementAt(dl);
					parent.addDevice("btspp://"+rd.getBluetoothAddress()+":1;authenticate=false;encrypt=false;master=false",friendlyName(rd)+ " no Service disc.");
				}
			}

//		}

	}
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		for (int i = 0; i < servRecord.length; i++) {
//			String connectionURL = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
//			parent.addDevice(connectionURL);
			records.addElement(servRecord[i]);
			parent.addDevice(
					servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false),
					friendlyName(servRecord[i].getHostDevice()));
		}
	}
	private String friendlyName(RemoteDevice rd){
		try {
			return rd.getFriendlyName(true);
		} catch (IOException e) {
			return rd.getBluetoothAddress();
		}

	}
	
	public void serviceSearchCompleted(int transID, int respCode) {		
		// first, find the service search transaction index
		int index = -1;

		for (int i = 0; i < searchIDs.length; i++) {
			if (searchIDs[i] == transID) {
				index = i;
				break;
			}
		}

		// error - unexpected transaction index
		if (index == -1) {
			System.err.println("Unexpected transaction index: " + transID);

			// FIXME: process the error case
		} else {
			searchIDs[index] = -1;
		}

		/*
		 * Actually, we do not care about the response code - if device is not reachable or no records, etc.
		 */

		// make sure it was the last transaction
//		parent.addDevice("look if all descovered");
		for (int i = 0; i < searchIDs.length; i++) {
			if (searchIDs[i] != -1) {
				return;
			}
		}
//		parent.addDevice("all discovered");
		// ok, all of the transactions are completed
		setState(SERVICE_SELECT);		
		synchronized (this) {
			notify();
		}
		
	}
	
	private void setState(int state) {
		this.state = state;
		parent.showState(stateText[state]);
	}

	
	private boolean waitUntilNotify() {
		if (isClosed) {
			return false;
		}
//		parent.addDevice("wait for notify");
		synchronized (this) {
			try {
				wait(); // until devices or service are found
			} catch (InterruptedException e) {
				System.err.println("Unexpected interruption: " + e);
				parent.addDevice("interrupted");
				return true;
			}
		}
//		parent.addDevice("got notify");
		if (isClosed) {
			return true;
		}
		return false;
	}
	//#endif
}

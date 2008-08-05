package de.ueller.gps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.ueller.gps.nmea.NmeaInput;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;

/**
 * 
 * This class shares the functionallity to read from the Bluetooth Gps receiver and handles
 * common functionality such as dealing with receiver quality and lost connections
 * 
 *  The protocoll specific decoding is handled in the abstract process() function of subclasses
 *
 */
public abstract class BtReceiverInput implements Runnable, LocationMsgProducer{
	private static final Logger logger = Logger.getInstance(NmeaInput.class,Logger.TRACE);

	protected InputStream ins;
	protected OutputStream rawDataLogger;
	protected Thread					processorThread;
	protected LocationMsgReceiver	receiver;
	protected boolean closed=false;
	protected byte connectQuality=100;
	protected int bytesReceived=0;
	protected int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	protected String message;
	protected int msgsReceived=1;

	public void init(InputStream ins,LocationMsgReceiver receiver) {
		this.ins = ins;
		this.receiver = receiver;
		processorThread = new Thread(this,"NMEA Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();

	}

	abstract protected void process() throws IOException;

	public void run(){
		receiver.receiveMessage("Start Bt Gps Receiver");
		// eat the buffer content
		try {
			try {
				byte [] buf = new byte[512]; 
				while (ins.available() > 0){
					int recieved = ins.read(buf);
					bytesReceived += recieved;
					if (rawDataLogger != null) {
						rawDataLogger.write(buf, 0, recieved);					
						rawDataLogger.flush();					
					}
				}
				receiver.receiveMessage("Erasing " + bytesReceived +" bytes");
				bytesReceived=100;			
			} catch (IOException e1) {
				receiver.receiveMessage("Closing: " + e1.getMessage());
				close("Closing: " + e1.getMessage());
			}

			byte timeCounter=41;
			while (!closed){
				timeCounter++;
				if (timeCounter > 40){
					timeCounter = 0;
					if(connectQuality > 100) {
						connectQuality=100;
					}
					if(connectQuality < 0) {
						connectQuality=0;
					}
					receiver.receiveStatistics(connectError,connectQuality);
					//watchdog if no bytes received in 10 sec then exit thread
					if (bytesReceived == 0){
						close("No Data from NMEA");
					} else {
						bytesReceived=0;
					}
				}
				try {
					process();
				} catch (IOException e) {
					receiver.receiveMessage("Closing: " + e.getMessage());
					close("Closed: " + e.getMessage());
				}
				if (! closed)
					try {
						synchronized (this) {
							connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
							wait(250);
						}				
					} catch (InterruptedException e) {
						//Nothing to do in this case
					}

			}
			if (message == null){
				receiver.locationDecoderEnd();
			} else {
				receiver.locationDecoderEnd(message);
			}
		} catch (OutOfMemoryError oome) {
			logger.fatal("GpsInput thread crashed as out of memory: " + oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			logger.fatal("GpsInput thread crashed unexpectedly: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see de.ueller.gps.nmea.LocationMsgProducer#close()
	 */
	public synchronized void close() {
		disableRawLogging();
		closed=true;
	}
	public synchronized void close(String message) {
		disableRawLogging();
		closed=true;
		this.message=message;
	}

	public void enableRawLogging(OutputStream os) {
		rawDataLogger = os;		
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception("Couldn't close raw gps logger", e);
			}
			rawDataLogger = null;
		}
	}

	public void addLocationMsgReceiver(LocationMsgReceiver receiver) {
		// TODO Auto-generated method stub

	}

}

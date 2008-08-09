package de.ueller.gps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.nmea.NmeaInput;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

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
	private OutputStream outs;
	protected OutputStream rawDataLogger;
	protected Thread					processorThread;
	protected LocationMsgReceiver	receiver;
	protected boolean closed=false;
	protected byte connectQuality=100;
	protected int bytesReceived=0;
	protected int[] connectError=new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	protected String message;
	protected int msgsReceived=1;
	
	protected class KeepAliveTimer extends TimerTask {
		public void run() {
			if (outs != null) {
				try {
					logger.debug("Writing bogus keep-alive");
					outs.write(0);
				} catch (IOException e) {
					logger.info("Closing keep alive timer");
					this.cancel();
				}
			}
		}		
	}

	public void init(InputStream ins, OutputStream outs, LocationMsgReceiver receiver) {
		this.ins = ins;
		this.outs = outs;
		this.receiver = receiver;
		processorThread = new Thread(this,"NMEA Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();
		/**
		 * There is at least one, perhaps more BT gps receivers, that
		 * seem to kill the bluetooth connection if we don't send it
		 * something for some reason. Perhaps due to poor powermanagment?
		 * We don't have anything to send, so send an arbitrary 0.
		 */
		if (Trace.getInstance().getConfig().getBtKeepAlive()) {
			TimerTask tt = new KeepAliveTimer();
			logger.info("Setting keep alive timer: " + tt);
			Timer t = new Timer();
			t.schedule(tt, 1000,1000);
		}
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
				try {
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
							throw new IOException("No Data from Gps");
						} else {
							bytesReceived=0;
						}
					}
					process();
				} catch (IOException e) {
					/**
					 * The bluetooth connection seems to have died,
					 * try and reconnect. If the reconnect was successful
					 * the reconnect function will call the init function,
					 * which will create a new thread. In that case we simply
					 * exit this old thread. If the reconnect was unsuccessful
					 * then we close the connection and give an error message.
					 */
					logger.info("Failed to read from GPS trying to reconnect: " + e.getMessage());
					receiver.receiveSolution("~~");
					if (!Trace.getInstance().autoReconnectBtConnection()) {
						logger.info("Gps bluethooth could not reconnect");
						receiver.receiveMessage("Closing: " + e.getMessage());
						close("Closed: " + e.getMessage());
					} else {
						logger.info("Gps bluetooth reconnect was successful");
						return;
					}
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

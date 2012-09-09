

import java.io.IOException;
import java.io.InputStream;

import net.sharenav.sharenav.data.Position;
import net.sharenav.gps.data.Satelit;
import net.sharenav.gps.location.NmeaInput;
import net.sharenav.gps.location.NmeaMessage;
import net.sharenav.gps.location.LocationMsgReceiver;
import junit.framework.TestCase;

public class NmeaInputTest extends TestCase implements LocationMsgReceiver {

	public NmeaInputTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSentence() {
		NmeaMessage m=new NmeaMessage(this);
		m.getBuffer().setLength(0);
		m.getBuffer().append("GGA,095145.000,4919.6827,N,01121.1592,E,1,05,1.4,471.2,M,47.7,M,,0000*56");
		m.decodeMessage();
	}
	public void testInput(){
		InputStream stream = NmeaInputTest.class.getResourceAsStream("/GPS-NMEA-Test.txt");
		NmeaInput ni=new NmeaInput(true,stream,this);
try {
	while (stream.available() > 80)
		ni.process();
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
	}
//	public 

	public void receiveMessage(String s) {
		System.out.println("msg: " +s);
		
	}

	public void receivePosition(Position pos) {
		System.out.println("pos " + pos.latitude + " " + pos.longitude);
	}

	public void receiveSolution(String s) {
		// TODO Auto-generated method stub
	}

	public void receiveSatellites(Satelit[] sats) {
		System.out.println("got satellites");
	}

	public void receiveStatistics(int[] statRecord, byte qualtity) {
		// TODO Auto-generated method stub
	}

	public void sirfDecoderEnd() {
		// TODO Auto-generated method stub
	}

	public void locationDecoderEnd() {
		// TODO Auto-generated method stub
	}

	public void locationDecoderEnd(String msg) {
		// TODO Auto-generated method stub
	}

}

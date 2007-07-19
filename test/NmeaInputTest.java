

import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;
import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.nmea.NmeaMessage;
import de.ueller.midlet.gps.LocationMsgReceiver;
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

	public void receivePosItion(Position pos) {
		System.out.println("pos " + pos.latitude + " " + pos.longitude);
	}

	public void receiveSolution(String s) {
		// TODO Auto-generated method stub
		
	}

	public void receiveStatelit(Satelit[] sat) {
		System.out.println("got statelite");
		
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

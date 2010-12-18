//#if polish.api.min-siemapi
package de.ueller.midlet.gps.importexport;
import com.siemens.mp.game.Light;

public class SiemGameLight { 
	public SiemGameLight() { 
	} 

	public static void SwitchOn() { 
		Light.setLightOn(); 
	}
	
	public static void SwitchOff() {
		Light.setLightOff();
	}
}
//#endif
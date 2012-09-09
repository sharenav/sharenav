//#if polish.api.min-siemapi
package net.sharenav.midlet.util;
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
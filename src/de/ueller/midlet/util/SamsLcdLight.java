//#if polish.api.min-samsapi
package de.ueller.midlet.util;
import com.samsung.util.LCDLight;

public class SamsLcdLight { 
	public SamsLcdLight() { 
	} 

	public static void on(int duration) { 
		LCDLight.on(duration); 
	}
	public static void off() { 
		LCDLight.off(); 
	}
}
//#endif
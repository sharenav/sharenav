//#if polish.api.min-samsapi
package de.ueller.midlet.gps.importexport;
import com.samsung.util.LCDLight;

public class SamsLCDLight { 
	public SamsLCDLight() { 
	} 

	public static void on(int duration) { 
		LCDLight.on(duration); 
	}
	public static void off() { 
		LCDLight.off(); 
	}
}
//#endif
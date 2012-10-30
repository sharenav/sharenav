package net.sharenav.osmToShareNav;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


public class RouteSoundSyntax {
	private final static byte SYNTAX_FORMAT_VERSION = 1;
	private ResourceBundle rb = null;
	private InputStream is = null;	
	private DataOutputStream dsi = null;
	private ArrayList<String> soundNames = new ArrayList<String>(1);
	
	public RouteSoundSyntax(String styleFileDirectory, String additionalSrcPath, String destinationPathAndFile) {
	
		String info = "Using " + styleFileDirectory + additionalSrcPath + "/syntax.cfg";
		// try syntax.cfg from file system
		try {			
			is = new FileInputStream(styleFileDirectory + additionalSrcPath + "/syntax.cfg");
		} catch (Exception e) {
			// try internal syntax.cfg
			try {			
				info = "Using internal syntax.cfg from " + additionalSrcPath;
				is = getClass().getResourceAsStream("/media/" + additionalSrcPath + "/syntax.cfg");
			} catch (Exception e2) {
				;
			}
		}			

		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println(info + " for specifying route and sound syntax");
		}

		if (is != null) {
			try {
				InputStreamReader isr;
				// try reading syntax.cfg with UTF-8 encoding
				try {
					isr = new InputStreamReader(is, "UTF-8");
					try {			
						rb = new PropertyResourceBundle(isr);
					} catch (Exception e) {
						System.out.println ("ERROR: PropertyResourceBundle for syntax.cfg could not be created from InputStreamReader");
						e.printStackTrace();
						System.exit(1);
					}
				} catch (NoSuchMethodError nsme) {
					/* Give warning if creating the UTF-8-reader for syntax.cfg fails and continue with default encoding,
					 * this can happen with Java 1.5 environments
					 * more information at: http://sourceforge.net/projects/sharenav/forums/forum/677687/topic/4063854 
					 */
					System.out.println("Warning: Cannot use UTF-8 encoding for decoding syntax.cfg file as it requires Java 1.6+");
					try {			
						rb = new PropertyResourceBundle(is);
					} catch (Exception e) {
						System.out.println ("ERROR: PropertyResourceBundle for syntax.cfg could not be created from InputStream");
						e.printStackTrace();
						System.exit(1);
					}
				}
			} catch (UnsupportedEncodingException e1) {
					System.out.println ("ERROR: InputStreamReader for syntax.cfg could not be created");
					e1.printStackTrace();
					System.exit(1);
			}			
		} else {
			System.out.println ("ERROR: syntax.cfg not found in the " + additionalSrcPath + " directory");
			System.exit(1);
		}

		// create syntax.dat
		try {
			FileOutputStream foi = new FileOutputStream(destinationPathAndFile);
			dsi = new DataOutputStream(foi);
		} catch (Exception e) {
			;
		}
		
		if (dsi == null) {
			System.out.println ("ERROR: could not create " + destinationPathAndFile);
			System.exit(1);
		}
		
		try { 
			dsi.writeByte(SYNTAX_FORMAT_VERSION);	
				
			final String directionNames[] = { "default", "hardright", "right", "halfright", "straighton", "halfleft", "left", "hardleft"};
			for (int i=0; i < directionNames.length; i++) {
				getAndWriteString("direction." + directionNames[i] + ".screen");
				getAndWriteString("direction." + directionNames[i] + ".sound");
			}

			final String bearDirectionNames[] = { "right", "left"};
			for (int i=0; i < bearDirectionNames.length; i++) {
				getAndWriteString("beardir." + bearDirectionNames[i] + ".screen");
				getAndWriteString("beardir." + bearDirectionNames[i] + ".sound");
			}
			
			final String exitNames[] = { "1", "2", "3", "4", "5", "6"};
			for (int i=0; i < exitNames.length; i++) {
				getAndWriteString("roundabout.exit." + exitNames[i] + ".screen");
				getAndWriteString("roundabout.exit." + exitNames[i] + ".sound");
			}
			
			final String distanceNames[] = { "100", "200", "300", "400", "500", "600", "700", "800"};
			for (int i=0; i < distanceNames.length; i++) {
				getAndWriteString("distances." + distanceNames[i] + ".sound");
			}
				
			String componentNames[] = { "normal.sound", "prepare.sound", "in.sound", "then.sound", "normal.screen", "in.screen" };
			String instructionTypes[] = {"simpledirection", "beardir", "uturn", "roundabout",
				"entermotorway", "beardirandentermotorway",
				"leavemotorway", "beardirandleavemotorway",
				"intotunnel", "outoftunnel", "areacross", "areacrossed", "destreached" };
			for (int i=0; i < instructionTypes.length; i++) {
				for (int c=0; c < componentNames.length; c++) {
					getAndWriteString(instructionTypes[i] + "." + componentNames[c]);
				}			
			}
			
			
			getAndWriteString("soon.sound");
			getAndWriteString("again.sound");
			getAndWriteString("meters.sound");
			getAndWriteString("yards.sound");
			getAndWriteString("checkdirection.screen");
			getAndWriteString("checkdirection.sound");
			getAndWriteString("followstreet.sound");
			getAndWriteString("speedlimit.sound");
			getAndWriteString("recalculation.sound");
	
			// magic number
			dsi.writeShort(0x3550);
		} catch (IOException ioe) {
			System.out.println ("ERROR writing to " + destinationPathAndFile);
			System.exit(1);
		}
	}
	
	
	public void getAndWriteString(String key) throws IOException {
		String s = key;
		try {
			s = rb.getString(key).trim();
		} catch (MissingResourceException e) {
			System.out.println("syntax.cfg: " + key + " not found");
			if (key.startsWith("distances.")) {
				String key2 = "meters." + key.substring(10);
				System.out.println("syntax.cfg: trying " + key2);
				try {
					s = rb.getString(key2).trim();
				} catch (MissingResourceException e2) {
					System.out.println("syntax.cfg: " + key2 + " not found");
				}
			}
		}
		dsi.writeUTF(s);
		if (key.endsWith(".sound")) {
			rememberSounds(s);
		}
		//System.out.println(key + ": " + s);
	}

	private void rememberSounds(String sequence) {
		String s[] = sequence.split("[;]");
		for (int i = 0; i < s.length; i++) {
			if (s[i].length() > 0 && !s[i].startsWith("%")) {
				if (!soundNames.contains(s[i])) {
					soundNames.add(s[i]);
					//System.out.println(s[i]);
				}
			}
		}
		
	}
	
	public Object[] getSoundNames() {
		rememberSounds("CONNECT;DISCONNECT;CAMERA_ALERT");
		return soundNames.toArray();
	}
	
}
	
	

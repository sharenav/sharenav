/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.routing;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
//#if polish.android
import de.enough.polish.android.midlet.MidletBridge;
import android.content.res.AssetManager;
import android.content.Context;
//#endif

import de.ueller.util.HelperRoutines;
import de.ueller.util.Logger;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.mapdata.QueueReader;
import de.ueller.gpsmid.ui.Trace;

import net.sourceforge.util.zip.ZipFile;

import de.enough.polish.util.Locale;

public class RouteSyntax {
	private final static byte SYNTAX_FORMAT_VERSION = 1;
	private static Logger logger;
	
	private class SyntaxInstructionTypes {
		final static int simpleDirection = 0;
		final static int beardir = 1;
		final static int uturn = 2;
		final static int roundabout = 3;
		final static int enterMotorway = 4;
		final static int bearDirAndEnterMotorway = 5;
		final static int leaveMotorway = 6;
		final static int bearDirAndLeaveMotorway = 7;
		final static int intoTunnel = 8;
		final static int outOfTunnel = 9;
		final static int areaCross = 10;
		final static int areaCrossed = 11;
		final static int destReached = 12;
		final static int count = 13;
	}

	private class SyntaxTemplateComponents {
		final static int there = 0;
		final static int prepare = 1;
		final static int in = 2;
		final static int then = 3;
		final static int startOfTextComponents = 4;
		final static int thereText = 4;
		final static int inText = 5;
	}

		
	/**  */
	class SyntaxTemplate {
		public String thereSyntax;
		public String prepareSyntax;
		public String inSyntax;
		public String thenSyntax;
		public String thereTextSyntax;
		public String inTextSyntax;

		public SyntaxTemplate(	String thereSyntax, String prepareSyntax,
								String inSyntax, String thenSyntax,
								String thereTextSyntax,	String inTextSyntax
		) {
			this.thereSyntax = thereSyntax;
			this.prepareSyntax = prepareSyntax;
			this.inSyntax = inSyntax;
			this.thenSyntax = thenSyntax;
			this.thereTextSyntax = thereTextSyntax;
			this.inTextSyntax = inTextSyntax;
			// System.out.println("new: " + thereSyntax + "/" + prepareSyntax + "/" + inSyntax + "/" + thenSyntax + "/" + thereTextSyntax + "/" + inTextSyntax);
		}
		
		public String getComponent(int syntaxComponent) {
			switch(syntaxComponent) {
				case SyntaxTemplateComponents.there: return this.thereSyntax;
				case SyntaxTemplateComponents.prepare: return this.prepareSyntax;
				case SyntaxTemplateComponents.in: return this.inSyntax;
				case SyntaxTemplateComponents.then: return this.thenSyntax;
				case SyntaxTemplateComponents.thereText: return this.thereTextSyntax;			
				case SyntaxTemplateComponents.inText: return this.inTextSyntax;			
			}
			return "UnknownComponent";
		}
	}
	
	private static String [] simpleDirectionTexts;
	private static String [] bearDirectionTexts;
	private static String [] roundAboutExitTexts;
	private static String checkDirectionText;

	private static String [] simpleDirectionSounds;
	private static String [] bearDirectionSounds;
	private static String [] roundAboutExitSounds;
	private static String [] distancesSounds;
	private static String soonSound; 
	private static String againSound; 
	private static String metersSound = "METERS"; 
	private static String yardsSound = "YARDS"; 
	private static String followStreetSound; 
	private static String checkDirectionSound; 
	private static String recalculationSound; 
	private static String speedLimitSound; 
	
	private static SyntaxTemplate [] syntaxTemplates = new SyntaxTemplate[SyntaxInstructionTypes.count];

	private static RouteSyntax instance = null;
	
	private static boolean routeSyntaxAvailable;
	
	public RouteSyntax() {
		logger = Logger.getInstance(RouteSyntax.class, Logger.DEBUG);
		readSyntax();
		instance = this;
	}
	
	public static RouteSyntax getInstance() {
		if (instance == null) {
			instance = new RouteSyntax();
		}
		return instance;
	}
	
	public boolean readSyntax() {
		routeSyntaxAvailable = false;
		int i;
		String syntaxDat = null;
		if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
			syntaxDat = "/" + Configuration.getSoundDirectory() + "/syntax.dat";
		} else {
			if (Configuration.getMapUrl().endsWith("/")) {
				syntaxDat = Configuration.getMapUrl() + Configuration.getSoundDirectory() + "/syntax.dat";
			} else {
				syntaxDat = Configuration.getSoundDirectory() + "/syntax.dat";
			}
		}
		try {
			InputStream is = null;
			if (Configuration.usingBuiltinMap() || Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS)) {
				//#if polish.android
				// for builtin maps, open as asset from bundle
				is = MidletBridge.instance.getResources().getAssets().open(syntaxDat.substring(1));
				//#else
				// for builtin maps, open from bundle
				is = getClass().getResourceAsStream(syntaxDat);
				//#endif
			}
			else {
				//#if polish.api.fileconnection
				// either in map dir or map bundle
				if (Configuration.getMapUrl().endsWith("/")) {
					// map dir
					FileConnection fc = (FileConnection) Connector.open(syntaxDat, Connector.READ);
					is = fc.openInputStream();
				} else {
					// zip map 
					if (Configuration.mapZipFile == null) {
						Configuration.mapZipFile = new ZipFile(Configuration.getMapUrl(), -1);
					}
					if (Configuration.zipFileIsApk) {
						syntaxDat = "assets/" + syntaxDat;
					}

					is = Configuration.mapZipFile.getInputStream(Configuration.mapZipFile.getEntry(syntaxDat));
				}
				//#else
				//This should never happen.
				is = null;
				logger.fatal(Locale.get("routesyntax.ErrorFS")/*Error, we do not have access to the filesystem, but our syntax data is supposed to be there!*/);
				//#endif

			}
			if (is == null) {
				logger.error(Locale.get("routesyntax.ErrorOpening")/*Error opening */ + syntaxDat);
				return false;							
			}

			DataInputStream dis = new DataInputStream(is);
			if (dis.readByte() != SYNTAX_FORMAT_VERSION) {
				logger.error(syntaxDat + Locale.get("routesyntax.corrupt")/* corrupt*/);
				return false;			
			}			
			
			simpleDirectionTexts = new String[8];
			simpleDirectionSounds = new String[simpleDirectionTexts.length];
			for (i = 0; i < simpleDirectionTexts.length; i++) {
				simpleDirectionTexts[i] = dis.readUTF();
				simpleDirectionSounds[i] = dis.readUTF();
			}

			bearDirectionTexts = new String[2];
			bearDirectionSounds = new String[bearDirectionTexts.length];
			for (i = 0; i < bearDirectionTexts.length; i++) {
				bearDirectionTexts[i] = dis.readUTF();
				bearDirectionSounds[i] = dis.readUTF();
			}
			
			roundAboutExitTexts = new String[6];
			roundAboutExitSounds = new String[roundAboutExitTexts.length];
			for (i = 0; i < roundAboutExitTexts.length; i++) {
				roundAboutExitTexts[i] = dis.readUTF();
				roundAboutExitSounds[i] = dis.readUTF();
			}
			
			distancesSounds = new String[8];
			for (i = 0; i < distancesSounds.length; i++) {
				distancesSounds[i] = dis.readUTF();
			}
						
			for (i = 0; i < SyntaxInstructionTypes.count; i++) {
				syntaxTemplates[i] = 
					new SyntaxTemplate(	dis.readUTF(), dis.readUTF(),
										dis.readUTF(), dis.readUTF(),
										dis.readUTF(), dis.readUTF()
					);
			}
			
			soonSound = dis.readUTF();
			againSound = dis.readUTF();
			if (Legend.enableMap67Sounds) {
				metersSound = dis.readUTF();
				yardsSound = dis.readUTF();
			}
			checkDirectionText = dis.readUTF();
			checkDirectionSound = dis.readUTF();
			followStreetSound = dis.readUTF();
			speedLimitSound = dis.readUTF();
			recalculationSound = dis.readUTF();

			if (dis.readShort() != 0x3550) {
				logger.error(syntaxDat + Locale.get("routesyntax.corrupt")/* corrupt*/);
				return false;
			}
			
		} catch (IOException ioe) {
			logger.error(Locale.get("routesyntax.ErrorReading")/*error reading */ + syntaxDat);
			return false;			
		}
		System.out.println(syntaxDat + " read successfully");
		routeSyntaxAvailable = true;
		return true;
	}
	
	
	public static String getSyntaxTemplate(int instruction, int syntaxComponent) {
		String returnString = "";
		if (!routeSyntaxAvailable) {
			return returnString;
		}
		if (instruction <= RouteInstructions.RI_HARD_LEFT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%direction%", simpleDirectionTexts[instruction]);
			} else {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%DIRECTION%", simpleDirectionSounds[instruction]);					
			}
		} else if (instruction == RouteInstructions.RI_BEAR_LEFT || instruction == RouteInstructions.RI_BEAR_RIGHT
					|| instruction == RouteInstructions.RI_BEAR_LEFT_ENTER_MOTORWAY || instruction == RouteInstructions.RI_BEAR_RIGHT_ENTER_MOTORWAY
					|| instruction == RouteInstructions.RI_BEAR_LEFT_LEAVE_MOTORWAY || instruction == RouteInstructions.RI_BEAR_RIGHT_LEAVE_MOTORWAY
					) {
			int instType;
			int baseInst;
			switch (instruction) {
				case RouteInstructions.RI_BEAR_LEFT_ENTER_MOTORWAY:
				case RouteInstructions.RI_BEAR_RIGHT_ENTER_MOTORWAY:
					baseInst = RouteInstructions.RI_BEAR_RIGHT_ENTER_MOTORWAY;
					instType = SyntaxInstructionTypes.bearDirAndEnterMotorway; break;
				case RouteInstructions.RI_BEAR_LEFT_LEAVE_MOTORWAY:
				case RouteInstructions.RI_BEAR_RIGHT_LEAVE_MOTORWAY:
					baseInst = RouteInstructions.RI_BEAR_RIGHT_LEAVE_MOTORWAY;
					instType = SyntaxInstructionTypes.bearDirAndLeaveMotorway;
					break;
				default:
					baseInst = RouteInstructions.RI_BEAR_RIGHT;
					instType = SyntaxInstructionTypes.beardir;
			}
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[instType].getComponent(syntaxComponent), "%bear_dir%", bearDirectionTexts[instruction - baseInst]);
			} else {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[instType].getComponent(syntaxComponent), "%BEAR_DIR%", bearDirectionSounds[instruction - baseInst]);				
			}
		} else if (instruction >= RouteInstructions.RI_1ST_EXIT && instruction <= RouteInstructions.RI_6TH_EXIT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%exit%", roundAboutExitTexts[instruction - RouteInstructions.RI_1ST_EXIT]);
			} else {
				returnString = HelperRoutines.replaceAll(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%EXIT%", roundAboutExitSounds[instruction - RouteInstructions.RI_1ST_EXIT]);				
			}
		} else {
			switch (instruction) {
				case RouteInstructions.RI_UTURN: returnString = syntaxTemplates[SyntaxInstructionTypes.uturn].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_ENTER_MOTORWAY: returnString = syntaxTemplates[SyntaxInstructionTypes.enterMotorway].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_LEAVE_MOTORWAY: returnString = syntaxTemplates[SyntaxInstructionTypes.leaveMotorway].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_AREA_CROSS: returnString = syntaxTemplates[SyntaxInstructionTypes.areaCross].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_AREA_CROSSED: returnString = syntaxTemplates[SyntaxInstructionTypes.areaCrossed].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_INTO_TUNNEL: returnString = syntaxTemplates[SyntaxInstructionTypes.intoTunnel].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_OUT_OF_TUNNEL: returnString = syntaxTemplates[SyntaxInstructionTypes.outOfTunnel].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_DEST_REACHED: returnString = syntaxTemplates[SyntaxInstructionTypes.destReached].getComponent(syntaxComponent); break;
			}
		}				
		return returnString;		
	}
	
	
	public static String getSoundInstruction(int instruction) {
		return getSyntaxTemplate(instruction, SyntaxTemplateComponents.there);
	}

	
	public static String getTextInstruction(int instruction) {
		return getSyntaxTemplate(instruction, SyntaxTemplateComponents.thereText);
	}
	
	
	public static String getSoundInstructionPrepare(int instruction) {
		return getSyntaxTemplate(instruction, SyntaxTemplateComponents.prepare);
	}
	
	public static String getSoundInstructionIn(int instruction, int inDistance) {
		if(!routeSyntaxAvailable) {
			return "";
		}
		String s = HelperRoutines.replaceAll( getSyntaxTemplate(instruction, SyntaxTemplateComponents.in) , "%METERS%", distancesSounds[(inDistance / 100) - 1]);
		s = HelperRoutines.replaceAll(s, "%UNIT%", (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC) ? metersSound : yardsSound));
		return HelperRoutines.replaceAll(s, "%DISTANCE%", distancesSounds[(inDistance / 100) - 1]);
	}

	public static String getTextInstructionIn(int instruction, int inDistance) {
		String s = HelperRoutines.replaceAll( getSyntaxTemplate(instruction, SyntaxTemplateComponents.inText), "%meters%m", Trace.showDistance(inDistance, Trace.DISTANCE_GENERIC));
		return HelperRoutines.replaceAll(s, "%distance%", Trace.showDistance(inDistance, Trace.DISTANCE_GENERIC));
	}

	public static String getSoundInstructionThen(int instructionThen, boolean soon, boolean again) {
		String returnString = getSyntaxTemplate(instructionThen, SyntaxTemplateComponents.then);
		
		String soonReplacement = "";
		if (soon) {
			soonReplacement = soonSound;
		}
		returnString = HelperRoutines.replaceAll(returnString, "%SOON%", soonReplacement);			

		String againReplacement = "";
		if (again) {
			againReplacement = againSound;
		}
		returnString = HelperRoutines.replaceAll(returnString, "%AGAIN%", againReplacement);			

		while (returnString.indexOf(";;") != -1) {
			System.out.println(returnString);
			returnString = HelperRoutines.replaceAll(returnString, ";;", ";");
		}
		
		return returnString;
	}
		

	public static String getCheckDirectionText() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return checkDirectionText;
	}
	
	public static String getFollowStreetSound() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return followStreetSound;
	}

	public static String getCheckDirectionSound() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return checkDirectionSound;
	}

	public static String getRecalculationSound() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return recalculationSound;
	}

	public static String getSpeedLimitSound() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return speedLimitSound;
	}
	
	public static String getDestReachedSound() {
		if (!routeSyntaxAvailable) {
			return "";
		}
		return getSoundInstruction(RouteInstructions.RI_DEST_REACHED);
	}	

}

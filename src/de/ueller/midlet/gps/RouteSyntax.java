/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.StringTools;
import de.ueller.gpsMid.mapData.QueueReader;


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
	private static String [] metersSounds;
	private static String soonSound; 
	private static String againSound; 
	private static String followStreetSound; 
	private static String checkDirectionSound; 
	private static String recalculationSound; 
	private static String speedLimitSound; 
	
	private static SyntaxTemplate [] syntaxTemplates = new SyntaxTemplate[SyntaxInstructionTypes.count];

	private static RouteSyntax instance = null;
	
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
		int i;
		//#if polish.android
		String syntaxDat = Configuration.getMapUrl() + Configuration.getSoundDirectory() + "/syntax.dat";
		//#else
		String syntaxDat = "/" + Configuration.getSoundDirectory() + "/syntax.dat";
		//#endif
		try {
			//#if polish.android
			FileConnection fc = (FileConnection) Connector.open(syntaxDat, Connector.READ);
			InputStream is = fc.openInputStream();
			//#else
			InputStream is = QueueReader.class.getResourceAsStream(syntaxDat);
			//#endif
			DataInputStream dis = new DataInputStream(is);
			if (dis.readByte() != SYNTAX_FORMAT_VERSION) {
				logger.error(syntaxDat + " corrupt");
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
			
			metersSounds = new String[8];
			for (i = 0; i < metersSounds.length; i++) {
				metersSounds[i] = dis.readUTF();
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
			checkDirectionText = dis.readUTF();
			checkDirectionSound = dis.readUTF();
			followStreetSound = dis.readUTF();
			speedLimitSound = dis.readUTF();
			recalculationSound = dis.readUTF();

			if (dis.readShort() != 0x3550) {
				logger.error(syntaxDat + " corrupt");
				return false;
			}
			
		} catch (IOException ioe) {
			logger.error("error reading " + syntaxDat);
			return false;			
		}
		System.out.println(syntaxDat + " read successfully");
		return true;
	}
	
	
	public static String getSyntaxTemplate(int instruction, int syntaxComponent) {
		String returnString = "";
		if (instruction <= RouteInstructions.RI_HARD_LEFT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%direction%", simpleDirectionTexts[instruction]);
			} else {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%DIRECTION%", simpleDirectionSounds[instruction]);					
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
				returnString = StringTools.replace(syntaxTemplates[instType].getComponent(syntaxComponent), "%bear_dir%", bearDirectionTexts[instruction - baseInst]);
			} else {
				returnString = StringTools.replace(syntaxTemplates[instType].getComponent(syntaxComponent), "%BEAR_DIR%", bearDirectionSounds[instruction - baseInst]);				
			}
		} else if (instruction >= RouteInstructions.RI_1ST_EXIT && instruction <= RouteInstructions.RI_6TH_EXIT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%exit%", roundAboutExitTexts[instruction - RouteInstructions.RI_1ST_EXIT]);
			} else {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%EXIT%", roundAboutExitSounds[instruction - RouteInstructions.RI_1ST_EXIT]);				
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
		return StringTools.replace( getSyntaxTemplate(instruction, SyntaxTemplateComponents.in) , "%METERS%", metersSounds[(inDistance / 100) - 1]);
	}

	public static String getTextInstructionIn(int instruction, int inDistance) {
		return StringTools.replace( getSyntaxTemplate(instruction, SyntaxTemplateComponents.inText) , "%meters%", "" + inDistance);
	}

	public static String getSoundInstructionThen(int instructionThen, boolean soon, boolean again) {
		String returnString = getSyntaxTemplate(instructionThen, SyntaxTemplateComponents.then);
		
		String soonReplacement = "";
		if (soon) {
			soonReplacement = soonSound;
		}
		returnString = StringTools.replace(returnString, "%SOON%", soonReplacement);			

		String againReplacement = "";
		if (again) {
			againReplacement = againSound;
		}
		returnString = StringTools.replace(returnString, "%AGAIN%", againReplacement);			

		while (returnString.indexOf(";;") != -1) {
			System.out.println(returnString);
			returnString = StringTools.replace(returnString, ";;", ";");
		}
		
		return returnString;
	}
		

	public static String getCheckDirectionText() {
		return checkDirectionText;
	}
	
	public static String getFollowStreetSound() {
		return followStreetSound;
	}

	public static String getCheckDirectionSound() {
		return checkDirectionSound;
	}

	public static String getRecalculationSound() {
		return recalculationSound;
	}

	public static String getSpeedLimitSound() {
		return speedLimitSound;
	}
	
	public static String getDestReachedSound() {
		return getSoundInstruction(RouteInstructions.RI_DEST_REACHED);
	}	

}

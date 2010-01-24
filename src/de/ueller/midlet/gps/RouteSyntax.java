/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.StringTools;


public class RouteSyntax {
	
	private class SyntaxInstructionTypes {
		final static int simpleDirection = 0;
		final static int uturn = 1;
		final static int roundabout = 2;
		final static int enterMotorway = 3;
		final static int bearAndEnterMotorway = 4;
		final static int leaveMotorway = 5;
		final static int bearAndLeaveMotorway = 6;
		final static int intoTunnel = 7;
		final static int outOfTunnel = 8;
		final static int areaCross = 9;
		final static int areaCrossed = 10;
		final static int destReached = 11;
		final static int count = 12;
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
	private static String [] roundAboutExitTexts;
	private static String [] bearDirTexts;
	private static String checkDirectionText;

	private static String [] simpleDirectionVoices;
	private static String [] roundAboutExitVoices;
	private static String [] metersVoices;
	private static String [] bearDirVoices;
	private static String soonVoice; 
	private static String againVoice; 
	private static String followStreetVoice; 
	private static String checkDirectionVoice; 
	private static String recalculationVoice; 
	private static String speedLimitVoice; 
	
	private static SyntaxTemplate [] syntaxTemplates = new SyntaxTemplate[SyntaxInstructionTypes.count];

	private static RouteSyntax instance = null;
	private static String currentLanguage = "";
	
	public RouteSyntax() {
		String language = Configuration.getLanguage();
		if (language.equalsIgnoreCase("DE")) {
			initializeDE();
		} else {
			initializeEN();	
		}
		currentLanguage = language;
		instance = this;
	}
	
	public static RouteSyntax getInstance() {
		if (instance == null || !currentLanguage.equalsIgnoreCase(Configuration.getLanguage())) {
			instance = new RouteSyntax();
		}
		return instance;
	}
	
	public void initializeEN() {
		simpleDirectionTexts = new String[] { "mark",
				"hard right", "right", "half right",
				"bear right", "straight on", "bear left",
				"half left", "left", "hard left" };

		roundAboutExitTexts = new String[] { "1", "2", "3", "4", "5", "6" };

		bearDirTexts = new String[] { "b.left", "b.right" };
		
		simpleDirectionVoices = new String[] { "",
			"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
			"BEAR;RIGHT", "STRAIGHTON", "BEAR;LEFT",
			"HALF;LEFT", "LEFT", "HARD;LEFT" };

		roundAboutExitVoices = new String[] { "1ST", "2ND", "3RD", "4TH", "5TH", "6TH" };
		
		metersVoices  = new String[] { "100", "200", "300", "400", "500", "600", "700", "800" };

		bearDirVoices  = new String[] { "BEAR;LEFT", "BEAR;RIGHT" };
		
		soonVoice = "SOON"; 

		againVoice = "AGAIN"; 
		
		syntaxTemplates[SyntaxInstructionTypes.simpleDirection] = 
			new SyntaxTemplate(	"%DIRECTION%", "PREPARE;%DIRECTION%",
								"IN;%METERS%;METERS;%DIRECTION%", ";THEN;%SOON%;%DIRECTION%;%AGAIN%",
						   		"%direction%", "%direction% in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.uturn] = 
			new SyntaxTemplate(	"UTURN", "PREPARE;UTURN",
								"IN;%METERS%;METERS;UTURN", ";THEN;%SOON%;UTURN;%AGAIN%",
						   		"u-turn", "u-turn in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.roundabout] = 
			new SyntaxTemplate(	"", "RAB;%EXIT%;RABEXIT",
								"IN;%METERS%;METERS;RAB;%EXIT%;RABEXIT", ";THEN;%SOON%;RAB;%EXIT%;RABEXIT",
						   		"r.about exit %exit%", "r.about exit %exit% in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.enterMotorway] = 
			new SyntaxTemplate(	"ENTER_MOTORWAY", "PREPARE;TO;ENTER_MOTORWAY",
								"IN;%METERS%;METERS;ENTER_MOTORWAY", ";THEN;%SOON%;ENTER_MOTORWAY",
						   		"enter the motorway", "enter motorway in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.bearAndEnterMotorway] = 
			new SyntaxTemplate(	"%BEAR_DIR%;TO;ENTER_MOTORWAY", "PREPARE;TO;%BEAR_DIR%;TO;ENTER_MOTORWAY",
								"IN;%METERS%;METERS;%BEAR_DIR%;TO;ENTER_MOTORWAY", ";THEN;%SOON%;%BEAR_DIR%;TO;ENTER_MOTORWAY",
						   		"%bear_dir% enter the motorway", "%bear_dir% enter motorway in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.leaveMotorway] = 
			new SyntaxTemplate(	"LEAVE_MOTORWAY", "PREPARE;TO;LEAVE_MOTORWAY",
								"IN;%METERS%;METERS;LEAVE_MOTORWAY", ";THEN;%SOON%;LEAVE_MOTORWAY",
						   		"leave the motorway", "leave motorway in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.bearAndLeaveMotorway] = 
			new SyntaxTemplate(	"%BEAR_DIR%;TO;LEAVE_MOTORWAY", "PREPARE;TO;%BEAR_DIR%;TO;LEAVE_MOTORWAY",
								"IN;%METERS%;METERS;%BEAR_DIR%;TO;LEAVE_MOTORWAY", ";THEN;%SOON%;%BEAR_DIR%;TO;LEAVE_MOTORWAY",
						   		"%bear_dir% leave motorway", "%bear_dir% leave motorway in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.areaCross] = 
			new SyntaxTemplate(	"AREA_CROSS", "PREPARE;TO;AREA_CROSS",
								"IN;%METERS%;METERS;AREA_CROSS", ";THEN;%SOON%;AREA_CROSS",
						   		"cross area", "cross area in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.areaCrossed] = 
			new SyntaxTemplate(	"AREA_CROSSED", "PREPARE;TO;AREA_CROSSED",
								"IN;%METERS%;METERS;AREA_CROSSED", ";THEN;%SOON%;AREA_CROSSED",
						   		"area crossed", "area crossed in %meters%m"
			);
		
		syntaxTemplates[SyntaxInstructionTypes.intoTunnel] = 
			new SyntaxTemplate(	"INTO_TUNNEL", "",
								"IN;%METERS%;METERS;INTO_TUNNEL", ";THEN;%SOON%;INTO_TUNNEL",
						   		"into tunnel", "into tunnel in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.outOfTunnel] = 
			new SyntaxTemplate(	"OUT_OF_TUNNEL", "",
								"IN;%METERS%;METERS;OUT_OF_TUNNEL", ";THEN;%SOON%;OUT_OF_TUNNEL",
						   		"out of tunnel", "out of tunnel in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.destReached] = 
			new SyntaxTemplate(	"DEST_REACHED", "PREPARE;DEST_REACHED",
								"IN;%METERS%;METERS;DEST_REACHED", ";THEN;%SOON%;DEST_REACHED",
						   		"At destination", "At destination in %meters%m"
			);


		checkDirectionText = "check direction";

		followStreetVoice = "FOLLOW_STREET"; 
		checkDirectionVoice = "CHECK_DIRECTION";
		recalculationVoice = "ROUTE_RECALCULATION"; 
		speedLimitVoice = "SPEED_LIMIT"; 		
	}

	public void initializeDE() {
		simpleDirectionTexts = new String[] { "mark",
				"scharf rechts", "rechts", "halb rechts",
				"rechts halten", "geradeaus", "links halten",
				"halb links", "links", "scharf links" };

		roundAboutExitTexts = new String[] { "1", "2", "3", "4", "5", "6" };

		bearDirTexts = new String[] { "li.", "re." };
		
		simpleDirectionVoices = new String[] { "",
			"DE_SCHARF;DE_RECHTS", "DE_RECHTS", "DE_HALB;DE_RECHTS",
			"DE_RECHTS;DE_HALTEN", "DE_GERADEAUS", "DE_LINKS;DE_HALTEN",
			"DE_HALB;DE_LINKS", "DE_LINKS", "DE_SCHARF;DE_LINKS" };

		roundAboutExitVoices = new String[] { "DE_1TE", "DE_2TE", "DE_3TE", "DE_4TE", "DE_5TE", "DE_6TE" };
		
		metersVoices  = new String[] { "DE_100", "DE_200", "DE_300", "DE_400", "DE_500", "DE_600", "DE_700", "DE_800" };

		bearDirVoices  = new String[] { "DE_LINKS;DE_HALTEN", "DE_RECHTS;DE_HALTEN" };
		
		soonVoice = "DE_BALD"; 

		againVoice = "DE_NOCHMAL"; 
		
		syntaxTemplates[SyntaxInstructionTypes.simpleDirection] = 
			new SyntaxTemplate(	"%DIRECTION%", "DE_GLEICH;%DIRECTION%",
								"DE_IN;%METERS%;DE_METERN;%DIRECTION%", ";DE_DANN;%SOON%;%AGAIN%;%DIRECTION%",
						   		"%direction%", "%direction% in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.uturn] = 
			new SyntaxTemplate(	"DE_BITTE;DE_WENDEN", "DE_GLEICH;DE_BITTE;DE_WENDEN",
								"DE_IN;%METERS%;DE_METERN;DE_BITTE;DE_WENDEN", ";DE_DANN;%SOON%;%AGAIN%;WENDEN",
						   		"wenden", "wenden in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.roundabout] = 
			new SyntaxTemplate(	"", "DE_IM_KREISEL;DE_NIMM;DE_DIE;%EXIT%;DE_KREISEL_ABFAHRT",
								"DE_IN;%METERS%;DE_METERN;DE_NIMM;DE_IM_KREISEL;DE_DIE;%EXIT%;DE_KREISEL_ABFAHRT", ";DANN;NIMM;IM_KREISEL;%SOON%;DE_DIE;%EXIT%;DE_KREISEL_ABFAHRT",
						   		"Kreisausfahrt #%exit%", "Kreisausfahrt #%exit% in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.enterMotorway] = 
			new SyntaxTemplate(	"DE_AUF_AUTOBAHN", "DE_GLEICH;DE_AUF_AUTOBAHN",
								"DE_IN;%METERS%;DE_METERN;DE_AUF_AUTOBAHN", ";DE_DANN;%SOON%;DE_AUF_AUTOBAHN",
						   		"auf Autobahn", "auf Autobahn in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.bearAndEnterMotorway] = 
			new SyntaxTemplate(	"%BEAR_DIR%;DE_ZUM;DE_AUF_AUTOBAHN", "DE_GLEICH;%BEAR_DIR%;DE_ZUM;DE_AUF_AUTOBAHN",
								"DE_IN;%METERS%;DE_METERN;%BEAR_DIR%;DE_ZUM;DE_AUF_AUTOBAHN", ";DE_DANN;%SOON%;%BEAR_DIR%;DE_ZUM;DE_AUF_AUTOBAHN",
						   		"%bear_dir% auf Autobahn", "%bear_dir% auf Autobahn in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.leaveMotorway] = 
			new SyntaxTemplate(	"DE_DIE;DE_AUTOBAHN_VERLASSEN", "DE_GLEICH;DE_DIE;DE_AUTOBAHN_VERLASSEN",
								"DE_IN;%METERS%;DE_METERN;DE_DIE;DE_AUTOBAHN_VERLASSEN", ";DE_DANN;%SOON%;DE_DIE;DE_AUTOBAHN_VERLASSEN",
						   		"Autobahn verlassen", "Autobahn verlassen in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.bearAndLeaveMotorway] = 
			new SyntaxTemplate(	"%BEAR_DIR%;ZUM;AUTOBAHN_VERLASSEN", "DE_GLEICH;%BEAR_DIR%;DE_ZUM;DE_AUTOBAHN_VERLASSEN",
								"DE_IN;%METERS%;DE_METERN;%BEAR_DIR%;DE_ZUM;DE_AUTOBAHN_VERLASSEN", ";DE_DANN;%SOON%;%BEAR_DIR%;DE_ZUM;DE_AUTOBAHN_VERLASSEN",
						   		"%bear_dir% Autobahn verlassen", "%bear_dir% Autobahn verlassen in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.areaCross] = 
			new SyntaxTemplate(	"DE_UEBER_PLATZ", "DE_GLEICH;DE_UEBER_PLATZ",
								"DE_IN;%METERS%;DE_METERN;DE_UEBER_PLATZ", ";DE_DANN;%SOON%;DE_UEBER_PLATZ",
						   		"über den Platz", "über den Platz in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.areaCrossed] = 
			new SyntaxTemplate(	"DE_PLATZ_VERLASSEN", "DE_GLEICH;DE_PLATZ_VERLASSEN",
								"DE_IN;%METERS%;DE_METERN;DE_PLATZ_VERLASSEN", ";DE_DANN;%SOON%;DE_PLATZ_VERLASSEN",
						   		"Platz verlassen", "Platz verlassen in %meters%m"
			);
		
		syntaxTemplates[SyntaxInstructionTypes.intoTunnel] = 
			new SyntaxTemplate(	"DE_IN_DEN_TUNNEL", "",
								"DE_IN;%METERS%;DE_METERN;DE_IN_DEN_TUNNEL", ";DE_DANN;%SOON%;DE_IN_DEN_TUNNEL",
						   		"in den Tunnel", "in den Tunnel in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.outOfTunnel] = 
			new SyntaxTemplate(	"DE_AUS_DEM_TUNNEL", "",
								"DE_IN;%METERS%;DE_METERN;DE_AUS_DEM_TUNNEL", ";DE_DANN;%SOON%;DE_AUS_DEM_TUNNEL",
						   		"aus dem Tunnel", "aus dem Tunnel in %meters%m"
			);

		syntaxTemplates[SyntaxInstructionTypes.destReached] = 
			new SyntaxTemplate(	"DE_AM_ZIEL", "DE_GLEICH;DE_AM_ZIEL",
								"DE_IN;%METERS%;DE_METERN;DE_AM_ZIEL", ";DE_DANN;%SOON%;DE_AM_ZIEL",
						   		"Am Ziel", "Noch %meters%m zum Ziel"
			);

		checkDirectionText = "Richtung korrekt?";
		
		followStreetVoice = "DE_FOLGE_DER_STRASSE"; 
		checkDirectionVoice = "DE_RICHTUNG_PRUEFEN";
		recalculationVoice = "DE_NEUE_ROUTE"; 
		speedLimitVoice = "DE_GESCHWINDIGKEIT"; 
	}

	
	public static String getSyntaxTemplate(int instruction, int syntaxComponent) {
		String returnString = "";
		if (instruction <= RouteInstructions.RI_HARD_LEFT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%direction%", simpleDirectionTexts[instruction]);
			} else {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.simpleDirection].getComponent(syntaxComponent), "%DIRECTION%", simpleDirectionVoices[instruction]);					
			}
		} else if (instruction >= RouteInstructions.RI_1ST_EXIT && instruction <= RouteInstructions.RI_6TH_EXIT) {
			if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%exit%", roundAboutExitTexts[instruction - RouteInstructions.RI_1ST_EXIT]);
			} else {
				returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.roundabout].getComponent(syntaxComponent), "%EXIT%", roundAboutExitVoices[instruction - RouteInstructions.RI_1ST_EXIT]);				
			}
		} else {
			switch (instruction) {
				case RouteInstructions.RI_UTURN: returnString = syntaxTemplates[SyntaxInstructionTypes.uturn].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_ENTER_MOTORWAY: returnString = syntaxTemplates[SyntaxInstructionTypes.enterMotorway].getComponent(syntaxComponent); break;
				case RouteInstructions.RI_BEAR_LEFT_ENTER_MOTORWAY:
				case RouteInstructions.RI_BEAR_RIGHT_ENTER_MOTORWAY:
					if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
						returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.bearAndEnterMotorway].getComponent(syntaxComponent), "%bear_dir%", bearDirTexts[instruction - RouteInstructions.RI_BEAR_LEFT_ENTER_MOTORWAY]);
					} else {
						returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.bearAndEnterMotorway].getComponent(syntaxComponent), "%BEAR_DIR%", bearDirVoices[instruction - RouteInstructions.RI_BEAR_LEFT_ENTER_MOTORWAY]);						
					}
					break;
				case RouteInstructions.RI_BEAR_LEFT_LEAVE_MOTORWAY:
				case RouteInstructions.RI_BEAR_RIGHT_LEAVE_MOTORWAY:
					if (syntaxComponent >= SyntaxTemplateComponents.startOfTextComponents) {
						returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.bearAndLeaveMotorway].getComponent(syntaxComponent), "%bear_dir%", bearDirTexts[instruction - RouteInstructions.RI_BEAR_LEFT_LEAVE_MOTORWAY]);
					} else {
						returnString = StringTools.replace(syntaxTemplates[SyntaxInstructionTypes.bearAndLeaveMotorway].getComponent(syntaxComponent), "%BEAR_DIR%", bearDirVoices[instruction - RouteInstructions.RI_BEAR_LEFT_LEAVE_MOTORWAY]);						
					}
					break;
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
		return StringTools.replace( getSyntaxTemplate(instruction, SyntaxTemplateComponents.in) , "%METERS%", metersVoices[inDistance / 100]);
	}

	public static String getTextInstructionIn(int instruction, int inDistance) {
		return StringTools.replace( getSyntaxTemplate(instruction, SyntaxTemplateComponents.inText) , "%meters%", "" + inDistance);
	}

	public static String getSoundInstructionThen(int instructionThen, boolean soon, boolean again) {
		String returnString = getSyntaxTemplate(instructionThen, SyntaxTemplateComponents.then);
		
		String soonReplacement = "";
		if (soon) {
			soonReplacement = soonVoice;
		}
		returnString = StringTools.replace(returnString, "%SOON%", soonReplacement);			

		String againReplacement = "";
		if (again) {
			againReplacement = againVoice;
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
	
	public static String getFollowStreetVoice() {
		return followStreetVoice;
	}

	public static String getCheckDirectionVoice() {
		return checkDirectionVoice;
	}

	public static String getRecalculationVoice() {
		return recalculationVoice;
	}

	public static String getSpeedLimitVoice() {
		return speedLimitVoice;
	}
	
	public static String getDestReachedVoice() {
		return getSoundInstruction(RouteInstructions.RI_DEST_REACHED);
	}	

}

package de.ueller.gpsmid.ui;
/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

import javax.microedition.lcdui.*;

import java.io.IOException;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.midlet.util.ImageTools;
import de.ueller.util.Logger;
import de.enough.polish.util.Locale;


public class GuiRoute extends Form implements CommandListener, ItemCommandListener {
	private final static Logger logger = Logger.getInstance(GuiRoute.class,Logger.DEBUG);

	private static final Command CMD_SETUP_OK = new Command(Locale.get("generic.OK")/*Ok*/, GpsMidMenu.OK, 2);
	private static final Command CMD_SETUP_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, GpsMidMenu.BACK, 3);

	// for route settings popup; want the back key on Android to cancel
	private static final Command CMD_OK = new Command(Locale.get("generic.OK")/*Ok*/, Command.OK, 2);
	private static final Command CMD_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, Command.BACK, 3);
	
	private ChoiceGroup routingTravelModesGroup;
	private StringItem[] travelModeItems;
	private ImageItem[] travelModeImages;
	private Gauge gaugeRoutingEsatimationFac; 
	private ChoiceGroup routingTurnRestrictionsGroup;
	private ChoiceGroup routingTimeOrDistanceGroup;
	private ChoiceGroup continueMapWhileRouteing;
	private ChoiceGroup routingOptsGroup;
	private ChoiceGroup routingWarningOptsGroup;
	private ChoiceGroup routingAllowOptsGroup;
	private ChoiceGroup routingStrategyOptsGroup;
	private ChoiceGroup routingShowOptsGroup;
	private TextField  tfMainStreetNetDistanceKm;
	private TextField  tfMinRouteLineWidth;
	private TextField tfTrafficSignalCalcDelay;

	// other
	private GpsMidDisplayable parent;
	private boolean useAsSetupDialog;
	
	public GuiRoute(GpsMidDisplayable parent, boolean useAsSetupDialog) {
		super(Locale.get("guiroute.RouteToDestination")/*Route to destination*/);
		// Set up this Displayable to listen to command events
		this.parent = parent;

		setCommandListener(this);

		this.useAsSetupDialog = useAsSetupDialog;
		if (useAsSetupDialog) {
			addCommand(CMD_SETUP_OK);
			addCommand(CMD_SETUP_CANCEL);
			setTitle(Locale.get("guiroute.RoutingOptions")/*Routing Options*/);
		} else {
			addCommand(CMD_OK);
			addCommand(CMD_CANCEL);
		}

		String travelModes[] = new String[Legend.getTravelModes().length];
		for (int i=0; i<travelModes.length; i++) {
			travelModes[i]=Legend.getTravelModes()[i].travelModeName;
		}
		if (useAsSetupDialog
		    // FIXME consider if someone might want to use routing icons on other platforms too
		    //#if polish.android
		    //#else
		    || true
		    //#endif
			) {
			routingTravelModesGroup = new ChoiceGroup(Locale.get("guiroute.TravelBy")/*Travel by*/, Choice.EXCLUSIVE, travelModes, null);
			routingTravelModesGroup.setSelectedIndex(Configuration.getTravelModeNr(), true);
			append(routingTravelModesGroup);
		} else {
			travelModeItems = new StringItem[Legend.getTravelModes().length];
			travelModeImages = new ImageItem[Legend.getTravelModes().length];
			for (int i = 0; i < travelModes.length; i++) {
				travelModeItems[i] = new StringItem(i == 0 ? Locale.get("guiroute.TravelBy")/*Travel by*/ : "", travelModes[i], StringItem.BUTTON);
				// FIXME consider if someone might want to use routing icons on other platforms too
				//#if polish.android
				try {
					Image image = Image.createImage("/" + Configuration.getIconPrefix() + "r_" + travelModes[i] + ".png");

					float scale = 4 * image.getWidth() / this.getWidth();
					if (scale < 1.0f) {
						scale = 1;
					}

					travelModeImages[i] = new ImageItem(travelModes[i], 
									    ImageTools.scaleImage(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale)),
									    ImageItem.LAYOUT_RIGHT, travelModes[i]);
				} catch (IOException ioe) {
				}
				//#endif
				if (useAsSetupDialog) {
					travelModeItems[i].addCommand(CMD_SETUP_OK);
					travelModeItems[i].setDefaultCommand(CMD_SETUP_OK);
				} else {
					travelModeItems[i].addCommand(CMD_OK);
					travelModeItems[i].setDefaultCommand(CMD_OK);
				}
				travelModeItems[i].setItemCommandListener(this);
				//#style formItem
				append(travelModeItems[i]);
				if (travelModeImages[i] != null) {
					if (useAsSetupDialog) {
						travelModeImages[i].addCommand(CMD_SETUP_OK);
						travelModeImages[i].setDefaultCommand(CMD_SETUP_OK);
					} else {
						travelModeImages[i].addCommand(CMD_OK);
						travelModeImages[i].setDefaultCommand(CMD_OK);
					}
					travelModeImages[i].setItemCommandListener(this);
					//#style formItem
					append(travelModeImages[i]);
				}
			}
		}

		gaugeRoutingEsatimationFac=new Gauge(Locale.get("guiroute.AllowPoorRoutes") + "/" + Locale.get("guiroute.CalculationSpeed")/*Calculation speed*/, true, 10, Configuration.getRouteEstimationFac());
		append(gaugeRoutingEsatimationFac);

		String [] routingWarningOpts = new String[1];
		routingWarningOpts[0] = Locale.get("guiroute.SuppressRouteWarning")/*Älä varoita huonoista reiteistä*/;
		routingWarningOptsGroup = new ChoiceGroup(Locale.get("guiroute.RouteWarningSetting")/*Warning*/, Choice.MULTIPLE, routingWarningOpts, null);
		routingWarningOptsGroup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SUPPRESS_ROUTE_WARNING));
		append(routingWarningOptsGroup);

		String [] routingAim = new String[2];
		routingAim[0] = Locale.get("guiroute.time")/*"Shortest time"*/;
		routingAim[1] = Locale.get("guiroute.distance")/*Shortest distance*/;
		routingTimeOrDistanceGroup = new ChoiceGroup(Locale.get("guiroute.aim")/*Aim in routing*/, Choice.EXCLUSIVE, routingAim ,null);
		routingTimeOrDistanceGroup.setSelectedIndex( (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_AIM) ? 1 : 0), true);
		append(routingTimeOrDistanceGroup);

		String [] trStates = new String[2];
		trStates[0] = Locale.get("generic.On")/*On*/;
		trStates[1] = Locale.get("generic.Off")/*Off*/;
		routingTurnRestrictionsGroup = new ChoiceGroup(Locale.get("guiroute.TurnRestrictions")/*Turn restrictions*/, Choice.EXCLUSIVE, trStates ,null);
		routingTurnRestrictionsGroup.setSelectedIndex( (Configuration.getCfgBitSavedState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION) ? 0 : 1) ,true);
		append(routingTurnRestrictionsGroup);

		String [] routingAllowOpts = new String[2];
		routingAllowOpts[0] = Locale.get("guiroute.AllowMotorways")/*Motorways*/;
		routingAllowOpts[1] = Locale.get("guiroute.AllowTollRoads")/*TollRoads*/;
		routingAllowOptsGroup = new ChoiceGroup(Locale.get("guiroute.AllowOptsGroup")/*Allow*/, Choice.MULTIPLE, routingAllowOpts ,null);
		routingAllowOptsGroup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_USE_MOTORWAYS));
		routingAllowOptsGroup.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_USE_TOLLROADS));
		append(routingAllowOptsGroup);

		tfMainStreetNetDistanceKm = new TextField(Locale.get("guiroute.DistanceToMainStreet")/*Distance in km to main street net (used for large route distances):*/, Integer.toString(Configuration.getMainStreetDistanceKm()), 5, TextField.DECIMAL);
		append(tfMainStreetNetDistanceKm);

		String [] routingStrategyOpts = new String[3];
		boolean[] selRoutingStrategy = new boolean[3];
		routingStrategyOpts[0] = Locale.get("guiroute.LookForMotorways")/*Look for motorways*/; selRoutingStrategy[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY);
		routingStrategyOpts[1] = Locale.get("guiroute.BoostMotorways")/*Boost motorways*/; selRoutingStrategy[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS);
		routingStrategyOpts[2] = Locale.get("guiroute.BoostTrunksPrimarys")/*Boost trunks & primarys*/;
		selRoutingStrategy[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS);
		routingStrategyOptsGroup = new ChoiceGroup(Locale.get("guiroute.CalculationStrategies")/*Calculation strategies*/, Choice.MULTIPLE, routingStrategyOpts ,null);
		routingStrategyOptsGroup.setSelectedFlags(selRoutingStrategy);
		append(routingStrategyOptsGroup);

		if (useAsSetupDialog) {
			String [] routingShowOpts = new String[3];
			boolean[] selRoutingShow = new boolean[3];
			routingShowOpts[0] = Locale.get("guiroute.EstimatedDuration")/*Estimated duration*/; selRoutingShow[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ROUTE_DURATION_IN_MAP);
			routingShowOpts[1] = Locale.get("guiroute.ETA")/*ETA*/; selRoutingShow[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ETA_IN_MAP);
			routingShowOpts[2] = Locale.get("guiroute.OffsetToRouteLine")/*Offset to route line*/; selRoutingShow[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP);
			routingShowOptsGroup = new ChoiceGroup(Locale.get("guiroute.Infos")/*Infos in map screen*/, Choice.MULTIPLE, routingShowOpts ,null);
			routingShowOptsGroup.setSelectedFlags(selRoutingShow);
			append(routingShowOptsGroup);
	
			String [] routingBack = new String[3];
			routingBack[0] = Locale.get("guiroute.No")/*No*/;
			routingBack[1] = Locale.get("guiroute.AtCreation")/*At route line creation*/;
			routingBack[2] = Locale.get("guiroute.Yes")/*Yes*/;
			continueMapWhileRouteing = new ChoiceGroup(Locale.get("guiroute.ContinueMap")/*Continue map while calculation:*/, Choice.EXCLUSIVE, routingBack ,null);
			continueMapWhileRouteing.setSelectedIndex(Configuration.getContinueMapWhileRouteing(),true);
			append(continueMapWhileRouteing);
	
			
			tfMinRouteLineWidth = new TextField(Locale.get("guiroute.MinimumWidth")/*Minimum width of route line*/, Integer.toString(Configuration.getMinRouteLineWidth()), 1, TextField.DECIMAL);
			append(tfMinRouteLineWidth);
			
			String [] routingOpts = new String[7];
			boolean[] selRouting = new boolean[7];
			routingOpts[0] = Locale.get("guiroute.AutoRecalculation")/*Auto recalculation*/; selRouting[0]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_AUTO_RECALC);
			routingOpts[1] = Locale.get("guiroute.RouteBrowsing")/*Route browsing with up/down keys*/; selRouting[1]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BROWSING);
			routingOpts[2] = Locale.get("guiroute.HideQuietArrows")/*Hide quiet arrows*/; selRouting[2]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_HIDE_QUIET_ARROWS);
			routingOpts[3] = Locale.get("guiroute.AskForRoutingOptions")/*Ask for Routing Options*/; selRouting[3]=!Configuration.getCfgBitSavedState(Configuration.CFGBIT_DONT_ASK_FOR_ROUTING_OPTIONS);
			routingOpts[4] = Locale.get("guiroute.stopAtDest")/*Stop routing when at destination*/; selRouting[4]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_STOP_ROUTING_AT_DESTINATION);
			routingOpts[5] = Locale.get("guiroute.maparrows")/*Show in-map arrows*/; selRouting[5]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_NAVI_ARROWS_IN_MAP);
			routingOpts[6] = Locale.get("guiroute.bigarrows")/*Show big navigation arrows*/; selRouting[6]=Configuration.getCfgBitSavedState(Configuration.CFGBIT_NAVI_ARROWS_BIG);

			routingOptsGroup = new ChoiceGroup(Locale.get("guiroute.Other")/*Other*/, Choice.MULTIPLE, routingOpts ,null);
			routingOptsGroup.setSelectedFlags(selRouting);
			append(routingOptsGroup);
			
			tfTrafficSignalCalcDelay = new TextField(Locale.get("guiroute.SecondsDelayed")/*Seconds the examined route path gets delayed at traffic signals during calculation*/, Integer.toString(Configuration.getTrafficSignalCalcDelay()), 2, TextField.DECIMAL);
			append(tfTrafficSignalCalcDelay);
		}

	}

	public void commandAction(Command c, Item item) {
		// set travel mode
		// default to 0
		int match = 0;
		if (item != null) {
			for (int i = 0; i < Legend.getTravelModes().length; i++) {
				if (travelModeItems[i] == item) {
					match = i;
				}
				if (travelModeImages[i] == item) {
					match = i;
				}
				Configuration.setTravelMode(match);
			}
		}
		// forward item command action to form
		commandAction(c, (Displayable) null);
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_CANCEL || c == CMD_SETUP_CANCEL) {
			parent.show();
			return;
		}

		if (c == CMD_OK || c == CMD_SETUP_OK) {
			if (useAsSetupDialog
			    // FIXME consider if someone might want to use routing icons on other platforms too
			    //#if polish.android
			    //#else
			    || true
			    //#endif
				) {
				Configuration.setTravelMode(routingTravelModesGroup.getSelectedIndex());
			}
			// FIXME check if we need to drop connections when changing this
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_AIM, (routingTimeOrDistanceGroup.getSelectedIndex() == 1));			
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION, (routingTurnRestrictionsGroup.getSelectedIndex() == 0) );			
			Configuration.setRouteEstimationFac(gaugeRoutingEsatimationFac.getValue());

			String km=tfMainStreetNetDistanceKm.getString(); 
			try {
				Configuration.setMainStreetDistanceKm((int) (Float.parseFloat(km)));
			} catch (NumberFormatException e) {
			}

			Configuration.setCfgBitSavedState(Configuration.CFGBIT_SUPPRESS_ROUTE_WARNING, routingWarningOptsGroup.isSelected(0));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_USE_MOTORWAYS, routingAllowOptsGroup.isSelected(0));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_USE_TOLLROADS, routingAllowOptsGroup.isSelected(1));			
			
			boolean[] selStrategyRouting = new boolean[3];
			routingStrategyOptsGroup.getSelectedFlags(selStrategyRouting);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY, selStrategyRouting[0]);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS, selStrategyRouting[1]);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS, selStrategyRouting[2]);
		
			if (useAsSetupDialog) {
				boolean[] selShowRouting = new boolean[3];
				routingShowOptsGroup.getSelectedFlags(selShowRouting);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ROUTE_DURATION_IN_MAP, selShowRouting[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ETA_IN_MAP, selShowRouting[1]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP, selShowRouting[2]);
	
				Configuration.setContinueMapWhileRouteing(continueMapWhileRouteing.getSelectedIndex());
				
				String w=tfMinRouteLineWidth.getString(); 
				Configuration.setMinRouteLineWidth( 
						(int) (Float.parseFloat(w)) 
				); 
				
				boolean[] selRouting = new boolean[7];
				routingOptsGroup.getSelectedFlags(selRouting);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_AUTO_RECALC, selRouting[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BROWSING, selRouting[1]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_HIDE_QUIET_ARROWS, selRouting[2]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_DONT_ASK_FOR_ROUTING_OPTIONS, !selRouting[3]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_STOP_ROUTING_AT_DESTINATION, selRouting[4]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_NAVI_ARROWS_IN_MAP, selRouting[5]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_NAVI_ARROWS_BIG, selRouting[6]);

				String s=tfTrafficSignalCalcDelay.getString(); 
				Configuration.setTrafficSignalCalcDelay( 
						(int) (Integer.parseInt(s)) 
				); 
			
			} else {
				Trace.getInstance().performIconAction(Trace.ROUTING_START_CMD, null);
				parent.show();
				return;
			}
			parent.show();
			return;
		}
	}
	
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}

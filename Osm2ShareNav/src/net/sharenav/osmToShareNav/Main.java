/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * 
 */
package net.sharenav.osmToShareNav;

import java.io.FileInputStream;
import java.io.FileNotFoundException;



public class Main {

	/**
	 * @param args
	 * @deprecated
	 */
	/* public static void main(String[] args) {
		if (args.length > 1){
			FileInputStream fr;
			try {
				Configuration c=new Configuration(args[0],args[1]);
				fr = new FileInputStream(args[0]);
				OxParser parser = new OxParser(fr,c);
				System.out.println("read Nodes " + parser.nodes.size());
				System.out.println("read Ways  " + parser.ways.size());
				CreateShareNavData cd=new CreateShareNavData(parser,args[1]);
				new SplitLongWays(parser);
				cd.exportMapToMid();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}		
		
	}
	*/
}

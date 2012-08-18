/**
 * GpsMid - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * See COPYING
 */
package de.ueller.osmToGpsMid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import de.ueller.osmToGpsMid.model.Bounds;

public class CellDB {

	public class Cell {
		int id;
		double lat;
		double lon;
		int mcc;
		int mnc;
		int lac;
		int cellid;

		@Override
		public String toString() {
			return "Cell MCC: " + mcc + " MNC: " + mnc + " LAC: " + lac
					+ " CellID: " + cellid + " (" + lat + "|" + lon + ")";
		}
	}

	public void parseCellDB() {
		HashMap<Long, ArrayList<Cell>> cells = new HashMap<Long, ArrayList<Cell>>();
		int noInvalid = 0;
		int noValid = 0;
		int mcc = 0;
		int mnc = 0;
		Configuration conf = Configuration.getConfiguration();
		Vector<Bounds> bounds = conf.getBounds();

		/*
		 * Determine if we should filter the cellIDs according to a country code
		 * or network operator code. by specifying e.g. useCellID=234,10 the
		 * file will only contain cells from country code 234 (UK) and operator
		 * 10 (O2)
		 */
		String cellConf = conf.getCellOperator();
		if (!cellConf.equalsIgnoreCase("true")) {
			String[] cellConfSplit = cellConf.split(",");
			try {
				switch (cellConfSplit.length) {
				case 1:
					mcc = Integer.parseInt(cellConfSplit[0]);
					break;
				case 2:
					mcc = Integer.parseInt(cellConfSplit[0].trim());
					mnc = Integer.parseInt(cellConfSplit[1].trim());
					break;
				default:
					System.out
							.println("Can't interpret useCellID properties. Skipping CellIDs");
					return;
				}
			} catch (NumberFormatException nfe) {
				System.out
						.println("Can't interpret useCellID properties. Skipping CellIDs");
				return;
			}
		}

		System.out.println("Scanning CellIDs in the area");
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(conf
					.getCellStream()));
			if (r == null) {
				System.out.println("WARNING: could not find cellID file, NOT including cell ids");
				return;
			}
			while (true) {
				String line = null;
				try {
					line = r.readLine();
					if (line == null) {
						break;
					}
				} catch (EOFException eof) {
					break;
				}
				
				String[] cellString = line.split(",");
				if (cellString == null || cellString.length != 12) {
					System.out.println("Invalid line in cellID file "
							+ cellString);
				}
				try {
					Cell c = new Cell();
					c.id = Integer.parseInt(cellString[0]);
					c.lat = Double.parseDouble(cellString[1]);
					c.lon = Double.parseDouble(cellString[2]);
					c.mcc = Integer.parseInt(cellString[3]);
					c.mnc = Integer.parseInt(cellString[4]);
					c.lac = Integer.parseInt(cellString[5]);
					c.cellid = Integer.parseInt(cellString[6]);
					Integer.parseInt(cellString[8]); // NoSample

					boolean isIn = false;
					if (bounds != null && bounds.size() != 0) {
						for (Bounds bound : bounds) {
							if (bound.isIn(c.lat, c.lon)) {
								isIn = true;
								continue;
							}
						}
					} else {
						isIn = true;
					}
					if (!isIn) {
						continue;
					}

					if ((c.mcc > 65000) || (c.mnc > 65000)) {
						noInvalid++;
						continue;
					}

					if (((mcc != 0) && (mcc != c.mcc))
							|| ((mnc != 0) && (mnc != c.mnc))) {
						// This cell is not for our operator
						continue;
					}

					if ((c.lac < 0) || (c.cellid < 0)) {
						noInvalid++;
						continue;
					}
					long key = (c.mcc << 48) + (c.mnc << 32) + c.lac;
					ArrayList<Cell> lacCells = cells.get(key);
					if (lacCells == null) {
						lacCells = new ArrayList<Cell>();
						cells.put(new Long(key), lacCells);
					}
					lacCells.add(c);
					noValid++;
				} catch (NumberFormatException nfe) {
				}
			}
			System.out.println("Found " + noValid
					+ " cellIDs in this area and ignored " + noInvalid);
			File f;
			// File f = new File(conf.getTempDir() + "/cellids/");
			// f.mkdir();
			/**
			 * Write out cellIDs into files based on MCC, MNC and LAC.
			 * Given that OpenCellID.org isn't complete, there are still
			 * quite a few LACs for which there is only a single cell in the
			 * db. In order to reduce the number of small files, we combine
			 * all LACs with less than 20 cells into a big operator file.
			 * If CellIDnoLOC option is set, write all cell id data
			 * into the file with no LAC in the name, for the benefit
			 * of users of Nokia and LG phones which can't get the LAC code.
			 */
			for (ArrayList<Cell> lacCells : cells.values()) {
				Cell c = lacCells.get(0);
				DataOutputStream dos = null;
				if (conf.getCellIDnoLAC() || lacCells.size() < 20) {
					String fname = conf.getTempDir() + "/c" + c.mcc + c.mnc
							+ ".id";
					if (conf.sourceIsApk) {
						fname = conf.getTempDir() +
							"/assets" + "/c" + c.mcc + c.mnc
							+ ".id";
					}
					f = new File(fname);
					if (!f.exists()) {
						f.createNewFile();
					}
					dos = new DataOutputStream(new FileOutputStream(f, true));
					for (Cell c2 : lacCells) {
						dos.writeInt(c2.lac);
						dos.writeInt(c2.cellid);
						dos.writeFloat((float) c2.lat);
						dos.writeFloat((float) c2.lon);
					}
					dos.close();
				}
				if (lacCells.size() >= 20) {
					String fname = conf.getTempDir() + "/c" + c.mcc + c.mnc
							+ c.lac + ".id";
					if (conf.sourceIsApk) {
						fname = conf.getTempDir() +
							"/assets" + "/c" + c.mcc + c.mnc
							+ c.lac + ".id";
					}
					f = new File(fname);
					f.createNewFile();
					dos = new DataOutputStream(new FileOutputStream(f));
					for (Cell c2 : lacCells) {
						dos.writeInt(c2.lac);
						dos.writeInt(c2.cellid);
						dos.writeFloat((float) c2.lat);
						dos.writeFloat((float) c2.lon);
					}
					dos.close();
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("WARNING: Could not find CellID file, NOT including cell ids");
			e.printStackTrace();
		}
	}

}

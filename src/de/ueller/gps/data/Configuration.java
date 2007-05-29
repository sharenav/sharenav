package de.ueller.gps.data;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class Configuration {
	
	public final static int VERSION=1;
	public final static int RENDER_LINE=0;
	public final static int RENDER_STREET=1;

	public final static int LOCATIONPROVIDER_SIRF=0; 
	public final static int LOCATIONPROVIDER_NMEA=1; 
	public final static int LOCATIONPROVIDER_JSR179=2;
	
	private final static byte[] empty="".getBytes();

	private String btUrl;
	private int locationProvider=0;
	private int render=RENDER_STREET;
	
	
	
	public Configuration() {
		read();
	}

	private void read(){
	RecordStore	database;
	try {
		database = RecordStore.openRecordStore("Receiver", false);
		btUrl=readString(database, 1);
		locationProvider=readInt(database, 2);
		render=readInt(database, 3);
		database.closeRecordStore();
	} catch (Exception e) {
		btUrl=null;
	}
	}
	
	private void write(String s, int idx) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			byte[] data=s.getBytes();
			while (database.getNumRecords() < idx){
				database.addRecord(empty, 0, empty.length);
			}
			database.setRecord(1, data,0,data.length);
			database.closeRecordStore();
		} catch (Exception e) {
		}
	}
	private void write(int i,int idx){
		write(""+i,idx);
	}


	public String readString(RecordStore database,int idx){
		try {
			byte[] data=database.getRecord(2);
			return(new String(data));
		} catch (Exception e) {
			return null;
		} 
	}
	public int readInt(RecordStore database,int idx){
		try {
			return Integer.parseInt(readString(database, idx));
		} catch (Exception e){
			return 0;
		}
	}

	public String getBtUrl() {
		return btUrl;
	}

	public void setBtUrl(String btUrl) {
		this.btUrl = btUrl;
		write(btUrl, 1);
	}

	public int getLocationProvider() {
		return locationProvider;
	}

	public void setLocationProvider(int locationProvider) {
		this.locationProvider = locationProvider;
		write(locationProvider,2);
	}

	public int getRender() {
		return render;
	}

	public void setRender(int render) {
		this.render = render;
		write(render,3);
	}
	
}

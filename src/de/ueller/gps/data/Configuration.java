package de.ueller.gps.data;

import javax.microedition.rms.RecordStore;

public class Configuration {
	
	public final static int VERSION=1;
	public final static int RENDER_LINE=0;
	public final static int RENDER_STREET=1;

	public final static int LOCATIONPROVIDER_SIRF=0; 
	public final static int LOCATIONPROVIDER_NMEA=1; 
	public final static int LOCATIONPROVIDER_JSR179=2;
	public final static int LOCATIONPROVIDER_NONE=3;

	public final static String[] LOCATIONPROVIDER={"Sirf","NMEA","JSR179","None"};
	private final static byte[] empty="".getBytes();

	private String btUrl;
	private int locationProvider=0;
	private int render=RENDER_STREET;
	private String gpxUrl;
	
	
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
			gpxUrl=readString(database, 4);
			database.closeRecordStore();
		} catch (Exception e) {

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
			database.setRecord(idx, data,0,data.length);
			database.closeRecordStore();
			System.out.println("wrote " + s + " to " + idx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void write(int i,int idx){
		write(""+i,idx);
	}


	public String readString(RecordStore database,int idx){
		try {
			byte[] data=database.getRecord(idx);
			return(new String(data));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}
	public int readInt(RecordStore database,int idx){
		try {
			return Integer.parseInt(readString(database, idx));
		} catch (Exception e){
			e.printStackTrace();
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
	
	public void setGpxUrl(String url) {
		this.gpxUrl = url;
		write(url,4);
	}

	public String getGpxUrl() {
		return gpxUrl;
	}

}

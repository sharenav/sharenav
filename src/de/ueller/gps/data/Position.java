/**
 * SirfDecoder
 * 
 * takes an InputStream and interpret layer 3 and layer 4. Than make
 * callbacks to the receiver witch ahas to implement SirfMsgReceiver 
 *
 * @version $Revision$$ ($Name$)
 * @autor Harald Mueller james22 at users dot sourceforge dot net
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.gps.data;

//import java.text.DateFormat;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
import java.util.Date;


public class Position {
	/**
	 * position in degrees
	 */
	public float latitude;
	public float longitude;
	public float altitude;
	/**
	 * Speed over ground in m/s
	 */
	public float speed;
	public float course;
	public int mode=-1;
	public Date date;
//	 
//	 private static DateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy");
//	 private static NumberFormat nf = NumberFormat.getInstance();
	/**
	 * 
	 */
		public Position(float latitude,float longitude,float altitude,float speed,float course,int mode,Date date) {
			this.latitude=latitude;
			this.longitude=longitude;
			this.altitude=altitude;
			this.speed=speed;
			this.course=course;
			this.mode=mode;
			this.date=date;
		}
		public Position(String latitude,String longitude,String altitude,String speed,String course,String mode,String date,String time) {
			try {
				this.latitude=Float.parseFloat(latitude);
				this.longitude=Float.parseFloat(longitude);
				this.altitude=Float.parseFloat(altitude);
				this.speed=Float.parseFloat(speed);
				this.course=Float.parseFloat(course);
				this.mode=Integer.parseInt(mode);
//				this.date=new Date(dayFormat.parse(date).getTime()+ Math.round(Float.parseFloat(time)*86400000l));
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}		
}

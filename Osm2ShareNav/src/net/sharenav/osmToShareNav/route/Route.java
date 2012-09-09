/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.route;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.JFrame;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import net.sharenav.osmToShareNav.MyMath;
import net.sharenav.osmToShareNav.model.Node;


/**
 * @author hmu
 * 
 */
public class Route {
	
//	final public static transient double HALF_PI=Math.PI/2d;
//	final public static transient double FAC_RADTODEC =  180.0d/Math.PI;
//	final public static transient double FAC_DECTORAD =  Math.PI / 180d;
//	final public static transient double PLANET_RADIUS = 6371000.8d;
	/**
	 * The URL of the route calculation at open route service
	 */
	private static final String ORS_URL_CALC_ROUTE="http://www.openrouteservice.org/php/OpenLSRS_DetermineRoute.php";
	private static final String ORS_URL_REV_SEARCH="http://www.openrouteservice.org/php/OpenLSLUS_Geocode.php";
	/*Lon=8.65809499075895&Lat=49.48157911916741&MaxResponse=10&_=*/
	private static final String ORS_URL_ADDR_SEARCH="http://www.openrouteservice.org/php/OpenLSLUS_Geocode.php";
	/*FreeFormAdress=Schwarzenbruck, Brixener strasse&MaxResponse=25&_=*/
	Pattern startPattern = Pattern.compile("<gml:LineString");
	Pattern posPattern = Pattern.compile("<gml:pos>(-*[0-9.]+) (-*[0-9.]+)</gml:pos>");
	Pattern countryPattern = Pattern.compile("<xls:Address countryCode=\"(.*)\">");
	Pattern streetPattern = Pattern.compile("<xls:Street officialName=\"(.*)\"/>");
	Pattern subDivPattern = Pattern.compile("<xls:Place type=\"CountrySubdivision\">(.*)</xls:Place>");
	Pattern cityPattern = Pattern.compile("<xls:Place type=\"Municipality\">(.*)</xls:Place>");
	Pattern zipPattern = Pattern.compile(" <xls:PostalCode>(.*)</xls:PostalCode>");

	Vector<Node> routeResult = new Vector<Node>();
	Vector<Location> routeList=new Vector<Location>();
	private double corridorR=10000d;
	private JMapViewer map;
	/**
	 * @param routeList
	 */
	public Route(Vector<Location> routeList,double corridorWidth, JMapViewer map) {
		this.routeList = routeList;
		this.corridorR = corridorWidth/2d;
		this.map=map;
		calculateRoute();
		
	}
	

//	private Outline createOutline(){
//		Outline o=new Outline();
//		Node p1=routeResult.elementAt(0);
//		Node p2=routeResult.elementAt(1);
//		Node o1;
//		Node o2;
//		double dir[]=MyMath.calcDistanceAndCourse(p1, p2);
//		p1=MyMath.moveBearingDist(p1, dir[1]+180d, corridorR);
//		
////		follow the path forward on the right side
//		for (int i=1;i<routeResult.size();i++){
//			p1=p2;
//			p2=routeResult.elementAt(i);	
//			System.out.println("process " + i + "("+routeResult.size()+") " + p1 + p2);
//			dir=MyMath.calcDistanceAndCourse(p1, p2);
//			o1=MyMath.moveBearingDist(p1, dir[1]+90d, corridorR);
//			o2=MyMath.moveBearingDist(p2, dir[1]+90d, corridorR);
//			o.append(new Vertex(o1, o));
//			o.append(new Vertex(o2, o));	
//			for (int i1=1;i1<(o.vertexCount()-1);i1++){
//				double dirO[]=MyMath.calcDistanceAndCourse(o.getNode(i1-1), o.getNode(i1));
//				Node inter=MyMath.intersection(o.getNode(i1-1),dirO[1],o1,dir[1]);
//				System.out.println("intersection = " + inter);
//			}
//		}
//		return o;
//	}
	
	/**
	 * 
	 */
	public Route() {

	}


	public Area createArea() {
		Area a = null;
		// Outline o=new Outline()
		Node last = null;
		Node n1 = routeResult.elementAt(0);
		double firstDir = 0;
		for (int i = 1; i < routeResult.size(); i++) {
			Node n2 = routeResult.elementAt(i);
			double dir[] = MyMath.calcDistanceAndCourse(n1, n2);
			double delta = Math.abs((720+dir[1] - firstDir)%360);
			if (dir[0] > corridorR /*|| delta > 20 */ || i==routeResult.size()) {
				firstDir=dir[1];
//				Node nb = MyMath.moveBearingDist(n1, dir[1] + 180d, corridorR);
				Node nb1 = MyMath.moveBearingDist(n1, dir[1] - 90d, corridorR);
				Node nb2 = MyMath.moveBearingDist(n1, dir[1] - 126d, corridorR);
				Node nb3 = MyMath.moveBearingDist(n1, dir[1] - 162d, corridorR);
				Node nb4 = MyMath.moveBearingDist(n1, dir[1] - 198d, corridorR);
				Node nb5 = MyMath.moveBearingDist(n1, dir[1] - 234d, corridorR);
				Node nb6 = MyMath.moveBearingDist(n1, dir[1] - 270d, corridorR);
//				Node nf = MyMath.moveBearingDist(n2, dir[1], corridorR);
				Node nf1 = MyMath.moveBearingDist(n2, dir[1] - 90d, corridorR);
				Node nf2 = MyMath.moveBearingDist(n2, dir[1] - 54d, corridorR);
				Node nf3 = MyMath.moveBearingDist(n2, dir[1] - 18d, corridorR);
				Node nf4 = MyMath.moveBearingDist(n2, dir[1] + 18d, corridorR);
				Node nf5 = MyMath.moveBearingDist(n2, dir[1] + 54d, corridorR);
				Node nf6 = MyMath.moveBearingDist(n2, dir[1] + 90d, corridorR);
				Path2D p = new Path2D.Double();
				p.moveTo(nb1.lat, nb1.lon);
				p.lineTo(nb2.lat, nb2.lon);
				p.lineTo(nb3.lat, nb3.lon);
				p.lineTo(nb4.lat, nb4.lon);
				p.lineTo(nb5.lat, nb5.lon);
				p.lineTo(nb6.lat, nb6.lon);
				p.lineTo(nf1.lat, nf1.lon);
				p.lineTo(nf2.lat, nf2.lon);
				p.lineTo(nf3.lat, nf3.lon);
				p.lineTo(nf4.lat, nf4.lon);
				p.lineTo(nf5.lat, nf5.lon);
				p.lineTo(nf6.lat, nf6.lon);
				p.closePath();
				if (a == null) {
					a = new Area(p);
				} else {
					a.add(new Area(p));
				}
				n1 = n2;
				// if (i%100==0){
				LinkedList<MapMarker> marker = new LinkedList<MapMarker>();
				System.out.println("process " + i + "(" + routeResult.size()
						+ ") ");
				PathIterator it = a.getPathIterator(null);
				double[] coords = new double[6];
				int co = 0;
				while (!it.isDone()) {
					it.currentSegment(coords);
					marker.add(new MapMarkerDot(coords[0], coords[1]));
					it.next();
					co++;
				}
				System.out.println("area lines " + co);
				map.setMapMarkerList(marker);
				map.setDisplayToFitMapMarkers();
				map.repaint();
				// }
			} else {
				System.out.println("skip node " + i + " because dist="+dir[0]+ " delta dir="+delta);
			}

		}
		return a;
	}
	
	private void calculateRoute() {
		routeResult = new Vector<Node>();
		if (routeList != null && routeList.size() > 1) {;
			try {
				fetchRoute("Fastest",false);
				fetchRoute("Fastest",true);
				fetchRoute("Shortest",false);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}


	private void fetchRoute(String routepref, boolean noMotorways)
			throws MalformedURLException, IOException, ProtocolException {
		StringBuffer post = new StringBuffer();
		post.append(createPar("Start", routeList.firstElement().getNode()));
		post.append("&");
		post.append(createPar("End", routeList.lastElement().getNode()));
		post.append("&");
		post.append(createPar("Via", routeList));
		post.append("&lang=en&distunit=KM&routepref=");
		post.append(routepref);
		post.append("&avoidAreas=&useTMC=false&noMotorways=");
		post.append(noMotorways);
		post.append("&noTollways=false&instructions=false&_=");
		BufferedReader in = openHttpResponse(ORS_URL_CALC_ROUTE,post.toString());
		decodeRouteXML(in);
		in.close();
//		map.repaint();
	}

	private BufferedReader openHttpResponse(String urlString,String post)
			throws MalformedURLException, IOException, ProtocolException {
		URL url;
		url = new URL(urlString);
//		url = new URL(ORS_URL_CALC_ROUTE);
		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setReadTimeout(240000); // 30 seconds read timeout
		urlConn.setDoOutput(true);
		urlConn.setRequestMethod("POST");
		urlConn.setRequestProperty("User-agent", "OSM2ShareNav");
//		urlConn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.6) Gecko/20100625 Firefox/3.6.6");
		urlConn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
		urlConn.setRequestProperty("Pragma", "no-cache");
		urlConn.setRequestProperty("Cache-Control", "no-cache");
		urlConn.setRequestProperty("Accept-Encoding", "gzip,deflate");
		String length=""+post.length();
		System.out.println("Length = " + length);
		urlConn.setRequestProperty("Content-Length",length);
		
		
		OutputStreamWriter out = new OutputStreamWriter(
				urlConn.getOutputStream());
		out.write(post);
		out.close();
//		urlConn.
		if ("gzip".equals(urlConn.getContentEncoding())){
		System.out.println(urlString);
		System.out.println(""+post.length()+":"+post);
		BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(urlConn.getInputStream()),Charset.forName("UTF-8")));
		
		return in;
		} else {
			System.out.println("Resposonse is " + urlConn.getContentEncoding() );
		} return null;
	}

	private void decodeRouteXML(BufferedReader in) throws IOException {
		String decodedString;
		boolean start = false;
		while ((decodedString = in.readLine()) != null) {
			System.out.print(decodedString);
			if (start) {
				Matcher m = posPattern.matcher(decodedString);
				if (m.find()) {
					double lon = Double.parseDouble(m.group(1));
					double lat = Double.parseDouble(m.group(2));
					Node c = new Node((float)lat, (float)lon,-1);
					System.out.println(" --> " + c);
					routeResult.add(c);
//					map.addMapMarker(new MapMarkerDot(Color.RED, lat, lon));
				} else {
					System.out.println(" start but no match");
				}
			} else if (startPattern.matcher(decodedString).find()) {
				start = true;
				System.out.println(" --> found start ");
			} else {
				System.out.println(" not jet started");
			}
			// System.out.println(decodedString);
		}

	}
	
	public void revResolv(Location l){
		StringBuffer post = new StringBuffer();
		post.append(createPar("Lon", l.getNode().lon));
		post.append("&");
		post.append(createPar("Lat", l.getNode().lat));
		post.append("&MaxResponse=10&_=");
		try {
			BufferedReader in = openHttpResponse(ORS_URL_REV_SEARCH,post.toString());
			String s;
			decodeGml(in, l);
			in.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * @param string
	 * @param firstElement
	 * @return
	 */
	private String createPar(String string, Node c) {
		return string + "=" + c.lon + "," + c.lat;
	}
	/**
	 * @param string
	 * @param firstElement
	 * @return
	 */
	private String createPar(String string,float v) {
		return string + "=" + v;
	}
	/**
	 * @param string
	 * @param firstElement
	 * @return
	 */
	private Object createPar(String par,Vector<Location> li) {
		/*
		<xls:ViaPoint><xls:Position><gml:Point><gml:pos>11.2264324 49.3543942</gml:pos></gml:Point></xls:Position></xls:ViaPoint>
	    <xls:ViaPoint><xls:Position><gml:Point><gml:pos>11.2320758 49.3839049</gml:pos></gml:Point></xls:Position></xls:ViaPoint>
		*/
		StringBuffer v=new StringBuffer(par);
		v.append("=");
		for (int i=1; i<(li.size()-1);i++){
			Node c=li.get(i).getNode();
			v.append("<xls:ViaPoint><xls:Position><gml:Point><gml:pos>");
			v.append(c.lon + " " + c.lat);
			v.append("</gml:pos></gml:Point></xls:Position></xls:ViaPoint>");
		}
		return v.toString();
	}
	
	private void decodeGml(BufferedReader in,Location l) throws IOException{
		String decodedString;
		while ((decodedString = in.readLine()) != null) {
			System.out.print(decodedString);
			Matcher m = countryPattern.matcher(decodedString);
			if (m.find()){
				l.setCountry(m.group(1));
			}
			m = cityPattern.matcher(decodedString);
			if (m.find()){
				l.setCity(m.group(1));
			}
			m = zipPattern.matcher(decodedString);
			if (m.find()){
				l.setZip(m.group(1));
			}
			m = streetPattern.matcher(decodedString);
			if (m.find()){
				l.setStreet(m.group(1));
			}
		}
			

	}
	
	public static void main(String[] args) {
		Route r=new Route(null, 100000,null);
		try {
			BufferedReader in = new BufferedReader(new FileReader("route.xml"));
			r.decodeRouteXML(in);
			in.close();
			r.map = new JMapViewer(new MemoryTileCache(), 4);
			r.map.setSize(1000, 800);
			JFrame f=new JFrame();
			f.setMinimumSize(new Dimension(1200, 1000));
//			f.setSize(1000, 800);
			f.add(r.map);
			f.pack();
			f.setVisible(true);	
			for (int i=0;i<r.routeResult.size();i++){
				Node node = r.routeResult.get(i);
				r.map.addMapMarker(new MapMarkerDot(node.lat, node.lon));
			}
			r.map.setDisplayToFitMapMarkers();
			r.map.repaint();
			r.createArea();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}

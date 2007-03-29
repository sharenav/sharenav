package de.ueller.midlet.gps.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Hashtable;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.DataReader;


public class Way {
	public byte type;
	public Short nameIdx=null;
	public int maxspeed;
	public short[][] paths;
//	public short[] path;
	public float minLat;
	public float minLon;
	public float maxLat;
	public float maxLon;
	private final static Logger logger=Logger.getInstance(Way.class,Logger.ERROR);

	/**
	 * the flag should be readed by caller. if Flag == 128 this is a dummy
	 * Way and can ignored.
	 * @param is
	 * @param f
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream	is,byte f) throws IOException{
		minLat=is.readFloat();
		minLon=is.readFloat();
		maxLat=is.readFloat();
		maxLon=is.readFloat();
//		if (is.readByte() != 0x58){
//			logger.error("worng magic after way bounds");
//		}
		type=is.readByte();
		if ((f & 1) == 1){
//			logger.debug("read name");
//			name=is.readUTF();
			nameIdx=new Short(is.readShort());
		}
		if ((f & 2) == 2){
//			logger.debug("read maxspeed");
			maxspeed=is.readByte();
		}		
		byte pathCount;
		if ((f & 4) == 4){
			pathCount = is.readByte();
//			logger.debug("Multipath "+ pathCount);
		} else {
			pathCount=1;
		}
		paths=new short[pathCount][];
//		logger.debug("read paths count="+pathCount);
		for (byte pc=0;pc < pathCount; pc++){
			short count = is.readByte();
			short[] path = new short[count];
			paths[pc]=path;
			logger.debug("read path count="+count);
			for (short i=0; i< count;i++){
				path[i]=is.readShort();
//				logger.debug("read node id=" + path[i]);
			}
//			if (is.readByte() != 0x59 ){
//				logger.error("wrong magic code after path");
//			}
		}
	}
}

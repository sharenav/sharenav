package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.Tile;

public class NamesNew extends QueueReader {
	private short[] startIndexes=null;

	public void readData(Tile tt) throws IOException {
		// TODO Auto-generated method stub

	}
	private void readIndex() throws IOException {
		InputStream is = getClass().getResourceAsStream("/names-idx.dat");
//		logger.info("read names-idx");
		DataInputStream ds = new DataInputStream(is);

		short[] nameIdxs = new short[255];
		short count=0;
		nameIdxs[count++]=0;
		while (ds.available() > 0) {
			nameIdxs[count++] = ds.readShort();
		}
		startIndexes = new short[count];
		for (int l = 0; l < count; l++) {
			startIndexes[l] = nameIdxs[l];
		}
//		logger.info("read names-idx ready");
	}

}

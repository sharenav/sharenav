package de.ueller.midlet.gps.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class PersistEntity {

	public int id = -1;
	public String displayName;

	protected DataInputStream getByteInputStream(byte[] data) {
		ByteArrayInputStream bs = new ByteArrayInputStream(data);
		DataInputStream ds = new DataInputStream(bs);
		return ds;
	}
	protected DataOutputStream getByteOutputStream() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(os);
		return ds;
	}

}

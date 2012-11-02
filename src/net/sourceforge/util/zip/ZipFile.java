/**
 * This class is a complete rewrite which tries to provide most basic
 * functionality for reading zip files in J2ME environments.  It tries
 * to adhere to the handling set forth by java.util.zip of SUN's JDK.
 * The implementation was written from scratch using:
 * http://www.pkware.com/documents/casestudies/APPNOTE.TXT
 * 
 * Copyright (c) 2009 Christian Müller <cmue81 at \g\m\x dot \d\e>
 * 					 <trendypack at users dot sourceforge dot net>
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 */ 
package net.sourceforge.util.zip;


//#if polish.api.fileconnection
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import de.enough.polish.util.HashMap;
import de.enough.polish.util.zip.GZipInputStream;
//#endif
//#if polish.android
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
//#endif

public class ZipFile {
//#if polish.api.fileconnection
//#if polish.android
	private File file;
	private RandomAccessFile raFile;
	private InputStream ais;
//#else
	private FileConnection fc;
//#endif
	private HashMap contents;
	private int contents_limit;
	/*private DataOutputStream log;*/

	public ZipFile(String fileUrl, int limitIndexCache) throws IOException {
/*
		FileConnection fc2 = (FileConnection) Connector.open("file:///E:/log.txt");

		if (fc2.exists())
			fc2.delete();
		fc2.create();
		log = fc2.openDataOutputStream();

*/
//#if polish.android
		// strip file:// prefix
		file = new File(fileUrl.substring("file://".length()));
		if (file == null)
			throw new IOException("file unreadable");
		//ais = new FileInputStream(file);
		raFile = new RandomAccessFile(file, "r");
		if (raFile == null)
			throw new IOException("file unreadable");
		//if (ais == null)
		//	throw new IOException("file unreadable");
//#else
		fc = (FileConnection) Connector.open(fileUrl, Connector.READ);
		if (fc == null || !fc.canRead())
			throw new IOException("file unreadable");
//#endif

		contents = new HashMap();
		contents_limit = limitIndexCache;
		if (contents_limit<0 && !(readEntries()>0))
			throw new IOException("no entries in file");
/*
		InputStream tmp;
		for (int v, i=2; i!=0; i--) {
			log.writeChars("before reopen META-INF/MANIFEST.MF\n\n\n"); log.flush();
			tmp = getInputStream(getEntry("META-INF/MANIFEST.MF"));
			while ((v=tmp.read())!=-1)
				log.writeChar(v);
			tmp.close();
			tmp = null;
		}

		log.writeChars("before reopen for printing index\n\n\n"); log.flush();
		for (de.enough.polish.util.Iterator i = contents.keysIterator();i.hasNext();) {
			String s = i.next();
			log.writeChars(s+"\n  size: "+((ZipEntry)contents.get(s)).getSize()+"\n\n");
		}

		log.close();
		log = null;

		contents = new HashMap(); // empty HashMap, don't hand out streams while testing
*/
	}

	public ZipFile(String fileUrl) throws IOException {
		this(fileUrl, -1);
	}

	/**
	 * Get a @ZipEntry for the given name, use different methods depending on
	 * whether there is a limited index cache or all of it. If @limitIndexCache
	 * is negative, the whole index of the ZipFile was read into a HashMap at
	 * constructor time.  The elements in the HashMap are of type ZipEntry.
	 * 
	 * If @limitIndexCache is zero, no caching will be used at all and the
	 * lookup is done reading out sequentially the ZipFile's central directory
	 * each time this method is called.
	 * 
	 * If @limitIndexCache is a positive number above zero, the number of items
	 * in the HashMap will not exceed this number and a new item not yet in the
	 * HashMap will displace exactly this item which was accessed before all
	 * other items in the HashMap. The elements in the HashMap are of type
	 * LinkedZipEntry in this case.
	 * 
	 * @param name The filename of the entry to look for.
	 * @return a @ZipEntry or null
	 */
	public ZipEntry getEntry(String name) {
		return
			(contents_limit<0)
			? (ZipEntry) contents.get(name)
			: findEntry(name);
	}

	public synchronized InputStream getInputStream(ZipEntry e) throws IOException {
		InputStream ret = null;

		if (e != null) {
			byte [] b = new byte [(int)e.getCompressedSize()+8];
			int i = b.length-8;
	
			/*log.writeChars("attempting "+e.getSize()); log.flush();*/
			//#if polish.android
			raFile.seek(e.offset);
			raFile.read(b, 0, i);
			//#else
			ret = fc.openInputStream();
			ret.skip(e.offset);
			ret.read(b, 0, i);
			ret.close();
			//#endif
			ret = null;
			/*log.writeChars("done "+e.getSize()); log.flush();*/
	
			switch (e.getMethod()) {
			case ZipEntry.TYPE_STORED: {
				ret = new ByteArrayInputStream(b, 0, i);
				break;
			}
			case ZipEntry.TYPE_DEFLATE: {
				long n = e.getSize(); n <<= 32;
				do {
					b[i++] = (byte)(n&0xFF); n >>>= 8;
				} while (i<b.length); 
	
				ret = new GZipInputStream(new ByteArrayInputStream(b, 0, i),
						GZipInputStream.TYPE_DEFLATE, false);
				break;
			}
			default:
				throw new IOException("invalid encoding");
			}
		}
		
		return ret;
	}

	private ZipEntry readEntry(TinyBufInputStream f, boolean cendir) throws IOException {
		ZipEntry e = new ZipEntry("");

		int fnlen, eflen, colen=0, gpbits=0;	// fnlen, eflen are reused occasionally..
		byte [] fn = new byte [32];

		if (cendir) f.skip(2);					// version made by
		f.skip(2); 								// version needed to extract
		gpbits = f.readShortLE();				// gp bit flag
		e.setMethod(f.readShortLE());			// compression method

		f.skip(8);								// mod time and date, CRC-32
		e.setCompressedSize(f.readIntLE());		// compressed size
		e.setSize(f.readIntLE());				// uncompressed size

		fnlen = f.readShortLE();				// filename length
		eflen = f.readShortLE();				// extra field length
		if (cendir) {
			colen = f.readShortLE();			// comment length
			f.skip(8);							// disknumstart, file attr
			e.offset = f.readIntLE()+ZipConstants.LOCHDR+fnlen+eflen;
		}

		// read filename for entry
		while (fnlen > fn.length)
			fn = new byte [2*fn.length];
		f.read(fn, 0, fnlen);			
		e.setName(new String(fn, 0, fnlen, "ISO-8859-1"));	// "UTF-8", "US-ASCII", ..

		// read extra field(s) if present
		while (eflen > 0) {
			fnlen = f.readShortLE();			// header id of (header,data) pairs
			if (fnlen == 1)						//   look for Zip64 header for pair
				e.setZip64Entry(fnlen == 1);	
			fnlen = f.readShortLE();			// get length of data for pair
			f.skip(fnlen);						// and skip that data

			eflen -= (4 + fnlen);				// proceed with next extra field
		}

		if (cendir) {
			f.skip(colen);
		}
		else {
			// save file data offset
			e.offset = f.tell();
			
			// read data descriptor and skip to next LOCHDR
			if ((gpbits & 0x08) != 0) {
				/* crc-32 and file sizes not yet known,
				 * find data descriptor that succeeds file data
				 * 
				 * there are two issues using the following method
				 * 1) an EXTSIG can be found, so that the 8-12 byte read after EXTSIG
				 *    equals actual data_bytes read, but it's just part of the stream
				 * 2) if (actual data_bytes != compressedSize), we resume looking for the
				 *    signature after already having skipped 4 bytes (the crc-32 field)
				 */
				
				fnlen = f.readIntLE();				// last 4 bytes of data, little endian
				do {
					while (fnlen != ZipConstants.EXTSIG) {
						fnlen >>>= 8;
						fnlen   |= (f.read()<<24);
					}
	
					eflen = f.tell()-e.offset-4;	// actual data_bytes we read
					f.skip(4);						// skip crc-32 entry
					if (e.isZip64Entry())			// read compressedSize
						fnlen = (int)f.readLongLE();
					else
						fnlen = f.readIntLE();
				} while (fnlen != eflen);			// if sanity check fails, keep looking
	
				// data descriptor found (compressedSize == actual data_bytes)
				e.setCompressedSize(fnlen);
				if (e.isZip64Entry())
					e.setSize((int)f.readLongLE());
				else
					e.setSize(f.readIntLE());
			}
			else {
				/* crc-32 and file sizes known and valid,
				 * no data descriptor present, skip to next LOCHDR
				 */
				f.skip(e.getCompressedSize());
			}
		}
		
		return e;
	}

	private int readEntries() {
		try {
			TinyBufInputStream f = new TinyBufInputStream(1600);
			ZipEntry e;

			/* if there is a central directory, use it for more speed,
			 * otherwise run through the file to discover all entries
			 */
			boolean cdir = f.seekCENDIR();
			int oursig = cdir ? ZipConstants.CENSIG : ZipConstants.LOCSIG;

			while (f.readIntLE()==oursig) {
				e = readEntry(f, cdir);
				contents.put(e.getName(), e);
			}
	
			f.close();
		}
		catch (IOException e) { }
		
		return contents.size();
	}

	/**
	 * This method is used to fetch an entry if we do not (want to) hold an
	 * index of all contents of the ZipFile in memory. It parses the ZipFile for
	 * an entry and caches up to @contents_size entries for later reference in
	 * the hope that this is of any use for the higher level code using ZipFile.
	 * 
	 * If the usage pattern is such, that the probability of reading a file
	 * twice is very low, i.e. it is more probable that the entry is kicked out
	 * of the cache most of the time before being read a second time, then it
	 * is suggested to either increase @contents_size at constructor time or set
	 * it to zero to not use the cache at all.
	 * 
	 * Note that the lookup is still done using a HashMap, so cached entries are
	 * found in O(log n) time.  Also, for small caches it might be more memory
	 * effective to just use Vector instead of a Double Linked List.  Using
	 * Vector might save some memory at the cost of CPU cycles, but it is not
	 * implemented at this time.
	 */
	private ZipEntry findEntry(String name) {
		ZipEntry e = (ZipEntry) contents.get(name);
		
		if (e != null) {
			((LinkedZipEntry) e).moveToStart();
		}
		else {
			try {
				TinyBufInputStream f = new TinyBufInputStream(1600);
	
				boolean cdir = f.seekCENDIR();
				int oursig = cdir ? ZipConstants.CENSIG : ZipConstants.LOCSIG;
	
				while (f.readIntLE()==oursig) {
					e = readEntry(f, cdir);
					if (e.getName().equals(name)) {
						// requested entry found
						if (contents_limit>1) {
							// use humble LRU cache
							LinkedZipEntry le = new LinkedZipEntry(e);
							if (contents.size() >= contents_limit)
								contents.remove(LinkedZipEntry.removeEnd().getName());
							contents.put(le.getName(), le);
						}
						break;
					}
				}
	
				f.close();
			}
			catch (IOException ex) { }
		}

		return e;
	}

	private class TinyBufInputStream extends InputStream {
		private InputStream is;

		private byte [] buf;
		private int bufp, buflen, buflen_orig, fp, n;
		int cendir_entries, cendir_size;
		
		TinyBufInputStream(int buffer_length) throws IOException {
			buflen_orig = buffer_length;
			reset();
		}

		public void close() throws IOException {
			is.close();
			is = null;
		}

		public void reset() throws IOException {
			if (is != null)	close();

			//#if polish.android
			is = new FileInputStream(file);
			//#else
			is = fc.openInputStream();
			//#endif
			buf = new byte [buflen_orig];
			bufp = buflen = buflen_orig;
			fp = -buflen_orig;
		}

		/**
		 * Returns the byte read or throws an IOException on EOF or
		 * other error cases.  We do not handle EOF by returning -1
		 * to ease the implementation of readIntLE, etc. methods.
		 * 
		 * @return the byte read (0..255)
		 * @throws IOException
		 */
		public int read() throws IOException {
			if (bufp>=buflen)
				if (!dobuf())
					// due to no error checking in read{Short,Int,Long}LE
					throw new IOException("eof or error");

			return buf[bufp++]&0xFF;
		}

		public int read(byte[] b) throws IOException {
			return this.read(b, 0, b.length);
		}

		/**
		 * Reads bytes from stream into <b>, starting with <b[off]> and
		 * ending with <b[off+bytes_read]>.  bytes_read can be less
		 * than the <len> requested bytes.
		 * 
		 * @return the number of bytes read into b
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			int ln = len;

			while (ln>0) {
				if (bufp>=buflen)
					if (!dobuf())
						break;
	
				n = buflen-bufp;
				if (ln<n) n=ln;

				System.arraycopy(buf, bufp, b, off, n);
				off  += n;
				ln   -= n;
				bufp += n;
			}

			return len-ln;
		}

		int readShortLE() throws IOException {
			return read()|(read()<<8);
		}

		int readIntLE() throws IOException {
			return read()|(read()<<8)|(read()<<16)|(read()<<24);
		}

		long readLongLE() throws IOException {
			return read()|(read()<<8)|(read()<<16)|(read()<<24)
			      |(((long)read())<<32)|(((long)read())<<40)
			      |(((long)read())<<48)|(((long)read())<<56);
		}

		int skip(int n) { // does not override skip(long) on purpose
			bufp+=n;
			return n;
		}

		int tell() {
			return fp+bufp;
		}

		/**
		 * Seeks to the offset of the central directory if it is a ZipFile.
		 * Calling this method resets the InputStream.
		 * @return offset
		 * @throws IOException
		 */
		boolean seekCENDIR() throws IOException {
			int ret=0;

			if (tell()>0) reset();
			if (readIntLE()==ZipConstants.LOCSIG) {
				// we do not parse archive comments,
				// this is just a quick shot to get to the directory
				//#if polish.android
				skip((int)file.length()-22-4);
				//#else
				skip((int)fc.fileSize()-22-4);
				//#endif
				if (readIntLE()==ZipConstants.ENDSIG) {
					skip(6);
					cendir_entries = readShortLE();
					cendir_size = readIntLE();
					ret = readIntLE();

					reset();
					skip(ret);
				}
			}

			return ret>0;
		}

		private boolean dobuf() throws IOException {
			is.skip(bufp-buflen);

			n = is.read(buf, 0, buflen);
			if (n<=0)
				return false;
			if (n<buflen)
				buflen=n;

			fp+=bufp;
			bufp=0;

			return true;
		}
	}
//#endif
}

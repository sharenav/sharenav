/* ZipEntry.java --
   Copyright (C) 2001, 2002, 2004, 2005 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package net.sourceforge.util.zip;

import de.enough.polish.util.zip.GZipOutputStream;

/**
 * This class represents a member of a zip archive.  ZipFile and
 * ZipInputStream will give you instances of this class as information
 * about the members in an archive.  On the other hand ZipOutputStream
 * needs an instance of this class to create a new member.
 *
 * @author Jochen Hoenicke 
 */
public class ZipEntry implements ZipConstants
{
  //private static final byte KNOWN_SIZE    = 1;
  //private static final byte KNOWN_CSIZE   = 2;
  //private static final byte KNOWN_CRC     = 4;
  //private static final byte KNOWN_TIME    = 8;
  //private static final byte KNOWN_DOSTIME = 16;
  //private static final byte KNOWN_EXTRA   = 32;
  private static final byte KNOWN_ZIP64   = 64;
  
  public static final int TYPE_STORED  = 0;
  public static final int TYPE_DEFLATE = 8;

  /** Immutable name of the entry */
  private String name;
  /** Uncompressed size */
  private int size = -1;
  /** Compressed size */
  private int compressedSize = -1;
  /** Compression method */
  private byte method = -1;

  /** CRC of uncompressed data */
  //private int crc;
  /** Comment or null if none */
  //private String comment = null;
  /** Extra data */
  //private byte[] extra = null;
  /**
   * The 64bit Java encoded millisecond time since the beginning of the epoch.
   * Only valid if KNOWN_TIME is set in known.
   */
  //private long time;
  /** Flags specifying what we know about this entry */
  private byte known = 0;

  //int flags;              /* used by ZipOutputStream */
  int offset;             /* used by ZipFile and ZipOutputStream */

  public ZipEntry(ZipEntry e) {
	  name = e.name;
	  method = e.method;
	  compressedSize = e.compressedSize;
	  size = e.size;

	  known = e.known;
	  offset = e.offset;
  }

  /**
   * Creates a zip entry with the given name.
   * @param name the name. May include directory components separated
   * by '/'.
   */
  public ZipEntry(String name)
  {
	  setName(name);
  }

  void setName(String name)
  {
	  this.name = name;
  }
  /**
   * Returns the entry name.  The path components in the entry are
   * always separated by slashes ('/').  
   */
  public String getName()
  {
    return name;
  }

  /**
   * Sets the time of last modification of the entry.
   * @time the time of last modification of the entry.
   *
  public void setTime(long time)
  {
    this.time = time;
    this.known |= KNOWN_TIME;
    this.known &= ~KNOWN_DOSTIME;
  }*/

  /**
   * Gets the time of last modification of the entry.
   * @return the time of last modification of the entry, or -1 if unknown.
   *
  public long getTime()
  {
    // The extra bytes might contain the time (posix/unix extension)
    parseExtra();

    if ((known & KNOWN_TIME) != 0)
      return time;
    else
      return -1;
  }*/

  /**
   * Sets the size of the uncompressed data.
   * @exception IllegalArgumentException if size is not in 0..0xffffffffL
   */
  public void setSize(int size)
  {
    if (size<0)
    	throw new IllegalArgumentException("data too large for j2me");
    this.size = size;
  }

  /**
   * Gets the size of the uncompressed data.
   * @return the size or -1 if unknown.
   */
  public int getSize()
  {
    return size;
  }

  /**
   * Sets the size of the compressed data.
   */
  public void setCompressedSize(int csize)
  {
    if (csize<0)
    	throw new IllegalArgumentException("data too large for j2me");
    this.compressedSize = csize;
  }

  /**
   * Gets the size of the compressed data.
   * @return the size or -1 if unknown.
   */
  public int getCompressedSize()
  {
    return compressedSize;
  }

  /**
   * Sets the crc of the uncompressed data.
   * @exception IllegalArgumentException if crc is not in 0..0xffffffffL
   *
  public void setCrc(long crc)
  {
    if ((crc & 0xffffffff00000000L) != 0)
	throw new IllegalArgumentException();
    this.crc = (int) crc;
    this.known |= KNOWN_CRC;
  }*/

  /**
   * Gets the crc of the uncompressed data.
   * @return the crc or -1 if unknown.
   *
  public long getCrc()
  {
    return (known & KNOWN_CRC) != 0 ? crc & 0xffffffffL : -1L;
  }*/

  /**
   * Sets the compression method.
   * @exception IllegalArgumentException if method is not supported.
   * @see GZipOutputStream#TYPE_DEFLATE
   */
  public void setMethod(int method)
  {
    if (method != TYPE_DEFLATE && method != TYPE_STORED)
    	throw new IllegalArgumentException();
    this.method = (byte) method;
  }

  /**
   * Gets the compression method.  
   * @return the compression method or -1 if unknown.
   */
  public int getMethod()
  {
    return method;
  }

  /**
   * Sets the extra data.
   * @exception IllegalArgumentException if extra is longer than 0xffff bytes.
   *
  public void setExtra(byte[] extra)
  {
    if (extra == null) 
      {
	this.extra = null;
	return;
      }
    if (extra.length > 0xffff)
      throw new IllegalArgumentException();
    this.extra = extra;
  }

  private void parseExtra()
  {
    // Already parsed?
    if ((known & KNOWN_EXTRA) != 0)
      return;

    if (extra == null)
      {
	known |= KNOWN_EXTRA;
	return;
      }

    try
      {
	int pos = 0;
	while (pos < extra.length) 
	  {
	    int sig = (extra[pos++] & 0xff)
	      | (extra[pos++] & 0xff) << 8;
	    int len = (extra[pos++] & 0xff)
	      | (extra[pos++] & 0xff) << 8;
	    if (sig == 0x5455) 
	      {
		// extended time stamp
		int flags = extra[pos];
		if ((flags & 1) != 0)
		  {
		    long time = ((extra[pos+1] & 0xff)
			    | (extra[pos+2] & 0xff) << 8
			    | (extra[pos+3] & 0xff) << 16
			    | (extra[pos+4] & 0xff) << 24);
		    setTime(time*1000);
		  }
	      }
	    pos += len;
	  }
      }
    catch (ArrayIndexOutOfBoundsException ex)
      {
	// be lenient
      }

    known |= KNOWN_EXTRA;
    return;
  }*/

  /**
   * Gets the extra data.
   * @return the extra data or null if not set.
   *
  public byte[] getExtra()
  {
    return extra;
  }*/

  /**
   * Sets the entry comment.
   * @exception IllegalArgumentException if comment is longer than 0xffff.
   *
  public void setComment(String comment)
  {
    if (comment != null && comment.length() > 0xffff)
      throw new IllegalArgumentException();
    this.comment = comment;
  }*/

  /**
   * Gets the comment.
   * @return the comment or null if not set.
   *
  public String getComment()
  {
    return comment;
  }*/

  public void setZip64Entry(boolean yes) {
	  if (yes) known |= KNOWN_ZIP64;
  }

  /**
   * Gets true, if the entry is a directory.  This is solely
   * determined by the name, a trailing slash '/' marks a directory.  
   */
  public boolean isDirectory()
  {
    int nlen = name.length();
    return nlen > 0 && name.charAt(nlen - 1) == '/';
  }

  /**
   * Gets true, if the entry is a directory.  This is solely
   * determined by the name, a trailing slash '/' marks a directory.  
   */
  public boolean isZip64Entry()
  {
    return (known & KNOWN_ZIP64) != 0;
  }

  /**
   * Gets the string representation of this ZipEntry.  This is just
   * the name as returned by getName().
   */
  public String toString()
  {
    return name;
  }

  /**
   * Gets the hashCode of this ZipEntry.  This is just the hashCode
   * of the name.  Note that the equals method isn't changed, though.
   *
  public int hashCode()
  {
    return name.hashCode();
  }*/
}

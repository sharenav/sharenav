/*
 * BufferedInputStream.java
 * based on http://code.google.com/p/studyme/source/browse/trunk/StudyME/com/studystack/studyme/BufferedInputStream.java?r=3
 * (GPL V2)
 */

package de.ueller.util;

import java.io.IOException;
import java.io.InputStream;

public class BufferedInputStream extends InputStream {
    
    public BufferedInputStream( final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private InputStream inputStream;

    public int read() throws IOException {
        // System.out.println( "read() pos=" + pos + "bufSize=" + bufSize );
        if (pos >= bufSize) {
            pos = 0;
            // buffer empty or all bytes read, so load buffer
            bufSize = inputStream.read(buffer);
        }

        if (pos < bufSize) {
            // return next byte in buffer

            int retVal = buffer[pos] & 0x00FF;
            pos++;
            // System.out.println( "read returning " + retVal );
            return retVal;
        } else {
            // System.out.println( "read returning negative one." );
            return -1;
        }
    }

    public int available() throws IOException {
        int avail = (bufSize - pos) + inputStream.available();
        // System.out.println( "available() returning avail" );
        return avail;
    }

    public void close() throws IOException {
        // System.out.println( "close()" );
        this.inputStream.close();
        this.inputStream = null;
    }

    public long skip(final long n) throws IOException {
        // System.out.println( "skip " + n );
        long i;
        for (i = 0; i < n; i++) {
            if (read() == -1) {
                break;
            }
        }
        return i;
    }

    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(final byte[] buf, int offset, final int len) throws IOException {
        // System.out.println( "BufferedInputString.read( buf, offset=" + offset
        // + ", len=" + len + ") buf.length=" + buf.length );
        int bytesRead = 0;
        while (offset < buf.length && bytesRead < len) {
            // System.out.println( "offset=" + offset + " bytesRead=" +
            // bytesRead + " len=" + len );
            int nextByte = -1;
            try {
                nextByte = read();
            } catch (IOException e) {
                if (bytesRead == 0) {
                    throw (e);
                }
            }
            if (nextByte != -1) {
                buf[offset] = (byte) nextByte;
                bytesRead++;
                offset++;
            } else {
                break;
            }
        }
        return bytesRead;
    }

    private byte[] buffer = new byte[1024];

    private int pos = 0;

    private int bufSize = 0;
}

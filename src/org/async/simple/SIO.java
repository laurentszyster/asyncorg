/*  Copyright (C) 2007 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of version 2 of the GNU General Public License as
 *  published by the Free Software Foundation.
 *  
 *   http://www.gnu.org/copyleft/gpl.html
 *  
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA
 *  
 */

package org.async.simple;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Functional conveniences to handle simple I/O.
 */
public class SIO {

    /**
     * The default file I/O buffer size, 4096 bytes.
     * 
     * @p 4046 bytes is the maximum block size for many file system, so
     * it makes a rather good default size for file I/O buffers.
     */
    public static final int fioBufferSize = 4096;
    /**
     * The default network I/O buffer size, 16384 bytes.
     * 
     * @p A string of 16KB can represent 3,4 pages of 66 lines and 72 columns 
     * of ASCII characters. Or more information than what most people can 
     * read in a few minutes. It is therefore a reasonable maximum to set for 
     * chunks of I/O in an application web interface updated by its user every 
     * few seconds.
     * 
     * @p Sixteen Kilobytes (16384 8-bit bytes) happens to be the defacto 
     * standard buffer size for TCP peers and networks since it's the maximum 
     * UDP datagram size in use. Which makes anything smaller than this limit 
     * a better candidate for the lowest possible IP network latency.
     * 
     * @p Finally, 16KB buffers can hold more than 65 thousand concurrent
     * responses in one GB of RAM, a figure between at least one order of 
     * magnitude larger than what you can reasonably expect from a J2EE 
     * container running some commodity hardware. At an average speed of
     * 0.5 millisecond per concurrent request/response 16KB buffers sums
     * up to 36MBps, less than 1Gbps.
     * 
     * @p So, that sweet sixteen spot is also a safe general maximum for a 
     * web 2.0 application controller that needs to keep lean on RAM and
     * wants the fastest possible network and the smallest possible
     * imbalance between input and output.
     */
    public static final int netBufferSize = 16384;

    /**
     * Read byte arrays from a <code>BufferedReader</code> by chunks of 
     * <code>fioBufferSize</code> until the input stream buffered is
     * closed, accumulate those chunks in a <code>StringBuffer</code>,
     * join them and  then returns a UNICODE string (implicitely using the 
     * default character set encoding). 
     * 
     * @p This is a "low-level" API to support convenience to glob files,
     * URLs resources or any input stream that can be wrapped with a
     * <code>BufferedReader</code>.
     * 
     * @param reader to to glob.
     * @return the string read 
     * @throws IOException
     */
    protected static final String read (BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            char[] buffer = new char[fioBufferSize];
            int readLength = reader.read(buffer);
            while (readLength > 0) {
                sb.append(buffer, 0, readLength);
                readLength = reader.read(buffer);
            }
            if (readLength > 0){
                sb.append(buffer, 0, readLength);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    /**
     * Try to read a complete <code>InputStream</code> into a 
     * <code>String</code> using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read(System.in);
     *     
     * @param stream to read from
     * @return the string read or <code>null</code>
     */
    public static final String read (InputStream stream) throws IOException {
        return read (new BufferedReader(
            new InputStreamReader(stream), fioBufferSize
            ));
     }

    /**
     * Try to open and read a complete <code>URL</code> into a 
     * <code>String</code> using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read(new URL("http://w3c.org/"));
     *     
     * @p Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime. 
     * 
     * @param url to read from
     * @return the string read or <code>null</code>
     */
    public static final String read (URL url) throws IOException {
        return read (url.openStream());
    }

    /**
     * Try to open and read a named file into a <code>String</code>
     * using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read("my.xml");
     *     
     * @param name the file's name
     * @return the string read or <code>null</code>
     * @throws IOException 
     */
    public static final String read (String name) 
    throws IOException {
        return read(new BufferedReader(
            new FileReader(name), fioBufferSize
            ));
    }

    protected static final class ReadBytes implements Iterator<byte[]> {
        private InputStream _input;
        private byte[] _next;
        private int _read;
        public ReadBytes (InputStream input, int chunk) 
        throws IOException {
            _input = input;
            _next = new byte[chunk];
            _read = _input.read(_next);
        }
        public final boolean hasNext() {
            return _read > 0;
        }
        public final byte[] next () {
            byte[] data = new byte[_read];
            ByteBuffer.wrap(data).put(_next, 0, _read);
            try {
                _read = _input.read(_next);
                if (_read == -1) {
                    _input.close();
                }
            } catch (IOException e) {
                ;
            }
            return data;
        }
        public final void remove () {}
    }
    public static final Iterator<byte[]> read(InputStream input, int chunk) 
    throws IOException {
        return new ReadBytes(input, chunk);
    }
    protected static final class ReadStrings implements Iterator<String> {
        private InputStream _input;
        private byte[] _next;
        private int _read;
        private CharsetDecoder _decoder;
        public ReadStrings (InputStream input, int chunk, String encoding) 
        throws IOException {
        	_decoder = Charset.forName(encoding).newDecoder();
            _input = input;
            _next = new byte[chunk];
            _read = _input.read(_next);
        }
        public final boolean hasNext() {
            return _read > 0;
        }
        public final String next () {
        	byte[] data = new byte[_read];
        	ByteBuffer buffer = ByteBuffer.wrap(data); 
            buffer.put(_next, 0, _read);
            buffer.flip();
            try {
                _read = _input.read(_next);
                if (_read == -1) {
                    _input.close();
                }
            } catch (IOException e) {
                ;
            }
            CharBuffer decoded = CharBuffer.allocate(data.length); 
            _decoder.decode(buffer, decoded, false);
            decoded.flip();
            return decoded.toString();
        }
        public final void remove () {}
    }
    public static final Iterator<String> read(
		InputStream input, int chunk, String encoding
    	) throws IOException {
    	return null;
    }
    /**
     * ...
     * 
     * @param filename
     * @param text
     * @throws Exception
     */
    public static final void write (String filename, String text) 
    throws Exception {
    	write(filename, text, "UTF-8");
    }
    /**
     * Write a text string to a file in a 
     * 
     * @param filename
     * @param text
     * @param encoding
     * @throws Exception
     */
    public static final void write (String filename, String text, String encoding) 
    throws Exception {
    	OutputStreamWriter osw = new OutputStreamWriter(
			new FileOutputStream(new File(filename)), encoding
			);
    	try {
    		osw.write(text);
    	} finally {
    		osw.close();
    	}
    }
    /**
     * Send to an output stream the content of a byte buffer by chunks of
     * <code>netBufferSize</code>, starting at a given offset and stopping 
     * after a given length, yielding to other threads after each chunk.
     * 
     * @param stream to send output to
     * @param buffer of bytes to read from
     * @param offset to start from
     * @param length of output to send
     * @throws IOException
     * 
     * @p TCP/IP is a reliable stream protocol (TCP) implemented on top
     * of an unreliable packet protocol (IP) and this has implications
     * when programming network peers.
     * 
     * @p As soon as latency between peers rise up, buffering data and sending 
     * it in chunks that fit the common TCP windows is the only way to reach
     * decent performances. On the contrary, unbuffered output may translate
     * into colossal waiste of network bandwith, RAM space and CPU power. 
     * 
     * @p So, to avoid the worse case regardless of the runtime environment, 
     * responses should be buffered at once and then chunked out in blocks 
     * that try their best to fit the local operating system buffers and the
     * usual network packets sizes.
     * 
     * @p Note also the importance of yielding execution to other threads
     * after each chunk is sent in order <em>not</em> to overflow the OS
     * network buffers and avoid as much wait state as possible.
     * 
     */
    public static final void send (
        OutputStream stream, ByteBuffer buffer, int offset, int length
        ) throws IOException {
        buffer.position(offset);
        byte[] bytes = new byte[netBufferSize];
        while (length > netBufferSize) {
            length -= netBufferSize;
            buffer.get(bytes);
            stream.write(bytes);
            stream.flush();
            Thread.yield();
        }
        buffer.get(bytes, 0, length);
        stream.write(bytes, 0, length);
        stream.flush();
    }

    /**
     * Send to an output stream the content of a byte buffer by chunks of
     * <code>netBufferSize</code>, yielding to other threads after each chunk.
     * 
     * @param stream to send output to
     * @param buffer of bytes to read from
     * @throws IOException
     */
    public static final void send (OutputStream stream, ByteBuffer buffer) 
    throws IOException {
        send(stream, buffer, 0, buffer.capacity());
    }

    /**
     * Fill a <code>byte</code> buffer with data read from an 
     * <code>InputStream</code>, starting at a given <code>offset</code>
     * until the buffer is full or the stream is closed, then return
     * the position in the buffer after the last byte received (ie:
     * the length of the buffer if it was filled).
     * 
     * @pre import org.less4j.Simple;
     *import java.net.Socket;
     *
     *byte[] buffer = new byte[4096];
     *Socket conn = new Socket("server", 1234);
     *try {
     *    int pos = Simple.recv(conn.getInputStream(), buffer, 0);
     *    if (pos == buffer.length)
     *        System.out.println("buffer filled");
     *} finally {
     *    conn.close();
     *}
     * 
     * 
     * @param stream to receive from
     * @param buffer of bytes to fill
     * @param offset to start from
     * @return the position in the buffer after the last byte received
     * @throws IOException
     */
    public static final int recv (InputStream stream, byte[] buffer, int offset) 
    throws IOException {
        int len = 0;
        while (offset < buffer.length) {
            len = stream.read(buffer, offset, buffer.length - offset);
            if (len > -1)
                offset += len; 
            else if (len == 0)
                Thread.yield(); // ... wait for input, cooperate!
            else 
                break; // ... end of stream.
        }
        return offset;
    }
    /**
     * Recursively glob files whose names match a regular expression from a 
     * directory to extend a <code>List</code> of <code>File</code>.
     * 
     * @param paths to glob
     * @param regular expression to match 
     * @param files list to extend
     */
    public static final void glob (
        String path, String[] names, Pattern regular, ArrayList<File> files
        ) {
        File file;
        String[] content;
        String filename;
        for (int i=0; i<names.length; i++) {
            filename = path + '/' + names[i];
            file = new File(filename);
            if (file.isHidden()) {
                ;
            } else if (regular.matcher(names[i]).matches()) {
                content = file.list();
                if (content == null) {
                    files.add(file);
                } else {
                    glob(filename, content, regular, files);
                }
            }
        }
    };
    /**
     * Glob from a root path the list of files whose names match a compiled
     * regular expression.
     * 
     * @param root to glob
     * @param regular expression to match
     * @return a list of files
     */
    public static final ArrayList<File> glob (String root, Pattern regular) {
        File file = new File(root);
        ArrayList<File> files = new ArrayList();
        glob(
            file.getAbsolutePath().replace('\\', '/'), 
            file.list(), regular, files
            );
        return files;
    };
    /**
     * Glob from a root path the list of files whose names match a regular 
     * expression string.
     * 
     * @param root to glob
     * @param regular expression to match
     * @return a list of files
     */
    public static final ArrayList<File> glob (String root, String regular) {
        return glob(root, Pattern.compile(regular));
    };
}

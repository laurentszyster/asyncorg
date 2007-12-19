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

package org.async.core;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

/**
 * Functional conveniences to support <a 
 * href="http://cr.yp.to/proto/netstrings.txt"
 * >netstrings</a> applications.
 * 
 * @h3 Synopsis
 * 
 * @p Netstrings provide a safe and efficient way to implement pipelining
 * network protocols (ie: where requests and responses can be sent and
 * received in asynchronous batches).
 * 
 * @pre import org.async.core.Netstring;
 *import java.net.Socket;
 *import java.util.Iterator;
 *
 *Socket conn = new Socket("127.0.0.2", 3999)
 *try {
 *    // send two requests to ansqlite server
 *    Netstring.write(
 *        conn.getOutputStream(), 
 *        "[\"CREATE TABLE relation (s, p, o, c)\", []]", "UTF-8"
 *        );
 *    Netstring.write(
 *        conn.getOutputStream(), 
 *        "[\"SELECT * FROM relation\", []]", "UTF-8"
 *        );
 *    // iterate through the responses received
 *    Iterator netstring = new Netstring.read(
 *      conn.getInputStream(), 16384, "UTF-8"
 *      );
 *    while (netstring.hasNext())
 *        System.out.println (netstring.next());
 *} finally {
 *    conn.close ();
 *}
 */
public final class Netstring {
    
    private static final String 
    _unexpected_end = "unexpected InputStream's end";
    private static final String 
    _invalid_prologue = "invalid netstring prologue"; 
    private static final String 
    _invalid_epilogue = "invalid netstring epilogue"; 
    private static final String 
    _too_long = "too long netstring";
    
    protected static final class NetstringIterator implements Iterator {
        private Object _next;
        private byte[] _buffer;
        private int _end;
        private String _encoding, _error;
        public NetstringIterator (byte[] buffer, String encoding) {
            _end = -1;
            _buffer = buffer;
            _encoding = encoding;
            _next = pull ();
        }
        protected final Object pull () {
            _end++; 
            if (_end >= _buffer.length) {
                _error = "end of netstrings"; return null;
            }
            int pos = _end;
            byte c;
            do {
                c = _buffer[pos];
                if (c == 58) {
                    break; 
                } else if (c < 48 || c > 57) {
                    _error = _invalid_prologue; 
                    return null;
                }
                pos ++;
            } while (pos < _end);
            int len = Integer.parseInt(new String(_buffer, _end, pos - _end));
            pos++;
            _end = pos + len;
            if (_end > _buffer.length){
                len = _buffer.length - pos;
                try {
                    return new String(_buffer, pos, len, _encoding);
                } catch (UnsupportedEncodingException e) {
                    return new String(_buffer, pos, len);
                }
            } else if (_buffer[_end] == 44) {
                try {
                    return new String(_buffer, pos, len, _encoding);
                } catch (UnsupportedEncodingException e) {
                    return new String(_buffer, pos, len);
                }
            } else {
                _error = _invalid_epilogue; return null;
            }
        }
        public final boolean hasNext () {
            return (_next != null);
        }
        public final Object next () {
            if (_next == null)
                throw new NoSuchElementException(_error);
            Object result = _next;
            _next = pull ();
            return result;
        }
        public final void remove () {};
    }
    
    /**
     * Iterate through netstrings found in a byte <code>buffer</code> and
     * decode them from the given character set <code>encoding</cide>.
     * 
     * @pre byte[] buffer = new byte[]{
     *    '0', ':', ',', '1', ':', 'A', ',', '2', ':', 'B', 'C', ','
     *}
     *Iterator netstrings = Netstring.decode(buffer, "UTF-8");
     *String data;
     *while (netstrings.hasNext())
     *    data = (String) netstrings.next();
     *
     *
     * @param buffer a <code>byte</code> array
     * @param encoding
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static final Iterator decode (byte[] buffer, String encoding) {
        return new NetstringIterator(buffer, encoding);
    }

    public static final void encode (byte[] bytes, ByteBuffer bb) 
    throws BufferOverflowException {
        byte[] digits = Integer.toString(bytes.length).getBytes();
        int len = bytes.length + digits.length + 2;
        if (len > bb.remaining()) {
            throw new BufferOverflowException();
        }
        bb.put(digits); // len
        bb.put((byte)58); // :
        bb.put(bytes); // encoded 8-bit byte string
        bb.put((byte)44); // ,
    }
    
    public static final void 
    write (OutputStream stream, byte[] buffer)
    throws IOException {
        byte[] digits = Integer.toString(buffer.length).getBytes();
        ByteBuffer bb = ByteBuffer.wrap(
            new byte[buffer.length + digits.length + 2]
            );
        bb.put(digits); // len
        bb.put((byte)58); // :
        bb.put(buffer); // encoded 8-bit byte string
        bb.put((byte)44); // ,
        stream.write(bb.array());
    }
       
    /**
     * A low level interface to send data from a <code>byte</code> array
     * as one netstring.
     * 
     * @pre byte[] data = new byte[]{
     *    'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'
     *};
     *Socket conn = new Socket("server", 1234);
     *try {
     *    OutputStream os = conn.getOutputStream();
     *    Netstring.write(os, data, 0, 5);
     *    Netstring.write(os, data, 5, 5);
     *} finally {
     *    conn.close();
     *}
     * 
     * @param stream to send output
     * @param buffer the <code>byte</code> from which to send data
     * @param off position of the first byte to send in <code>buffer</code>
     * @param len the number of bytes to send
     * @throws IOException
     */
    public static final void 
    write (OutputStream stream, byte[] buffer, int off, int len) 
    throws IOException {
        byte[] digits = Integer.toString(len).getBytes();
        ByteBuffer bb = ByteBuffer.wrap(new byte[len + digits.length + 2]);
        bb.put(digits); // len
        bb.put((byte)58); // :
        bb.put(buffer, off, len); // encoded 8-bit byte string
        bb.put((byte)44); // ,
        stream.write(bb.array());
    }
    
    /**
     * Iterate through <code>String</code>s, queue the encoded 8-bit byte 
     * netstrings in a single <code>ByteBuffer</code>, then dump it in a 
     * single <code>byte</code> array, write it to the <code>conn</code>
     * socket's output stream and finally flush that stream. 
     * 
     * @pre Iterator netstrings = Static.iter(new String[]{
     *    "one", "two", "three", "four"
     *});
     *OutputStream stream = new Socket("server", 1234).getOutputStream();
     *try {
     *    Netstring.write(stream, netstrings, "UTF-8");
     *} finally {
     *    conn.close();
     *}
     *
     * <p>The code above connects to a host named 'server' on TCP port 1234,
     * sends the netstrings below:</p>
     * 
     * @pre 3:one,3:two,5:three,4:four,
     * 
     * <p>The purpose is to buffer small strings before actually sending
     * data through the socket in order to minimize local overhead and
     * network latency. I found out writing a byte at a time to a TCP
     * socket <code>OutputStream</code> that each byte can waste a few 
     * UDP datagrams and considerably slow down its application.</p>
     * 
     * @param stream to write output
     * @param string to encode as 8-bit bytes
     * @param encoding character set
     * @throws IOException
     */
    public static final void 
    write (OutputStream stream, Iterator strings, String encoding) 
    throws IOException {
        int chunk = 0;
        ArrayList netstrings = new ArrayList();
        byte[] digits, buffer;
        while (strings.hasNext()) {
            buffer = Static.encode((String) strings.next(), encoding);
            digits = Integer.toString(buffer.length).getBytes();
            chunk = chunk + buffer.length + digits.length + 2;
            netstrings.add(new byte[][]{digits, buffer});
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[chunk]);
        Iterator bytes = netstrings.iterator();
        byte[][] digits_buffer;
        while (bytes.hasNext()) {
            digits_buffer = (byte[][]) bytes.next();
            bb.put(digits_buffer[0]); // encoded string's length
            bb.put((byte)58); // :
            bb.put(digits_buffer[1]); // the bytes string encoded
            bb.put((byte)44); // ,
        }
        stream.write(bb.array());
    }
    
    protected static final class ByteCollector implements Iterator {
        private int _limit;
        private byte[] _buffer;
        private InputStream _is = null;
        public String error = null;
        public ByteCollector (InputStream is, int limit)
        throws IOException {
            _is = is;
            _limit = limit;
            _buffer = new byte[Integer.toString(limit).length() + 1];
        }
        public final boolean hasNext () {
            return true; // synchronous API can't stall
        }
        public final Object next () {
            int read = 0, len, c;
            try {
                // read the prologue up to ':', assert numeric only
                do {
                    c = _is.read(); 
                    if (c == 58) {
                        break; 
                    } else if (c < 48 || c > 57)
                        throw new NoSuchElementException(_invalid_prologue);
                    else if (c == -1)
                        break;
                    else if (read < _buffer.length)
                        _buffer[read] = (byte) c;
                    else
                        throw new NoSuchElementException(_too_long);
                    read ++;
                } while (true);
                if (read == 0) {
                    _is = null;
                    return null; // nothing more to read, stop iteration.
                }
                len = Integer.parseInt(new String(_buffer, 0, read));
                if (len > _limit)
                    throw new NoSuchElementException(_too_long);
                
                byte[] bytes = new byte[len];
                read = _is.read(bytes);
                if (read < len)
                    throw new NoSuchElementException(_unexpected_end);
                
                if (_is.read() != 44)
                    throw new NoSuchElementException(_invalid_epilogue);
                
                return bytes;
                
            } catch (IOException e) {
                throw new NoSuchElementException(e.toString());
            }
        }
        public final void remove () {}
    } 
    
    /**
     * Instanciate an <code>Iterator</code> of <code>byte[]</code> received.
     * 
     * @pre InputStream is = new Socket("server", 1234).getInputStream();
     *try {
     *    Iterator bytes = Netstring.read(is, 16384);
     *    byte[] data = bytes.next();
     *    while (data != null)
     *        data = bytes.next();
     *} (catch NoSuchElementException e) {
     *    ; // handle error
     *} finally {
     *    is.close();
     *}
     *
     * <p>The code above opens a connection to "server" on TCP port 1234
     * and receive all netstrings one by one until the connection is closed
     * by the server.</p>
     * 
     * @param conn the <code>Socket</code> to receive from 
     * @param limit the maximum netstring length
     * @return an <code>Iterator</code> of <code>byte[]</code>
     * @throws IOException
     */
    public static final Iterator read (InputStream stream, int limit) 
    throws IOException {
        return new ByteCollector (stream, limit);
    }

    protected static final class StringIterator implements Iterator {
        private Iterator _byteIterator;
        private String _encoding;
        public StringIterator (Iterator byteIterator, String encoding) 
        throws IOException, Error {
            _byteIterator = byteIterator; 
            _encoding = encoding;
        }
        public final boolean hasNext() {
            return _byteIterator.hasNext();
        }
        public final Object next() {
            byte[] buffer = (byte[]) _byteIterator.next();
            try {
                return new String(buffer, _encoding);
            } catch (UnsupportedEncodingException e) {
                return new String(buffer);
            }
        }
        public final void remove () {/* worse than failure, repeated ... */}
    }
    
    /**
     * Instanciate an <code>Iterator</code> of <code>String</code> received
     * and decoded from the given 8-bit character set <code>encoding</code>.
     * 
     * @pre InputStream is = new Socket("server", 1234).getInputStream();
     *try {
     *    Iterator strings = Netstring.read(conn, 16384, "UTF-8");
     *    String data = strings.next ();
     *    while (data != null)
     *        data = strings.next();
     *} (catch NoSuchElementException e) {
     *    ; // handle error
     *} finally {
     *    is.close();
     *}
     *
     * <p>The code above opens a connection to "server" on TCP port 1234,
     * then receive and decode from "UTF-8" all incoming netstrings until the 
     * connection is closed by the server.</p>
     * 
     * @param conn the <code>Socket</code> to receive from 
     * @param limit the maximum netstring length
     * @param encoding the character set to decode <code>byte</code> received
     * @return an <code>Iterator</code> of <code>String</code>
     * @throws IOException
     */
    public final static Iterator read (
        InputStream stream, int limit, String encoding
        ) throws IOException {
        return new StringIterator(
            new ByteCollector(stream, limit), encoding
            );
    }
    
}

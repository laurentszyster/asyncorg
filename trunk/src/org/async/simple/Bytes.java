/*  Copyright (C) 2007 - 2008 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU Lesser General Public License as published by the Free 
 *  Software Foundation; either version 2.1 of the License, or (at your option) 
 *  any later version.
 *  
 *  This library is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 *  FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more 
 *  details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License along 
 *  with this library; if not, write to the Free Software Foundation, Inc., 
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  
 */

package org.async.simple;

import java.io.UnsupportedEncodingException;

import java.util.Iterator;

/**
 * Functional conveniences to support 8-bit bytes protocols.
 */
public final class Bytes {
    /**
     * A convenient constant: "\r\n".
     */
    public static final byte[] CRLF = "\r\n".getBytes();
    /**
     * A convenient constant: "\r\n\r\n".
     */
    public static final byte[] CRLFCRLF = "\r\n\r\n".getBytes();
    /**
     * Find the starting position of a bytes string in another one.
     * 
     * @param what to search
     * @param in a bytes string
     * @param from the given position
     * @return the starting position of a match or -1 if not found
     * 
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    0
     *    ) == 6;
     *
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    5
     *    ) == 6;
     *    
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    7
     *    ) == -1;
     *    
     * @test return Bytes.find(
     *    Bytes.encode("world", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    0
     *    ) == -1;
     */
    public static final int find (byte[] what, byte[] in, int from) {
        int i;
        int limit = in.length - what.length;
        for (; from < limit; from++) {
            if (in[from]==what[0]) {
                for (i=1; i<what.length; i++) {
                    if (in[from+i]!=what[i]) {
                        break;
                    }
                }
                if (i==what.length) {
                    return from;
                }
            }
        }
        for (i=1; i<what.length; i++) {
            if (in[from+i]!=what[i]) {
                break;
            }
        }
        if (i==what.length) {
            return from;
        }
        return -1;
    }
    /**
     * The UTF-8 character set name constant.
     */
    public static final String UTF8 = "UTF-8";
    /**
     * Try to encode a UNICODE string to an named 8-bit bytes character set or
     * use the "default" encoding (whatever it may be).
     * 
     * @param unicode string to encode
     * @param encoding to use
     * @return 8-bit bytes
     */
    public static final byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }
    protected static final class StringsEncoder implements Iterator<byte[]> {
    	private Iterator<String> _unicodes;
    	private String _encoding;
    	public StringsEncoder (Iterator<String> unicodes, String encoding) {
    		_unicodes = unicodes;
    		_encoding = encoding;
    	}
    	public final boolean hasNext() {
    		return _unicodes.hasNext();
    	}
    	public final byte[] next() {
    		return encode(_unicodes.next(), _encoding);
    	}
    	public final void remove () {
    		_unicodes.remove();
    	}
    }
    public static final Iterator<byte[]> encode (
		Iterator<String> unicodes, String encoding
		) {
    	return new StringsEncoder(unicodes, encoding);
    }
    /**
     * Try to decode a UNICODE string to an named 8-bit bytes character set or
     * use the "default" encoding (whatever it may be).
     * 
     * @param bytes to decode
     * @param encoding to use
     * @return a UNICODE string
     */
    public static final String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }

    protected static final class StringsDecoder implements Iterator<String> {
        private Iterator<byte[]> _bytes;
        private String _encoding;
        public StringsDecoder (Iterator<byte[]> bytes, String encoding) {
            _bytes = bytes; 
            _encoding = encoding;
        }
        public final boolean hasNext() {
            return _bytes.hasNext();
        }
        public final String next() {
        	return decode(_bytes.next(), _encoding);
        }
        public final void remove () {
        	_bytes.remove();
        }
    }
    
	public static final Iterator<String> decode (
		Iterator<byte[]> bytes, String encoding
		) {
		return new StringsDecoder(bytes, encoding);
	}
    
}

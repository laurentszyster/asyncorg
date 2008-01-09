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

import java.io.UnsupportedEncodingException;

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
        return -1;
    }
    /**
     * 
     */
    public static final String UTF8 = "UTF-8";
    /**
     * 
     * @param unicode
     * @param encoding
     * @return
     */
    public static final byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }
    /**
     * 
     * @param bytes
     * @param encoding
     * @return
     */
    public static final String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }

}

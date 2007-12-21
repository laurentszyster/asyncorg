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

import java.io.UnsupportedEncodingException;

/**
 * Functional conveniences to support 8-bit bytes protocols.
 */
public class Bytes {

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

    public static final String UTF8 = "UTF-8";

    public static final byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }

    public static final String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }

}

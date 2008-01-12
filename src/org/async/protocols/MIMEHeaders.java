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

package org.async.protocols;

import org.async.simple.Bytes;
import org.async.simple.Strings;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public class MIMEHeaders {
    private static final byte[] _COLON = new byte[]{':'}; 
    private static final byte[] _SPACE = new byte[]{' '}; 
    private static final byte[] _CRLF = "\r\n".getBytes(); 
    private static final void put (
        HashMap headers, String name, String value
        ) {
        if (headers.containsKey(name)) {
            if (headers.get(name) instanceof ArrayList) {
                ((ArrayList) headers.get(name)).add(value);
            } else {
                Object first = headers.get(name);
                ArrayList list = new ArrayList();
                list.add(first);
                list.add(value);
                headers.put(name, list);
            }
        } else {
            headers.put(name, value);
        }
    }
    public static final void update(HashMap headers, byte[] bytes, int pos) {
        int spaceAt, colonAt, crlfAt;
        String name = null, value = "";
        while (pos < bytes.length) {
            spaceAt = Bytes.find(_SPACE, bytes, pos);
            colonAt = Bytes.find(_COLON, bytes, pos);
            if (0 < colonAt && (spaceAt == -1 || colonAt < spaceAt)) {
                if (name != null) {
                    put(headers, name, value.trim());
                }
                name = (new String(bytes, pos, colonAt-pos)).toLowerCase();
                value = "";
                pos = colonAt + 1;
            }
            crlfAt = Bytes.find(_CRLF, bytes, pos);
            if (crlfAt == -1) {
                value = value + (new String(bytes, pos, bytes.length-pos));
                break;
            } else {
                value = value + (new String(bytes, pos, crlfAt-pos));
                pos = crlfAt + 2;
            }
        }
        if (name != null) {
            put(headers, name, value.trim());
        }
    }
    public static final void update(HashMap headers, String string, int pos) {
        int spaceAt, colonAt, crlfAt;
        String name = null, value = "";
        while (pos < string.length()) {
            spaceAt = string.indexOf(" ", pos);
            colonAt = string.indexOf(":", pos);
            if (0 < colonAt && (spaceAt == -1 || colonAt < spaceAt)) {
                if (name != null) {
                    put(headers, name, value.trim());
                }
                name = string.substring(pos, colonAt).toLowerCase();
                value = "";
                pos = colonAt + 1;
            }
            crlfAt = string.indexOf(Strings.CRLF, pos);
            if (crlfAt == -1) {
                value = value + string.substring(pos);
                break;
            } else {
                value = value + string.substring(pos, crlfAt);
                pos = crlfAt + 2;
            }
        }
        if (name != null) {
            put(headers, name, value.trim());
        }
    }
    public static final Iterator<String> options (
        HashMap headers, String name
        ) {
        String options = (String) headers.get(name);
        if (options == null) {
            return null;
        } else {
            return Strings.split(options, ',');
        } 
    }
}

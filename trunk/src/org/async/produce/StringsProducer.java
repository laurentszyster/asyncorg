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

package org.async.produce;

import org.async.chat.Producer;
import org.async.simple.Objects;

import java.util.Iterator;

/**
 * The obvious producer of 8-bit encoded UNICODE strings iterator, with 
 * a convenience for wrapping <code>String</code> arrays.
 * 
 */
public class StringsProducer implements Producer {
    // TODO: replace by a less naive encoder ...
    protected Iterator _strings;
    protected String _encoding;
    public StringsProducer(Iterator strings, String encoding) {
        _strings = strings;
        _encoding = encoding;
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        if (_strings.hasNext()) {
            return ((String) _strings.next()).getBytes(_encoding);
        } else {
            return null;
        }
    }
    /**
     * ...
     * 
     * @param strings
     * @param encoding
     * @return
     * 
     * @pre String[] strings = new String[] {
     *    "GET / HTTP/1.0\r\n",
     *    "Host: 127.0.0.1:8080\r\n",
     *    "\r\n"
     *    }; 
     *StringProducer producer = StringProducer.wrap(strings, "UTF-8");
     *dispatcher.push (producer);
     *
     */
    public static final 
    StringsProducer wrap (String[] strings, String encoding) {
        return new StringsProducer(Objects.iter((Object[])strings), encoding);
    }
}

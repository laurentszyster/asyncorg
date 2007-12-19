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

package org.async.chat.producers;

import org.async.chat.Producer;
import org.async.core.Static;

import java.util.Iterator;

/**
 * The obvious producer of 8-bit bytes iterator, with a convenience for 
 * wrapping <code>String</code> arrays.
 * 
 */
public class BytesProducer implements Producer {
    protected Iterator _bytes;
    public BytesProducer(Iterator bytes) {
        _bytes = bytes;
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        if (_bytes.hasNext()) {
            return (byte[]) _bytes.next();
        } else {
            return null;
        }
    }
    /**
     * ...
     * 
     * @param bytes
     * @return
     * 
     * @pre dispatcher.push (BytesProducer.wrap(new byte[][]{
     *    "GET / HTTP/1.0\r\n".getBytes(),
     *    "Host: 127.0.0.1:8080\r\n".getBytes(),
     *    "\r\n".getBytes()
     *    }));
     * 
     */
    public static final 
    BytesProducer wrap (byte[][] bytes) {
        return new BytesProducer(Static.iter(bytes));
    }
}

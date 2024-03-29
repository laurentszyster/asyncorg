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

package org.async.chat;

import org.simple.Bytes;

public class StringProducer implements Producer {
    private byte[] _buffer;
    public StringProducer (String buffer, String encoding) {
        _buffer = Bytes.encode(buffer, encoding);
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        try {
            return _buffer;
        } finally {
            _buffer = null;
        }
    }

}

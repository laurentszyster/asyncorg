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

package org.async.web;

import org.async.chat.Producer;
import org.simple.Strings;
import org.simple.Bytes;
import java.util.ArrayList;
import java.nio.ByteBuffer;

public class ChunkProducer implements Producer {
    
    private static final byte[] _terminator = "0\r\n\r\n".getBytes();
    
    protected Producer _wrapped = null;
    protected byte[] _footers = _terminator;

    public ChunkProducer(Producer wrapped) {
        _wrapped = wrapped;
    }
    
    public ChunkProducer(Producer wrapped, ArrayList footers) {
        _wrapped = wrapped;
        footers.add(Strings.CRLF);
        _footers = (
            "0\r\n" + Strings.join(Strings.CRLF, footers.iterator())
            ).getBytes();
    }
    
    public boolean stalled() {
        return _wrapped != null && _wrapped.stalled();
    }

    public byte[] more() throws Throwable {
        if (_wrapped == null) {
            return null;
        } else {
            byte[] data = _wrapped.more();
            if (data == null) {
                _wrapped = null;
                return _footers;
            } else {
                byte[] length = Integer.toString(data.length, 16).getBytes();
                ByteBuffer bytes = ByteBuffer.wrap(
                    new byte[data.length + length.length + 4] 
                    );
                bytes.put(length);
                bytes.put(Bytes.CRLF);
                bytes.put(data);
                bytes.put(Bytes.CRLF);
                return bytes.array();
            }
        }
    }

}

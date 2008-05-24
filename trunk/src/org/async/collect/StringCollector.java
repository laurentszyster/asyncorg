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

package org.async.collect;

import org.async.chat.Collector;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class StringCollector implements Collector {
    protected CharsetDecoder _decoder;
    protected StringBuilder _sb;
    public StringCollector (String encoding) {
        _decoder = Charset.forName(encoding).newDecoder();
        _sb = new StringBuilder();
    }
    public StringCollector (StringBuilder buffer, String encoding) {
        _decoder = Charset.forName(encoding).newDecoder();
        _sb = buffer;
    }
    public final void handleData(byte[] data) throws Throwable {
        CharBuffer decoded = CharBuffer.allocate(data.length); 
        _decoder.decode(ByteBuffer.wrap(data), decoded, false);
        decoded.flip();
        _sb.append(decoded.toString()); 
    }
    public final boolean handleTerminator() throws Throwable {
        // _decoder.flush(CharBuffer.allocate(0));
        return true;
    }
    public final String toString() {
        return _sb.toString();
    }
}

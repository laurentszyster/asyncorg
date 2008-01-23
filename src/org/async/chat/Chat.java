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

import java.nio.ByteBuffer;

import org.async.simple.Bytes;

/**
 * A convenience to encapsulate protocols. Usefull for instance to nest
 * a MULTIPART collector in a gzip decoder in a Chunk collector.
 * 
 * @pre collector = new Chat(protocol);
 * 
 * @p ...
 * 
 */
public class Chat implements Channel, Collector {
    protected static final boolean _notEndsWith (
        ByteBuffer haystack, int end, byte[] needle, int length
        ) {
        for (int i=0; i < length; i++) {
            if (haystack.get(end+i) != needle[i]) {
                return true;
            }
        }
        return false;
    }
    protected static final int _findPrefixAtEnd (
        ByteBuffer haystack, byte[] needle
        ) {
        int end = haystack.limit();
        int l = needle.length - 1;
        while (l > 0 && _notEndsWith(haystack, end-l, needle, l)) {
            l--;
        }
        return l;
    }
    protected static final byte[] _collectIn (
        int length, ByteBuffer buffer
        ) {
        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }
    public static final boolean collect (
        Channel channel, Collector collector, ByteBuffer buffer
        ) throws Throwable {
        byte[] bytes;
        Object terminator;
        int pos;
        int lb = buffer.remaining();
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[lb];
            buffer.get(bytes);
            buffer.flip();
        }
        while (lb > 0) {
            terminator = channel.getTerminator();
            if (terminator == null) { // collect all
                collector.handleData(_collectIn(lb, buffer));
            } else if (terminator instanceof Integer) {
                int t = ((Integer)terminator).intValue();
                if (lb < t) { // collect part of chunk
                    channel.setTerminator(t - lb);
                    collector.handleData(_collectIn(lb, buffer));
                } else { // collect end of a chunk and terminate 
                    channel.setTerminator(0);
                    collector.handleData(_collectIn(t, buffer));
                    if (collector.handleTerminator()) {
                        return true;
                    }
                }
            } else { // look for a terminator
                pos = buffer.position();
                byte[] needle = (byte[]) terminator;
                int found = Bytes.find(needle, bytes, pos);
                if (found < 0) { // not found, look for a prefix at the end.
                    found = _findPrefixAtEnd(buffer, needle);
                    if (found == 0) {
                        collector.handleData(_collectIn(lb, buffer));
                    } else if (found != lb) {
                        collector.handleData(_collectIn(lb-found, buffer));
                    }
                    break;
                } else { // found, maybe collect and terminate.
                    if (found > pos) {
                        collector.handleData(_collectIn(found-pos, buffer));
                    }
                    buffer.position(buffer.position()+needle.length);
                    if (collector.handleTerminator()) {
                        return true;
                    }
                }
            }
            lb = buffer.remaining();
        }
        return false;
    }
    private ByteBuffer _buffer;
    private Collector _collector;
    private Object _terminator = null;
    private boolean _stalled = false;
    public Chat(Protocol protocol) {
        _collector = protocol;
        protocol.channel = this;
    };
    public Object getTerminator() {
        return _terminator;
    }
    public void setTerminator() {
        _terminator = null;
    }
    public void setTerminator(int terminator) {
        _terminator = new Integer(terminator);
    }
    public void setTerminator(byte[] terminator) {
        _terminator = terminator;
    }
    public void handleData(byte[] data) throws Throwable {
        if (_buffer == null) {
            _buffer = ByteBuffer.wrap(data);
        } else {
            byte[] left = new byte[_buffer.remaining()];
            _buffer = ByteBuffer.allocate(left.length + data.length);
            _buffer.put(left);
            _buffer.put(data);
            _buffer.flip();
        }
        _stalled = collect(this, _collector, _buffer);
        if (_buffer.remaining() > 0) {
            _buffer.compact();
        } else {
            _buffer = null;
        }
    }
    public boolean handleTerminator() throws Throwable {
        if (_buffer != null) {
            _stalled = collect(this, _collector, _buffer);
            _buffer = null;
        }
        return _stalled;
    }

}

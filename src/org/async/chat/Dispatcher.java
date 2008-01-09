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

import org.async.core.Loop;
import org.async.core.Stream;
import org.async.simple.Bytes;

import java.nio.ByteBuffer;

public abstract class Dispatcher extends Stream implements Channel  {
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
    protected static final byte[] _collectIn (int length, ByteBuffer buffer) {
        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }
    public static final boolean collect (Channel channel, ByteBuffer buffer) 
    throws Throwable {
        Object terminator;
        int pos;
        int lb = buffer.remaining();
        while (lb > 0) {
            terminator = channel.getTerminator();
            if (terminator == null) { // collect all
                channel.handleData(_collectIn(lb, buffer));
            } else if (terminator instanceof Integer) {
                int t = ((Integer)terminator).intValue();
                if (lb < t) { // collect part of chunk
                    channel.setTerminator(t - lb);
                    channel.handleData(_collectIn(lb, buffer));
                } else { // collect end of a chunk and terminate 
                    channel.setTerminator(0);
                    channel.handleData(_collectIn(t, buffer));
                    if (channel.handleTerminator()) {
                        return true;
                    }
                }
            } else { // look for a terminator
                pos = buffer.position();
                byte[] needle = (byte[]) terminator;
                int found = Bytes.find(needle, buffer.array(), pos);
                if (found < 0) { // not found, look for a prefix at the end.
                    found = _findPrefixAtEnd(buffer, needle);
                    if (found == 0) {
                        channel.handleData(_collectIn(lb, buffer));
                    } else if (found != lb) {
                        channel.handleData(_collectIn(lb-found, buffer));
                    }
                    break;
                } else { // found, maybe collect and terminate.
                    if (found > pos) {
                        channel.handleData(_collectIn(found-pos, buffer));
                    }
                    buffer.position(buffer.position()+needle.length);
                    if (channel.handleTerminator()) {
                        return true;
                    }
                }
            }
            lb = buffer.remaining();
        }
        return false;
    }
    protected Object _terminator;
    public Dispatcher () {
        super();
    }
    public Dispatcher (Loop loop) {
        super(loop);
    }
    public Dispatcher (int in, int out) {
        super(in, out);
    }
    public Dispatcher (Loop loop, int in, int out) {
        super(loop, in, out);
    }
    public final Object getTerminator () {
        return _terminator;
    }
    public final void setTerminator(int terminator) {
        _terminator = new Integer(terminator);
    }
    public final void setTerminator(byte[] terminator) {
        if (terminator == null) {
            throw new Error("null byte[] terminator");
        } else if (terminator.length > 0){
            _terminator = terminator;
        } else {
            throw new Error("empty byte[] terminator");
        }
    }
    public final void setTerminator() {
        _terminator = null;
    }
    public final void push (byte[] data) {
        if (data == null) {
            throw new Error("null byte[] pushed");
        } else if (data.length > 0){
            _fifoOut.add(ByteBuffer.wrap(data));
        }
    }
    public final void push (Producer producer) {
        if (producer == null) {
            throw new Error("null Producer pushed");
        } else {
            _fifoOut.add(producer);
        }
    }
    public final void collect () throws Throwable {
        _stalledIn = collect(this, _bufferIn);
    }
    public boolean writable () {
        if (_bufferOut.position() > 0) {
            return true;
        } else if (_fifoOut.isEmpty()) {
            return !_connected;
        } else {
            Object queued = _fifoOut.peek(); 
            return !(
                _connected &&
                (queued instanceof Producer) && 
                ((Producer) queued).stalled()
                );
        }
    }
    public final boolean produce () throws Throwable {
        Object first;
        while (!_fifoOut.isEmpty()) {
            first = _fifoOut.removeFirst();
            if (first == null) {
                if (_bufferOut.position() == 0) {
                    _bufferOut.limit(0);
                    return false;
                } else {
                    _fifoOut.addFirst(null); // we'll be done soon ...
                    break;
                }
            } else if (first instanceof Producer){
                Producer producer = (Producer) first;
                if (producer.stalled()) {
                    break;
                }
                byte[] data = producer.more();
                if (data != null) {
                    _fifoOut.addFirst(producer);
                    if (_fillOut(data)) {
                        break;
                    }
                }
            } else if (_fillOut(((ByteBuffer) first).array())) {
                break; 
            }
        }
        return true;
    }
    public abstract void handleData (byte[] data) throws Throwable ;
    public abstract boolean handleTerminator () throws Throwable ;
}

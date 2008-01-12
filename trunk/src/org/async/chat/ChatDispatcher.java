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

import java.nio.ByteBuffer;

public abstract class ChatDispatcher extends Stream 
implements Channel, Collector  {
    protected Object _terminator;
    public ChatDispatcher () {
        super();
    }
    public ChatDispatcher (Loop loop) {
        super(loop);
    }
    public ChatDispatcher (int in, int out) {
        super(in, out);
    }
    public ChatDispatcher (Loop loop, int in, int out) {
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
        _stalledIn = Collect.collect(this, this, _bufferIn);
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

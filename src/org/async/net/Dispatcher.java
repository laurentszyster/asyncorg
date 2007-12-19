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

package org.async.net;

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.channels.SocketChannel;

import org.async.core.Stream;

/**
 * A <code>Stream</code> dispatcher implementation for <a 
 * href="http://cr.yp.to/protocols/netstring.txt"
 * >netstring</a> network peers.
 * 
 * @h3 Synopsis
 * 
 * @p See org.async.Netlog and org.async.Netlogger for actual implementations 
 * of asynchronous netstring client and server channels. 
 */
public abstract class Dispatcher extends Stream {
    protected int _terminator = 0;
    protected Collector _collector;
    public Dispatcher (int in, int out) {
        super(in, out);
    }
    public final void push (byte[] data) {
        byte[] length = Integer.toString(data.length).getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(
            data.length + length.length + 2
            );
        buffer.put(length);
        buffer.put((byte)':');
        buffer.put(data);
        buffer.put((byte)',');
        _fifoOut.add(buffer);
    }
    public abstract 
        Collector handleCollect (int length) throws Throwable;
    
    public abstract void 
        handleCollected (Collector netstring) throws Throwable;
    
    public final void collect () throws Throwable {
        int prev, pos;
        int lb = _bufferIn.limit();
        int next = _terminator;
        if (next > 0) {
            if (next > lb) {
                _stalledIn = _collector.collect(_bufferIn.array());
                _terminator = next - lb;
                _bufferIn.reset();
                return;
            } else if (_bufferIn.get(next-1) == ',') {
                _stalledIn = _collector.terminate(
                    _bufferIn.get(new byte[next-1]).array()
                    );
                handleCollected(_collector);
                if (_stalledIn) {
                    _terminator = 0;
                    return;
                }
            } else {
                throw new Exception("3 missing comma");
            }
            prev = next;
        } else {
            prev = 0;
        }
        while (prev < lb) {
            pos = -1;
            for (int i=prev; i<lb; i++) {
                if (_bufferIn.get(i) == ':') {
                    pos = i;
                    break;
                }
            }
            if (pos < 0) {
                if (prev > 0) {
                    
                }
            } else {
                
            }
        }
        _stalledIn = false;
        _terminator = 0;
        _bufferIn.reset();
    }
    public boolean writable () {
        return !(
            _bufferOut.position() == 0 && 
            _fifoOut.isEmpty() && 
            ((SocketChannel)_channel).isConnected()
            );
    }
    public final boolean produce () {
        int mark = _bufferOut.limit();
        _bufferOut.limit(_bufferOut.capacity());
        _bufferOut.position(mark);
        ByteBuffer data;
        while (!_fifoOut.isEmpty()) {
            data = (ByteBuffer) _fifoOut.removeFirst();
            if (data == null) {
                if (_bufferOut.position() == 0) {
                    _bufferOut.limit(0);
                    return false;
                } else {
                    _fifoOut.addFirst(null); // we'll be done soon ...
                    break;
                }
            } else {
                try {
                    _bufferOut.put(data);
                } catch (BufferOverflowException e) {
                    int left = _bufferOut.remaining(); 
                    _bufferOut.put(data.array(), 0, left);
                    data.position(left);
                    _fifoOut.addFirst(data.slice());
                    break;
                }
            }
        }
        return true;
    }
    public void handleConnect() {
        log("DEBUG", "handleConnect");
    }
    public void handleClose() {
        log("DEBUG", "handleClose");
    }
}

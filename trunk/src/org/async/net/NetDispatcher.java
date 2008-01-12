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

import org.async.core.Loop;
import org.async.core.Stream;

/**
 * A <code>Stream</code> dispatcher implementation for <a 
 * href="http://cr.yp.to/protocols/netstring.txt"
 * >netstring</a> applications.
 * 
 * @h3 Synopsis
 * 
 * @p See <code>Netlog</code> and <code>Netlogger</code> for implementations 
 * of netstring client and server channels. 
 */
public abstract class NetDispatcher extends Stream {
    
    private static final String 
    ERROR_INVALID_PROLOGUE = "invalid netstring prologue"; 
    
    private static final String 
    ERROR_INVALID_EPILOGUE = "missing comma";
    
    private static final String 
    ERROR_TOO_LONG = "too long netstring";
    
    protected int _terminator = 0;
    protected int _limit = 10; // 9.999.999.999 10GB
    public NetDispatcher () {
        super();
    }
    public NetDispatcher (int in, int out) {
        super(in, out);
    }
    public NetDispatcher (Loop loop) {
        super(loop);
    }
    public NetDispatcher (Loop loop, int in, int out) {
        super(loop, in, out);
    }
    public final void push (byte[] data) {
        byte[] length = Integer.toString(data.length).getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(
            data.length + length.length + 2
            );
        buffer.put(length);
        buffer.put((byte)58);
        buffer.put(data);
        buffer.put((byte)44);
        _fifoOut.add(buffer);
    }
    public final void collect () throws Throwable {
        byte[] data;
        int prev, next, pos, length;
        int lb = _bufferIn.limit();
        if (_terminator > 0) {
            if (_terminator > lb) {
                data = new byte[lb];
                _bufferIn.get(data);
                handleData(data);
                _terminator = _terminator - lb;
                return;
            } else if (_bufferIn.get(_terminator) == 44) {
                data = new byte[_terminator];
                _bufferIn.get(data);
                _bufferIn.position(_terminator);
                _terminator = lb;
                handleData(data);
                _stalledIn = handleTerminator();
                if (_stalledIn) {
                    return;
                }
            } else {
                throw new Exception(ERROR_INVALID_EPILOGUE);
            }
            prev = _terminator;
        } else {
            prev = 0;
        }
        byte[] digits = new byte[10];
        int i;
        byte c = 0;
        while (prev < lb) {
            for (i=0, pos=prev; i < _limit; i++, pos++) {
                c = _bufferIn.get(pos);
                if (c == 58) {
                    break; 
                } else if (c < 48 || c > 57) {
                    throw new Exception(ERROR_INVALID_PROLOGUE); 
                } else {
                    digits[i] = c;
                }
            }
            if (i == _limit) {
                throw new Exception(ERROR_TOO_LONG); 
            }
            if (c != 58) { // Not found!
                break;
            } else { // Found!
                _bufferIn.position(pos+1);
                length = Integer.parseInt(new String(digits, 0, i));
                if (!handleLength(length)) {
                    _stalledIn = true;
                    return;
                };
                next = pos + length + 2;
                if (next > lb) {
                    if (pos < lb) {
                        data = new byte[lb - pos - 2];
                        _bufferIn.get(data);
                        handleData(data);
                    }
                    _terminator = next - lb;
                    return;
                } else if (_bufferIn.get(next - 1) == 44){
                    data = new byte[next - pos - 2];
                    _bufferIn.get(data);
                    _bufferIn.position(next);
                    _terminator = 0;
                    handleData(data);
                    _stalledIn = handleTerminator();
                    if (_stalledIn) {
                        return;
                    }
                } else {
                    throw new Exception(ERROR_INVALID_PROLOGUE); 
                }
            }
            prev = next;
        }
        _terminator = 0;
    }
    public boolean writable () {
        return !(
            _bufferOut.remaining() == 0 && 
            _fifoOut.isEmpty() && 
            _connected
            );
    }
    public final boolean produce () {
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
            } else if (_fillOut(((ByteBuffer) first).array())) {
                break; 
            }
        }
        return true;
    }
    public abstract boolean handleLength (int length) throws Throwable;
    public abstract void handleData (byte[] data) throws Throwable;
    public abstract boolean handleTerminator () throws Throwable;
}

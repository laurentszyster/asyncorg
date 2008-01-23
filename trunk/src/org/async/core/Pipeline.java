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

package org.async.core;

import java.util.LinkedList;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;


public abstract class Pipeline extends Dispatcher {
    protected boolean _stalledIn = false;
    protected ByteBuffer _bufferIn;
    protected ByteBuffer _bufferOut;
    protected LinkedList _fifoOut = new LinkedList();
    public Pipeline () {
        super();
        _bufferIn = ByteBuffer.wrap(new byte[16384]);
        _bufferOut = ByteBuffer.wrap(new byte[16384]);
        return;
    }
    public Pipeline(int in, int out) {
        super();
        _bufferIn = ByteBuffer.wrap(new byte[in]);
        _bufferOut = ByteBuffer.wrap(new byte[out]);
        return;
    }
    public Pipeline (Loop loop) {
        super(loop);
        _bufferIn = ByteBuffer.wrap(new byte[16384]);
        _bufferOut = ByteBuffer.wrap(new byte[16384]);
        return;
    }
    public Pipeline(Loop loop, int in, int out) {
        super(loop);
        _bufferIn = ByteBuffer.wrap(new byte[in]);
        _bufferOut = ByteBuffer.wrap(new byte[out]);
        return;
    }
    public final void push (ByteBuffer data) {
        _fifoOut.add(data);
    }
    public void pull () throws Throwable {
        _stalledIn = false;
        if (_bufferIn.limit() > 0) {
            _bufferIn.flip();
            collect();
            _bufferIn.compact();
        }
    }
    public boolean readable () {
        return !(_stalledIn || (_bufferIn.remaining() == 0));
    }
    public final void handleAccept () throws Throwable {
        throw new Error("Unexpected accept event");
    }
    public final void handleWrite () throws Throwable {
        if (produce()) {
            _bufferOut.flip();
            send(_bufferOut);
            _bufferOut.compact();
        } else {
            close ();
        }
    }
    public final void handleRead () throws Throwable {
        if (recv(_bufferIn) > 0) {
            _bufferIn.flip();
            collect();
            _bufferIn.compact();
        }
    }
    protected final boolean _fillOut (byte[] data) {
        try {
            _bufferOut.put(data); // .put(data) fails on position() :-(
        } catch (BufferOverflowException e) {
            int left = _bufferOut.remaining(); 
            _bufferOut.put(data, 0, left);
            _fifoOut.addFirst(ByteBuffer.wrap(data, left, data.length-left));
            return true;
        }
        return false;
    }
    public final void closeWhenDone () {
        _fifoOut.add(null);
    }
    public abstract void collect () throws Throwable;
    public abstract boolean produce () throws Throwable;
}

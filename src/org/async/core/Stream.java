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
import java.net.SocketAddress;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public abstract class Stream extends Dispatcher {
    protected boolean _stalledIn = false;
    protected ByteBuffer _bufferIn;
    protected ByteBuffer _bufferOut;
    protected LinkedList _fifoOut = new LinkedList();
    public Stream () {
        super();
        _bufferIn = ByteBuffer.wrap(new byte[16384]);
        _bufferOut = ByteBuffer.wrap(new byte[16384]);
        return;
    }
    public Stream(int in, int out) {
        super();
        _bufferIn = ByteBuffer.wrap(new byte[in]);
        _bufferOut = ByteBuffer.wrap(new byte[out]);
        return;
    }
    public Stream (Loop loop) {
        super(loop);
        _bufferIn = ByteBuffer.wrap(new byte[16384]);
        _bufferOut = ByteBuffer.wrap(new byte[16384]);
        return;
    }
    public Stream(Loop loop, int in, int out) {
        super(loop);
        _bufferIn = ByteBuffer.wrap(new byte[in]);
        _bufferOut = ByteBuffer.wrap(new byte[out]);
        return;
    }
    public final void connect (SocketAddress addr) throws Throwable {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        _channel = channel;
        _addr = addr;
        _add();
        if (channel.connect(addr)) {
            _connected = true;
            handleConnect();
        } else {
            _writable = SelectionKey.OP_CONNECT;
        }
    }
    public final int recv (ByteBuffer buffer) throws Throwable {
        int received = -1;
        try {
            received = ((ByteChannel) _channel).read(buffer);
        } catch (IOException e) {
            // ... simply close on IOException
        }
        if (received == -1) {
            close();
            handleClose();
        } else if (received > 0) {
            bytesIn = bytesIn + received;
            whenIn = _loop._now;
        }
        return received;
    }
    public final int send (ByteBuffer buffer) throws Throwable {
        int sent = ((ByteChannel) _channel).write(buffer);
        if (sent > 0) {
            bytesOut = bytesOut + sent;
            whenOut = _loop._now;
        }
        return sent;
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
            if (send(_bufferOut) > 0) {
                _bufferOut.compact();
            }
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
    protected final boolean _fillOut (ByteBuffer data) {
        try {
            _bufferOut.put(data.array()); // .put(data) fails on position() :-(
        } catch (BufferOverflowException e) {
            int left = _bufferOut.remaining(); 
            _bufferOut.put(data.array(), 0, left);
            data.position(left);
            _fifoOut.addFirst(data.slice());
            return true;
        }
        return false;
    }
    public final void closeWhenDone () {
        if (writable()) {
            _fifoOut.add(null);
        } else {
            close();
        }
    }
    public abstract void collect () throws Throwable;
    public abstract boolean produce () throws Throwable;
}

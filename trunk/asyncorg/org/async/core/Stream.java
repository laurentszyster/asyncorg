package org.async.core;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.LinkedList;
import java.net.Socket;
import java.net.SocketException;


public abstract class Stream extends Dispatcher {
    protected boolean _stalledIn = false;
    protected ByteBuffer _bufferIn;
    protected ByteBuffer _bufferOut;
    protected LinkedList _fifoOut = new LinkedList();
    public void push (ByteBuffer data) {
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
    public abstract void collect () throws Throwable;
    public abstract boolean produce () throws Throwable;
    public void createSocket() {
        int in, out; 
        Socket socket = ((SocketChannel) _channel).socket();
        try {
            in = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            in = 16384;
        }
        _bufferIn = ByteBuffer.wrap(new byte[in]);
        try {
            out = socket.getSendBufferSize();
        } catch (SocketException e) {
            out = 16384;
        }
        _bufferOut = ByteBuffer.wrap(new byte[out]);
    }
    public boolean readable () {
        return !(_stalledIn || (_bufferIn.remaining() == 0));
    }
    public final void handleAccept () throws Throwable {
        log("Unexpected accept event", "DEBUG: ");
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
    public final void closeWhenDone () {
        if (writable()) {
            _fifoOut.add(null);
        } else {
            close();
        }
    }
}

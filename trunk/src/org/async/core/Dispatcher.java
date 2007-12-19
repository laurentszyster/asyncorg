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

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import java.io.IOException;

import java.net.SocketAddress;


/**
 * A base abstract class for all dispatchers in <code>org.async</code>.
 * 
 * @p This class provides a (much) more concise and practical API than what 
 * <code>java.nio.channels</code> offers for asynchronous socket, without the 
 * complications required by the presence of threads and the absence
 * of the good multiplexing couple provided by <code>AsyncLoop</code>.
 * 
 * @p It comes with the basic meters demanded by inactivity timeouts, bandwith 
 * throttling and I/O accounting.
 * 
 */
public abstract class Dispatcher extends Call {
    //
    // protected interfaces between AsyncLoop._io and AsynCore, may
    // vary to support something else than <code>java.nio.channels</code>
    //
    protected String _repr;
    protected AbstractSelectableChannel _channel = null;
    protected SelectionKey _key = null;
    protected int _index;
    protected final void _add () {
        _index = _loop._dispatched.size();
        _loop._dispatched.add(this);
    }
    protected final void _remove () {
        _loop._dispatched.remove(_index);
        _channel = null;
        _key.cancel();
        _key.attach(null);
        _key = null;
    }
    protected int _writable = SelectionKey.OP_WRITE;
    protected int _readable = SelectionKey.OP_READ;
    protected boolean _connected = false;
    protected final void _handle () throws Loop.Exit {
        try {
            if (_key.isConnectable() && !_connected) {
                ((SocketChannel) _channel).finishConnect();
                _connected = true;
                _writable = SelectionKey.OP_WRITE;
                handleConnect();
            } else if (_key.isWritable()) {
                handleWrite();
            }
            if (_key.isAcceptable()) {
                handleAccept();
            } else if (_key.isReadable()) {
                if (!_connected) {
                    _connected = true;
                    handleConnect();
                }
                handleRead();
            }
        } catch (Loop.Exit e) {
            throw e;
        } catch (Throwable e) {
            handleError(e);
        }
    }
    // 
    // Application Programming Interfaces
    //
    public long whenIn = -1;
    public long whenOut = -1;
    public long bytesIn = 0;
    public long bytesOut = 0;
    public Dispatcher() {}
    public Dispatcher(Loop loop) {
        _loop = loop;
    }
    public final void log (String message) {
        _loop.log.out(this.toString() + " " + message);
    }
    public final void log (String category, String message) {
        _loop.log.err(category, this.toString() + " " + message);
    }
    public final void log (Throwable exception) {
        _loop.log.traceback(exception);
    }
    public final void bind (SocketAddress addr) throws Throwable {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        _channel = channel;
        _add();
    }
    public final void listen (SocketAddress addr) throws Throwable {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        channel.socket().setReuseAddress(true);
        _channel = channel;
        _add();
        _readable = SelectionKey.OP_ACCEPT;
    }
    public final void connect (SocketAddress addr) throws Throwable {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        _channel = channel;
        _add();
        if (channel.connect(addr)) {
            _connected = true;
            handleConnect();
        } else {
            _writable = SelectionKey.OP_CONNECT;
        }
    }
    public final void accept (Dispatcher dispatcher) throws Throwable {
        ServerSocketChannel channel = (ServerSocketChannel) _channel;
        SocketChannel accepted = channel.accept();
        accepted.configureBlocking(false);
        dispatcher._channel = accepted;
        dispatcher._connected = true;
        dispatcher._add();
        dispatcher.apply(this);
    }
    public final SocketAddress recvfrom (ByteBuffer buffer) 
    throws Throwable {
        int start = buffer.position();
        SocketAddress from = ((DatagramChannel) _channel).receive(buffer);
        int received = buffer.position() - start;
        if (received > 0) {
            bytesIn = bytesIn + received;
            whenIn = _loop._now;
        }
        return from;
    }
    public final int sendto (ByteBuffer buffer, SocketAddress addr) 
    throws Throwable {
        int sent = ((DatagramChannel) _channel).send(buffer, addr);
        if (sent > 0) {
            bytesOut = bytesOut + sent;
            whenOut = _loop._now;
        }
        return sent;
    }
    public final int recv (ByteBuffer buffer) throws Throwable {
        int received = ((ByteChannel) _channel).read(buffer);
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
    public final void close () {
        try {
            _channel.close();
        } catch (IOException e) {
            log(e);
        } finally {
            _connected = false;
            _remove();
        }
    }
    // To override ...
    /**
     * Sets the writable status of this dispatcher in its loop's selector. 
     * 
     * @return <code>true</code> if the dispatcher is writable.
     * 
     * @pre public final int writable() {
     *    return true;
     *}
     */
    public abstract boolean writable();
    /**
     * Sets the readable status of this dispatcher in its loop's selector. 
     * 
     * @return <code>true</code> if the dispatcher is readable.
     * 
     * @pre public final int readable() {
     *    return true;
     *}
     */
    public abstract boolean readable();
    /**
     * Handle an accept event, when the dispatcher is readable and its
     * listening socket .
     * 
     * @throws Throwable
     * 
     * @p Note that this method is not expected 
     */
    public abstract void handleAccept() throws Throwable;
    /**
     * Handle a connect event, when the dispatcher's socket is connected
     * to its peer.
     * 
     * @throws Throwable
     */
    public abstract void handleConnect() throws Throwable;
    /**
     * Handle a write event, when a dispatcher is writable and the socket's 
     * outgoing buffer has room for more output data.
     * 
     * @throws Throwable
     */
    public abstract void handleWrite() throws Throwable;
    /**
     * Handle a read event.
     * 
     * @throws Throwable
     */
    public abstract void handleRead() throws Throwable;
    /**
     * Handle the close event, when a peer closes a readable connection. 
     * 
     * @throws Throwable
     * 
     * @p The purpose of this method is to handle case were something must be 
     * done upon the peer closing the connection. For instance, to scavenge a 
     * dispatcher by creating a new socket:
     * 
     * @pre private SocketAddress addr; 
     *public final void handleClose() throws Throwable {
     *    connect(addr);
     *}
     * 
     * @p This method may also implement the end of a protocol.
     * 
     * @p Note that dispatchers closed by a call to their <code>close</code> 
     * method will not fire this event and therefore not call 
     * <code>handleClose</code>. To handle the finalization of a dispatcher,
     * assign a <code>Simple.Function</code> to its <code>continuation</code>.
     */
    public abstract void handleClose() throws Throwable;
    /**
     * Handle anything throwed by an I/O handler and catched in 
     * <code>Async.dispatch</code>, by default log a traceback and
     * close the dispatcher.
     * 
     * @param error throwed by an I/O handler
     * 
     * @p Note that this is the only non-abstract handler method of
     * <code>AsyncCore</code>, one that its applications may not implement.
     */
    public void handleError(Throwable error) {
        log(error);
        close();
    }
}

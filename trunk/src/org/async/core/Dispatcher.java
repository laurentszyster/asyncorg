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
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import java.net.SocketAddress;

import java.io.IOException;


/**
 * A base abstract class for all dispatchers in <code>org.async</code>.
 * 
 * @h3 Note
 * 
 * @p This class provides a (much) more concise and practical API than 
 * <code>java.nio.channels</code>, without the complications required by the 
 * presence of threads. Note that it comes with the basic meters demanded by 
 * inactivity timeouts, bandwith throttling and I/O accounting.
 * 
 */
public abstract class Dispatcher extends Call {
    //
    // protected interfaces between AsyncLoop._io and AsynCore, may
    // vary to support something else than <code>java.nio.channels</code>
    //
    protected AbstractSelectableChannel _channel = null;
    protected SelectionKey _key = null;
    protected int _writable = SelectionKey.OP_WRITE;
    protected int _readable = SelectionKey.OP_READ;
    protected boolean _connected = false;
    protected SocketAddress _addr; 
    protected String _name = "";
    protected final void _add () {
        _loop._dispatched.put(this._name, this);
    }
    protected final void _remove () {
        _loop._dispatched.remove(_name);
        _channel = null;
        if (_key != null) {
            _key.cancel();
            _key.attach(null);
            _key = null;
        }
    }
    protected final void _handle () throws Loop.Exit {
        int ops = _key.readyOps(); 
        if ((ops & _key.interestOps()) == 0) {
            return;
        }
        try {
            if ((ops & SelectionKey.OP_CONNECT) != 0) {
                ((SocketChannel) _channel).finishConnect();
                _connected = true;
                _writable = SelectionKey.OP_WRITE;
                handleConnect();
            } else if ((ops & SelectionKey.OP_WRITE) != 0) {
                handleWrite();
            }
            if (_key == null) {
                return;
            } else if ((ops & SelectionKey.OP_ACCEPT) != 0) {
                handleAccept();
            } else if ((ops & SelectionKey.OP_READ) != 0) {
                if (!_connected) {
                    _connected = true;
                    _writable = SelectionKey.OP_WRITE;
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
    /**
     * Time in milliseconds at which the <code>Dispatcher</code> was
     * instanciated.
     */
    public long when;
    /**
     * Time in milliseconds at which the last call to <code>recv</code> or
     * <code>recvfrom</code> was made, or <code>-1</code> if none were
     * ever made.
     */
    public long whenIn = -1;
    /**
     * Time in milliseconds at which the last call to <code>send</code> or
     * <code>sendto</code> was made, or <code>-1</code> if none were
     * ever made.
     */
    public long whenOut = -1;
    /**
     * Number of bytes received.
     */
    public long bytesIn = 0;
    /**
     * Number of bytes sent.
     */
    public long bytesOut = 0;
    /**
     * Create a new dispatcher to be added to the <code>Static.loop</code>.
     *
     */
    public Dispatcher () {
        when = Static.loop._now;
    }
    /**
     * Create a new dispatcher to be added to a given <code>Loop</code>.
     *
     * @param loop of this dispatcher
     */
    public Dispatcher(Loop loop) {
        _name = this.toString();
        this._loop = loop;
        when = whenIn = whenOut = loop._now;
    }
    public String toString () {
        return _name;
    }
    /**
     * Log a message to this dispatcher's loop logging interface, categorized
     * by this dispatcher's name.
     * 
     * @param message to log
     */
    public final void log (String message) {
        _loop.log(_name, message);
    }
    /**
     * Log an error message categorized by this dispatcher's name and log
     * the error's traceback.
     * 
     * @param error to log
     */
    public final void log (Throwable error) {
        _loop.log(_name, error.getMessage());
        _loop.log(error);
    }
    public final void listen (SocketAddress address) throws Throwable {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            listen(address, 5);
        } else {
            listen(address, 1000);
        }
    }
    /**
     * ...
     * 
     * @param addr
     * @throws Throwable
     */
    public final void listen (SocketAddress address, int backlog) 
    throws Throwable {
        _name = address.toString();
        _addr = address;
        listen(backlog);
    }
    /**
     * ...
     * 
     * @throws Throwable
     */
    public final void listen (int backlog) throws Throwable {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(_addr, backlog);
        channel.socket().setReuseAddress(true);
        _channel = channel;
        _add();
        _readable = SelectionKey.OP_ACCEPT;
        _connected = true;
    }
    /**
     * 
     * @return a new <code>SocketChannel</code> or <code>null</code>
     */
    public final SocketChannel accept () {
        try {
            return ((ServerSocketChannel) _channel).accept();
        } catch (IOException e) {
            return null;
        }
    }
    /**
     * ...
     * 
     * @param dispatcher
     * @throws Throwable
     */
    public final void accepted (SocketChannel channel) throws Throwable {
        _channel = channel;
        _channel.configureBlocking(false);
        _addr = channel.socket().getRemoteSocketAddress();;
        _name = _addr.toString();
        _writable = 0;
        _add();
    }
    /**
     * Find out wether this dispatcher is connected or not.
     * 
     * @return true if the dispatcher is connected
     */
    public final boolean connected () {
        return _connected;
    }
    /**
     * Set the dispatcher address, update the dispatcher's name to the
     * address string representation and try to connect a socket to that
     * address.
     * 
     * @param address to connect
     * @throws Throwable
     */
    public final void connect (SocketAddress address) throws Throwable {
        _name = address.toString();
        _addr = address;
        connect();
    }
    /**
     * Try to connect to this dispatcher's current address.
     * 
     * @throws Throwable
     */
    public void connect () throws Throwable {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        _channel = channel;
        _add();
        if (channel.connect(_addr)) {
            _connected = true;
            handleConnect();
        } else {
            _writable = SelectionKey.OP_CONNECT;
        }
    }
    /**
     * Try to read from this dispatcher's socket, fill a byte buffer, account
     * and return the number of bytes received or -1 if a close event was
     * signaled by the socket. 
     * 
     * @param buffer to read into
     * @return the number of bytes received, -1 if the dispatcher was closed
     * @throws Throwable
     * 
     * @p Note that this method may <code>close</code> the dispatcher and 
     * call its <code>handleClose</code> method if an <code>IOException</code> 
     * occurs or a close event was signaled by the socket.
     * 
     */
    public int recv (ByteBuffer buffer) throws Throwable {
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
    /**
     * Try to write a buffer's remaining to this dispatcher's socket, account
     * and return the number of bytes sent. 
     * 
     * @param buffer to write from
     * @return the number of bytes sent
     * @throws Throwable
     */
    public int send (ByteBuffer buffer) throws Throwable {
        int sent = ((ByteChannel) _channel).write(buffer);
        if (sent > 0) {
            bytesOut = bytesOut + sent;
            whenOut = _loop._now;
        }
        return sent;
    }
    /**
     * Set the dispatcher address, update the dispatcher's name to the
     * address string representation and try to bind a socket to that
     * address.
     * 
     * @param address to bind to
     * @throws Throwable
     */
    public final void bind (SocketAddress address) throws Throwable {
        _name = address.toString();
        _addr = address;
        bind();
    }
    /**
     * Bind this dispatcher to its socket current address.
     * 
     * @param address to bind to
     * @throws Throwable
     */
    public void bind () throws Throwable {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(_addr);
        _channel = channel;
        _add();
    }
    /**
     * Try to read a datagram from this dispatcher's socket into a byte 
     * buffer, account the number of bytes received and return the address 
     * of the peer or null if no datagram was available. 
     * 
     * @param buffer to read into
     * @return the <code>SocketAddress</code> of the sending peer or null
     * @throws Throwable
     * 
     * @p Note that this method may <code>close</code> the dispatcher and 
     * call its <code>handleClose</code> method if an <code>IOException</code> 
     * occurs.
     * 
     */
    public SocketAddress recvfrom (ByteBuffer buffer) 
    throws Throwable {
        SocketAddress from = null;
        int start = buffer.position();
        try {
            from = ((DatagramChannel) _channel).receive(buffer);
        } catch (IOException e) {
            close();
            handleClose();
        }
        if (from != null) { // read event & nothing to read ? 
            int received = buffer.position() - start;
            if (received > 0) {
                bytesIn = bytesIn + received;
                whenIn = _loop._now;
            }
        }
        return from;
    }
    /**
     * Try to write a buffer's content to a given address, account
     * and return the number of bytes sent. 
     * 
     * @param buffer to write from
     * @return the number of bytes sent
     * @throws Throwable
     */
    public int sendto (ByteBuffer buffer, SocketAddress addr) 
    throws Throwable {
        int sent = ((DatagramChannel) _channel).send(buffer, addr);
        if (sent > 0) {
            bytesOut = bytesOut + sent;
            whenOut = _loop._now;
        }
        return sent;
    }
    public final boolean inactive(int in, int out) {
        return (whenIn < _loop._now - in || whenOut < _loop._now - out);
    }
    /**
     * Try to close this dispatcher's socket or log an error traceback.
     */
    public void close () {
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
     * done upon the peer closing the connection. 
     * 
     * @p For instance, to scavenge a dispatcher by creating a new socket:
     * 
     * @pre public final void handleClose() throws Throwable {
     *    connect();
     *}
     * 
     * @p This method may also implement the end of a protocol where the
     * peer is expected to close the connection. Or to handle an unexpected
     * connection close before the end of a protocol.
     * 
     * @p Note that dispatchers closed by a call to their <code>close</code> 
     * method will not fire this event and therefore not call 
     * <code>handleClose</code>. 
     * 
     * @p To handle the finalization of a dispatcher, to do something once
     * it has been closed, assign instead a <code>Simple.Function</code> to 
     * its <code>continuation</code>.
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

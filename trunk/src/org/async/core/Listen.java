package org.async.core;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public abstract class Listen extends Dispatcher {
    protected boolean _accepting = true;
    public final void listen (SocketAddress addr) throws Throwable {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        channel.socket().setReuseAddress(true);
        _channel = channel;
        _addr = addr;
        _add();
        _readable = SelectionKey.OP_ACCEPT;
    }
    public final void accept (Dispatcher dispatcher) throws Throwable {
        ServerSocketChannel channel = (ServerSocketChannel) _channel;
        SocketChannel accepted = channel.accept();
        if (accepted != null) {
            accepted.configureBlocking(false);
            dispatcher._channel = accepted;
            dispatcher._addr = accepted.socket().getLocalSocketAddress();
            dispatcher._add();
            dispatcher.apply(this);
        }
    }
    public boolean writable () {
        return false;
    } 
    public boolean readable () {
        return _accepting;
    }
    public void handleConnect() throws Throwable {
        throw new Error("Unexpected connect event");
    }
    public void handleRead() throws Throwable {
        throw new Error("Unexpected read event");
    }
    public void handleWrite() throws Throwable {
        throw new Error("Unexpected write event");
    }
}

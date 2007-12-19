package org.async.core;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public abstract class Peer extends Dispatcher {
    public final void bind (SocketAddress addr) throws Throwable {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        _channel = channel;
        _add();
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
}

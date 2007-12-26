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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public abstract class Peer extends Dispatcher {
    public final void bind (SocketAddress addr) throws Throwable {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
        _channel = channel;
        _addr = addr;
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

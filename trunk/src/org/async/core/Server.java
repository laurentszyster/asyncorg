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

import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.HashSet;


public abstract class Server extends Dispatcher {
    protected static class Maintenance extends Scheduled {
        Server _server;
        public Maintenance (Server server) {
            _server = server;
        }
        public final long apply(Loop loop) {
            if (_server._dispatchers.isEmpty()) {
                _server._active = null;
                _server.serverSleep();
                return -1;
            } else {
                _server.serverMaintain();
                return when + _server.precision;
            }
        }
    }
    private Maintenance _active = null;
    protected boolean _accepting = true;
    protected long _accepted = 0; 
    protected long _dispatched = 0; 
    protected HashSet<Pipeline> _dispatchers = new HashSet(); 
    public int precision = 1000;
    public int timeout = 3000;
    public Server () {
        super();
    }
    public Server (Loop loop) {
        super(loop);
    }
    public Object apply (Object arg) throws Throwable {
        log("shutdown { \"when\": " + _loop.now() + " }");
        close();
        return Boolean.TRUE;
    }
    public void close() {
        super.close();
        Iterator<Pipeline> streams = _dispatchers.iterator();
        while (streams.hasNext()) {
            streams.next().closeWhenDone();
        }
    }
    public final boolean writable () {
        return false;
    } 
    public final boolean readable () {
        return _accepting;
    }
    public final void handleAccept() throws Throwable {
        SocketChannel socket = accept();
        if (socket != null) {
            Pipeline channel = serverAccept();
            channel.accepted(socket);
            _dispatchers.add(channel);
            _accepted++;
            if (_active == null) {
                _active = new Maintenance(this);
                _loop.timeout(_active, 3000);
                serverWakeUp();
            }
        }
    }
    public final void handleConnect() throws Throwable {
        throw new Error("Unexpected connect event");
    }
    public final void handleRead() throws Throwable {
        throw new Error("Unexpected read event");
    }
    public final void handleWrite() throws Throwable {
        throw new Error("Unexpected write event");
    }
    public void handleClose() throws Throwable {
        log("unexpected close event");
    }
    public final boolean isActive() {
        return _active != null;
    }
    public final long channelsAccepted () {
        return _accepted;
    }
    public final long channelsDispatched () {
        return _dispatched;
    }
    public final Iterator<Pipeline> channels() {
        return _dispatchers.iterator();
    }
    public final void serverClose(Dispatcher channel) {
        bytesIn += channel.bytesIn;
        bytesOut += channel.bytesOut;
        _dispatched++;
        _dispatchers.remove(channel);
    }
    public final void serverCloseInactivesWhenDone (int in, int out) {
        Pipeline stream;
        Iterator<Pipeline> streams = _dispatchers.iterator();
        while (streams.hasNext()) {
            stream = streams.next();
            if (stream._fifoOut.isEmpty() && stream.inactive(in, out)) {
                stream.closeWhenDone();
            }
        }
    }
    public abstract Pipeline serverAccept();
    public void serverWakeUp () {
        log("wake_up " +
            "{ \"when\": " + _loop.now() + 
            ", \"precision\": " + precision + 
            ", \"timeout\": " + timeout + " }");
    }
    public void serverMaintain() {
        serverCloseInactivesWhenDone(timeout, 0);
    };
    public void serverSleep () {
        log("sleep " +
            "{ \"when\": " + _loop.now() +
            ", \"accepted\": " + _accepted + 
            ", \"dispatched\": " + _dispatched + 
            ", \"bytesIn\": " + bytesIn + 
            ", \"bytesOut\": " + bytesOut + " }");
        _dispatched = _accepted = bytesIn = bytesOut = 0;
    }
}


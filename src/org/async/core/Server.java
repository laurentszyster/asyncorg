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
    protected boolean _accepting = true;
    protected long _accepted = 0; 
    protected long _dispatched = 0; 
    protected HashSet<Dispatcher> _dispatchers = new HashSet(); 
    public Server () {
        super();
    }
    public Server (Loop loop) {
        super(loop);
    }
    public Object apply (Object value) throws Throwable {
        close();
        return Boolean.TRUE;
    }
    public final boolean writable () {
        return false;
    } 
    public boolean readable () {
        return _accepting;
    }
    public void handleAccept() throws Throwable {
        SocketChannel socket = accept();
        if (socket != null) {
            Dispatcher channel = serverAccept();
            channel.accepted(socket);
            _dispatchers.add(channel);
            _accepted++;
            if (_dispatchers.size() == 1) {
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
    public abstract Dispatcher serverAccept() throws Throwable;
    public final Iterator<Dispatcher> serverDispatchers() {
        return _dispatchers.iterator();
    }
    public void serverClose(Dispatcher channel) {
        bytesIn += channel.bytesIn;
        bytesOut += channel.bytesOut;
        _dispatched++;
        _dispatchers.remove(channel);
        if (_dispatchers.isEmpty()) {
            serverSleep();
        }
    }
    public abstract void serverWakeUp();
    public abstract void serverSleep();
}


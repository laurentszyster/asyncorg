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

package org.async;

import org.async.core.Loop;
import org.async.core.Static;
import org.async.core.Loginfo;
import org.async.core.Function;
import org.async.net.Collector;
import org.async.net.Dispatcher;
import org.async.simple.Bytes;

import java.util.HashMap;
import java.util.Iterator;
import java.net.InetSocketAddress;

/**
 * A <code>Loginfo</code> implementation that logs netstrings to a server.
 * 
 * @pre Netlogs netlog = new Netlog();
 *netlog.connect("127.0.0.1", 12345);
 *netlog.connect("127.0.0.2", 12345, "DEBUG");
 *
 */
public class Netlog implements Function, Loginfo {
    protected class Channel extends Dispatcher {
        public final Object apply(Object value) throws Throwable {
            return null;
        }
        public final void handleConnect() throws Throwable {
            log("connected");
        }
        public final Collector handleCollect(int length) throws Throwable {
            throw new Error("Unexpected collect event");
        }
        public final void handleCollected() throws Throwable {
            throw new Error("Unexpected collected event");
        }
        public final void handleClose() throws Throwable {
            if (!_fifoOut.isEmpty()) {
                _bufferOut.clear();
                connect(); // reconnect on failure ...
            }
        }
        public final void push (String data) throws Throwable {
            if (_channel == null) {
                connect(); // reconnect closed channel ...
            }
            push(Bytes.encode(data, Bytes.UTF8));
        }
    }
    protected Loop _loop;
    protected Loginfo _wrapped = null;
    protected Channel _out = new Channel(); 
    protected Channel _traceback = new Channel(); 
    protected HashMap _categories = new HashMap();
    public Netlog () {
        _attach(Static.loop);
    }
    public Netlog (Loop loop) {
        _attach(loop);
    }
    protected final void _attach(Loop loop) {
        _loop = loop;
        _wrapped = _loop.setLogger(this); 
        _loop.exits.add(this);
    }
    protected final void _detach () {
        if (_wrapped != null) try { // try to close all channels ...
            _loop.setLogger(_wrapped);
            disconnect();
        } finally { // ... and make sure to break all channel references
            _wrapped = null; 
            _out = _traceback = null;
            _categories = null;
        }
    }
    public final void connect(InetSocketAddress addr) throws Throwable {
        _out.connect(addr);
    }
    public final void connect(InetSocketAddress addr, String category) 
    throws Throwable {
        if (category.equals("TRACEBACK")) {
            _traceback.connect(addr);
        } else {
            Channel channel = new Channel();
            channel.connect(addr);
            _categories.put(category, channel);
        }
    }
    public final void disconnect() {
        _out.closeWhenDone();
        _traceback.closeWhenDone();
        Iterator category = _categories.values().iterator();
        while (category.hasNext()) {
            ((Channel) category.next()).closeWhenDone();
        }
    }
    public final void disconnect(String category) {
        ((Channel) _categories.remove(category)).closeWhenDone();
    }
    public final Object apply(Object value) {
        _detach();
        return null;
    }
    public final void out (String message) {
        try {
            _out.push(message);
        } catch (Throwable e) {
            _detach();
        }
    }
    public final void err (String category, String message) {
        Channel _err = (Channel) _categories.get(category);
        if (_err == null) {
            _wrapped.err(category, message);
        } else try {
            _err.push(message);
        } catch (Throwable e) {
            _detach();
        }
    }
    public final void traceback (Throwable error) {
        try {
            _traceback.push(error.getMessage());
        } catch (Throwable e) {
            _detach();
        }
    }
}

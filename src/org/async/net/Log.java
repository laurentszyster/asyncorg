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

package org.async.net;

import org.async.core.Loop;
import org.async.core.Loginfo;
import org.async.simple.Bytes;
import org.async.simple.Fun;

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
public class Log implements Fun, Loginfo {
    protected class Channel extends NetDispatcher {
        public final Object apply(Object value) throws Throwable {
            return null;
        }
        public final void handleConnect() throws Throwable {
            log("connected");
        }
        public final boolean handleLength(int length) throws Throwable {
            throw new Error("unexpected input data");
        }
        public final void handleData (byte[] data) {
            throw new Error("unexpectedly reachable code");
        }
        public final boolean handleTerminator() throws Throwable {
            throw new Error("unexpectedly reachable code");
        }
        public final void handleClose() throws Throwable {
            log("close");
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
    protected Channel _traceback = null; 
    protected HashMap _categories = new HashMap();
    public static final Log attach (Loop loop) {
        Log log = (new Log());
        log._attach(loop);
        return log;
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
            _traceback = new Channel();
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
            _wrapped.traceback(e);
        }
    }
    public final void err (String category, String message) {
        Channel _err = (Channel) _categories.get(category);
        if (_err == null) {
            _wrapped.err(category, message);
        } else try {
            _err.push(message);
        } catch (Throwable e) {
            _wrapped.traceback(e);
        }
    }
    public final void traceback (Throwable error) {
        if (_traceback == null) {
            _wrapped.traceback(error);
        } else try {
            StringBuilder sb = new StringBuilder();
            sb.append(error.getClass().getName());
            sb.append(": ");
            sb.append(error.getMessage());
            StackTraceElement[] st = error.getStackTrace();
            for (int i=0; i<st.length; i++) {
                sb.append("\r\n\tat ");
                sb.append(st[i].getClassName());
                sb.append('.');
                sb.append(st[i].getMethodName());
                sb.append('(');
                sb.append(st[i].getFileName());
                sb.append(':');
                sb.append(st[i].getLineNumber());
                sb.append(')');
            }
            _traceback.push(sb.toString());
        } catch (Throwable e) {
            _wrapped.traceback(e);
        }
    }
}

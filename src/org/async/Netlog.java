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

import java.util.HashMap;
import java.util.Iterator;
import java.net.SocketAddress;

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
        public final Object apply(Object value) {
            return null;
        }
        public final void handleConnect() {
            
        }
        public final Collector handleCollect(int length) throws Throwable {
            throw new Error("Unexpected collect event");
        }
        public final void handleCollected() throws Throwable {
            throw new Error("Unexpected collected event");
        }
        public final void handleClose() {
            
        }
        public final void handleError(Throwable error) {
            
        }
    }
    protected Loop _loop;
    protected Loginfo _wrapped = null;
    protected Channel _out = null; 
    protected Channel _traceback = null; 
    protected HashMap _categories = null;
    public Netlog () {
        _attach(Static.loop);
    }
    public Netlog (Loop loop) {
        _attach(loop);
    }
    protected final void _attach(Loop loop) {
        _loop = loop;
        _wrapped = _loop.log; 
        _loop.exits.add(this);
        _loop.log = this;
    }
    public final void connect(SocketAddress addr) {
        
    }
    public final void connect(SocketAddress addr, String category) {
        if (category.equals("TRACEBACK")) {
            
        } else {
            
        }
    }
    public final Object apply(Object value) {
        _out.closeWhenDone();
        _traceback.closeWhenDone();
        Iterator category = _categories.values().iterator();
        while (category.hasNext()) {
            ((Channel) category.next()).closeWhenDone();
        }
        _loop.log = _wrapped;
        _wrapped = null; 
        _loop = null;
        _out = _traceback = null;
        _categories = null;
        return null;
    }
    public final void out (String message) {
        if (_out == null) {
            _wrapped.out(message);
        } else {
            _out.push(Static.encode(message, Static.UTF8));
        }
    }
    public final void err (String category, String message) {
        Channel _err = (Channel) _categories.get(category);
        if (_err == null) {
            _wrapped.err(category, message);
        } else {
            _err.push(Static.encode(message, Static.UTF8));
        }
    }
    public final void traceback (Throwable error) {
        if (_traceback == null) {
            _wrapped.traceback(error);
        } else {
            _traceback.push(Static.encode(error.getMessage(), Static.UTF8));
        }
    }
}

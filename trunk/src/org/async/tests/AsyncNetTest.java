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

package org.async.tests;

import org.async.core.Static;
import org.async.net.Collector;
import org.async.net.Dispatcher;

import java.net.InetSocketAddress;

public final class AsyncNetTest extends Dispatcher {

    public final void handleConnect() {
        log("connected");
        push("one".getBytes());
        push("two".getBytes());
        push("three".getBytes());
    }
    
    public final Collector handleCollect(int length) throws Throwable {
        throw new Error("unexpected input data");
    }

    public final void handleCollected() throws Throwable {
        throw new Error("unexpectedly reachable code");
    }
    
    public final void handleClose() throws Throwable {
        log("closed, bytesOut: " + Long.toString(bytesOut));
    }
    
    public final Object apply(Object input) throws Throwable {
        return null;
    }

    public static final void main (String[] args) throws Throwable {
        (new AsyncNetTest()).connect(
            new InetSocketAddress("127.0.0.2", 12345)
            );
        Static.loop.dispatch();
        System.exit(0);
    }
    
}

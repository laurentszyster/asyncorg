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

import org.async.net.Collector;
import org.async.net.Dispatcher;

public final class AsyncNetCollectTest extends Dispatcher {
    public static final class AsyncNetCollectorTest implements Collector {
        private StringBuffer _sb = new StringBuffer();
        public final boolean collect (byte[] data) {
            _sb.append(new String(data));
            return false;
        }
        public final boolean terminate (byte[] data) {
            _sb.append(new String(data));
            return false;
        }
        public final String toString() {
            return _sb.toString();
        }
    } 
    public final void handleConnect() {
    }
    public final Collector handleCollect(int length) throws Throwable {
        return new AsyncNetCollectorTest();
    }
    public final void handleCollected() throws Throwable {
        log(_collector.toString());
    }
    public final void handleClose() throws Throwable {
    }
    public final Object apply(Object input) throws Throwable {
        _bufferIn.put(((String) input).getBytes());
        _bufferIn.flip();
        collect();
        _bufferIn.compact();
        return null;
    }
    public static final void main (String[] args) throws Throwable {
        AsyncNetCollectTest net = new AsyncNetCollectTest();
        net.apply("3:one,3:two,5:th");
        net.apply("ree,");
    } 
}

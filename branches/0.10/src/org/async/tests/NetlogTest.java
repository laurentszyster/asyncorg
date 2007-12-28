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

import org.async.Netlog;
import org.async.core.Function;
import org.async.core.Static;

import java.net.InetSocketAddress;

public class NetlogTest {
    public static final void main (String[] args) throws Throwable {
        Netlog netlog = new Netlog();
        netlog.connect(new InetSocketAddress("127.0.0.2", 12345));
        netlog.connect(new InetSocketAddress("127.0.0.2", 12345), "TRACEBACK");
        Static.loop.log("one");
        Static.loop.timeout(3000, new Function() {
            public final Object apply (Object current) {
                Static.loop.log("two");
                return new Long(-1);
            }
        });
        Static.loop.dispatch();
    }
}

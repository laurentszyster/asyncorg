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

import org.async.core.Static;
import org.async.net.Log;
import org.simple.Fun;

import java.net.InetSocketAddress;

public class NetlogTest {
    public static final void main (String[] args) throws Throwable {
        Log netlog = Log.attach(Static.loop);
        netlog.connect(
            new InetSocketAddress("127.0.0.2", 12345)
            );
        netlog.connect(
            new InetSocketAddress("127.0.0.2", 12345), "TRACEBACK"
            );
        Static.loop.log("one");
        Static.loop.timeout(2000, new Fun() {
            public final Object apply (Object current) throws Throwable {
                Static.loop.log("two");
                return new Long(-1);
            }
        });
        Static.loop.timeout(4000, new Fun() {
            public final Object apply (Object current) throws Throwable {
                throw new Error("three");
            }
        });
        Static.loop.timeout(6000, new Fun() {
            public final Object apply (Object current) throws Throwable {
                Static.loop.log("four");
                return new Long(-1);
            }
        });
        Static.loop.dispatch();
    }
}

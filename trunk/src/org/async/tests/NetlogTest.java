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

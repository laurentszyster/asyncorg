package org.async.tests;

import org.async.core.Static;
import org.async.protocols.HttpServer;

public class HttpServerTest {
    public static void main (String[] args) throws Throwable {
        Static.loop.hookShutdown();
        try {
            HttpServer server = new HttpServer(".");
            server.httpListen((args.length > 0) ? args[0]: "127.0.0.2:8765");
            server.httpRoute(
                "GET 127.0.0.2:8765/status", "org.async.web.HttpServerStatus"
                );
            Static.loop.exits.add(server);
            server = null;
        } catch (Throwable e) {
            Static.loop.log(e);
        }
        Static.loop.dispatch();
    }
}

package org.async.tests;

import org.async.core.Static;
import org.async.web.HttpServer;
import org.async.web.HttpServerState;
import org.async.web.HttpFile;

public class HttpServerTest {
    public static void main (String[] args) throws Throwable {
        Static.loop.hookShutdown();
        try {
            HttpServer server = new HttpServer(".");
            server.httpListen((args.length > 0) ? args[0]: "127.0.0.2:8765");
            server.httpRoute(
                "GET " + server.httpHost() + "/doc", new HttpFile("doc")
                );
            server.httpRoute(
                "GET " + server.httpHost() + "/state", new HttpServerState()
                );
            Static.loop.exits.add(server);
            server = null;
        } catch (Throwable e) {
            Static.loop.log(e);
        }
        Static.loop.dispatch();
    }
}

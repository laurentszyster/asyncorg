package org.async.tests;

import java.net.InetSocketAddress;

import org.async.core.Static;
import org.async.protocols.HttpServer;
import org.async.protocols.JSON;
import org.async.simple.Bytes;

import java.util.Iterator;

public class HttpServerTest extends HttpServer {
    public boolean httpContinue(HttpServer.Actor http) {
        JSON.Array concurrent = new JSON.Array();
        JSON.Object json = JSON.dict(new Object[]{
            "accepted", new Long(_accepted),
            "concurrent", concurrent
        });
        HttpServer.Channel channel;
        Iterator channels = _dispatchers.iterator();
        while (channels.hasNext()) {
            channel = (HttpServer.Channel) channels.next();
            concurrent.add(JSON.dict(new Object[]{
                "name", channel.toString(),
                "bytesIn", channel.bytesIn,
                "bytesOut", channel.bytesOut
            }));
        }
        http.responseHeader(
            "Content-Type", "text/javascript; charset=UTF-8"
            );
        http.response(200, Bytes.encode(JSON.pprint(json), Bytes.UTF8));
        return false;
    }
    public static void main (String[] args) throws Throwable {
        Static.loop.hookShutdown();
        HttpServer server = new HttpServerTest();
        server.listen(new InetSocketAddress(
            (args.length > 0) ? args[0]: "127.0.0.2", 
            (args.length > 1) ? Integer.parseInt(args[1]): 80
            ));
        Static.loop.exits.add(server);
        server.log("listen { \"when\": " + System.currentTimeMillis() + " }");
        Static.loop.dispatch();
    }
}

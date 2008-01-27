package org.async.web;

import java.util.Iterator;

import org.async.protocols.JSON;
import org.async.simple.Bytes;

public class HttpServerState implements HttpServer.Handler {
    public final boolean httpContinue(HttpServer.Actor http) {
        Runtime rt = Runtime.getRuntime();
        HttpServer server = http.channel().server();
        JSON.Array concurrent = new JSON.Array();
        JSON.Object json = JSON.dict(
            "accepted", server.channelsAccepted(),
            "accepting", server.readable(),
            "active", server.isActive(),
            "bytesIn", server.bytesIn,
            "bytesOut", server.bytesOut,
            "channels", concurrent,
            "date", http.date(),
            "dispatched", server.channelsDispatched(),
            "memory free", rt.freeMemory(),
            "memory maximum", rt.maxMemory(),
            "memory total", rt.totalMemory(),
            "routes", server.httpRoutes()
        );
        HttpServer.Channel channel;
        Iterator channels = server.channels();
        while (channels.hasNext()) {
            channel = (HttpServer.Channel) channels.next();
            concurrent.add(JSON.dict(
                "bytesIn", channel.bytesIn,
                "bytesOut", channel.bytesOut,
                "name", channel.toString(),
                "when", channel.when,
                "whenIn", channel.whenIn,
                "whenOut", channel.whenOut
            ));
        }
        http.responseHeader("Control-Cache", "no-cache");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, Bytes.encode(JSON.pprint(json), Bytes.UTF8));
        return false;
    }
    public final void httpCollected (HttpServer.Actor http) throws Throwable {
        http.channel().log("unexpected request body");
    }
}

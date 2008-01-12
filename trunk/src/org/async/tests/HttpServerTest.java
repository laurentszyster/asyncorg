package org.async.tests;

import java.net.InetSocketAddress;

import org.async.core.Static;
import org.async.protocols.HttpServer;
import org.async.simple.Bytes;

import java.util.Iterator;

public class HttpServerTest extends HttpServer {
    public HttpServerTest () {
        super(16384, 16384);
    }
    public Object apply (Object arg) throws Throwable {
        log("shutdown");
        return super.apply(arg);
    }
    public void serverWakeUp () {
        log("wake up");
    }
    public void serverSleep () {
        if (connected()) {
            log("sleep");
        } else {
            log("stop, bytes in: " + bytesIn + ", out: " + bytesOut);
        }
    }
    private void _repr(HttpServer.Channel conn, StringBuilder sb) {
        sb.append(conn.toString());
        sb.append("<br />Bytes received: ");
        sb.append(conn.bytesIn);
        sb.append(", bytes sent: ");
        sb.append(conn.bytesOut);
    } 
    public boolean httpContinue(HttpServer.Actor http) {
        HttpServer.Channel channel = http.channel();
        if (channel.connected()) {
            StringBuilder body = new StringBuilder();
            body.append("<html><head><title>");
            body.append(http.method());
            body.append(' ');
            body.append(http.uri());
            body.append("</title></head><body><h1>");
            body.append(http.protocol());
            body.append(' ');
            body.append("200");
            body.append("</h1><p>Accepted until now: <strong>");
            body.append(_accepted);
            body.append("</strong></p>");
            Iterator channels = serverDispatchers();
            if (channels.hasNext()) {
                body.append("<p>Concurrent connections</p><ol><li>");
                _repr((HttpServer.Channel) channels.next(), body);
                while (channels.hasNext()) {
                    body.append("</li><li>");
                    _repr((HttpServer.Channel) channels.next(), body);
                }
                body.append("</li></ol>");
            }
            body.append("</body></html>");
            http.responseHeader("Content-Type", "text/html; charset=UTF-8");
            http.response(200, Bytes.encode(body.toString(), Bytes.UTF8));
            Static.loop.log(channel.toString() + " " + http.toString());
        }
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
        Static.loop.dispatch();
    }
}

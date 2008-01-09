package org.async.tests;

import java.net.InetSocketAddress;

import org.async.core.Static;
import org.async.simple.Bytes;
import org.async.chat.ByteProducer;
import org.async.protocols.HttpServer;

import java.util.Iterator;

public class HttpServerTest extends HttpServer {
    public HttpServerTest () {
        super(16384, 16384);
    }
    public boolean httpContinue(HttpServer.Actor http) {
        HttpServer.Channel channel = http.channel();
        if (channel.connected()) {
            StringBuffer body = new StringBuffer();
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
            Iterator channels = _dispatchers.iterator();
            if (channels.hasNext()) {
                body.append("<p>Connections</p><ol><li>");
                body.append(((Channel) channels.next()).toString());
                while (channels.hasNext()) {
                    body.append("</li><li>");
                    body.append(((Channel) channels.next()).toString());
                }
                body.append("</li></ol>");
            }
            body.append("</body></html>");
            byte[] bytes = Bytes.encode(body.toString(), Bytes.UTF8);
            http.responseHeader("Connection", "close");
            http.responseHeader("Content-Length", Integer.toString(bytes.length));
            http.response(200, new ByteProducer(bytes));
            channel.closeWhenDone();
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

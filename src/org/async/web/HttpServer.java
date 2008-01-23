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

package org.async.web;

import org.async.core.Loop;
import org.async.core.Server;
import org.async.core.Pipeline;
import org.async.chat.ChatDispatcher;
import org.async.chat.Producer;
import org.async.chat.Collector;
import org.async.produce.ByteProducer;
import org.async.protocols.HTTP;
import org.async.protocols.MIMEHeaders;
import org.async.simple.Bytes;
import org.async.simple.Strings;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Calendar;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * An extensible HTTP/1.1 server for high-performance stateful applications.
 * 
 * @h3 Synopsis
 * 
 * @pre import org.async.core.Static;
 *import org.async.web.HttpServer;
 * 
 *public class HttpServerTest {
 *    public static void main (String[] args) throws Throwable {
 *        Static.loop.hookShutdown();
 *        try {
 *            HttpServer server = new HttpServer(".");
 *            server.httpListen("127.0.0.1:8765");
 *            server.httpRoute(
 *                "GET localhost:8765/status", "org.async.web.HttpServerStatus"
 *                );
 *            Static.loop.exits.add(server);
 *            server = null;
 *        } catch (Throwable e) {
 *            Static.loop.log(e);
 *        }
 *        Static.loop.dispatch();
 *    }
 *}
 * 
 * Running
 * 
 */
public class HttpServer extends Server {
    public static class Actor implements Producer {
        protected Channel _channel;
        protected String _status = null;
        protected String _method = "GET";
        protected URI _uri = null;
        protected String _protocol = "HTTP/0.9";
        protected String _host;
        protected HashMap<String,String> _requestHeaders = new HashMap();
        protected Collector _requestBody = null;
        protected HashMap<String,String> _responseHeaders = new HashMap();
        protected Producer _responseBody = null;
        private Producer _producer = null;
        public Actor (Channel channel, String request, String buffer, int pos) 
        throws Throwable {
            _channel = channel;
            Iterator parts = Strings.split(request, ' ');
            if (parts.hasNext()) {
                _method = ((String) parts.next()).toUpperCase();
                if (parts.hasNext()) {
                    _uri = new URI((String) parts.next());
                    if (parts.hasNext()) {
                        _protocol = ((String) parts.next()).toUpperCase();
                    }
                } else {
                    _uri = new URI("/");
                }
            }
            MIMEHeaders.update(_requestHeaders, buffer, pos);
            _host = _requestHeaders.get("host");
            if (_host == null) {
                _host = _channel._server._host;
            }
        }
        public String toString() {
            return (
                _channel.toString()  
                + " " + _method 
                + " " + _uri 
                + " " + _protocol 
                + " " + _status
                );
        }
        /**
         * Dispatch the actor to
         * 
         * @param route
         * @return
         * @throws Throwable
         */
        public final boolean action (String route) throws Throwable {
            return _channel._server._handlers.get(route).httpContinue(this);
        }
        public final Calendar calendar () {
            return _channel._server._calendar;
        }
        public final String date () {
            return _channel._server._date;
        }
        public final Channel channel () {
            return _channel;
        }
        public final String method() {
            return _method;
        }
        public final URI uri() {
            return _uri;
        }
        public final String protocol() {
            return _protocol;
        }
        public final String requestHeader(String name) {
            String value = _requestHeaders.get(name);
            if (value == null) {
                return "";
            } else {
                return value;
            }
        };
        public final String cookie(String name) {
            return null;
        }
        public final void cookie(String name, String value) {
        }
        public final HashMap responseHeaders() {
            return _responseHeaders;
        };
        public final String responseHeader(String name) {
            String value = _responseHeaders.get(name);
            if (value == null) {
                return "";
            } else {
                return value;
            }
        };
        public final void responseHeader(String name, String value) {
            _responseHeaders.put(name, value);
        };
        public final String status() {
            return _status;
        }
        public final void response (int status) {
            _status = Integer.toString(status);
            byte[] body = HTTP.RESPONSES.get(_status).getBytes();
            _responseHeaders.put(
                "Content-Length", Integer.toString(body.length)
                );
            _responseBody = new ByteProducer(body);
        }
        public final void response (int status, Producer body) {
            _status = Integer.toString(status);
            _responseBody = body;
        }
        public final void response (int status, byte[] body) {
            _status = Integer.toString(status);
            if (_protocol != "HTTP/1.1") {
                _responseHeaders.put(
                    "Content-Length", Integer.toString(body.length)
                    );
            }
            _responseBody = new ByteProducer(body);
        }
        private boolean _produced = false; 
        public final boolean produced () {
            return _produced;
        }
        public final boolean stalled() {
            return (_status == null || (
                _producer != null && _producer.stalled()
                ));
        }
        public final byte[] more() throws Throwable {
            if (_producer == null) {
                if (_produced) {
                    _channel._server.httpLog(this);
                    return null;
                } else {
                    _produced = true;
                    if (_responseBody != null) {
                        if (_protocol.equals("HTTP/1.1")) {
                            _responseHeaders.put("Transfer-Encoding", "chunked");
                            _producer = new ChunkProducer(_responseBody);
                            if (requestHeader("connection")
                                    .toLowerCase().equals("keep-alive")) {
                                _responseHeaders.put("Connection", "keep-alive");
                            } else {
                                _responseHeaders.put("Connection", "close");
                                _channel.closeWhenDone();
                            }
                        } else {
                            if (
                                _responseHeaders.containsKey("Content-Length") &&
                                requestHeader("connection")
                                    .toLowerCase().equals("keep-alive")
                                ) {
                                _responseHeaders.put("Connection", "keep-alive");
                            } else {
                                _responseHeaders.put("Connection", "close");
                                _channel.closeWhenDone();
                            }
                            _producer = _responseBody;
                        }
                    }
                    String name;
                    StringBuilder sb = new StringBuilder();
                    sb.append(_protocol);
                    sb.append(' ');
                    sb.append(_status);
                    sb.append(' ');
                    sb.append(HTTP.RESPONSES.get(_status));
                    sb.append(Strings.CRLF);
                    sb.append("Server: asyncorg\r\nDate: ");
                    sb.append(_channel._server._date);
                    sb.append(Strings.CRLF);
                    Iterator names = _responseHeaders.keySet().iterator();
                    while (names.hasNext()) {
                        name = (String) names.next();
                        sb.append(name);
                        sb.append(": ");
                        sb.append(_responseHeaders.get(name));
                        sb.append(Strings.CRLF);
                    }
                    sb.append(Strings.CRLF);
                    return Bytes.encode(sb.toString(), "UTF-8");
                }
            } else {
                byte[] data = _producer.more();
                if (data == null) {
                    _channel._server.httpLog(this);
                }
                return data;
            }
        }
    }
    public static class Channel extends ChatDispatcher {
        protected HttpServer _server;
        protected Collector _body = null;
        protected StringBuilder _buffer = new StringBuilder();
        public Channel (HttpServer server) {
            super(server._loop, server._bufferSizeIn, server._bufferSizeOut);
            _server = server;
        }
        public final HttpServer server() {
            return _server;
        }
        public Object apply(Object input) throws Throwable {
            return null;
        }
        public final void handleConnect() throws Throwable {
            setTerminator(Bytes.CRLFCRLF);
        }
        public final void handleData(byte[] data) throws Throwable {
            if (_body == null) {
                _buffer.append(Bytes.decode(data, Bytes.UTF8));
            } else {
                _body.handleData(data);
            }
        }
        public final boolean handleTerminator() throws Throwable {
            if (_body == null) {
                String buffer = _buffer.toString();
                _buffer = new StringBuilder();
                int lb = buffer.length();
                int pos = 0;
                int crlfAt = buffer.indexOf(Strings.CRLF);
                while (crlfAt == pos) {
                    pos += 2;
                    if (pos < lb) {
                        crlfAt = buffer.indexOf(Strings.CRLF, pos);
                    } else {
                        return false; 
                    }
                }
                String request;
                if (crlfAt > -1) {
                    request = buffer.substring(pos, crlfAt);
                } else if (crlfAt == -1 && lb > 0) {
                    request = buffer;
                } else {
                    return false;
                }
                Actor http = new Actor(this, request, buffer, crlfAt + 2);
                push(http);
                if (_server.httpContinue(http)) {
                    return true;
                }
                return httpCollect (http);
            } else if (_body.handleTerminator()) {
                _body = null;
                setTerminator(Bytes.CRLFCRLF);
                if (_server.httpContinue((Actor) _fifoOut.getFirst())) {
                    return true;
                }
            }
            return false;
        }
        public boolean httpCollect(Actor http) {
            if (http._requestBody == null) {
                String method = http._method;
                if (!(
                    method.equals("GET") || 
                    method.equals("HEAD") || 
                    method.equals("DELETE")
                    )) {
                    http.response(500); // server error: no request body
                    return true;
                }
            } else {
                String te = http._requestHeaders.get("transfer-encoding");
                if (te != null && te.toLowerCase().startsWith("chunked")) {
                    _body = new ChunkCollector(
                        this, http._requestBody, http._requestHeaders
                        );
                } else {
                    _body = http._requestBody;
                }
            }
            return false;
        }
        public final void handleClose() throws Throwable {
            // TODO: find out what to do when an HTTP server channel
            //       is closed by the peer (and that should not be done
            //       if it is closed by the server itself ...)
        }
        public final void close () {
            super.close();
            _server.serverClose(this);
            while (!_fifoOut.isEmpty()) {
                Object head = _fifoOut.removeFirst();
                if (head instanceof Actor) {
                    _server.httpContinue((Actor) head);
                }
            }
        }
    }
    public static interface Handler {
        public boolean httpContinue(Actor http) throws Throwable;
    }
    protected int _bufferSizeIn = 16384;
    protected int _bufferSizeOut = 16384;
    protected String _host;
    protected Calendar _calendar = Calendar.getInstance();
    protected String _date;
    protected File _root;
    protected HashMap<String,Handler> _handlers = new HashMap();
    public HttpServer (String root) {
        super();
        _root = new File(root);
    }
    public HttpServer (Loop loop, String root) {
        super(loop);
        _root = new File(root);
    }
    public Pipeline serverAccept() {
        return new Channel(this);
    }
    public void serverWakeUp() {
        super.serverWakeUp();
        _calendar.setTimeInMillis(_loop.now());
        _date = HTTP.date(_calendar);
    }
    public void serverMaintain() {
        super.serverMaintain();
        _calendar.add(Calendar.MILLISECOND, precision);
        _date = HTTP.date(_calendar);
    };
    public final void httpRoute (String route, String className) {
        try {
            _handlers.put(
                route, (Handler) Class.forName(className).newInstance()
                );
        } catch (Throwable e) {
            log(e);
        }
    }
    public final void httpRoute (String route, Handler handler) {
        _handlers.put(route, handler);
    }
    public final Handler httpHandler (String route) {
        return _handlers.get(route);
    }
    public final Iterator<String> httpRoutes () {
        return _handlers.keySet().iterator();
    }
    public final void httpListen(String address) throws Throwable {
        _host = address;
        int colonAt = address.indexOf(':');
        if (colonAt < 0) {
            listen(new InetSocketAddress(address, 80));
        } else {
            listen(new InetSocketAddress(
                address.substring(0, colonAt), 
                Integer.parseInt(address.substring(colonAt+1))
                ));
        }
        log("listen { \"when\": " + System.currentTimeMillis() + " }");
    }
    protected boolean httpContinue(Actor http) {
        try { // to route to a handler, maybe continue ...
            String base = http._method + ' ' + http._host;
            String path = http._uri.getPath();
            String route = base + path;
            if (_handlers.containsKey(route)) { // -> context/subject/predicate
                return _handlers.get(route).httpContinue(http);
            } 
            int slashAt = path.indexOf('/', 1);
            if (slashAt > 0) {
                route = base + path.substring(0, slashAt + 1);
                if (_handlers.containsKey(route)) { // -> context/subject/
                    return _handlers.get(route).httpContinue(http);
                }
            }
            route = base + '/';
            if (_handlers.containsKey(route)) { // -> context/
                return _handlers.get(route).httpContinue(http);
            } 
            http.response(404); // Not Found
        } catch (Throwable e) {
            log(e);
            http.response(500); // Server Error
        }
        return false; // not continued, proceed to collect the next request
    };
    public void httpLog(Actor http) {
        _loop.log(http.toString());
    };
}

/*

This HTTP server routes a request for the URI below:

    "http://context/subject/predicate"

to one of the handlers mapped by the following keys:

    "context/subject/predicate"
    "context/subject/"
    "context/"

or reply with a "404 Not Found" response.

More elaborate routing may be implemented by handlers, usually using PCRE
to match URIs and extract their metadata.

*/
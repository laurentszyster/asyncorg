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
import org.async.protocols.JSON;
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
 */
public class HttpServer extends Server {
    public static class Actor implements Producer {
        protected Channel _channel;
        protected Handler _handler;
        protected String _status = null;
        protected String _method = "GET";
        protected URI _uri = null;
        protected String _protocol = "HTTP/0.9";
        protected String _host;
        protected HashMap<String,String> _requestHeaders = new HashMap();
        protected Collector _requestBody = null;
        protected HashMap<String,String> _responseHeaders = new HashMap();
        protected Producer _responseBody = null;
        protected HashMap<String,String> _responseCookies = null;
        private Producer _producer = null;
        /**
         * The actor's transition state represented as a 
         * <code>JSON.Object</code>.
         */
        public JSON.Object state = null;
        protected Actor (
            Channel channel, String request, String buffer, int pos
            ) throws Throwable {
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
            HTTP.update(_requestHeaders, buffer, pos);
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
        public final boolean handleRequest (String route) throws Throwable {
            return _channel._server._handlers.get(route).handleRequest(this);
        }
        public final void handleCollected (String route) throws Throwable {
            _channel._server._handlers.get(route).handleCollected(this);
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
        public final String requestHeader(String name, String defaultValue) {
            String value = _requestHeaders.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        };
        public final String requestCookie(String name) {
            return null;
        }
        public final Collector requestBody() {
            return _requestBody;
        };
        public final HashMap responseHeaders() {
            return _responseHeaders;
        };
        public final String responseHeader(String name, String defaultValue) {
            String value = _responseHeaders.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        };
        public final void responseCookie(String name, String value) {
            if (_responseCookies == null) {
                _responseCookies = new HashMap<String, String>();
            }
            _responseCookies.put(name, value);
        }
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
        public final void response (
            int status, HashMap<String, String> headers, Producer body
            ) {
            _status = Integer.toString(status);
            _responseHeaders.putAll(headers);
            _responseBody = body;
        }
        public final void response (
            int status, HashMap<String, String> headers
            ) {
            _status = Integer.toString(status);
            _responseHeaders.putAll(headers);
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
                            if (requestHeader("connection", "")
                                    .toLowerCase().equals("keep-alive")) {
                                _responseHeaders.put("Connection", "keep-alive");
                            } else {
                                _responseHeaders.put("Connection", "close");
                                _channel.closeWhenDone();
                            }
                        } else {
                            if (
                                _responseHeaders.containsKey("Content-Length") &&
                                requestHeader("connection", "")
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
        public final void collect (Collector body) {
            _requestBody = body;
        }
        public final void pull (Collector body) {
            _requestBody = body;
            _channel.httpContinue();
            _channel.pull();
        }
    }
    public static class Channel extends ChatDispatcher {
        protected HttpServer _server;
        protected Actor _http;
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
                _http = new Actor(this, request, buffer, crlfAt + 2);
                push(_http);
                if (_server.httpContinue(_http)) {
                    return true;
                }
                httpContinue ();
            } else if (_body.handleTerminator()) {
                setTerminator(Bytes.CRLFCRLF);
                _body = null;
                if (_http._handler != null && _http._requestBody != null) {
                    _http._handler.handleCollected(_http);
                }
                _http = null;
            }
            return false;
        }
        public void httpContinue() {
            if (_http._requestBody == null) {
                String method = _http._method;
                if ((
                    method.equals("GET") || 
                    method.equals("HEAD") || 
                    method.equals("DELETE")
                    )) {
                    return;
                } 
                _body = Collector.DEVNULL;
            } else {
                _body = _http._requestBody;
            }
            String te = _http._requestHeaders.get("transfer-encoding");
            if (te != null && te.toLowerCase().startsWith("chunked")) {
                _body = new ChunkCollector(
                    this, _body, _http._requestHeaders
                    );
            } 
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
    public interface Handler {
        public void handleConfigure(String route) throws Throwable; 
        public boolean handleRequest(Actor http) throws Throwable;
        public void handleCollected(Actor http) throws Throwable;
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
    public final String httpHost () {
        return _host;
    }
    public final void httpRoute (String route, Handler handler) {
        try {
            handler.handleConfigure(route);
            _handlers.put(route, handler);
        } catch (Throwable e) {
            log(e);
        }
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
            Handler handler = http._handler = _handlers.get(route); 
            if (handler!=null) { // -> context/subject/predicate
                return handler.handleRequest(http);
            } 
            int slashAt = path.indexOf('/', 1);
            if (slashAt > 0) {
                route = base + path.substring(0, slashAt);
                http._handler = handler = _handlers.get(route);
                if (handler!=null) { // -> context/subject
                    return handler.handleRequest(http);
                }
            }
            route = base + '/';
            http._handler = handler = _handlers.get(route);
            if (handler!=null) { // -> context/
                return handler.handleRequest(http);
            }
            http.response(404); // Not Found
        } catch (Throwable e) {
            log(e);
            http.response(500); // Server Error
        }
        return false;
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
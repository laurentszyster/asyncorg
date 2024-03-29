/*  Copyright (C) 2007-2008 Laurent A.V. Szyster
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
import org.async.chat.ByteProducer;
import org.async.chat.ChatDispatcher;
import org.async.chat.Producer;
import org.async.chat.Collector;
import org.protocols.HTTP;
import org.protocols.JSON;
import org.simple.Bytes;
import org.simple.Strings;
import org.simple.Objects;

import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Calendar;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * An extensible HTTP/1.1 server for high-performance state-full applications.
 */
public class HttpServer extends Server {
	/**
	 * HTTP headers set by the server.
	 */
    public static final HashSet<String> HEADERS = Objects.set(
		"content-length", 
		"connection", 
		"date", 
		"server", 
		"set-cookie",
		"transfer-encoding"
		);
    /**
     * An Actor holding one HTTP/1.1 resource state transition with support
     * for IRTD2 and JSON. It can be applied to handle simple HTTP/1.0 
     * transitions as well as Web 2.0 transactions.
     * 
     * @h3 Synopsis
     * 
     * @p The <code>HttpServer</code> instantiate <code>Actor</code> instances
     * and pass them to the <code>Handler</code>'s <code>handleRequest</code>
     * and (maybe) the <code>handleCollected</code> instance method.
     * 
     * @p <code>Actor</code> class provides 
     * 
     * @p Note that <code>Actor</code> is a <code>Producer</code> and can
     * therefore be 
     */
    public static class Actor implements Producer {
        private Producer _producer = null;
        protected Channel _channel;
        protected long _when;
        protected String _status = null;
        protected String _method = "GET";
        protected URI _uri = null;
        protected String _protocol = "HTTP/0.9";
        protected String _host;
        protected HashMap<String,String> _requestHeaders = new HashMap();
        protected HashMap<String, String> _requestCookies = null;
        protected Collector _requestBody = null;
        protected HashMap<String,String> _responseHeaders = new HashMap();
        protected Producer _responseBody = null;
        protected HashMap<String, String> _responseCookies = null;
        protected long _objectSize = 0;
        public Controller handler;
        /**
         * An articulated description of the HTTP method, host name and URL path
         * requested, split in two between an origin and a destination.
         * 
         * <p>For instance,</p>
         * 
         * <pre>["GET example.com:8765\/public", "\/index.html"]</pre>
         *  
         * <p>could be the route for:</pre>
         * 
         * <pre>GET example.com:8765/public/index.html</pre>
         * 
         */
        public String[] about;
        /**
         * The identity of this resource state transition user agent.
         */
        public String identity;
        /**
         * The rights (or roles) of the user agent for this resource state 
         * transition 
         */
        public String rights;
        /**
         * The SHA1 digest authenticating the previous resource state
         * transition (ie: request) of its user agent. 
         */
        public String digested;
        /**
         * The SHA1 digest identifying this Actor previous request resource state
         * transition (ie: request) of its user agent. 
         */
        public String digest;
        /**
         * The actor's transition state represented as a 
         * <code>JSON.Object</code>.
         */
        public JSON.Object state = new JSON.Object();
        //
        protected Actor (
            Channel channel, String request, String buffer, int pos
            ) throws Throwable {
            _channel = channel;
            _when = _channel._server._loop.now();
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
        public final long when () {
            return _when;
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
        public final String get(String name) {
            return _requestHeaders.get(name);
        }
        public final String get(String name, String defaultValue) {
            String value = _requestHeaders.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        };
        public final String getCookie(String name) {
            if (_requestCookies == null) {
                _requestCookies = HTTP.cookies(get("cookie", null));
            }
            return _requestCookies.get(name);
        }
        public final Collector body() {
            return _requestBody;
        }
        public final void set(String name, String value) {
        	if (HEADERS.contains(name.toLowerCase())) {
        		throw new Error("HTTP header already set by the server:" + name);
        	}
            _responseHeaders.put(name, value);
        }
        public final void setCookie(String name, String value) {
            if (_responseCookies == null) {
                _responseCookies = new HashMap<String, String>();
            }
            _responseCookies.put(name, value);
        }
        public final String status() {
            return _status;
        }
        public final void error (int status) {
            _status = Integer.toString(status);
            byte[] body = HTTP.RESPONSES.get(_status).getBytes();
            _responseHeaders.put(
                "Content-Length", Integer.toString(body.length)
                );
            _responseBody = new ByteProducer(body);
        	handler = null;
        }
        public final void reply (int status, Producer body) {
            _status = Integer.toString(status);
            _responseBody = body;
        	handler = null;
        }
        public final void reply (
            int status, HashMap<String, String> headers
            ) {
            _status = Integer.toString(status);
            _responseHeaders.putAll(headers);
        	handler = null;
        }
        public final void reply (
            int status, HashMap<String, String> headers, Producer body
            ) {
            _status = Integer.toString(status);
            _responseHeaders.putAll(headers);
            _responseBody = body;
        	handler = null;
        }
        public final void reply (int status, byte[] body) {
            _status = Integer.toString(status);
            if (_protocol != "HTTP/1.1") {
                _responseHeaders.put(
                    "Content-Length", Integer.toString(body.length)
                    );
            }
            _responseBody = new ByteProducer(body);
        	handler = null;
        }
        public final void reply (int status, String body, String encoding) {
            reply(status, Bytes.encode(body, encoding));
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
                            if (get("connection", "keep-alive")
                                    .toLowerCase().equals("keep-alive")) {
                                _responseHeaders.put("Connection", "keep-alive");
                            } else {
                                _responseHeaders.put("Connection", "close");
                                _channel.closeWhenDone();
                            }
                        } else {
                            if (
                                _responseHeaders.containsKey("Content-Length") &&
                                get("connection", "")
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
                    if (_responseCookies != null) {
                        names = _responseCookies.keySet().iterator();
                        while (names.hasNext()) {
                            name = (String) names.next();
                            sb.append("Set-Cookie: ");
                            sb.append(name);
                            sb.append("=");
                            sb.append(_responseCookies.get(name));
                            sb.append(Strings.CRLF);
                        }
                    }
                    sb.append(Strings.CRLF);
                    return Bytes.encode(sb.toString(), "UTF-8");
                }
            } else {
                byte[] data = _producer.more();
                if (data == null) {
                    _channel._server.httpLog(this);
                } else {
                	_objectSize += data.length;
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
                if (_server.httpContinue(_http)) {
                    return true;
                }
                httpContinue ();
            } else if (_body.handleTerminator()) {
                setTerminator(Bytes.CRLFCRLF);
                _body = null;
                if (_http.handler != null && _http._requestBody != null) {
                    try {
                        _http.handler.handleBody(_http);
                    } catch (Throwable e) {
                        log(e);
                        _http.error(500); // Server error
                    }
                }
                _http = null;
            }
            return false;
        }
        public final void httpContinue() {
            push(_http);
            if (_http._requestBody == null) {
                String method = _http._method;
                if ((
                    method.equals("GET") || 
                    method.equals("HEAD") || 
                    method.equals("DELETE")
                    )) {
                    _http = null;
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
            } else {
                String cl = _http._requestHeaders.get("content-length");
                if (cl != null) {
                    setTerminator(Integer.valueOf(cl));
                }
            }
        }
        public final void handleClose() throws Throwable {
            while (!_fifoOut.isEmpty()) {
                Object head = _fifoOut.removeFirst();
                if (head instanceof Actor) {
                    _loop.log("dropped", ((Actor) head).toString());
                }
            }
        }
        public final void close () {
            super.close();
            _server.serverClose(this);
        }
    }
    public interface Controller {
        public boolean handleRequest(Actor http) throws Throwable;
        public void handleBody(Actor http) throws Throwable;
    }
    protected int _bufferSizeIn = 16384;
    protected int _bufferSizeOut = 16384;
    protected String _host;
    protected Calendar _calendar = Calendar.getInstance();
    protected String _date;
    protected File _root;
    protected HashMap<String,Controller> _controllers = new HashMap();
    public boolean test = false;
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
    }
    public final String httpHost () {
        return _host;
    }
    public final String httpDate () {
        return _date;
    }
    /**
     * Route requests in the realm of a path to an <code>Handler</code>
     * instance.
     * 
     * @param path of the realm handled
     * @param handler of requests starting with this path
     */
    public final void httpRoute (String path, Controller handler) {
        _controllers.put(path, handler);
    }
    public final Iterator<String> httpRoutes () {
        return _controllers.keySet().iterator();
    }
    public final Controller httpHandler (String route) {
        return _controllers.get(route);
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
            String path = http._uri.getRawPath();
            String route = http._host + path;
            Controller handler = _controllers.get(route); 
            if (handler != null) { 
            	// ["example.com/context/predicate/subject"]
            	http.about = new String[]{route};
                http.handler = handler;
                return handler.handleRequest(http);
            } 
            int slashAt = path.indexOf('/', 1);
            if (slashAt > 0) {
            	route = http._host + path.substring(0, slashAt);
                handler = _controllers.get(route);
                if (handler != null) {
                	// ["example.com/context", "/predicate/subject"]
                	http.about = new String[]{route, path.substring(slashAt)};
                    http.handler = handler;
                    return handler.handleRequest(http);
                }
            }
            handler = _controllers.get(http._host);
            if (handler != null) { 
            	// ["example.com", "/context/predicate/subject"]
            	http.about = new String[]{http._host, path};
                http.handler = handler;
                return handler.handleRequest(http);
            }
            http.error(404); // Not Found
        } catch (Throwable e) {
            log(e);
            http.error(500); // Server Error
        }
        return false;
    }
    public void httpLog(Actor http) { // TODO: replace with a Fun ?
        _loop.log((
            http._channel.toString()
            + " - " + ((http.identity == null) ? "-" : http.identity)
            + " [" + _date 
            + "] \"" + http._method 
            + " " + http._uri
            + " " + http._protocol 
            + "\" " + http._status
            + " " + http._objectSize
            // TODO: add referrer URL here to comply with common log standards ?
            + " " + ((http.digest == null) ? "-" : http.digest)
            + " " + ((http.digested == null) ? "-" : http.digested)
            ));
    }
}
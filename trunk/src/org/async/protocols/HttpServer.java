package org.async.protocols;

import org.async.core.Server;
import org.async.chat.ByteProducer;
import org.async.chat.Dispatcher;
import org.async.chat.Producer;
import org.async.chat.Collector;
import org.async.simple.Bytes;
import org.async.simple.Objects;
import org.async.simple.Strings;

import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

import java.nio.channels.SocketChannel;

public abstract class HttpServer extends Server {
    public static final String HTTP11 = "HTTP//1.1";
    protected static HashMap _RESPONSES = Objects.dict(new String[]{
        "100", "Continue",
        "101", "Switching Protocols",
        "200", "OK", 
        "201", "Created",
        "202", "Accepted",
        "203", "Non-Authoritative Information",
        "204", "No Content",
        "205", "Reset Content",
        "206", "Partial Content",
        "300", "Multiple Choices",
        "301", "Moved Permanently",
        "302", "Moved Temporarily",
        "303", "See Other",
        "304", "Not Modified",
        "305", "Use Proxy",
        "400", "Bad Request",
        "401", "Unauthorized",
        "402", "Payment Required",
        "403", "Forbidden",
        "404", "Not Found",
        "405", "Method Not Allowed",
        "406", "Not Acceptable",
        "407", "Proxy Authentication Required",
        "408", "Request Time-out",
        "409", "Conflict",
        "410", "Gone",
        "411", "Length Required",
        "412", "Precondition Failed",
        "413", "Request Entity Too Large",
        "414", "Request-URI Too Large",
        "415", "Unsupported Media Type",
        "500", "Internal Server Error", 
        "501", "Not Implemented",
        "502", "Bad Gateway",
        "503", "Service Unavailable",
        "504", "Gateway Time-out",
        "505", "HTTP Version not supported"
        });
    public static class Actor implements Producer {
        protected Channel _channel;
        protected String _status = null;
        protected String _method = "GET";
        protected String _uri = "/";
        protected String _protocol = "HTTP/0.9";
        protected HashMap<String,String> _requestHeaders = new HashMap();
        protected Collector _requestBody = null;
        protected HashMap<String,String> _responseHeaders = new HashMap();
        protected Producer _responseBody = null;
        private Producer _producer = null;
        protected final void request (Iterator parts) {
            if (parts.hasNext()) {
                _method = ((String) parts.next()).toUpperCase();
                if (parts.hasNext()) {
                    _uri = ((String) parts.next());
                    if (parts.hasNext()) {
                        _protocol = ((String) parts.next()).toUpperCase();
                    }
                }
            }
        }
        public String toString() {
            return _method + " " + _uri + " " + _protocol + " " + _status;
        }
        public final Channel channel () {
            return _channel;
        }
        public final String method() {
            return _method;
        }
        public final String uri() {
            return _uri;
        }
        public final String protocol() {
            return _protocol;
        }
        public final HashMap requestHeaders() {
            return _requestHeaders;
        };
        public final String requestHeader(String name) {
            String value = _requestHeaders.get(name);
            if (value == null) {
                return "";
            } else {
                return value;
            }
        };
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
                    return null;
                } else {
                    _produced = true;
                    if (_responseBody == null) {
                        _responseBody = new ByteProducer(new byte[]{});
                    }
                    if (_protocol.equals(HTTP11)) {
                        _responseHeaders.put("Transfer-Encoding", "chunked");
                        _producer = new ChunkProducer(_responseBody);
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
                    String name;
                    StringBuilder sb = new StringBuilder();
                    sb.append(_protocol);
                    sb.append(' ');
                    sb.append(_status);
                    sb.append(' ');
                    sb.append(_RESPONSES.get(_status));
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
                return _producer.more();
            }
        }
    }
    public static class Channel extends Dispatcher {
        protected HttpServer _server;
        protected Collector _body = null;
        protected StringBuilder _buffer = new StringBuilder();
        public Channel (HttpServer server, int in, int out) {
            super(server._loop, in, out);
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
                Actor http = new Actor();
                http._channel = this;
                http.request(Strings.split(request, ' '));
                push(http);
                MIMEHeaders.update(
                    http.requestHeaders(), buffer, crlfAt + 2
                    );
                if (_server.httpContinue(http)) {
                    return true;
                }
                return httpContinue (http);
            } else if (_body.handleTerminator()) {
                _body = null;
                setTerminator(Bytes.CRLFCRLF);
                if (_server.httpContinue((Actor) _fifoOut.getFirst())) {
                    return true;
                }
            }
            return false;
        }
        public boolean httpContinue(Actor http) {
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
                String te = (String) http._requestHeaders.get("transfer-encoding");
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
            while (!_fifoOut.isEmpty()) {
                Object head = _fifoOut.removeFirst();
                if (head instanceof Actor) {
                    _server.httpContinue((Actor) head);
                }
            }
        }
        public final void close () {
            super.close();
            _server._dispatchers.remove(this);
        }
    }
    protected long _accepted = 0; 
    protected HashSet _dispatchers = new HashSet();
    protected int _channelBufferIn = 16384;
    protected int _channelBufferOut = 16384;
    public HttpServer () {
        super();
    }
    public HttpServer (int in, int out) {
        super();
        _channelBufferIn = in;
        _channelBufferOut = out;
    }
    public Iterator connections() {
        return _dispatchers.iterator();
    } 
    public Object apply(Object input) throws Throwable {
        close();
        return null;
    }
    public void handleAccept() throws Throwable {
        SocketChannel socket = accept();
        if (socket != null) {
            Channel channel = new Channel(
                this, _channelBufferIn, _channelBufferOut
                );
            channel.accepted(socket);
            _dispatchers.add(channel);
            _accepted++;
        }
    }
    public void handleClose() throws Throwable {
        Iterator channels = _dispatchers.iterator();
        while (channels.hasNext()) {
            ((Channel) channels.next()).closeWhenDone();
        }
    }
    public abstract boolean httpContinue(Actor http);
}

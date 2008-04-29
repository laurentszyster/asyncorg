package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.HTTP;
import java.io.File;

public class HttpFile implements HttpServer.Handler {
    protected int _pathLength;
    protected String _root;
    protected String _cacheControl;
    public HttpFile() {
        _new(".", "max-age=3600;");
    }
    public HttpFile(String path) {
        _new(path, "max-age=3600;");
    }
    public HttpFile(String path, String cacheControl) {
        _new(path, cacheControl);
    }
    protected final void _new (String path, String cacheControl) {
        _root = (new File(path)).getAbsolutePath().replace('\\', '/');
        _cacheControl = cacheControl;
    }
    public boolean handleIdentify(HttpServer.Actor http) throws Throwable {
        return true;
    }
    public final void handleConfigure(String route) throws Throwable {
        int slashAt = route.indexOf('/');
        if (slashAt < 0) {
            throw new Error("invalid HTTP route identifier");
        } else {
            _pathLength = route.length() - slashAt;
            if (_pathLength == 1) {
                _pathLength = 0;
            } 
        }
    }
    public final boolean handleRequest(HttpServer.Actor http) 
    throws Throwable {
        http.responseHeader("Cache-control", _cacheControl);
        String path = http.uri().getPath();
        if (path.indexOf("../") > -1) {
            http.response(400); // Bad request
        } else {
            // http.channel().log(_root + path.substring(_pathLength));
            File file = new File(_root + path.substring(_pathLength));
            if (file.exists() && file.isFile()) {
                HTTP.Entity entity = new HTTP.FileEntity(file);
                String method = http.method();
                if (method.equals("GET")) {
                    http.response(200, entity.headers, entity.body()); 
                } else if (method.equals("HEAD")) {
                    http.response(200, entity.headers);
                } else {
                    http.response(501); // Not implemented
                }
            } else {
                http.response(404); // Not Found
            }
        }
        return false;
    }
    public final void handleCollected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

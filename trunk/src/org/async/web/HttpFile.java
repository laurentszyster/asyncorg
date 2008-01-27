package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.HTTP;
import java.io.File;

public class HttpFile implements HttpServer.Handler {
    protected String _root;
    protected String _cacheControl;
    public HttpFile() {
        configure(".", "max-age=3600;");
    }
    public HttpFile(String path) {
        configure(path, "max-age=3600;");
    }
    public HttpFile(String path, String cacheControl) {
        configure(path, cacheControl);
    }
    public final void configure (String path, String cacheControl) {
        _root = (new File(path)).getAbsolutePath();
        _cacheControl = cacheControl;
    }
    public final boolean httpContinue(HttpServer.Actor http) 
    throws Throwable {
        http.responseHeader("Cache-control", _cacheControl);
        String path = http.uri().getPath();
        if (path.indexOf("../") > -1) {
            http.response(400); // Bad request
        } else {
            File file = new File(_root + path);
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
    public final void httpCollected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

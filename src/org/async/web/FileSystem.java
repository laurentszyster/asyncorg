package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.HTTP;
import java.io.File;

public class FileSystem implements HttpServer.Handler {
    protected String _root;
    protected String _cacheControl;
    public FileSystem() {
        _new(".", "max-age=3600;");
    }
    public FileSystem(String path) {
        _new(path, "max-age=3600;");
    }
    public FileSystem(String path, String cacheControl) {
        _new(path, cacheControl);
    }
    protected final void _new (String path, String cacheControl) {
        _root = (new File(path)).getAbsolutePath().replace('\\', '/');
        _cacheControl = cacheControl;
    }
    public boolean identify(HttpServer.Actor http) throws Throwable {
        return true;
    }
    public final void configure(String route) throws Throwable {
    }
    public final boolean request(HttpServer.Actor http) 
    throws Throwable {
        String path = http.uri().getPath();
        if (path.indexOf("../") > -1) {
            http.response(400); // Bad request
        } else {
        	File file = new File(_root + http.about[1]);
            if (file.exists() && file.isFile() && !file.isHidden()) {
                HTTP.Entity entity = new HTTP.FileEntity(file);
                String method = http.method();
                if (method.equals("GET")) {
                    http.responseHeader("Cache-control", _cacheControl);
                    http.response(200, entity.headers, entity.body()); 
                } else if (method.equals("HEAD")) {
                    http.responseHeader("Cache-control", _cacheControl);
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
    public final void collected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

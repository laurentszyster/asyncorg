package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.HTTP;
import org.async.simple.SIO;
import java.io.File;
import java.util.Iterator;
import java.util.HashMap;

/**
 * ...
 */
public class HttpFileCache implements HttpServer.Handler {
    protected String _path;
    protected String _root;
    protected HashMap<String,HTTP.Entity> _cache;
    protected String _cacheControl;
    public HttpFileCache() 
    throws Throwable {
        _new(".", "max-age=3600;");
    }
    public HttpFileCache(String path) 
    throws Throwable {
        _new(path, "max-age=3600;");
    }
    public HttpFileCache(String path, String cacheControl) 
    throws Throwable {
        _new(path, cacheControl);
    }
    protected final void _new (String path, String cacheControl) 
    throws Throwable {
        // load entities in the cache, set content-type, Etag and last-modified
        _root = (new File(path)).getAbsolutePath();
        _cacheControl = cacheControl;
    }
    public final void handleConfigure(String route) throws Throwable {
        int slashAt = route.indexOf('/');
        if (slashAt < 0) {
            throw new Error("invalid HTTP route identifier");
        } else {
            _path = route.substring(slashAt);
        }
        _cache = new HashMap<String, HTTP.Entity>();
        HTTP.FileEntity entity;
        int rootLength = _root.length();
        Iterator<File> files = SIO.glob(_root, "^[^.].*$").iterator();
        while (files.hasNext()) {
            entity = new HTTP.FileEntity(files.next());
            _cache.put(
                _path + entity.absolutePath.substring(rootLength).replace('\\', '/'), 
                new HTTP.CacheEntity(entity)
                );
        }
    }
    public final boolean handleRequest(HttpServer.Actor http) 
    throws Throwable {
        http.responseHeader("Cache-control", _cacheControl);
        HTTP.Entity entity = _cache.get(http.uri().getPath());
        if (entity == null) {
            http.response(404); // Not Found
        } else {
            String method = http.method();
            if (method.equals("GET")) {
                http.response(200, entity.headers, entity.body()); 
            } else if (method.equals("HEAD")) {
                http.response(200, entity.headers);
            } else {
                http.response(501); // Not implemented
            }
        }
        return false;
    }
    public final void handleCollected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

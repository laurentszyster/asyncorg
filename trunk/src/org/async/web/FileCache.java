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
public class FileCache implements HttpServer.Handler {
    protected String _path;
    protected String _root;
    protected HashMap<String,HTTP.Entity> _cache;
    protected String _cacheControl;
    public FileCache() 
    throws Throwable {
        _new(".", "max-age=3600;");
    }
    public FileCache(String path) 
    throws Throwable {
        _new(path, "max-age=3600;");
    }
    public FileCache(String path, String cacheControl) 
    throws Throwable {
        _new(path, cacheControl);
    }
    protected final void _new (String path, String cacheControl) 
    throws Throwable {
        // load entities in the cache, set content-type, Etag and last-modified
        _root = (new File(path)).getAbsolutePath();
        _cacheControl = cacheControl;
    }
    public boolean identify(HttpServer.Actor http) throws Throwable {
        return true;
    }
    public final void configure(String route) throws Throwable {
        _cache = new HashMap<String, HTTP.Entity>();
        HTTP.FileEntity entity;
        String key;
        int rootLength = _root.length();
        Iterator<File> files = SIO.glob(_root, "^[^.].*$").iterator();
        while (files.hasNext()) {
            entity = new HTTP.FileEntity(files.next());
            key = entity.absolutePath.substring(rootLength).replace('\\', '/');
            _cache.put(key, new HTTP.CacheEntity(entity));
        }
    }
    public final boolean request(HttpServer.Actor http) 
    throws Throwable {
        http.responseHeader("Cache-control", _cacheControl);
        String key = http.uri().getPath();
        HTTP.Entity entity = _cache.get(key);
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
    public final void collected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

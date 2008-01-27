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
    protected HashMap<String,HTTP.Entity> _cache = new HashMap();
    protected String _cacheControl;
    public HttpFileCache() {
        configure(".", "max-age=3600;");
    }
    public HttpFileCache(String path) {
        configure(path, "max-age=3600;");
    }
    public HttpFileCache(String path, String cacheControl) {
        configure(path, cacheControl);
    }
    public final void configure (String path, String cacheControl) {
        String root = (new File(path)).getAbsolutePath();
        int rootLength = root.length();
        _cacheControl = cacheControl;
        // load entities in the cache, set content-type, Etag and last-modified
        HTTP.FileEntity entity;
        Iterator<File> files = SIO.glob(root, "[^.]^.*$").iterator();
        while (files.hasNext()) {
            entity = new HTTP.FileEntity(files.next());
            _cache.put(entity.absolutePath.substring(rootLength), entity);
        }
    }
    public final boolean httpContinue(HttpServer.Actor http) 
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
    public final void httpCollected(HttpServer.Actor http) {
        // pass, there's nothing to do for an unexpected request body.
    }
}

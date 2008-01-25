package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.HTTP;
import org.async.protocols.SHA1;
import org.async.simple.SIO;
import java.io.File;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * ...
 */
public class HttpCache implements HttpServer.Handler {
    protected HashMap<String,HTTP.Entity> _cache = new HashMap();
    protected HashSet<String> _etags = new HashSet();
    public String path = ".";
    public String cacheControl = "max-age=3600;";
    public final void configure(HttpServer server) {
        // load entities in the cache, set content-type, Etag and last-modified
        HTTP.Entity entity;
        String name;
        String length;
        long lastModified;
        SHA1 sha1;
        StringBuffer sb;
        Iterator<File> files = SIO.glob(path, "[^.]^.*$").iterator();
        while (files.hasNext()) {
            File file = files.next();
            // Etag
            sha1 = new SHA1();
            sb = new StringBuffer();
            name = file.getAbsolutePath();
            sb.append(name);
            length = Long.toString(file.length());
            sb.append(length);
            lastModified = file.lastModified();
            sb.append(lastModified);
            sha1.update(sb.toString().getBytes());
            // Entity
            entity = new HTTP.Entity();
            entity.headers.put("Content-Length", length);
            entity.headers.put("Last-Modified", HTTP.date(lastModified));
            _cache.put(name, entity);
        }
    }
    public final boolean httpContinue(HttpServer.Actor http) 
    throws Throwable {
        http.responseHeader("Cache-control", cacheControl);
        HTTP.Entity entity = _cache.get(http.uri().getPath());
        if (entity == null) {
            http.response(404); // Not Found
        } else {
            String method = http.method();
            if (method.equals("GET")) {
                http.response(200, entity.headers, entity.body); 
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

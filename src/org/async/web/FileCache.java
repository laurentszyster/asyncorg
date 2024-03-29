package org.async.web;

import org.async.web.HttpServer;
import org.async.chat.BytesProducer;
import org.protocols.HTTP;
import org.simple.SIO;
import java.io.File;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * ...
 */
public class FileCache implements HttpServer.Controller {
    public static final class Entity extends HTTP.Entity {
        private LinkedList<byte[]> _bytes = new LinkedList<byte[]>(); 
        public Entity (HTTP.Entity cached) throws Throwable {
            headers = cached.headers;
            _bytes = BytesProducer.tee(cached.body());
        }
        public Iterator<byte[]> body () {
            return _bytes.iterator(); 
        }
    }
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
        _cache = new HashMap<String, HTTP.Entity>();
        HTTP.FileEntity entity;
        String key;
        int rootLength = _root.length();
        Iterator<File> files = SIO.glob(_root, "^[^.].*$").iterator();
        while (files.hasNext()) {
            entity = new HTTP.FileEntity(files.next());
            key = entity.absolutePath.substring(rootLength).replace('\\', '/');
            _cache.put(key, new Entity(entity));
        }
    }
    public final boolean handleRequest(HttpServer.Actor http) 
    throws Throwable {
        String key = http.uri().getPath();
        HTTP.Entity entity = _cache.get(key);
        if (entity == null) {
            http.error(404); // Not Found
        } else {
            String method = http.method();
            if (method.equals("GET")) {
                http.set("Cache-control", _cacheControl);
                http.reply(200, entity.headers, new BytesProducer(entity.body())); 
            } else if (method.equals("HEAD")) {
                http.set("Cache-control", _cacheControl);
                http.reply(200, entity.headers);
            } else {
                http.error(501); // Not implemented
            }
        }
        return false;
    }
    public final void handleBody(HttpServer.Actor http) {
		throw new Error("unexpected call");
    }
}

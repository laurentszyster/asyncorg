package org.async.web;

import org.async.web.HttpServer;
import org.async.chat.BytesProducer;
import org.protocols.HTTP;
import java.io.File;

public class FileSystem implements HttpServer.Controller {
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
    public final boolean handleRequest(HttpServer.Actor http) 
    throws Throwable {
        String path = http.uri().getPath();
        if (path.indexOf("../") > -1) {
            http.error(400); // Bad request
        } else {
        	File file = new File(_root + http.about[1]);
            if (file.exists() && file.isFile() && !file.isHidden()) {
                HTTP.Entity entity = new HTTP.FileEntity(file);
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
            } else {
                http.error(404); // Not Found
            }
        }
        return false;
    }
    public final void handleBody(HttpServer.Actor http) {
		throw new Error("unexpected call");
    }
}

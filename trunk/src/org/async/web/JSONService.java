package org.async.web;

import org.async.web.HttpServer;
import org.async.protocols.JSON;
import org.async.protocols.JSONR;

/**
 * ...
 */
public abstract class JSONService implements HttpServer.Handler {
    protected JSONR.Type _model;
    public JSONService (JSONR.Type model) {
        _model = model;
    }
    // The interface.
    public abstract String transition ();
    public abstract boolean identify (HttpServer.Actor http);
    public abstract boolean identified (HttpServer.Actor http);
    public abstract boolean apply (HttpServer.Actor http);
    public final boolean httpContinue(HttpServer.Actor http) 
    throws Throwable {
        if (!(identified(http)||identify(http))) {
            http.response(403); // Not authorized
        } else {
            String method = http.method();
            if (method.equals("GET")) {
                // urlencoded and/or X-JSON input
                apply(http);
            } else if (method.equals("POST")) {
                // JSON and/or X-JSON encoded input
                if (http.requestHeader("content-type").startsWith(
                    "text/javascript"
                    )) {
                    // assign a UTF-8 decoding collector
                } else {
                    http.response(400); // Bad request
                }
            } else {
                http.response(501); // Not implemented
            }
        }
        return false;
    }

    public final void httpCollected(HttpServer.Actor http) 
    throws Throwable {
        // parse and validate the response
        if (_model == null) {
            ;
        }
        apply(http);
    }
}

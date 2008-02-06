package org.async.web;

import java.util.Iterator;
import java.net.URLDecoder;
import org.async.web.HttpServer;
import org.async.collect.StringCollector;
import org.async.protocols.JSON;
import org.async.simple.Strings;

/**
 * ...
 */
public abstract class JSONService implements HttpServer.Handler {
    private static final String UTF8 = "UTF-8";
    public abstract boolean apply (HttpServer.Actor http);
    public static final JSON.Object parseQueryString (String query) 
    throws Throwable {
        JSON.Object json = new JSON.Object();
        String arg, name, value;
        int equalAt;
        Iterator<String> args = Strings.split(query, '&');
        while (args.hasNext()) {
            arg = args.next();
            equalAt = arg.indexOf('=');
            if (equalAt < 0) {
                name = URLDecoder.decode(arg, UTF8);
                value = null;
            } else {
                name = URLDecoder.decode(arg.substring(0, equalAt), UTF8);
                value = URLDecoder.decode(arg.substring(equalAt+1), UTF8);
            }
            if (json.containsKey(name)) {
                java.lang.Object curr = json.get(name);
                if (curr instanceof JSON.Array) {
                    ((JSON.Array) curr).add(value);
                } else {
                    json.put(name, JSON.list(curr, value));
                }
            } else {
                json.put(name, value);
            }
        }
        return json;
    }
    public final boolean httpContinue(HttpServer.Actor http) 
    throws Throwable {
        String method = http.method();
        if (method.equals("GET")) {
            // expect urlencoded query string and maybe X-JSON input
            String query = http.uri().getRawQuery();
            if (query != null) {
                http.state = parseQueryString(query);
            } else {
                http.state = new JSON.Object();
            }
            String xjson = http.requestHeader("X-JSON", null);
            if (xjson != null) {
                new JSON().update(http.state, xjson);
            }
            apply(http);
        } else if (method.equals("POST")) {
            // expect JSON and/or X-JSON encoded input
            http.collect(new StringCollector(new StringBuilder(), "UTF-8"));
        } else {
            http.response(501); // Not implemented
        }
        return false;
    }

    public final void httpCollected(HttpServer.Actor http) 
    throws Throwable {
        // parse and validate the JSON request body POSTED
        Object json = null;
        json = JSON.decode(
            ((StringCollector)http.requestBody()).toString()
            );
        if (json != null && json instanceof JSON.Object)
            http.state = (JSON.Object) json;
        else {
            http.state = new JSON.Object();
            http.state.put("arg0", json);
        }
        apply(http);
    }
}

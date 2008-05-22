package org.async.web;

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;
import org.async.collect.StringCollector;
import org.async.protocols.JSON;
import org.async.protocols.JSONR;
import org.async.simple.Strings;

import java.util.Iterator;
import java.net.URLDecoder;

/**
 * The redefinition of a web service, using JSON in place of SOAP and
 * JSONR instead of WSDL.
 *
 */
public abstract class Service implements Handler {
    public static final 
    void parseQueryString (String query, JSON.Object json) throws Throwable {
        String arg, name, value;
        int equalAt;
        Iterator<String> args = Strings.split(query, '&');
        while (args.hasNext()) {
            arg = args.next();
            equalAt = arg.indexOf('=');
            if (equalAt < 0) {
                name = URLDecoder.decode(arg, "UTF-8");
                value = null;
            } else {
                name = URLDecoder.decode(arg.substring(0, equalAt), "UTF-8");
                value = URLDecoder.decode(arg.substring(equalAt+1), "UTF-8");
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
    }
    public final boolean request(Actor http) throws Throwable {
        String method = http.method();
        if (method.equals("GET")) {
            String query = http.uri().getRawQuery();
            if (query != null) {
                parseQueryString(query, http.state);
                JSONR.Type type = type(http);
                if (type != null) {
                    JSONR.validate(http.state, type);
                }
                service(http);
            } else {
                resource(http);
            }
        } else if (method.equals("POST")) {
            http.collect(new StringCollector(new StringBuilder(), "UTF-8"));
        } else {
            http.response(501); // Not implemented
        }
        return false;
    }

    public final void collected(Actor http) throws Throwable {
        Object input = null;
        String body = ((StringCollector) http.requestBody()).toString();
        JSONR.Type type = type(http);
        if (type == null) {
            input = (new JSON()).eval(body);
        } else {
            input = (new JSONR(type)).eval(body);
        }
        if (input != null && input instanceof JSON.Object) {
            ((JSON.Object) input).putAll(http.state);
            http.state = (JSON.Object) input;
        } else {
            http.state.put("arg0", input);
        }
        service(http);
    }
    public abstract JSONR.Type type (Actor http) throws Throwable;
    public abstract void service (Actor http) throws Throwable;
    public abstract void resource (Actor http) throws Throwable;
}

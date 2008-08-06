package org.async.web;

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;
import org.async.collect.StringCollector;
import org.async.protocols.JSON;
import org.async.protocols.JSONR;

/**
 * The redefinition of a web service, using JSON in place of SOAP and
 * JSONR instead of WSDL.
 *
 */
public abstract class Service implements Handler {
    public final boolean request(Actor http) throws Throwable {
        String method = http.method();
        if (method.equals("GET")) {
            String query = http.uri().getRawQuery();
            if (query != null) {
                JSON.parseURLencoded(query, http.state);
                JSONR.Type type = type(http);
                if (type != null) {
                    JSONR.validate(http.state, type);
                }
            }
            service(http);
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
}

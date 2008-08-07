package org.async.web;

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;
import org.async.collect.StringCollector;
import org.async.protocols.JSONR;
import org.async.protocols.JSON;
import org.async.simple.Fun;

/**
 * The redefinition of a web service, using JSON in place of SOAP and
 * JSONR instead of WSDL.
 *
 */
public class Service implements Handler {
	private Fun _function;
    private JSONR.Type _type = null;
    public Service (Fun function) {
    	_function = function;
    }
    public Service (Fun function, JSONR.Type type) {
    	_function = function;
    	_type = type;
    }
    public final boolean request(Actor http) throws Throwable {
        String method = http.method();
        if (method.equals("GET")) {
            String query = http.uri().getRawQuery();
            if (query != null) {
                JSON.parseURLencoded(query, http.state);
                if (_type != null) {
                    JSONR.validate(http.state, _type);
                }
            }
            _function.apply(http);
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
        if (_type == null) {
            input = (new JSON()).eval(body);
        } else {
            input = (new JSONR(_type)).eval(body);
        }
        if (input != null && input instanceof JSON.Object) {
            ((JSON.Object) input).putAll(http.state);
            http.state = (JSON.Object) input;
        } else {
            http.state.put("arg0", input);
        }
        _function.apply(http);
    }
}

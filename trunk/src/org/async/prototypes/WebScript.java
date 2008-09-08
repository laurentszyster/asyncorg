package org.async.prototypes;

import org.async.web.HttpServer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public final class WebScript implements HttpServer.Controller {
    private ScriptableObject _scope;
    private Function _request = null;
    private Function _collected = null;
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _request = Stateful._function(_scope, "handleRequest");
        _collected = Stateful._function(_scope, "handleBody");
    }
	public final boolean handleRequest(HttpServer.Actor http) throws Throwable {
        if (_request == null) {
            http.error(501); // Not implemented
            return false;
        } else {
            return ((Boolean) Context.jsToJava(_request.call(
                Stateful.context, _scope, _scope, new Object[]{http}
                ), Boolean.class)).booleanValue();
        } 
    }
	public final void handleBody(HttpServer.Actor http) throws Throwable {
        if (_collected != null) {
        	_collected.call(Stateful.context, _scope, _scope, new Object[]{http});
        }
	}
}
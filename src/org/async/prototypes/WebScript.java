package org.async.prototypes;

import org.async.web.HttpServer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public final class WebScript implements HttpServer.Handler {
    private ScriptableObject _scope;
    private Function _configure = null;
    private Function _identify = null;
    private Function _request = null;
    private Function _collected = null;
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _configure = Stateful._function(_scope, "configure");
        _identify = Stateful._function(_scope, "identify");
        _request = Stateful._function(_scope, "request");
        _collected = Stateful._function(_scope, "collected");
    }
    public final void configure (String route) throws Throwable {
        if (_configure != null) {
            _configure.call(
                Stateful.context, _scope, _scope, new Object[]{route}
                );
        }
    }
    public final boolean identify (HttpServer.Actor http) throws Throwable {
        if (_identify == null) {
            http.response(401); // Unauthorized
            return false;
        } else {
            return ((Boolean) Context.jsToJava(_identify.call(
                Stateful.context, _scope, _scope, new Object[]{http}
                ), Boolean.class)).booleanValue();
        }
    }
	public final boolean request(HttpServer.Actor http) throws Throwable {
        if (_request == null) {
            http.response(501); // Not implemented
            return false;
        } else {
            return ((Boolean) Context.jsToJava(_request.call(
                Stateful.context, _scope, _scope, new Object[]{http}
                ), Boolean.class)).booleanValue();
        } 
    }
	public final void collected(HttpServer.Actor http) throws Throwable {
        if (_collected != null) {
        	_collected.call(Stateful.context, _scope, _scope, new Object[]{http});
        }
	}
}
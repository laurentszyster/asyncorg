package org.async.prototypes;

import org.async.protocols.JSONR;
import org.async.web.Service;
import org.async.web.HttpServer.Actor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public class WebScript extends Service {
    private ScriptableObject _scope;
    private Function _configure = null;
    private Function _identify = null;
    private Function _type = null;
    private Function _service = null;
    private Function _resource = null;
    public static final WebScript bind(Object scope) {
        WebScript prototype = new WebScript();
        prototype._bind(
            (ScriptableObject) Context.jsToJava(scope, ScriptableObject.class)
            );
        return prototype;
    }
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _configure = Stateful._function(_scope, "configure");
        _identify = Stateful._function(_scope, "identify");
        _type = Stateful._function(_scope, "type");
        _service = Stateful._function(_scope, "service");
        _resource = Stateful._function(_scope, "resource");
    }
    public final void configure (String route) throws Throwable {
        if (_configure != null) {
            Stateful._call(_scope, _configure, new Object[]{route});
        }
    }
    public final boolean identify (Actor http) throws Throwable {
        if (_identify == null) {
            http.response(401); // Unauthorized
            return false;
        } else {
            return ((Boolean) Context.jsToJava(Stateful._call(
                _scope, _identify, new Object[]{http}
                ), Boolean.class)).booleanValue();
        }
    }
    public final JSONR.Type type (Actor http) throws Throwable {
        if (_type == null) {
            return null;
        } else {
            return (JSONR.Type) Context.jsToJava(Stateful._call(
                _scope, _type, new Object[]{http}
                ), JSONR.Type.class);
        }
    }
    public final void service (Actor http) throws Throwable {
        if (_service == null) {
            http.response(501); // Not implemented
        } else {
            Stateful._call(_scope, _service, new Object[]{http});
        }
    }
    public final void resource (Actor http) throws Throwable {
        if (_resource == null) {
            http.response(404); // Not found
        } else {
            Stateful._call(_scope, _resource, new Object[]{http});
        }
    }    
}

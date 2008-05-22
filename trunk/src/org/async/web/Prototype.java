package org.async.web;

import org.async.core.Static;
import org.async.simple.SIO;
import org.async.protocols.JSONR;
import org.async.web.HttpServer.Actor;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Function;

import java.io.File;

/**
 * A handler stub for Javascript implementations, usefull to prototype web
 * services with an interpreter first and compile them in Java only when they 
 * are stable. May also be used whenever the handler has no loops and spends 
 * most of its time in Java (such as a JSONR validation) or C (like an SQLite 
 * statement). 
 *
 * This class is also the <code>main</code> entry point for an asyncorg 
 * application.
 * 
 * Note that there is only one Rhino <code>Context</code> of execution since
 * there is only one thread (and welcome to this asynchronous world).
 * 
 */
public class Prototype extends Service {
    private static Context _cx;
    private ScriptableObject _scope;
    private Function _configure = null;
    private Function _identify = null;
    private Function _type = null;
    private Function _call = null;
    private Function _resource = null;
    public static final Prototype load(String path) throws Exception {
        Prototype prototype = new Prototype();
        ScriptableObject scope = new ImporterTopLevel(_cx, false);
        File file = new File(path);
        _cx.evaluateString(scope, SIO.read(path), file.getName(), 1, null);
        prototype._bind(scope);
        return prototype;
    }
    public static final Prototype bind(Object scope) {
        Prototype prototype = new Prototype();
        prototype._bind(
            (ScriptableObject) Context.jsToJava(scope, ScriptableObject.class)
            );
        return prototype;
    }
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _configure = _function(_scope, _cx, "configure");
        _identify = _function(_scope, _cx, "identify");
        _type = _function(_scope, _cx, "type");
        _call = _function(_scope, _cx, "call");
        _resource = _function(_scope, _cx, "resource");
    }
    
    protected final Object _callFun (Function function, Object[] args) 
    throws Exception {
        Scriptable scope = _cx.newObject(_scope);
        scope.setPrototype(_scope);
        scope.setParentScope(null);
        return function.call(
            _cx, scope, (Scriptable) Context.javaToJS(this, scope), args 
            );
    }
    
    protected static final Function _function (
        ScriptableObject scope, Context cx, String name
        ) {
        Object function = scope.get(name, scope);
        if (!(
            function == null || function == Scriptable.NOT_FOUND
            ) && function instanceof Function) {
            return (Function) function;
        } else 
            return null;
    }
    
    public final void configure (String route) throws Throwable {
        if (_configure != null) {
            _callFun(_configure, new Object[]{route});
        }
    }
    
    public final boolean identify (Actor http) throws Throwable {
        if (_identify == null) {
            http.response(401); // Unauthorized
            return false;
        } else {
            return ((Boolean) Context.jsToJava(_callFun(
                _identify, new Object[]{http}
                ), Boolean.class)).booleanValue();
        }
    }
    public final JSONR.Type type (Actor http) throws Throwable {
        if (_type == null) {
            return null;
        } else {
            return ((JSONR.Type) Context.jsToJava(_callFun(
                _type, new Object[]{http}
                ), JSONR.Type.class));
        }
    }
    public final void call (Actor http) throws Throwable {
        if (_call == null) {
            http.response(501); // Not implemented
        } else {
            _callFun(_call, new Object[]{http});
        }
    }
    
    public final void resource (Actor http) throws Throwable {
        if (_resource == null) {
            http.response(404); // Not found
        } else {
            _callFun(_resource, new Object[]{http});
        }
    }
    
    /**
     * Try to evaluate a Javascript named "./asyncorg.js" and execute the
     * open function if any
     * 
     * @param args
     * @throws Throwable
     */
    public static final void main (String[] args) throws Throwable {
        _cx = Context.enter();
        try {
            ScriptableObject scope = new ImporterTopLevel(_cx, false);
            File file = new File("asyncorg.js");
            String path = file.getAbsolutePath();
            _cx.evaluateString(scope, SIO.read(path), path, 1, null);
            Static.loop.hookShutdown();
            Scriptable self = (Scriptable) Context.javaToJS(Static.loop, scope);
            Function open = _function(scope, _cx, "open");
            if (open != null) {
                open.call(_cx, scope, self, args);
            } else {
                Static.loop.log("no function", "open");
            }
            try {
                Static.loop.dispatch();
            } finally {
                Function close = _function(scope, _cx, "close");
                if (close != null) {
                    close.call(_cx, scope, self, args);
                } else {
                    Static.loop.log("no function", "close");
                }
            }
        } catch (Throwable e) {
            Static.loop.log(e);
        } finally {
            Context.exit();
            _cx = null;
        }
    }
}

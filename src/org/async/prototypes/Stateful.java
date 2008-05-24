package org.async.prototypes;

import org.async.core.Static;
import org.async.simple.Fun;
import org.async.simple.SIO;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;

/**
 * A convenience to prototype stateful applications in JavaScript.
 */
public class Stateful {
    protected static final Object[] _NO_ARGS = new Object[]{};
    protected static Context _cx;
    protected static final Function _function (
        ScriptableObject scope, String name
        ) {
        Object function = scope.get(name, scope);
        if (!(
            function == null || function == Scriptable.NOT_FOUND
            ) && function instanceof Function) {
            return (Function) function;
        } else {
            return null;
        }
    }
    protected static final Object _call (
        Scriptable self, Function function, Object[] args
        ) throws Exception {
        Scriptable scope = _cx.newObject(self);
        scope.setPrototype(self);
        scope.setParentScope(null);
        return function.call(
            _cx, scope, (Scriptable) Context.javaToJS(self, scope), args 
            );
    }
    protected static final class FunScript implements Fun {
        private Function _scope = null;
        public FunScript(Function scope) {
            _scope = scope;
        }
        public final Object apply(Object input) throws Throwable {
            return (Object) Context.jsToJava(Stateful._call(
                _scope, _scope, new Object[]{input}
                ), Object.class);
        }
    }
    public static final Fun fun (Object function) throws Exception {
        if (!(
            function == null || function == Scriptable.NOT_FOUND
            ) && function instanceof Function) {
            return new FunScript((Function) function);
        } else {
            throw new Exception("the object bound is not a function");
        }
    }
    public static final ChatScript chat(Object scope) {
        ChatScript prototype = new ChatScript();
        prototype._bind(
            (ScriptableObject) Context.jsToJava(scope, ScriptableObject.class)
            );
        return prototype;
    }
    public static final WebScript web(Object scope) {
        WebScript prototype = new WebScript();
        prototype._bind(
            (ScriptableObject) Context.jsToJava(scope, ScriptableObject.class)
            );
        return prototype;
    }
    /**
     * Try to evaluate a named Javascript or the default file 
     * <code>asyncorg.js</code>, execute its <code>Open</code> 
     * function, dispatch the <code>Static.loop</code> and then finally
     * call its <code>Close</code> function.
     * 
     * @h3 Synopsis
     * 
     * @pre java -jar lib/asyncorg.jar [asyncorg.js]
     * 
     * @param args
     * @throws Throwable
     */
    public static final void main (String[] args) throws Throwable {
        Static.loop.hookShutdown();
        _cx = Context.enter();
        ScriptableObject scope = new ImporterTopLevel(_cx, false);
        Scriptable self = (Scriptable) Context.javaToJS(Static.loop, scope);
        try {
            if (args.length == 0) {
                args = new String[]{"asyncorg.js"};
            }
            File file = new File(args[0]);
            String path = file.getAbsolutePath();
            _cx.evaluateString(scope, SIO.read(path), path, 1, null);
            Function open = _function(scope, "Open");
            if (open != null) {
                open.call(_cx, scope, self, args);
            } else {
                Static.loop.log("no function", "Open");
            }
            try {
                Static.loop.dispatch();
            } finally {
                Function close = _function(scope, "Close");
                if (close != null) {
                    close.call(_cx, scope, self, args);
                } else {
                    Static.loop.log("no function", "Close");
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

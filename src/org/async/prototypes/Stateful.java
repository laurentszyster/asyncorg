package org.async.prototypes;

import org.async.core.Static;
import org.simple.Fun;
import org.simple.SIO;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;

/**
 * A convenience to prototype and configure stateful applications in 
 * JavaScript, providing a fast development cycle and a clear optimization
 * path down to Java and C libraries for CPU intensive processes.
 * 
 * It depends the Java 1.5 JRE, Rhino and usually on SQLite too.
 * 
 * See asyncorg.js for a complete demonstration.
 */
public final class Stateful {
    protected static final Object[] _NO_ARGS = new Object[]{};
    protected static Context context;
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
    protected static final class FunScript implements Fun {
        private Function _scope = null;
        public FunScript(Function scope) {
            _scope = scope;
        }
        public final Object apply(Object input) throws Throwable {
            return (Object) Context.jsToJava(
                _scope.call(context, _scope, _scope, new Object[]{input}),
                Object.class
                );
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
    public static final NetScript net(Object scope) {
        NetScript prototype = new NetScript();
        prototype._bind(
            (ScriptableObject) Context.jsToJava(scope, ScriptableObject.class)
            );
        return prototype;
    }
    public static final ChatScript chat(Object scope) {
        return chat(scope, 16384, 16384);
    }
    public static final ChatScript chat(Object scope, int in, int out) {
        ChatScript prototype = new ChatScript(in, out);
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
     * @param args
     * @throws Throwable
     */
    public static final void main (String[] args) throws Throwable {
        ContextFactory cf = new ContextFactory();
        context = cf.enterContext();
        ScriptableObject scope = new ImporterTopLevel(context, false);
        Scriptable self = (Scriptable) Context.javaToJS(Static.loop, scope);
        try {
            if (args.length == 0) {
                args = new String[]{"asyncorg.js"};
            }
            File file = new File(args[0]);
            String path = file.getAbsolutePath();
            context.evaluateString(scope, SIO.read(path), path, 1, null);
            Function open = _function(scope, "Open");
            if (open != null) {
                open.call(context, scope, self, args);
            } else {
                Static.loop.log("no function", "Open");
            }
            try {
                Static.loop.dispatch();
            } finally {
                Function close = _function(scope, "Close");
                if (close != null) {
                    close.call(context, scope, self, args);
                } else {
                    Static.loop.log("no function", "Close");
                }
            }
        } catch (Throwable e) {
            Static.loop.log(e);
        } finally {
            Context.exit();
            context = null;
        }
    }
}

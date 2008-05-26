package org.async.prototypes;

import org.async.net.NetDispatcher;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public class NetScript extends NetDispatcher {
    private ScriptableObject _scope;
    private Function _handleConnect = null;
    private Function _handleLength = null;
    private Function _handleData = null;
    private Function _handleTerminator = null;
    private Function _handleClose = null;
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _handleConnect = Stateful._function(_scope, "handleConnect");
        _handleLength = Stateful._function(_scope, "handleLength");
        _handleData = Stateful._function(_scope, "handleData");
        _handleTerminator = Stateful._function(_scope, "handleTerminator");
        _handleClose = Stateful._function(_scope, "handleClose");
    }
    public final void handleConnect() throws Throwable {
        if (_handleConnect == null) {
            throw new Exception("handleConnect not implemented");
        } else {
            _handleConnect.call(
                Stateful.context, _scope, _scope, new Object[]{this}
                );
        }
    }
    public boolean handleLength(int length) throws Throwable {
        if (_handleTerminator == null) {
            throw new Exception("handleLength not implemented");
        } else {
            return ((Boolean) Context.jsToJava(_handleLength.call(
                Stateful.context, _scope, _scope, new Object[]{this, length}
                ), Boolean.class)).booleanValue();
        }
    }
    public final void handleData(byte[] data) throws Throwable {
        if (_handleData == null) {
            throw new Exception("handleData not implemented");
        } else {
            _handleData.call(
                Stateful.context, _scope, _scope, new Object[]{this, data}
                );
        }
    }
    public final boolean handleTerminator() throws Throwable {
        if (_handleTerminator == null) {
            throw new Exception("handleTerminator not implemented");
        } else {
            return ((Boolean) Context.jsToJava(_handleTerminator.call(
                Stateful.context, _scope, _scope, new Object[]{this}
                ), Boolean.class)).booleanValue();
        }
    }
    public final void handleClose() throws Throwable {
        if (_handleClose == null) {
            throw new Exception("handleClose not implemented");
        } else {
            _handleClose.call(
                Stateful.context, _scope, _scope, new Object[]{this}
            );
        }
    }
    public Object apply(Object input) throws Throwable {
        return null;
    }
}

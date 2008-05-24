package org.async.prototypes;

import org.async.chat.ChatDispatcher;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public final class ChatScript extends ChatDispatcher {
    private ScriptableObject _scope;
    private Function _handleData = null;
    private Function _handleTerminator = null;
    private Function _handleConnect = null;
    private Function _handleClose = null;
    public ChatScript () {
        super(16384, 4096); // buffer 16KB in, 4KB out
    }
    protected final void _bind (ScriptableObject scope) {
        _scope = scope;
        _handleConnect = Stateful._function(_scope, "handleConnect");
        _handleData = Stateful._function(_scope, "handleData");
        _handleTerminator = Stateful._function(_scope, "handleTerminator");
        _handleClose = Stateful._function(_scope, "handleClose");
    }
    public final void handleConnect() throws Throwable {
        if (_handleConnect == null) {
            throw new Exception("handleConnect not implemented");
        } else {
            Stateful._call(_scope, _handleConnect, new Object[]{this});
        }
    }
    public final void handleData(byte[] data) throws Throwable {
        if (_handleData == null) {
            throw new Exception("handleData not implemented");
        } else {
            Stateful._call(_scope, _handleData, new Object[]{this, data});
        }
    }
    public final boolean handleTerminator() throws Throwable {
        if (_handleTerminator == null) {
            throw new Exception("handleTerminator not implemented");
        } else {
            return ((Boolean) Context.jsToJava(Stateful._call(
                _scope, _handleTerminator, new Object[]{this}
                ), Boolean.class)).booleanValue();
        }
    }
    public final void handleClose() throws Throwable {
        if (_handleClose == null) {
            throw new Exception("handleClose not implemented");
        } else {
            Stateful._call(_scope, _handleClose, new Object[]{this});
        }
    }
    public final Object apply(Object input) throws Throwable {
        return null;
    }
}

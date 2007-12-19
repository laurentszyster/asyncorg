package org.async.core;

import java.util.HashMap;

public abstract class Listen extends Dispatcher {
    protected boolean _accepting = true;
    protected HashMap _channels = new HashMap();
    public Object apply (Object value) throws Throwable {
        return null;
    }
    public boolean writable () {
        return false;
    } 
    public boolean readable () {
        return _accepting && _connected;
    }
    public void handleConnect() throws Throwable {
        throw new Error("Unexpected read event");
    }
    public void handleRead() throws Throwable {
        throw new Error("Unexpected read event");
    }
    public void handleWrite() throws Throwable {
        throw new Error("Unexpected write event");
    }
}

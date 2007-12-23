package org.async.tests;

import org.async.core.Static;
import org.async.net.Collector;
import org.async.net.Dispatcher;

import java.net.InetSocketAddress;

public final class AsyncNetTest extends Dispatcher {

    public final void handleConnect() {
        log("connected");
        push("one".getBytes());
        push("two".getBytes());
        push("three".getBytes());
    }
    
    public final Collector handleCollect(int length) throws Throwable {
        throw new Error("unexpected input data");
    }

    public final void handleCollected() throws Throwable {
        throw new Error("unexpectedly reachable code");
    }
    
    public final void handleClose() throws Throwable {
        log("closed, bytesOut: " + Long.toString(bytesOut));
    }
    
    public final Object apply(Object input) throws Throwable {
        return null;
    }

    public static final void main (String[] args) throws Throwable {
        (new AsyncNetTest()).connect(
            new InetSocketAddress("127.0.0.2", 12345)
            );
        Static.loop.dispatch();
        System.exit(0);
    }
    
}

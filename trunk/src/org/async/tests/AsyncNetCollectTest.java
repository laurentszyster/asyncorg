package org.async.tests;

import org.async.core.Static;
import org.async.net.Collector;
import org.async.net.Dispatcher;

public final class AsyncNetCollectTest extends Dispatcher {
    public static final class AsyncNetCollectorTest implements Collector {
        private StringBuffer _sb = new StringBuffer();
        public final boolean collect (byte[] data) {
            _sb.append(new String(data));
            return false;
        }
        public final boolean terminate (byte[] data) {
            _sb.append(new String(data));
            return false;
        }
        public final String toString() {
            return _sb.toString();
        }
    } 
    public final void handleConnect() {
    }
    public final Collector handleCollect(int length) throws Throwable {
        return new AsyncNetCollectorTest();
    }
    public final void handleCollected() throws Throwable {
        log(_collector.toString());
    }
    public final void handleClose() throws Throwable {
    }
    public final Object apply(Object input) throws Throwable {
        _bufferIn.put(((String) input).getBytes());
        _bufferIn.flip();
        collect();
        return null;
    }
    public static final void main (String[] args) throws Throwable {
        (new AsyncNetCollectTest()).apply(
            "3:one,3:two,5:three,"
            );
    } 
}

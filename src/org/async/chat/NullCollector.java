package org.async.chat;


public class NullCollector implements Collector {
    public void handleData(byte[] data) throws Throwable {
    }
    public boolean handleTerminator() throws Throwable {
        return false;
    }
}

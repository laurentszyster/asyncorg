package org.async.chat;

public interface Collector {
    public void handleData (byte[] data) throws Throwable;
    public boolean handleTerminator () throws Throwable;
}
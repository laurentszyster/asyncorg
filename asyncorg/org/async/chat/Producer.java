package org.async.chat;

public interface Producer {
    public boolean stalled ();
    public byte[] more () throws Throwable;
}
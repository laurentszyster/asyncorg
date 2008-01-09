package org.async.chat;

public class ByteProducer implements Producer {
    private byte[] _bytes;
    public ByteProducer (byte[] bytes) {
        _bytes = bytes;
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        try {
            return _bytes;
        } finally {
            _bytes = null;
        }
    }
}

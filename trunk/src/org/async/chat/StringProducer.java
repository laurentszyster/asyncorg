package org.async.chat;

import org.async.simple.Bytes;

public class StringProducer implements Producer {
    private byte[] _buffer;
    public StringProducer (String buffer, String encoding) {
        _buffer = Bytes.encode(buffer, encoding);
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        try {
            return _buffer;
        } finally {
            _buffer = null;
        }
    }

}

package org.async.protocols;

import org.async.chat.Producer;
import org.async.simple.Strings;
import org.async.simple.Bytes;
import java.util.ArrayList;
import java.nio.ByteBuffer;

public class ChunkProducer implements Producer {
    
    private static final byte[] _terminator = "0\r\n\r\n".getBytes();
    
    protected Producer _wrapped = null;
    protected byte[] _footers = _terminator;

    public ChunkProducer(Producer wrapped) {
        _wrapped = wrapped;
    }
    
    public ChunkProducer(Producer wrapped, ArrayList footers) {
        _wrapped = wrapped;
        footers.add(Strings.CRLF);
        _footers = (
            "0\r\n" + Strings.join(Strings.CRLF, footers.iterator())
            ).getBytes();
    }
    
    public boolean stalled() {
        return _wrapped != null && _wrapped.stalled();
    }

    public byte[] more() throws Throwable {
        if (_wrapped == null) {
            return null;
        } else {
            byte[] data = _wrapped.more();
            if (data == null) {
                _wrapped = null;
                return _footers;
            } else {
                byte[] length = Integer.toString(data.length, 16).getBytes();
                ByteBuffer bytes = ByteBuffer.wrap(
                    new byte[data.length + length.length + 4] 
                    );
                bytes.put(length);
                bytes.put(Bytes.CRLF);
                bytes.put(data);
                bytes.put(Bytes.CRLF);
                return bytes.array();
            }
        }
    }

}

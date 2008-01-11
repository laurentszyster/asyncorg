package org.async.protocols;

import org.async.chat.Dispatcher;
import org.async.chat.Collector;
import org.async.simple.Bytes;
import org.async.simple.Strings;

import java.util.HashMap;

public class ChunkCollector implements Collector {

    private static final int _COLLECT_SIZE = 0;
    private static final int _COLLECT_CHUNK = 1;
    private static final int _COLLECT_FOOTERS = 2;
    
    protected Collector _collector;
    protected Dispatcher _channel;
    protected HashMap _headers = null;
    protected int _state = 0;
    protected StringBuilder _buffer = new StringBuilder();
    
    public ChunkCollector(Dispatcher channel, Collector collector) {
        _collector = collector;
        _channel = channel;
        _state = _COLLECT_SIZE;
        _channel.setTerminator(Bytes.CRLF);
    }
    
    public ChunkCollector(
        Dispatcher channel, Collector collector, HashMap headers
        ) {
        _collector = collector;
        _channel = channel;
        _headers = headers;
        _state = _COLLECT_SIZE;
        _channel.setTerminator(Bytes.CRLF);
    }
    
    public void handleData(byte[] data) throws Throwable {
        switch(_state) {
        case _COLLECT_CHUNK:
            _collector.handleData(data);
            return;
        case _COLLECT_SIZE: 
        case _COLLECT_FOOTERS:
            _buffer.append(data);
            return;
        }
        throw new Error("invalid ChunkCollector state");
    }

    public boolean handleTerminator() throws Throwable {
        switch(_state) {
        case _COLLECT_SIZE:
            int size = Integer.parseInt(
                Strings.split(_buffer.toString(), ';').next(), 16
                );
            _buffer = null;
            if (size == 0) {
                _state = _COLLECT_FOOTERS;
                _channel.setTerminator(Bytes.CRLFCRLF);
            } else {
                _state = _COLLECT_CHUNK;
                _channel.setTerminator(size + 2);
            }
            return false;
        case _COLLECT_CHUNK:
            _state = _COLLECT_SIZE;
            _buffer = new StringBuilder();
            _channel.setTerminator(Bytes.CRLF);
            return false;
        case _COLLECT_FOOTERS:
            if (_headers != null) {
                MIMEHeaders.update(_headers, _buffer.toString(), 0);
            }
            _collector = null;
            _buffer = null;
            _state = -1;
            return true; // end of the collector!
        }
        throw new Error("invalid ChunkCollector state");
    }

}

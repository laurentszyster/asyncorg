package org.async.chat;

import org.async.Static;
import org.async.core.Stream;

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

public abstract class Dispatcher extends Stream {
    protected Object _terminator;
    public final void setTerminator(int terminator) {
        _terminator = new Integer(terminator);
    }
    public final void setTerminator(byte[] terminator) {
        if (terminator == null) {
            throw new Error("null byte[] terminator");
        } else if (terminator.length > 0){
            _terminator = terminator;
        } else {
            throw new Error("empty byte[] terminator");
        }
    }
    public final void setTerminator() {
        _terminator = null;
    }
    public final void push (byte[] data) {
        if (data == null) {
            throw new Error("null byte[] pushed");
        } else if (data.length > 0){
            _fifoOut.add(ByteBuffer.wrap(data));
        }
    }
    public final void push (Producer producer) {
        if (producer == null) {
            throw new Error("null Producer pushed");
        } else {
            _fifoOut.add(producer);
        }
    }
    protected final boolean _notEndsWith (
        ByteBuffer haystack, int end, byte[] needle, int length
        ) {
        for (int i=0; i < length; i++) {
            if (haystack.get(end+i) != needle[i]) {
                return true;
            }
        }
        return false;
    }
    protected final int _findPrefixAtEnd (ByteBuffer haystack, byte[] needle) {
        int end = haystack.limit();
        int l = needle.length - 1;
        while (l > 0 && _notEndsWith(haystack, end-l, needle, l)) {
            l--;
        }
        return l;
    }
    protected final byte[] _collectIn (int length) {
        byte[] data = new byte[length];
        _bufferIn.get(data);
        return data;
    }
    public final void collect () throws Throwable {
        int pos;
        int lb = _bufferIn.remaining();
        while (lb > 0) {
            if (_terminator == null) { // collect all
                handleData(_collectIn(lb));
            } else if (_terminator instanceof Integer) {
                int t = ((Integer)_terminator).intValue();
                if (lb < t) { // collect part of chunk
                    setTerminator(t - lb);
                    handleData(_collectIn(lb));
                } else { // collect end of a chunk and terminate 
                    setTerminator(0);
                    handleData(_collectIn(t));
                    if (handleTerminator()) {
                        _stalledIn = true;
                        break;
                    }
                }
            } else { // look for a terminator
                pos = _bufferIn.position();
                byte[] needle = (byte[]) _terminator;
                int found = Static.find(needle, _bufferIn.array(), pos);
                if (found < 0) { // not found, look for a prefix at the end.
                    found = _findPrefixAtEnd(_bufferIn, needle);
                    if (found == 0) {
                        handleData(_collectIn(lb));
                    } else if (found != lb) {
                        handleData(_collectIn(lb-found));
                    }
                    break;
                } else { // found, maybe collect and terminate.
                    if (found > pos) {
                        handleData(_collectIn(found-pos));
                    }
                    _bufferIn.position(_bufferIn.position()+needle.length);
                    if (handleTerminator()) {
                        _stalledIn = true;
                        break;
                    }
                }
            }
            lb = _bufferIn.remaining();
        }
    }
    public boolean writable () {
        if (_bufferOut.position() > 0) {
            return true;
        } else if (_fifoOut.isEmpty()) {
            return !_connected;
        } else {
            Object queued = _fifoOut.peek(); 
            return !(
                (queued instanceof Producer) && 
                ((Producer) queued).stalled() && 
                _connected
                );
        }
    }
    protected final boolean _fillOut (ByteBuffer data) {
        try {
            _bufferOut.put(data);
        } catch (BufferOverflowException e) {
            int left = _bufferOut.remaining(); 
            _bufferOut.put(data.array(), 0, left);
            data.position(left);
            _fifoOut.addFirst(data.slice());
            return true;
        }
        return false;
    }
    public final boolean produce () throws Throwable {
        Object first;
        while (!_fifoOut.isEmpty()) {
            first = _fifoOut.removeFirst();
            if (first == null) {
                if (_bufferOut.position() == 0) {
                    _bufferOut.limit(0);
                    return false;
                } else {
                    _fifoOut.addFirst(null); // we'll be done soon ...
                    break;
                }
            } else if (first instanceof Producer){
                Producer producer = (Producer) first;
                if (producer.stalled()) {
                    break;
                }
                byte[] data = producer.more();
                if (data != null) {
                    _fifoOut.addFirst(producer);
                    if (_fillOut(ByteBuffer.wrap(data))) {
                        break;
                    }
                }
            } else if (_fillOut((ByteBuffer) first)) {
                break; 
            }
        }
        return true;
    }
    public abstract void handleData (byte[] data) throws Throwable ;
    public abstract boolean handleTerminator () throws Throwable ;
}

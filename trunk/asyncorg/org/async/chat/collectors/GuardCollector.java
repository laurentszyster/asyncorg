package org.async.chat.collectors;

import org.async.chat.Collector;

public class GuardCollector implements Collector {
    protected Collector _guarded;
    protected int _limit;
    public GuardCollector (Collector guarded, int limit) {
        _guarded = guarded;
        _limit = limit;
    }
    public void handleData(byte[] data) throws Throwable {
        _limit = _limit - data.length;
        if (_limit < 0) {
            return;
        }
        _guarded.handleData(data);
    }
    public boolean handleTerminator() throws Throwable {
        return (_limit < 0);
    }
}

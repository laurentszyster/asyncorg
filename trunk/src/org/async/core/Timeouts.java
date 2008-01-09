package org.async.core;

import java.util.LinkedList;

public abstract class Timeouts extends Scheduled {
    protected final class Timeout {
        long _when;
        Object _reference;
        Timeout(long when, Object reference) {
            _when = when;
            _reference = reference;
        }
    }
    protected int _period;
    protected int _precision;
    protected Loop _loop;
    protected LinkedList _deque = null;
    public Timeouts (int period) {
        _loop = Static.loop;
        _period = (period < _loop._precision) ? 
            _loop._precision: period;
        _precision = _loop._precision;
    }
    public Timeouts (int period, int precision) {
        _loop = Static.loop;
        _period = (period < _loop._precision) ? 
            _loop._precision: period;
        _precision = (precision < _loop._precision) ? 
            _loop._precision: precision;
    }
    public Timeouts (int period, int precision, Loop loop) {
        _loop = loop;
        _period = (period < _loop._precision) ? 
            _loop._precision: period;
        _precision = (precision < _loop._precision) ? 
            _loop._precision: precision;
    }
    public final void push (Object reference) {
        if (_deque == null) {
            _start();
        }
        _deque.add(new Timeout(_loop._now, reference));
    }
    protected final void _start() {
        _loop._scheduled.add(this);
    }
    public final long apply (Loop loop) {
        Timeout to;
        long then = loop._now - _precision - _period;
        while (!_deque.isEmpty()) {
            to = (Timeout) _deque.peek();
            if (to._when < then) {
                _deque.removeFirst();
                timeout(to._reference);
            } else {
                break;
            }
        }
        if (_deque.isEmpty()) {
            stop();
            return -1;
        } else {
            return loop._now + _precision;
        }
    }
    public abstract void timeout (Object reference);
    public abstract void stop ();
}

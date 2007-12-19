package org.async.chat.producers;

import org.async.chat.Producer;
import org.async.core.Function;

/**
 * A wrapping <code>Producer</code> that stalls until its 
 * <code>Function</code> is applied.
 * 
 * @p Synopsis
 * 
 * @p This class can be directly applied to defer a producer already pushed
 * in an output queue.
 * 
 * @p It can also be extended with another implementation of its functional
 * interface, for instance to schedule recurring state transitions (think
 * producer timeouts or pulsing protocols, use your imagination ...).
 */
public class ScheduledProducer implements Function, Producer {
    protected boolean _stalled = true;
    protected Producer _defered;
    public ScheduledProducer(Producer defered) {
        _defered = defered;
    }
    public Object apply (Object value) throws Throwable {
        _stalled = false;
        return new Long(-1);
    }
    public final boolean stalled() {
        return _stalled;
    }
    public final byte[] more() throws Throwable {
        return _defered.more();
    }
}

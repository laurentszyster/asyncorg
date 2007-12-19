/*  Copyright (C) 2007 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of version 2 of the GNU General Public License as
 *  published by the Free Software Foundation.
 *  
 *   http://www.gnu.org/copyleft/gpl.html
 *  
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA
 *  
 */

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

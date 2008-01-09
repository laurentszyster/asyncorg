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

package org.async.core;

import java.util.Iterator;

public abstract class Call implements Function {
    protected Loop _loop = Static.loop;
    public Function finalization = null;
    /**
     * Defer an asynchronous call to this finalization if there is one.
     */
    public final void finalize () {
        if (finalization != null) {
            synchronized (_loop) {
                _loop._finalized.add(finalization);
            }
            finalization = null;
        }
    }
    public static final Call join (Iterable<Call> finalized, Call joining) {
        Iterator<Call> calls = finalized.iterator();
        while (calls.hasNext()) {
            calls.next().finalization = joining;
        }
        return joining;
    }
    protected static class List implements Function {
        private Iterable<Function> _calls;
        protected List(Iterable<Function> calls) {
            _calls = calls;
        }
        public final Object apply (Object input) throws Throwable {
            Iterator<Function> calls = _calls.iterator();
            while (calls.hasNext()) {
                input = calls.next().apply(input);
            }
            return input;
        }
    }
    /**
     * 
     * @param calls
     * @return
     */
    public static final Function list(Iterable<Function> calls) {
        return new List(calls);
    }
}

/* Note About Java's Garbage Collection Specifications.
 * 
 * Although it is possible to force finalizations to execute
 * in the same loop as I/O and time events triggered methods, they
 * be appended into the loop by order of the collected objects' 
 * instanciations. Even the default non-concurrent implementation 
 * provided by HotSpot's JVM will consider as garbage an instance
 * of which finalized method has not yet been called, so there's
 * no practical way to iterate through garbage collection incrementaly.
 * 
 * This makes sense in a single-process set-top-box OS supporting a
 * non-critical user interface which can be rebooted at will to reclaim
 * resources and is expected to leak without harm.
 * 
 * But it creates a serious risk in a network of multiple-process OS 
 * holding critical applications state and controlling entreprise 
 * resources.
 *  
 * Not beeing able to deterministicly reclaim the only limited resource
 * in time is the achille heels of "Java-The-VM-Specifications".
 */

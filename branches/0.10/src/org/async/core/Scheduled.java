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

/**
 * ...
 * 
 */
public abstract class Scheduled implements Comparable {
    /**
     * The time at which this event was scheduled, in milliseconds.
     */
    public long when = 0;
    /**
     * Compare this event's scheduled time to another one.
     * 
     * @param object to compare
     * @return -1, 1 and 0 if this event is scheduled before, after or at the
     *         time as the one compared to 
     */
    public final int compareTo (Object object) {
        Scheduled event = (Scheduled) object;
        if (when < event.when) {
            return -1;
        } else if (when > event.when) {
            return 1;
        } else {
            return 0;
        }
    };
    /**
     * The scheduled method to implement.
     * 
     * @return the time at which this event recur, or -1 if it does not
     * @throws Throwable
     * 
     * @p ...
     * 
     * @pre public long apply(Loop loop) throws Throwable {
     *    loop.log("scheduled", Long.toString(when));
     *    return -1;
     *}
     * 
     * @p ...
     * 
     * @pre private count = 0; 
     *public long apply(Loop loop) throws Throwable {
     *    loop.log("scheduled", Long.toString(when));
     *    if (count < 3) {
     *        count++;
     *        return when + 3000; // reschedule in 3 seconds ...
     *    } else {
     *        return -1; // stop now.
     *    }
     *}
     * 
     */
    public abstract long apply (Loop loop) throws Throwable;
    protected static final class _Function extends Scheduled {
        public Function function;
        public _Function (long when, Function function) {
            this.when = when;
            this.function = function;
        }
        public final long apply (Loop loop) throws Throwable {
            Object result = function.apply(new Long(when));
            if (result instanceof Number) {
                return ((Number) result).longValue();
            } else {
                throw new Error(
                    function.toString() 
                    + " must return a number, not: "
                    + result.toString()
                    );
            }
        };
    }
}

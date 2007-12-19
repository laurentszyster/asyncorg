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


public abstract class Scheduled implements Comparable {
    public long when = 0;
    public final int compareTo (Object o) {
        Scheduled event = (Scheduled) o;
        if (when < event.when) {
            return -1;
        } else if (when > event.when) {
            return 1;
        } else {
            return 0;
        }
    };
    public abstract long apply () throws Throwable;
    protected static final class _Function extends Scheduled {
        public Function function;
        public _Function (long when, Function function) {
            this.when = when;
            this.function = function;
        }
        public final long apply () throws Throwable {
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
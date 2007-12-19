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

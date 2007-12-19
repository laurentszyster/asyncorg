package org.async.tests;

import org.async.Static;
import org.async.core.Call;
import org.async.core.Function;

public class ContinuationTest {
    
    private static final class Log implements Function {
        public Object apply (Object value) {
            Static.loop.log("Log ( " + value.toString() + " )");
            return null;
        }
    }

    private static final Log log = new Log();
    
    public static final class Continued extends Call {
        private String _name;
        public Continued (String name) {
            _name = name;
            continuation = log;
        }
        public String toString () {
            return _name;
        }
        public Object apply (Object value) {
            Static.loop.log(
                this.toString() + " ( " + value.toString() + " )"
                );
            return null;
        }
    }
    
    public static final void main (String[] args) {
        try {
            (new Continued("A")).continuation = new Continued("B"); 
            Call.step (new Call[]{
                new Continued("A"),
                new Continued("B")
            }).continuation = log;
            Static.loop.dispatch();
        } catch (Throwable e) {
            Static.loop.log(e);
        }
    }
    
}

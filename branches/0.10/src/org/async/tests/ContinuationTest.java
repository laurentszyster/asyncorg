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

package org.async.tests;

import org.async.core.Call;
import org.async.core.Function;
import org.async.core.Static;

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

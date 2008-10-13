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

import org.async.core.Call;
import org.async.core.Static;
import org.simple.Objects;

class ContinuationTest {
    
    static class Continued extends Call {
        private String _name;
        public Continued (String name) {
            _name = name;
        }
        public Object apply (Object value) {
            Static.loop.log(_name + " ( " + value + " )");
            return value;
        }
    }

    static void test() {
        Call A = new Continued("A");
        Call B = new Continued("B");
        Call C = new Continued("C");
        Static.loop.timeout(1000, A);
        Static.loop.timeout(2000, C);
        Static.loop.timeout(3000, B);
        Call.join(Objects.list(A, B, C), new Continued("join"))
            .finalization = Call.list(Objects.list(
                new Continued("X"), new Continued("Y"), new Continued("Z")
                ));
    }
    
    public static void main (String[] args) {
        try {
            test();
            System.err.println("dispatch");
            Static.loop.dispatch();
        } catch (Throwable e) {
            Static.loop.log(e);
        }
    }
    
}

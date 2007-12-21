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

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Practical asynchronous continuations, kinda coroutine meets closures
 * in a Java loop. Slowly, unobviously and yet damn practical in I/O bound
 * stateful network applications experiencing high levels of concurrency and 
 * latency between processes. 
 * 
 * @h3 Synopsis
 * 
 * @p The purpose of <code>Call</code> is to program workflows of 
 * concurrent I/O-bound processes, passing a current state to each
 * continuations.
 * 
 * @h3 Not CALL/CC 
 * 
 * @p By name and purpose <code>Call</code> looks like an ugly implementation 
 * of LISP's <a 
 * href="http://community.schemewiki.org/?call-with-current-continuation"
 * >call/cc</a>. Here it is piggy-backed on the simplest behaviour of the  
 * expected from a JVM's garbage collector, but it does nevertheless provides 
 * practical coroutines to <code>AsyncLoop</code> applications (and for 
 * nothing else, I'm afraid).
 * 
 * @p Network workflow are statefull processes which depend on I/O. They 
 * spend most of their time waiting for some input to complete or some 
 * output to start. Holding their concurrent workflow state does not require 
 * speed but integrity and it consumes a lot less CPU but RAM.
 * 
 * @p The combination of fast non-blocking I/O and slow continuations
 * interleaved in one asynchronous loop is simple enough to run as fast and 
 * reliably as statefull entreprise workflow applications demand. 
 * 
 */
public abstract class Call implements Function {
    private Object _current = null; // state shared between calls.
    protected Loop loop = Static.loop; // this goes deep ...
    public Function continuation = null;
    public Call(Object current) {
        _current = current;
    }
    /**
     * Defer an asynchronous call to this continuation's function if there 
     * is one.
     */
    public final void finalize () {
        if (continuation != null) {
            loop._continued.add(this);
        }
    }
    protected final void cc () throws Throwable {
        if (continuation instanceof Call) {
            ((Call) continuation)._current = continuation.apply(_current);
        } else {
            continuation.apply(_current);
        }
    }
    public static final Call tail (Call call) {
        while (call.continuation instanceof Call) {
            call = (Call) call.continuation;
        }
        return call;
    }
    public static class Fork extends Call {
        protected ArrayList _branches;  
        public Fork () {
            super(null);
            _branches = new ArrayList();
        }
        public Fork add (Function function) {
            _branches.add(function);
            return this;
        }
        public final Object apply (Object current) throws Throwable {
            Iterator continued = _branches.iterator();
            while (continued.hasNext()) {
                Object fun = continued.next();
                if (fun instanceof Call) {
                    ((Call) fun).cc();
                } else {
                    ((Function) fun).apply(current);
                }
            }
            _branches = null;
            return null;
        }
    }
    public static final Fork fork (Function[] functions) {
        Fork branched = new Fork();
        for (int i=0; i<functions.length; i++) {
            branched.add(functions[i]);
        }
        return branched;
    }
    public static final void fork (
        Call branched, Function function
        ) {
        if (branched.continuation instanceof Fork) {
            ((Fork) branched.continuation)._branches.add(function); 
        } else {
            Fork branches = new Fork();
            branches._branches.add(branched.continuation);
            branches._branches.add(function);
            branched.continuation = branches;
        }
    }
    public static class Step extends Call {
        protected Call _head, _tail;
        public Step (Call[] continuations) {
            super(null);
            _head = continuations[0];
            Call next = _head;
            for (int i=1; i<continuations.length; i++) {
                tail(next).continuation = continuations[i];
                next = continuations[i];
            }
            _tail = tail(next);
        }
        public final Object apply (Object current) {
            if (_head == null) {
                _tail.continuation = this;
                _head = _tail = null;
            } else {
                ; // TODO: ???
            }
            return null;
        }
    }
    public static final Step step (Call[] continuations) {
        return new Step(continuations);
    }
    public static class Join extends Call {
        protected Call[] _continuations;
        protected Function _join = null;
        public Join (Call[] continuations, Function join) {
            super(null);
            _continuations = continuations;
            _join = join;
        }
        public final Object apply (Object current) throws Throwable {
            if (_continuations != null) {
                for (int i=0; i < _continuations.length; i++) {
                    _continuations[i].continuation = this;
                }
                _continuations = null;
            } else if (_join != null){
                _join.apply(current);
            }
            return null;
        }
    }
    public static final Join join (
        Call[] continuations, Function function 
        ) {
        return new Join(continuations, function);
    }
    public static final class Sleep extends Call {
        private static final class _Scheduled extends Scheduled {
            Function _call;
            public _Scheduled(int milliseconds, Function call) {
                when = milliseconds;
                _call = call;
            }
            public long apply () {
                _call = null;
                return -1;
            }
        }
        private int _milliseconds;
        public Sleep(int milliseconds) {
            super(null);
            _milliseconds = milliseconds;
        }
        public final Object apply (Object current) {
            loop._scheduled.add(new _Scheduled(_milliseconds, continuation));
            return null;
        }
    }
}

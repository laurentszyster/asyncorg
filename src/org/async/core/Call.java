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
 * @pre ...
 * 
 * @h3 Practical coroutines for I/O bound processes 
 * 
 * @p Network workflow are statefull processes which depend on I/O. They 
 * spend most of their time waiting for some input to complete or some 
 * output to start. The combination of fast non-blocking I/O and slow 
 * continuations interleaved in one asynchronous loop is simple enough to run 
 * as fast and reliably as statefull entreprise workflow applications demand. 
 * 
 * @p By name and purpose <code>Call</code> looks like an ugly implementation 
 * of LISP's <a 
 * href="http://community.schemewiki.org/?call-with-current-continuation"
 * >call/cc</a>. Here it is piggy-backed on the simplest behaviour of the  
 * expected from a JVM's garbage collector, but it does nevertheless provides 
 * practical coroutines to <code>org.async</code> applications (and for 
 * nothing else, I'm afraid).
 * 
 */
public abstract class Call implements Function {
    protected Object _closure = null; // state shared between calls.
    protected Loop _loop = Static.loop;
    public Function continuation = null;
    /**
     * Defer an asynchronous call to this continuation's function if there 
     * is one.
     */
    public final void finalize () {
        if (continuation != null) {
            _loop._continued.add(this);
        }
    }
    protected final void cc () throws Throwable {
        if (continuation instanceof Call) {
            ((Call) continuation)._closure = continuation.apply(_closure);
        } else {
            continuation.apply(_closure);
        }
    }
    public static final Call tail (Call call) {
        while (call.continuation instanceof Call) {
            call = (Call) call.continuation;
        }
        return call;
    }
    public static final class Closure extends Call {
        public Closure (Loop loop) {
            _loop = loop;
        }
        public final Object apply (Object current) {
            _closure = current;
            return null;
        }
    }
    public static final class Fork extends Call {
        protected ArrayList _branches;  
        public Fork () {
            _branches = new ArrayList();
        }
        public final Fork add (Function function) {
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
    public static final class Step extends Call {
        protected Call _head, _tail;
        public Step (Call[] continuations) {
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
                _tail = null;
            } else {
                ; // TODO: ???
            }
            return null;
        }
    }
    public static final Step step (Call[] continuations) {
        return new Step(continuations);
    }
    public static final class Join extends Call {
        protected Call[] _continuations;
        protected Function _join = null;
        public Join (Call[] continuations, Function join) {
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
        protected static final class _Scheduled extends Scheduled {
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
        protected int _milliseconds;
        public Sleep(int milliseconds) {
            _milliseconds = milliseconds;
        }
        public final Object apply (Object current) {
            _loop._scheduled.add(new _Scheduled(_milliseconds, continuation));
            return null;
        }
    }
}

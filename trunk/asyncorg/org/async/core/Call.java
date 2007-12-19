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

import org.async.Static;

/**
 * Practical asynchronous continuations.
 * 
 * @h3 Synopsis
 * 
 * @p The purpose of <code>Call</code> is to program workflows of 
 * concurrent I/O-bound processes. And do it as simply as possible:
 * 
 * @pre import org.async.core.Call;
 *  
 *Call.join(new Call[]{
 *    smtp.mailto("admin@home", "Started ..."),
 *    Call.chain(new Call[]{
 *        http.get("http://www./index.html"),
 *        http.post("http://127.0.0.1/index?"),
 *        }),
 *    
 *    }, Call.LogOut)
 *    .continuation = smtp.mailto("admin@home", "... done!");
 * 
 * @h3 Is this the call-with-current-continuation from Lisp?
 * 
 * @h3 Not CALL/CC 
 * 
 * @p Yes, <code>Call</code> looks like an ugly implementation of LISP's
 * <a 
 * href="http://community.schemewiki.org/?call-with-current-continuation"
 * >call/cc</a>'s idea. Here it is piggy-backed on the expected behaviour 
 * of the simplest JVM's garbage collector. It may be an damned unorthodox 
 * hack but it does provides practical coroutines to <code>AsyncLoop</code> 
 * applications (and for nothing else, I'm afraid).
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
 * @h3 Caveat
 * 
 * @p Don't mix with threads, or at least not in a stock JVM.
 * 
 * @p Like a lot of j-spelled thing, the JVM's memory management, garbage
 * collection and therefore its finalization API are "worse than failure"
 * when threading gets into the picture.
 * 
 * @p Contrary to what Sun marketeers and engineers pretend, the fact that
 * the HotSpot JVM has six different garbage collectors is just the symptom
 * of a severely broken design. Each new release of the JVM comes with a new
 * patch on that wooden leg and a new salvo of hype.
 * 
 * @p Luckily, avoiding threads altogether makes the default garbage collector
 * of a compliant JVM behave right enough for <code>Continuation</code> to 
 * work. Unfortunately there is no guarantee as to which state continuations
 * will be once their function is called.
 * 
 */
public abstract class Call implements Function {
    protected Loop _loop = Static.loop; 
    protected Object _current = null; // state shared between calls.
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
    public static final Call tail (Call call) {
        while (call.continuation instanceof Call) {
            call = (Call) call.continuation;
        }
        return call;
    }
    public static class Fork extends Call {
        protected ArrayList _branches;  
        public Fork () {
            _branches = new ArrayList();
        }
        public Fork add (Function function) {
            _branches.add(function);
            return this;
        }
        public final Object apply (Object value) throws Throwable {
            Iterator continued = _branches.iterator();
            while (continued.hasNext()) {
                ((Function) continued.next()).apply(value);
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
            _head = continuations[0];
            Call next = _head;
            for (int i=1; i<continuations.length; i++) {
                tail(next).continuation = continuations[i];
                next = continuations[i];
            }
            _tail = tail(next);
        }
        public final Object apply (Object value) {
            if (_head == null) {
                _tail.continuation = this;
                _head = _tail = null;
            } else {
                
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
            _continuations = continuations;
            _join = join;
        }
        public final Object apply (Object continued) throws Throwable {
            if (_continuations != null) {
                for (int i=0; i < _continuations.length; i++) {
                    _continuations[i].continuation = this;
                }
                _continuations = null;
            } else if (_join != null){
                _join.apply(continued);
            }
            return null;
        }
    }
    public static final Join join (
        Call[] continuations, Function function 
        ) {
        return new Join(continuations, function);
    }
}

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

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ClosedChannelException;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Iterator;

import java.lang.Runtime;

/**
 * An asynchronous loop around NIO selectable sockets, events scheduled 
 * in time and defered continuations of finalized instances. 
 * 
 * @p The preferred application is to use a single loop in a single thread,
 * as the static member and methods of <code>Async</code> do.
 * 
 * @p Managing more than one loop is still possible, but then they should all
 * be independant if their application don't want to combine the problems
 * of threads with the complexity asynchrony.
 * 
 */
public final class Loop {
    
    private static final class _STDOE implements Loginfo {
        public void out (String message) {
            System.out.println(message);
        }
        public void err (String category, String message) {
            System.err.println(category + " " + message);
        }
        public void traceback (Throwable error) {
            error.printStackTrace(System.err);
        }
    }
    private static final Loginfo _stdoe = new _STDOE();
    
    protected static final class Exit extends Exception {
        public Exit(String message) {
            super(message);
        }
    } 
    
    /**
     * See http://www.ibm.com/developerworks/java/library/i-signalhandling/
     * 
     * In essence, the JVM has little clue about signal handling and its API
     * have been designed for that brain-damaged undeterministic threading.
     * 
     * So, this is an ugly but practical hack to throw an Exit exception
     * from within the a asynchronous loop, then wait for it to complete
     * before this thread stops and that the JVM shutdown completes.
     * 
     */
    protected static class ShutdownHook extends Thread {
        private boolean _flag = false;
        private Object _monitor = new Object();
        public ShutdownHook() {
            Runtime.getRuntime().addShutdownHook(this);
        }
        public synchronized void set () {
            _flag = true;
        }
        public synchronized boolean get () {
            if (_flag) {
                _flag = false;
                return true;
            } else {
                return false;
            }
        }
        public final void run () {
            set();
            try {
                synchronized (_monitor) {
                    _monitor.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } 
        public void shutdown () {
            synchronized (_monitor) {
                _monitor.notify();
            }
        }
    }
    
    protected ShutdownHook _hook = null;
   
    protected long _now;
    protected Selector _selector;
    protected HashMap _dispatched = new HashMap();
    protected TreeSet _scheduled = new TreeSet();
    protected LinkedList _continued =  new LinkedList();
    
    protected Loginfo _log = _stdoe;
    /**
     * A measure of this loop's precision, set by default to 100 milliseconds.
     * It is used by the loop to sleep that much whenever there are no I/O
     * to dispatch and as a timeout for its I/O selector otherwise. 
     */
    protected int _precision = 100;
    /**
     * The number of readable or writable dispatchers in the last run of
     * this loop. 
     */
    protected int _concurrent = 0;
    /**
     * A limit on the maximum of <code>AsyncCore</code> dispatchers that can 
     * be registered concurrently in this loop's selector. By default it
     * is set to 512, a common limit set by some OS on the number of file
     * descriptors passed to the <code>select</code> call.
     * 
     * @pre loop.concurrency = 4096;
     * 
     * @p Note that setting <code>concurrency</code> to 
     */
    protected int _concurrency = 512;
    /**
     * The list of <code>Function</code> applied when an <code>Exit</code> 
     * exception was throwed in the loop.
     */
    public ArrayList exits = new ArrayList();
    
    /**
     * 
     * @pre AsyncLoop.loop = new AsyncLoop();
     * loop.schedule(new Scheduled())
     *
     */
    public Loop () {
        _now = System.currentTimeMillis();
    }
    public final void hookShutdown () {
        _hook = new ShutdownHook();
    }
    public final Loginfo setLogger(Loginfo log) {
        Loginfo previous = _log;
        if (log == null) {
            _log = _stdoe;
        } else {
            _log = log;
        }
        return previous;
    }
    public final void setPrecision (int milliseconds) throws Error {
        if (milliseconds < 10) {
            throw new Error("Precision lower than 10 milliseconds");
        }
        _precision = milliseconds;
    }
    public final long now () {
        return _now;
    }
    public final void log (String message) {
        _log.out(message);
    }
    public final void log (String message, String info) {
        _log.err(message, info);
    }
    public final void log (Throwable exception) {
        _log.traceback(exception);
    }
    public final int concurrency () {
        return _concurrency;
    }
    public final int precision () {
        return _precision;
    }
    /**
     * Get the <code>TreeSet</code> of <code>Scheduled</code> events in time.
     * 
     * @return an ordered set of scheduled events in time.
     */
    public final TreeSet scheduled () {
        return _scheduled;
    }
    /**
     * Schedule a function's application at an approximate time.
     * 
     * @p See <code>Scheduled.Function</code>.
     * 
     * @param when in milliseconds
     * @param function to apply
     */
    public final void schedule (long when, Function function) {
        _scheduled.add(new Scheduled._Function(when, function));
    }
    /**
     * Schedule a function's appliction in an approximate interval.
     * 
     * @param milliseconds
     * @param function
     */
    public final void timeout (int milliseconds, Function function) {
        _scheduled.add(new Scheduled._Function(_now + milliseconds, function));
    }
    private final void _dispatch_io () throws Exit {
        // register, set interest or cancel selection keys for all dispatchers.
        _concurrent = 0;
        int ops;
        Dispatcher dispatcher;
        Iterator dispatchers = _dispatched.values().iterator();
        while (dispatchers.hasNext() && _concurrent < _concurrency) {
            dispatcher = (Dispatcher) dispatchers.next();
            ops = (
                (dispatcher.writable() ? dispatcher._writable : 0) + 
                (dispatcher.readable() ? dispatcher._readable : 0)
                );
            if (dispatcher._key == null) try {
                dispatcher._key = dispatcher._channel.register(
                    _selector, ops, dispatcher
                    );
            } catch (ClosedChannelException e) {
                dispatcher.handleError(e);
            } else {
                dispatcher._key.interestOps(ops);
            }
            if (ops > 0) {
                _concurrent++;
            }
        }
        if (_concurrent == 0) { // try to sleep ...  
            try {
                Thread.sleep(_precision);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exit("InterruptedException");
            }
        } else {
            try {
                _selector.select(_precision);
            } catch (IOException e) {
                _log.traceback(e);
            }
        }
        // handle connect, write, accept and read events
        _now = System.currentTimeMillis();
        Iterator it = _selector.selectedKeys().iterator();
        while (it.hasNext()) {
            ((Dispatcher)((SelectionKey) it.next()).attachment())._handle();
        }
    }
    private final void _dispatch_scheduled() throws Exit {
        long recurr;
        Scheduled event;
        _now = System.currentTimeMillis();
        Iterator events = _scheduled.iterator();
        while (events.hasNext()) {
            event = (Scheduled) events.next();
            if (event.when > _now) {
                break;
            }
            events.remove();
            try {
                recurr = event.apply();
                if (recurr > -1) {
                    event.when = recurr;
                    _scheduled.add(event);
                }
            } catch (Exit e) {
                throw e;
            } catch (Throwable e) {
                _log.traceback(e);
            }
        }
    }
    /**
     * Collect the VM's garbage and returns wether <code>Continuation</code>s
     * were collected or not.
     * 
     * @return <code>true</code> if <code>Continuation</code>s were collected
     */
    public final boolean collect () {
        System.gc();
        return !(_continued.isEmpty());
    }
    private final void _dispatch_continuations () throws Exit {
        Call finalized;
        while (!_continued.isEmpty()) {
            finalized = ((Call) _continued.removeFirst());
            try {
                finalized.cc();
            } catch (Exit e) {
                throw e;
            } catch (Throwable e) {
                _log.traceback(e);
            } finally {
                finalized.continuation = null;
            }
        }
        finalized = null; // just to be on the safe side of finalization ;-)
    }
    private final boolean _notEmpty () {
        if (!_dispatched.isEmpty()) {
            return true;
        } else if (!_scheduled.isEmpty()) {
            return true;
        } else if (_continued.isEmpty()) {
            return collect();
        } else {
            return true;
        }
    }
    
    /**
     * Loop until all network I/O, events in time and continuations are
     * dispatched or an <code>AsyncLoop.Exit</code> exception is throwed
     * and not catched.
     */
    public final void dispatch () throws Throwable {
        if (_selector == null) {
            _selector = Selector.open();
        }
        while (_notEmpty()) {
            try {
                _now = System.currentTimeMillis();
                _dispatch_io();
                if (_hook != null && _hook.get()) {
                    throw new Loop.Exit("SIGINT");
                }
                _dispatch_scheduled ();
                _dispatch_continuations ();
            } catch (Exit e) {
                Function fun; 
                Iterator exit = exits.iterator();
                exits = new ArrayList();
                while (exit.hasNext()) {
                    fun = ((Function) exit.next());
                    if (((Boolean) fun.apply(e)).booleanValue()) {
                        exits.add(fun);
                    };
                }
                if (exits.isEmpty()) {
                    break;
                }
            }
        }
        if (_dispatched.isEmpty()) try {
            _selector.close();
        } finally {
            _selector = null;
        }
        if (_hook != null) {
            synchronized (_hook) {
                _hook.shutdown();
            }
        }
    }
}

/*
 * @p This loop won't make its applications scale as libevent's ones and it 
 * will trade more space to run fast enough. But it is a cure for comatose 
 * GC, threading headeaches and resource contention under high network I/O 
 * concurrency and latency. 
 * 
 * @p API design and implementation details are ported from Allegra, a fork of 
 * Medusa developped on the same principle as INNS asynchronous I/O loop.
 * 
 * @p So, this Old New Thing will probably run everywhere Java 1.4 runs today
 * because it is also the unobvious way to handle concurrency right in C as
 * in Python, for many operating systems as different as Linux and Windows.
 * 
 * @p This implementation relies on <code>java.nio</code> and should instead
 * use C bindings to the native OS socket calls. Until then it will carry
 * the weight of those old locks (although without contention).
 * 
 */
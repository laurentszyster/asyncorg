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

import org.async.simple.Fun;

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
    protected HashMap<String,Dispatcher> _dispatched = new HashMap();
    protected TreeSet<Scheduled> _scheduled = new TreeSet();
    protected LinkedList<Fun> _defered =  new LinkedList();
    
    protected Loginfo _log = _stdoe;
    protected int _precision = 100;
    /**
     * The number of readable or writable dispatchers in the last run of
     * this loop. 
     */
    protected int _concurrent = 0;
    protected int _concurrency = 512;
    /**
     * The list of <code>Fun</code> applied when an <code>Exit</code> 
     * exception was throwed in the loop.
     */
    public ArrayList exits = new ArrayList();
    
    /**
     * ...
     * 
     * @pre Loop.loop = new Loop();
     * 
     */
    public Loop () {
        _now = System.currentTimeMillis();
    }
    /**
     * Hook this loop's exit to the JVM shutdown procedure and enable a
     * gracefull shutdown signaled by SIGINT (aka CTRL-C).
     * 
     */
    public final void hookShutdown () {
        _hook = new ShutdownHook();
    }
    /**
     * Hook this loop's exit to the JVM shutdown procedure, enable a
     * gracefull shutdown signaled by SIGINT (aka CTRL-C) and set a list
     * of functions as exits.
     * 
     */
    public final void hookShutdown (Fun ... functions) {
        _hook = new ShutdownHook();
        for (int i=0; i<functions.length; i++) {
            exits.add(functions[i]);
        }
    }
    /**
     * Set this loop's <code>Loginfo</code> logger or reset it to the default
     * implementation (i.e.: STDOUT and STDERR) if the <code>logger</code>
     * argument is <code>null</code>, return the previous logger.
     * 
     * @param logger to set, null to reset the default
     * @return the previous logger
     */
    public final Loginfo setLogger(Loginfo logger) {
        Loginfo previous = _log;
        if (logger == null) {
            _log = _stdoe;
        } else {
            _log = logger;
        }
        return previous;
    }
    /**
     * Set this loop's precision in milliseconds and return the previous 
     * precision.
     * 
     * @param milliseconds of precision
     * @return the previous precision
     * @throws Error if the precision set is lower than 1ms
     */
    public final int setPrecision (int milliseconds) throws Error {
        if (milliseconds < 1) {
            throw new Error("Precision lower than 1 milliseconds");
        }
        int previous = _precision;
        _precision = milliseconds;
        return previous;
    }
    /**
     * Return this loop's current time in milliseconds as a <code>long</code>.
     * 
     * @return a time in milliseconds
     */
    public final long now () {
        return _now;
    }
    /**
     * Log a message via this loop's logger.
     * 
     * @param message to log out
     */
    public final void log (String message) {
        _log.out(message);
    }
    /**
     * Log a categorized message via this loop's logger.
     * 
     * @param category of the message
     * @param message to log out
     */
    public final void log (String category, String message) {
        _log.err(category, message);
    }
    /**
     * Log a throwable's stack trace via this loop's logger.
     * 
     * @param error to trace back
     */
    public final void log (Throwable error) {
        _log.traceback(error);
    }
    /**
     * Get this loop's maximum concurrency level.
     * 
     * @return the maximum number of dispatchers selected in a single run 
     * 
     * @p A limit on the maximum of dispatchers that can be registered 
     * concurrently in this loop's selector. By default it is set to 512, 
     * a common limit set by some OS on the number of file descriptors passed 
     * to the <code>select</code> call.
     */
    public final int concurrency () {
        return _concurrency;
    }
    /**
     * Get this loop's precision in milliseconds, set by default to 100ms
     * 
     * @p It is used by the loop to sleep that much whenever there are no
     * dispatchers to select, keeping the loop from exhausting the CPU when 
     * there are no I/O to handle. Practically, it also defines the best
     * precision possible for scheduled events when there are no I/O. 
     * 
     * @return the precision of the loop in milliseconds
     */
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
     * @param when in milliseconds
     * @param function to apply
     */
    public final void schedule (long when, Fun function) {
        _scheduled.add(new Scheduled._Function(when, function));
    }
    /**
     * Schedule a function's application at an approximate time from now.
     * 
     * @param milliseconds after now
     * @param event scheduled
     */
    public final void timeout (long milliseconds, Scheduled event) {
        event.when = _now + milliseconds;
        _scheduled.add(event);
    }
    /**
     * Schedule a function's application in an approximate timeout interval.
     * 
     * @param milliseconds to wait
     * @param function to apply
     */
    public final void timeout (int milliseconds, Fun function) {
        _scheduled.add(new Scheduled._Function(_now + milliseconds, function));
    }
    public final void defer (Fun function) {
    	_defered.add(function);
    }
    private static final void _sleep (int milliseconds) throws Exit {
        try {
            Thread.sleep(milliseconds);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exit("InterruptedException");
        }
    }
    private final void _dispatch_io () throws Exit {
        // register, set interest or cancel selection keys for all dispatchers.
        _concurrent = 0;
        int ops;
        Dispatcher dispatcher;
        Iterator<Dispatcher> dispatchers = _dispatched.values().iterator();
        while (dispatchers.hasNext() && _concurrent < _concurrency) {
            dispatcher = dispatchers.next();
            ops = (
                (dispatcher.writable() ? dispatcher._writable : 0) + 
                (dispatcher.readable() ? dispatcher._readable : 0)
                );
            if (ops > 0) {
                _concurrent++;
            }
            try {
                if (dispatcher._key == null) {
                    dispatcher._key = dispatcher._channel.register(
                        _selector, ops, dispatcher
                        );
                } else if (ops != dispatcher._key.interestOps()) {
                	/*
                    if (ops > 0) {
                        dispatcher._key.interestOps(ops);
                    } else {
                        dispatcher._key.cancel();
                        dispatcher._key = null;
                    }
                    */
                	dispatcher._key.interestOps(ops);
                }
            } catch (ClosedChannelException e) {
                dispatcher.handleError(e);
            }
        }
        if (_concurrent == 0) {
            _sleep(_precision);
            _now = System.currentTimeMillis();
            return;
        } else {
            try {
                _selector.select(_precision);
            } catch (IOException e) {
                _log.traceback(e);
            }
            _now = System.currentTimeMillis();
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
        Scheduled event = _scheduled.first();
        while (event.when <= _now) {
            _scheduled.remove(event);
            try {
                recurr = event.apply(this);
                if (recurr > _now) { // recurr in the future!
                    event.when = recurr;
                    _scheduled.add(event);
                } 
            } catch (Exit e) {
                throw e;
            } catch (Throwable e) {
                _log.traceback(e);
            }
            if (_scheduled.isEmpty()) {
                break;
            }
            event = _scheduled.first();
        }
    }
    private final void _dispatch_defered () throws Exit {
        synchronized(_defered) {
            while (!_defered.isEmpty()) {
                try {
                    _defered.removeFirst().apply(_defered);
                } catch (Exit e) {
                    throw e;
                } catch (Throwable e) {
                    _log.traceback(e);
                }
            }
        }
    }
    private final boolean _notEmpty () {
        if (!_dispatched.isEmpty()) {
            return true;
        } else if (_scheduled.isEmpty()) {
            System.gc();
            System.runFinalization();
            return !(_defered.isEmpty());
        } else if (_scheduled.first().when > _now) {
            System.gc();
            System.runFinalization();
        }
        return true;
    }
    
    /**
     * Loop until all network I/O, events in time and continuations are
     * dispatched or an <code>Loop.Exit</code> exception is throwed
     * and not catched.
     */
    public final void dispatch () throws Throwable {
        if (_selector == null) {
            _selector = Selector.open();
        }
        while (_notEmpty()) {
            try {
                _dispatch_io();
                if (_hook != null && _hook.get()) {
                    throw new Loop.Exit("SIGINT");
                }
                if (!_scheduled.isEmpty()) {
                    _dispatch_scheduled ();
                }
                _dispatch_defered ();
            } catch (Exit e) {
                Fun fun; 
                Iterator exit = exits.iterator();
                exits = new ArrayList();
                while (exit.hasNext()) {
                    fun = ((Fun) exit.next());
                    Object result = fun.apply(e);
                    if (
                        result instanceof Boolean && 
                        ((Boolean) result).booleanValue()
                        ) {
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
 * @p The path of least resistance toward native sockets is probably to
 * reuse the Apache Portable Runtime (APR) as provided by Tomcat 6.0:
 * 
 * http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/tomcat/jni/package-summary.html
 * 
 */
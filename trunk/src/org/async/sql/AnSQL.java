package org.async.sql;

import org.async.simple.Function;
import org.async.simple.Bytes;
import org.async.net.NetDispatcher;
import org.async.protocols.JSON;

import java.util.Iterator;
import java.util.LinkedList;
import java.nio.ByteBuffer;


/**
 * An SQL client for AnSQL servers.
 * 
 * @pre AnSQL.connect("127.0.0.2:3999").execute(
 *    "SELECT * from SALES where COUNTRY = ? and YEAR > ? ", 
 *    JSON.list("Belgium", 1998), 
 *    new Function () {
 *        public Object apply (Object response) {
 *            JSON.pprint(response, System.out);
 *            return null;
 *        }
 *    });
 *
 *
 */
public class AnSQL extends NetDispatcher  {
    private ByteBuffer _buffer;
    protected LinkedList<Function> _requests;
    protected final void _push (Object statement, Function callback) {
        if (_channel == null) try {
            connect();
        } catch (Throwable e) {
            try {
                callback.apply(e.getMessage());
            } catch (Throwable ee) {
                log(ee);
            }
            return;
        }
        push(Bytes.encode(JSON.encode(statement), Bytes.UTF8));
        _requests.add(callback);
    }
    public final void prepare (String statement, Function callback) {
        _push(JSON.list(statement, null), callback);
    }
    public final void prepare (
        Iterator<String> statements, Function callback
        ) {
        _push(JSON.list(statements, null), callback);
    }
    private static final JSON.Array _empty_list = JSON.list();
    public final void execute (String statement, Function callback) {
        _push(JSON.list(statement, _empty_list), callback);
    }
    public final void execute (
        String statement, Iterator parameters, Function callback
        ) {
        _push(JSON.list(statement, parameters), callback);
    }
    public final void batch (
        String statement, Iterator<Iterable> parameters, Function callback
        ) {
        _push(JSON.list(statement, parameters), callback);
    }
    public final void transaction (
        Iterator<String> statements, 
        Iterator<Iterable> parameters, 
        Function callback
        ) {
        _push(JSON.list(statements, parameters), callback);
    }
    public final Object apply(Object value) throws Throwable {
        return null;
    }
    public final void handleConnect() throws Throwable {
        log("connected");
    }
    public final boolean handleLength(int length) throws Throwable {
        _buffer = ByteBuffer.wrap(new byte[length]);
        return true;
    }
    public final void handleData (byte[] data) {
        _buffer.put(data);
    }
    public final boolean handleTerminator() throws Throwable {
        try {
            _requests.removeFirst().apply(
                JSON.decode(Bytes.decode(_buffer.array(), Bytes.UTF8))
                );
        } catch (Throwable e) {
            log(e);
        }
        return (_requests.isEmpty());
    }
    public final void handleClose() throws Throwable {
        log("close");
        while (!_requests.isEmpty()) {
            _requests.removeFirst().apply(null); 
        }
    }
}

package org.async.sql;

import org.async.simple.Fun;
import org.async.simple.Bytes;
import org.async.net.NetDispatcher;
import org.async.protocols.JSON;

import java.util.Iterator;
import java.util.LinkedList;
import java.nio.ByteBuffer;


/**
 * An SQL client for AnSQL servers.
 * 
 * @pre AnSQL.connection("127.0.0.2", 3999).execute(
 *    "SELECT * from SALES where COUNTRY = ? and YEAR > ? ", 
 *    JSON.list("Belgium", 1998), 
 *    new Fun {
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
    protected LinkedList<Fun> _requests;
    protected final void _push (Object statement, Fun callback) {
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
    public final void prepare (String statement, Fun callback) {
        _push(JSON.list(statement, null), callback);
    }
    public final void prepare (
        Iterator<String> statements, Fun callback
        ) {
        _push(JSON.list(statements, null), callback);
    }
    private static final JSON.Array _empty_list = JSON.list();
    public final void execute (String statement, Fun callback) {
        _push(JSON.list(statement, _empty_list), callback);
    }
    public final void execute (
        String statement, Iterator parameters, Fun callback
        ) {
        _push(JSON.list(statement, parameters), callback);
    }
    public final void batch (
        String statement, Iterator<Iterable> parameters, Fun callback
        ) {
        _push(JSON.list(statement, parameters), callback);
    }
    public final void transaction (
        Iterator<String> statements, 
        Iterator<Iterable> parameters, 
        Fun callback
        ) {
        _push(JSON.list(statements, parameters), callback);
    }
    public final Object apply(Object value) {
		close();
		return Boolean.TRUE;
    }
    public final void handleConnect() {
        log("connected");
    }
    public final boolean handleLength(int length) {
        _buffer = ByteBuffer.wrap(new byte[length]);
        return true;
    }
    public final void handleData (byte[] data) {
        _buffer.put(data);
    }
    public final boolean handleTerminator() {
        try {
            _requests.removeFirst().apply(
                JSON.decode(Bytes.decode(_buffer.array(), Bytes.UTF8))
                );
        } catch (Throwable e) {
            log(e);
        }
        return (_requests.isEmpty());
    }
    public final void handleClose() {
        log("closed");
        while (!_requests.isEmpty()) {
            try {
            	_requests.removeFirst().apply(null); 
            } catch (Throwable e) {
                log(e);
            }
        }
    }
    public static final AnSQL connection(String host, int port) {
    	AnSQL dispatcher = new AnSQL();
    	dispatcher.setAddress(host, port);
    	return dispatcher;
    }
}

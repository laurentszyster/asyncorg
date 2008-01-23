package org.async.sql;

import org.async.protocols.JSON;
import org.async.simple.Bytes;

import SQLite.Database;
import SQLite.Exception;
import SQLite.Stmt;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.HashMap;

public class AnSQLite {
    /**
     * The <code>SQLite.Database</code> wrapped.
     */
    protected Database _db = new Database();
    protected String _path = ":memory:";
    protected int _options = 0;
    protected HashMap<String, Stmt> _statements;
    public AnSQLite(String path, int options) {
        _path = path;
        _options = options;
    }
    public void open () throws Exception {
        _db.open(_path, _options);
        _statements = new HashMap();
    }
    public Stmt prepared (String statement) throws Exception {
        Stmt st = _statements.get(statement);
        if (st == null) {
            return _db.prepare(statement);
        } else {
            st.reset();
            return st;
        }
    }
    public final void close () throws Exception {
        Iterator<Stmt> statements = _statements.values().iterator();
        try {
            while (statements.hasNext()) {
                statements.next().close();
            }
            _db.close();
        } finally {
            _statements = null;
            _db = null;
        }
    }
    protected static final void _prepare (Stmt st, StringBuilder sb) 
    throws Exception {
        int count = st.column_count();
        if (count > 0) {
            int col = 0;
            sb.append('[');
            JSON.strb(sb, st.column_database_name(col));
            for (col=1; col<count; col++) {
                sb.append(',');
                JSON.strb(sb, st.column_database_name(col));
            }
            sb.append(']');
        } else {
            sb.append("null");
        }
    }
    protected static final void _bind (Stmt st, Iterator args) 
    throws Exception {
        int i = 0;
        Object arg;
        while (args.hasNext()) {
            arg = args.next();
            if (arg == null) { // null
                st.bind(i);
            } if (arg instanceof String) { // "text"
                st.bind(i, (String) arg);
            } else if (arg instanceof Integer) { // 100
                st.bind(i, ((Integer) arg).longValue());
            } else if (arg instanceof Double) { // 1.292020292e+6
                st.bind(i, ((Double) arg).doubleValue());
            } else if (arg instanceof BigDecimal) { // 1010.23 as text
                st.bind(i, ((BigDecimal) arg).toString());
            } else if (arg instanceof Boolean) { // true or false as 1 or 0
                st.bind(i, ((Boolean) arg) ? 1 : 0);
            }
            i++;
        }
    }
    protected static final void _resultset (Stmt st, StringBuilder sb) 
    throws Exception {
        int col, count = st.column_count();
        if (st.step()) {
            sb.append("[[");
            JSON.strb(sb, st.column(0));
            for (col=1; col<count; col++) {
                sb.append(',');
                JSON.strb(sb, st.column(col));
            }
            sb.append(']');
            while (st.step()) {
                sb.append(",[");
                JSON.strb(sb, st.column(0));
                for (col=1; col<count; col++) {
                    sb.append(',');
                    JSON.strb(sb, st.column(col));
                }
                sb.append(']');
            }
            sb.append(']');
        } else {
            sb.append("null");
        }
    }
    /**
     * Prepare a statement and return success or failure.
     * 
     * @param statement to prepare
     * @param response to comlete
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public final boolean prepare (String statement, StringBuilder response) {
        try {
            _prepare(prepared(statement), response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        } 
        return true;
    }
    /**
     * Prepare a batch of statements and complete the response, stops
     * on the first statement that raises an exception.
     * 
     * @param statements to prepare
     * @param response to complete
     */
    public final void prepare (Iterator statements, StringBuilder response) {
        response.append('[');
        if (prepare((String) statements.next(), response)) {
            while (statements.hasNext()) {
                response.append(',');
                if (!prepare((String) statements.next(), response)) {
                    break;
                }
            }
        }
        response.append(']');
    }
    /**
     * Execute a statement and skip any result, catch and return any 
     * <code>SQLite.Exception</code> throwed or <code>null</code>.
     * 
     * @param statement to execute
     * @return <code>null</code> on success an exception otherwise.
     */
    public final Exception command (String statement) {
        try {
            Stmt st = prepared(statement);
            while (st.step());
        } catch (Exception e) {
            return e; 
        }
        return null;
    }
    /**
     * Execute a statement without parameters, complete the
     * response <code>StringBuilder</code> and report success or failure.
     * 
     * @param statement to execute
     * @param response to complete
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public final boolean execute (String statement, StringBuilder response) {
        try {
            _resultset(prepared(statement), response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    public final boolean execute (
        String statement, Iterator parameters, StringBuilder response) {
        try {
            Stmt st = prepared(statement);
            _bind(st, parameters);
            _resultset(st, response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    public final boolean batch (
        String statement, 
        Iterator<Iterable> parameters, 
        StringBuilder response
        ) {
        try {
            Stmt st = prepared(statement);
            response.append('[');
            try {
                _bind(st, parameters.next().iterator());
                _resultset(st, response);
            } catch (Exception e) {
                JSON.strb(response, e.getMessage());
                response.append(']');
                return false;
            }
            while (parameters.hasNext()) {
                response.append(',');
                try {
                    st.reset();
                    _bind(st, parameters.next().iterator());
                    _resultset(st, response);
                } catch (Exception e) {
                    JSON.strb(response, e.getMessage());
                    response.append(']');
                    return false;
                }
            }
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        response.append(']');
        return true;
    }
    protected final boolean _execute (
        String statement, JSON.Array parameters, StringBuilder response
        ) {
        if (parameters == null) {
            return prepare(statement, response);
        } else if (parameters.size() == 0) {
            return execute(statement, response);
        } else if (parameters.get(0) instanceof Iterable) {
            return batch(statement, parameters.iterator(), response);
        } else {
            return execute(statement, parameters.iterator(), response);
        }
    }
    /**
     * Begin a new transaction and commit a sequence of statements or 
     * rollback, filling the <code>StringBuilder</code> with results.
     * 
     * @param statements to execute as one transaction
     * @param parameters of each statement
     * @param response to complete
     * @return <code>true</code> on sucess, <code>false</code> otherwise
     */
    public final boolean transaction (
        Iterator<String> statements, 
        Iterator<JSON.Array> parameters, 
        StringBuilder response
        ) {
        Exception e = command("BEGIN");
        if (e == null) {
            while (statements.hasNext()) {
                if (!_execute(
                    statements.next(), parameters.next(), response
                    )) {
                    command("ROLLBACK");
                    return false;
                };
            }
            command("COMMIT");
            return true;
        }
        return false;
    }
    /**
     * Decode and evaluate a JSON byte array and return a response string.
     * 
     * @param request byte string to execute
     * @param response to complete
     */
    public final void handle (byte[] request, StringBuilder response) {
        JSON.Array json = new JSON.Array();
        JSON.Error error = (new JSON()).extend(
            json, Bytes.decode(request, Bytes.UTF8)
            );
        if (error != null) {
            JSON.strb(response, error.getMessage());
        } else {
            int size = json.size();
            Object sql = (size > 0) ? json.get(0): null;
            Object parameters = (size > 1) ? json.get(1): null;
            if (sql == null){
                JSON.strb(response, "AnSQL error: missing statement(s)");
            } else if (sql instanceof String) {
                _execute((String) sql, (JSON.Array) parameters, response);
            } else if (sql instanceof JSON.Array) {
                if (parameters == null) {
                    prepare(((JSON.Array) sql).iterator(), response);
                } else {
                    transaction(
                        ((JSON.Array) sql).iterator(),
                        ((JSON.Array) parameters).iterator(),
                        response
                        );
                }
            } else {
                JSON.strb(response, "AnSQL error: invalid statement(s) type");
            }
        }
    }
    protected static final JSON.Array _prepare (Stmt st) 
    throws Exception {
        int count = st.column_count();
        if (count > 0) {
            JSON.Array result = new JSON.Array();
            for (int col=0; col<count; col++) {
                result.add(st.column_database_name(col));
            }
            return result;
        } else {
            return null;
        }
    }
    protected static final JSON.Array _resultset (Stmt st) 
    throws Exception {
        if (st.step()) {
            JSON.Array rs = new JSON.Array();
            int col, count = st.column_count();
            JSON.Array row;
            while (st.step()) {
                row = new JSON.Array();
                for (col=0; col<count; col++) {
                    row.add(st.column(col));
                }
                rs.add(row);
            }
            return rs;
        } else {
            return null;
        }
    }
    public final JSON.Array prepare (String statement) 
    throws Exception {
        return _prepare(prepared(statement));
    }
    public final JSON.Array prepare (Iterator<String> statements) 
    throws Exception {
        JSON.Array rs = new JSON.Array();
        while (statements.hasNext()) {
            rs.add(_prepare(prepared(statements.next())));
        }
        return rs;
    }
    public final JSON.Array execute (String statement) 
    throws Exception {
        return _resultset(prepared(statement));
    }
    public final JSON.Array execute (String statement, Iterator parameters) 
    throws Exception {
        Stmt st = prepared(statement);
        _bind(st, parameters);
        return _resultset(st);
    }
    public final JSON.Array batch (
        String statement, Iterator<Iterable> parameters
        ) throws Exception {
        JSON.Array result = new JSON.Array();
        Stmt st = prepared(statement);
        _bind(st, ((Iterable) parameters.next()).iterator());
        result.add(_resultset(st));
        while (parameters.hasNext()) {
            st.reset();
            _bind(st, ((Iterable) parameters.next()).iterator());
            result.add(_resultset(st));
        }
        return result;
    }
    protected final JSON.Array _execute (
        String statement, JSON.Array parameters 
        ) throws Exception  {
        if (parameters == null) {
            return prepare(statement);
        } else if (parameters.size() == 0) {
            return execute(statement);
        } else if (parameters.get(0) instanceof Iterable) {
            return batch(statement, parameters.iterator());
        } else {
            return execute(statement, parameters.iterator());
        }
    }
    public final JSON.Array transaction (
        Iterator<String> statements, 
        Iterator<JSON.Array> parameters
        ) throws Exception {
        Exception e = command("BEGIN");
        if (e == null) try {
            JSON.Array result = new JSON.Array();
            while (statements.hasNext()) {
                result.add(_execute(statements.next(), parameters.next()));
            }
            command("COMMIT");
            return result;
        } catch (Exception ee) {
            command("ROLLBACK");
            throw ee;
        } else {
            throw e;
        }
    }
}

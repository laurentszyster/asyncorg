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
    private Stmt _BEGIN;
    private Stmt _COMMIT;
    private Stmt _ROLLBACK;
    protected Database _db = null;
    protected String _path = ":memory:";
    protected int _options = 0;
    protected HashMap<String, Stmt> _statements;
    /**
     * 
     * @param path
     */
    public AnSQLite(String path) {
        _path = path;
    }
    /**
     * 
     * @param path
     * @param options
     */
    public AnSQLite(String path, int options) {
        _path = path;
        _options = options;
    }
    /**
     * 
     * @return
     */
    public final Database db () {
        return _db;
    }
    /**
     * 
     * @throws Exception
     */
    public final void open () throws Exception {
        _db = new Database();
        _db.open(_path, _options);
        _statements = new HashMap();
        _BEGIN = prepared("BEGIN");
        _COMMIT = prepared("COMMIT");
        _ROLLBACK = prepared("ROLLBACK");
    }
    /**
     * 
     * @throws Exception
     */
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
    /**
     * 
     * @param statement
     * @return
     * @throws Exception
     */
    public final Stmt prepared (String statement) throws Exception {
        Stmt st = _statements.get(statement);
        if (st == null) {
            return _db.prepare(statement);
        } else {
            st.reset();
            return st;
        }
    }
    /**
     * 
     * @param st
     * @param sb
     * @throws Exception
     */
    protected static final void _prepare (Stmt st, StringBuilder sb) 
    throws Exception {
        int count = st.column_count();
        if (count > 0) {
            int col = 0;
            sb.append('[');
            JSON.strb(sb, st.column_origin_name(col));
            for (col=1; col<count; col++) {
                sb.append(',');
                JSON.strb(sb, st.column_origin_name(col));
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
     * 
     * @return
     */
    public final Exception begin () {
        try {
            while (_BEGIN.step());
        } catch (Exception e) {
            return e; 
        }
        return null;
    }
    /**
     * 
     * @return
     */
    public final Exception commit () {
        try {
            while (_COMMIT.step());
        } catch (Exception e) {
            return e; 
        }
        return null;
    }
    /**
     * 
     * @return
     */
    public final Exception rollback () {
        try {
            while (_ROLLBACK.step());
        } catch (Exception e) {
            return e; 
        }
        return null;
    }
    /**
     * 
     * @param st
     * @param args
     * @throws Exception
     */
    public static final void bind (Stmt st, Iterator args) 
    throws Exception {
        int i = 1;
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
    /**
     * 
     * @param st
     * @param sb
     * @throws Exception
     */
    public static final void resultset (Stmt st, StringBuilder sb) 
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
    public static final void resultmap (Stmt st, StringBuilder sb) 
    throws Exception {
        int col, count = st.column_count();
        if (st.step()) {
            sb.append("{");
            if (count > 2) {
                JSON.strb(sb, st.column(0));
                sb.append(":[");
                JSON.strb(sb, st.column(1));
                for (col=2; col<count; col++) {
                    sb.append(',');
                    JSON.strb(sb, st.column(col));
                }
                sb.append(']');
                while (st.step()) {
                    sb.append(",");
                    JSON.strb(sb, st.column(0));
                    sb.append(":[");
                    JSON.strb(sb, st.column(1));
                    for (col=2; col<count; col++) {
                        sb.append(',');
                        JSON.strb(sb, st.column(col));
                    }
                    sb.append(']');
                }
            } else if (count > 1) {
                JSON.strb(sb, st.column(0));
                sb.append(":");
                JSON.strb(sb, st.column(1));
                while (st.step()) {
                    sb.append(",");
                    JSON.strb(sb, st.column(0));
                    sb.append(":");
                    JSON.strb(sb, st.column(1));
                }
            } else {
                JSON.strb(sb, st.column(0));
                sb.append(":null");
                while (st.step()) {
                    sb.append(",");
                    JSON.strb(sb, st.column(0));
                    sb.append(":null");
                }
            }
            sb.append('}');
        } else {
            sb.append("null");
        }
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
            resultset(prepared(statement), response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @param response
     * @return
     */
    public final boolean execute (
        String statement, Iterator parameters, StringBuilder response) {
        try {
            Stmt st = prepared(statement);
            bind(st, parameters);
            resultset(st, response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * 
     * @param statement
     * @param response
     * @return
     */
    public final boolean map (String statement, StringBuilder response) {
        try {
            resultmap(prepared(statement), response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @param response
     * @return
     */
    public final boolean map (
        String statement, Iterator parameters, StringBuilder response) {
        try {
            Stmt st = prepared(statement);
            bind(st, parameters);
            resultmap(st, response);
        } catch (Exception e) {
            JSON.strb(response, e.getMessage());
            return false;
        }
        return true;
    }
    public final boolean map (
		String statement, Iterable parameters, StringBuilder response
		) {
    	return map(statement, parameters.iterator(), response);
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @param response
     * @return
     */
    public final boolean batch (
        String statement, 
        Iterator<Iterable> parameters, 
        StringBuilder response
        ) {
        try {
            Stmt st = prepared(statement);
            response.append('[');
            try {
                bind(st, parameters.next().iterator());
                resultset(st, response);
            } catch (Exception e) {
                JSON.strb(response, e.getMessage());
                response.append(']');
                return false;
            }
            while (parameters.hasNext()) {
                response.append(',');
                try {
                    st.reset();
                    bind(st, parameters.next().iterator());
                    resultset(st, response);
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
    public final boolean batch (
        String statement, 
        Iterable<Iterable> parameters, 
        StringBuilder response
        ) {
    	return batch(statement, parameters.iterator(), response);
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @param response
     * @return
     */
    public final boolean statement (
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
        Exception e = begin();
        if (e == null) {
            while (statements.hasNext()) {
                if (!statement(
                    statements.next(), parameters.next(), response
                    )) {
                    rollback();
                    return false;
                };
            }
            return (commit() == null);
        }
        return false;
    }
    public final boolean transaction (
        Iterable<String> statements, 
        Iterable<JSON.Array> parameters, 
        StringBuilder response
        ) {
    	return transaction(statements.iterator(), parameters.iterator(), response);
    }
    /**
     * Decode and evaluate a byte array as AnSQL statement, try to execute it
     * and fill a response buffer with a JSON string of the result.
     * 
     * @param statement byte string to evaluate and execute
     * @param response to complete
     */
    public final void handle (byte[] request, StringBuilder response) {
        JSON.Array statement = new JSON.Array();
        JSON.Error error = (new JSON.Parser()).extend(
            statement, Bytes.decode(request, Bytes.UTF8)
            );
        if (error != null) {
            JSON.strb(response, error.getMessage());
        } else {
            handle(statement, response);
        }
    }
    /**
     * Try to execute AnSQL statement and fill a response buffer with a JSON 
     * string of the result.
     * 
     * @param statement to execute
     * @param response to complete
     */
    public final void handle (JSON.Array statement, StringBuilder response) {
        int size = statement.size();
        Object sql = (size > 0) ? statement.get(0): null;
        Object parameters = (size > 1) ? statement.get(1): null;
        if (sql == null){
            JSON.strb(response, "AnSQL error: missing statement(s)");
        } else if (sql instanceof String) {
            statement((String) sql, (JSON.Array) parameters, response);
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
    /**
     * 
     * @param statement
     * @return
     */
    public final String handle (JSON.Array statement) {
        StringBuilder sb = new StringBuilder();
        handle(statement, sb);
        return sb.toString();
    }
    protected static final JSON.Array _prepare (Stmt st) 
    throws Exception {
        int count = st.column_count();
        if (count > 0) {
            JSON.Array result = new JSON.Array();
            for (int col=0; col<count; col++) {
                result.add(st.column_origin_name(col));
            }
            return result;
        } else {
            return null;
        }
    }
    /**
     * 
     * @param st
     * @return
     * @throws Exception
     */
    public static final JSON.Array resultset (Stmt st) 
    throws Exception {
        if (st.step()) {
            JSON.Array rs = new JSON.Array();
            int col, count = st.column_count();
            JSON.Array row;
            do {
                row = new JSON.Array();
                for (col=0; col<count; col++) {
                    row.add(st.column(col));
                }
                rs.add(row);
            } while (st.step());
            return rs;
        } else {
            return null;
        }
    }
    /**
     * 
     * @param st
     * @return
     * @throws Exception
     */
    public static final JSON.Object resultmap (Stmt st) 
    throws Exception {
        if (st.step()) {
            JSON.Object rm = new JSON.Object();
            int col, count = st.column_count();
            if (count > 2) {
                JSON.Array row;
                do {
                    row = new JSON.Array();
                    for (col=1; col<count; col++) {
                        row.add(st.column(col));
                    }
                    rm.put(st.column_string(0), row);
                } while (st.step());
            } else if (count > 1) {
                do {
                    rm.put(st.column_string(0), st.column(1));
                } while (st.step());
            } else {
                do {
                    rm.put(st.column_string(0), null);
                } while (st.step());
            }
            return rm;
        } else {
            return null;
        }
    }
    /**
     * 
     * @param statement
     * @return
     * @throws Exception
     */
    public final JSON.Array prepare (String statement) 
    throws Exception {
        return _prepare(prepared(statement));
    }
    /**
     * 
     * @param statements
     * @return
     * @throws Exception
     */
    public final JSON.Array prepare (Iterator<String> statements) 
    throws Exception {
        JSON.Array rs = new JSON.Array();
        while (statements.hasNext()) {
            rs.add(_prepare(prepared(statements.next())));
        }
        return rs;
    }
    public final JSON.Array prepare (Iterable<String> statements) 
    throws Exception {
    	return prepare(statements.iterator());
    }
    /**
     * 
     * @param statement
     * @return
     * @throws Exception
     */
    public final JSON.Array execute (String statement) 
    throws Exception {
        return resultset(prepared(statement));
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @return
     * @throws Exception
     */
    public final JSON.Array execute (String statement, Iterator parameters) 
    throws Exception {
        Stmt st = prepared(statement);
        bind(st, parameters);
        return resultset(st);
    }
    public final JSON.Array execute (String statement, Iterable parameters) 
    throws Exception {
        Stmt st = prepared(statement);
        bind(st, parameters.iterator());
        return resultset(st);
    }
    /**
     * 
     * @param statement
     * @return
     * @throws Exception
     */
    public final JSON.Object map (String statement) 
    throws Exception {
        return resultmap(prepared(statement));
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @return
     * @throws Exception
     */
    public final JSON.Object map (String statement, Iterator parameters) 
    throws Exception {
        Stmt st = prepared(statement);
        bind(st, parameters);
        return resultmap(st);
    }
    public final JSON.Object map (String statement, Iterable parameters) 
    throws Exception {
        Stmt st = prepared(statement);
        bind(st, parameters.iterator());
        return resultmap(st);
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @return
     * @throws Exception
     */
    public final JSON.Array batch (
        String statement, Iterator<Iterable> parameters
        ) throws Exception {
        JSON.Array result = new JSON.Array();
        Stmt st = prepared(statement);
        bind(st, ((Iterable) parameters.next()).iterator());
        result.add(resultset(st));
        while (parameters.hasNext()) {
            st.reset();
            bind(st, ((Iterable) parameters.next()).iterator());
            result.add(resultset(st));
        }
        return result;
    }
    public final JSON.Array batch (
        String statement, Iterable<Iterable> parameters
        ) throws Exception {
    	return batch (statement, parameters.iterator());
    }
    /**
     * 
     * @param statement
     * @param parameters
     * @return
     * @throws Exception
     */
    public final JSON.Array statement (
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
    /**
     * 
     * @param statements
     * @param parameters
     * @return
     * @throws Exception
     */
    public final JSON.Array transaction (
        Iterator<String> statements, 
        Iterator<JSON.Array> parameters
        ) throws Exception {
        Exception e = begin();
        if (e == null) try {
            JSON.Array result = new JSON.Array();
            while (statements.hasNext()) {
                result.add(statement(statements.next(), parameters.next()));
            }
            commit();
            return result;
        } catch (Exception ee) {
            rollback();
            throw ee;
        } else {
            throw e;
        }
    }
    public final JSON.Array transaction (
        Iterable<String> statements, 
        Iterable<JSON.Array> parameters
        ) throws Exception {
    	return transaction(statements.iterator(), parameters.iterator());
    }
}

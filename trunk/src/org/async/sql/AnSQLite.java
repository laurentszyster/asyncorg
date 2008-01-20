package org.async.sql;

import org.async.protocols.JSON;
import org.async.simple.Bytes;

import SQLite.Database;
import SQLite.Exception;
import SQLite.Stmt;
import java.math.BigDecimal;
import java.util.Iterator;

public class AnSQLite {
    //
    // don't bother with an intermediate ResultSet instance, serialize to
    // JSON directly instead.
    //
    protected static final void prepare (Stmt st, StringBuilder sb) 
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
    protected static final void newrow (Stmt st, StringBuilder sb, int count) 
    throws Exception {
        int col = 0;
        sb.append('[');
        JSON.strb(sb, st.column(col));
        for (col=1; col<count; col++) {
            sb.append(',');
            JSON.strb(sb, st.column(col));
        }
        sb.append(']');
    } 
    protected static final void execute (
        Stmt st, Iterator args, StringBuilder sb
        ) throws Exception {
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
        int count = st.column_count();
        if (st.step()) {
            sb.append('[');
            newrow(st, sb, count);
            while (st.step()) {
                sb.append(',');
                newrow(st, sb, count);
            }
            sb.append(']');
        } else {
            sb.append("null");
        }
    }
    public Database db = new Database();
    public final boolean prepare (String statement, StringBuilder sb) {
        Stmt st = null;
        try {
            st = db.prepare(statement);
            prepare(st, sb);
        } catch (Exception e) {
            sb.append(e.getMessage());
            return false;
        } finally {
            if (st != null) try {
                st.close();
            } catch (Exception e) {
                ;
            }
        }
        return true;
    }
    public final void prepare (Iterator statements, StringBuilder sb) {
        sb.append('[');
        if (prepare((String) statements.next(), sb)) {
            while (statements.hasNext()) {
                sb.append(',');
                if (!prepare((String) statements.next(), sb)) {
                    break;
                }
            }
        }
        sb.append(']');
    }
    public final Exception execute (String statement) {
        Stmt st = null;
        try {
            st = db.prepare(statement);
            while (st.step());
        } catch (Exception e) {
            return e; 
        } finally {
            if (st != null) try {
                st.close();
            } catch (Exception e) {
                ;
            }
        }
        return null;
    }
    public final boolean execute (String statement, StringBuilder sb) {
        Stmt st = null;
        try {
            st = db.prepare(statement);
            int count = st.column_count();
            if (st.step()) {
                sb.append('[');
                newrow(st, sb, count);
                while (st.step()) {
                    sb.append(',');
                    newrow(st, sb, count);
                }
                sb.append(']');
            } else {
                sb.append("null");
            }
        } catch (Exception e) {
            sb.append(e.getMessage());
            return false;
        } finally {
            if (st != null) try {
                st.close();
            } catch (Exception e) {
                ;
            }
        }
        return true;
    }
    public final boolean execute (
        String statement, Iterator args, StringBuilder sb
        ) {
        Stmt st = null;
        try {
            st = db.prepare(statement);
            execute(st, args, sb);
        } catch (Exception e) {
            sb.append(e.getMessage());
            return false;
        } finally {
            if (st != null) try {
                st.close();
            } catch (Exception e) {
                ;
            }
        }
        return true;
    }
    public final boolean execute (
        String statement, JSON.Array parameters, StringBuilder sb
        ) {
        if (parameters.size() == 0) {
            return execute(statement, sb);
        }
        Stmt st = null;
        try {
            st = db.prepare(statement);
            Object first = parameters.get(0);
            if (first instanceof Iterable) {
                boolean error = false;
                sb.append('[');
                Iterator many = parameters.iterator();
                try {
                    execute(st, ((Iterable) many.next()).iterator(), sb);
                } catch (Exception e) {
                    sb.append(e.getMessage());
                    error = true;
                }
                while (many.hasNext()) {
                    sb.append(',');
                    try {
                        execute(st, ((Iterable) many.next()).iterator(), sb);
                        st.reset();
                    } catch (Exception e) {
                        sb.append(e.getMessage());
                        error = true;
                    }
                }
                sb.append(']');
                return error;
            } else {
                execute(st, parameters.iterator(), sb);
            }
        } catch (Exception e) {
            sb.append(e.getMessage());
            return false;
        } finally {
            if (st != null) try {
                st.close();
            } catch (Exception e) {
                ;
            }
        }
        return true;
    }
    public final void transaction (
        Iterator statements, Iterator parameters, StringBuilder sb
        ) {
        Exception e = execute("BEGIN");
        if (e == null) {
            while (statements.hasNext()) {
                if (!execute(
                    (String) statements.next(), 
                    (JSON.Array) parameters.next(), 
                    sb
                    )) {
                    execute("ROLLBACK");
                    break;
                };
            }
            execute("COMMIT");
        } 
    }
    public final StringBuilder request (byte[] bytes, StringBuilder response) {
        JSON.Array json = new JSON.Array();
        JSON.Error error = (new JSON()).extend(
            json, Bytes.decode(bytes, Bytes.UTF8)
            );
        if (error != null) {
            JSON.strb(response, error.getMessage());
            return response;
        } 
        int size = json.size();
        Object sql = (size > 0) ? json.get(0): null;
        Object parameters = (size > 1) ? json.get(1): null;
        if (sql == null){
            JSON.strb(response, "AnSQL error: missing statement(s)");
        } else if (sql instanceof String) {
            if (parameters == null) {
                prepare((String) sql, response);
            } else {
                execute((String) sql, (JSON.Array) parameters, response);
            }
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
        return response;
    }
}

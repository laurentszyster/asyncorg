package org.async.sql;

import org.async.protocols.Netunicode;
import org.async.protocols.PublicNames;
import org.async.protocols.PublicRDF;

import SQLite.Stmt;
import SQLite.Exception;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An SQL metabase, the implementation of a PubliRDF subset backed by AnSQLite.
 */
public class Metabase implements PublicRDF {
    private Stmt _INSERT_STATEMENT;
    private Stmt _UPDATE_STATEMENT;
    private Stmt _SELECT_OBJECT;
    private Stmt _SELECT_CONTEXTS;
    private Stmt _INSERT_INDEX;
    private Stmt _UPDATE_INDEX;
    private Stmt _SELECT_INDEX;
    private LinkedList<String[]> _statements = new LinkedList<String[]>();
    protected AnSQLite _ansql;
    protected int _horizon;
    /**
     * Instanciates a Metabase and eventually creates the necessary tables
     * and indexes in the SQL database used: <code>m4statements</code>,
     * <code>m4contexts</code>, <code>m4subjects</code>, <code>m4indexes</code>.
     * 
     * @param ansql database
     * @param horizon ...
     * @throws Exception
     */
    public Metabase (AnSQLite ansql, int horizon) throws Exception {
        ansql.execute(
            "CREATE TABLE IF NOT EXISTS m4statements ("
            + "subject, predicate, object, context, " 
            + "PRIMARY KEY (subject, predicate, context)"
            + ")"
            );
        ansql.execute(
            "CREATE INDEX IF NOT EXISTS m4contexts "
            + "ON m4statements (subject, predicate)"
            );
        ansql.execute(
            "CREATE INDEX IF NOT EXISTS m4subjects "
            + "ON m4statements (subject)"
            );
        ansql.execute(
            "CREATE TABLE IF NOT EXISTS m4indexes ("
            + "name, names, "
            + "PRIMARY KEY (name)"
            + ")"
            );
        _INSERT_STATEMENT = ansql.prepared(
            "INSERT INTO m4statements (subject, predicate, object, context) "
            + "VALUES (?, ?, ?, ?)"
            );
        _UPDATE_STATEMENT = ansql.prepared(
            "UPDATE m4statements SET object = ? "
            + "WHERE subject = ? AND predicate = ? AND context = ?"
            );
        _SELECT_OBJECT = ansql.prepared(
            "SELECT object FROM m4statements "
            + "WHERE subject = ? AND predicate = ? AND context = ?"
            );
        _SELECT_CONTEXTS = ansql.prepared(
            "SELECT context, object FROM m4statements "
            + "WHERE subject = ? AND predicate = ?"
            );
        _INSERT_INDEX = ansql.prepared(
            "INSERT INTO m4indexes (name, names) VALUES (?, ?)"
            );
        _UPDATE_INDEX = ansql.prepared(
            "UPDATE m4indexes SET names = ? WHERE name = ?"
            );
        _SELECT_INDEX = ansql.prepared(
            "SELECT names FROM m4indexes WHERE name = ?"
            );
        _ansql = ansql;
        _horizon = horizon;
    }
    /**
     * Interpret a statement sent to the metabase and push the result to be
     * retrieved with the <code>next</code> method.
     */
    public final void send ( // let's generate ;-)
        String subject, String predicate, String object, String context
        ) throws Throwable {
        if (object.length() == 0) {
            if (context.length() == 0) {
                _SELECT_CONTEXTS.reset();
                _SELECT_CONTEXTS.bind(1, subject);
                _SELECT_CONTEXTS.bind(2, predicate);
                if (_SELECT_CONTEXTS.step()) {
                    while (_SELECT_CONTEXTS.step()) {
                        _statements.add(new String[]{
                            subject, predicate,
                            _SELECT_CONTEXTS.column_string(0), 
                            _SELECT_CONTEXTS.column_string(1)
                            });
                    }
                }
            } else {
                String[] statement = new String[]{subject, predicate, null, context};
                statement[2] = get(subject, predicate, context);
                _statements.add(statement);
            }
        } else {
            String[] statement = new String[]{subject, predicate, object, context};
            statement[2] = put(subject, predicate, context, object);
            _statements.add(statement);
        }
    }
    public final boolean hasNext() { // not stalled ?
        return !_statements.isEmpty();
    }
    public final String[] next() { // more ...
        return _statements.removeFirst();
    }
    public final void remove() {
        _statements.removeFirst();
    }
    /**
     * Insert or update a statement's object in the metabase, eventually index
     * the subject and return null or the previous object if any.  
     * 
     * @param subject
     * @param predicate
     * @param context
     * @param object
     * @return
     * @throws Exception
     */
    public final String put (
        String subject, String predicate, String context, String object
        ) throws Exception {
        String previous = get(subject, predicate, context);
        _ansql.begin();
        try {
            if (previous == null) {
                String index = indexed(subject);
                if (index == null) {
                    PublicNames names = new PublicNames(subject, _horizon);
                    if (names.encoded.equals(subject)) {
                        indexes(index, subject, context);
                    } else {
                        throw new Exception("invalid subject");
                    }
                } else if (!index.equals("")) {
                    indexes(index, subject, context);
                }
                _INSERT_STATEMENT.reset();
                _INSERT_STATEMENT.bind(1, subject);
                _INSERT_STATEMENT.bind(2, predicate);
                _INSERT_STATEMENT.bind(3, object);
                _INSERT_STATEMENT.bind(4, context);
                _INSERT_STATEMENT.step();
            } else {
                _UPDATE_STATEMENT.reset();
                _UPDATE_STATEMENT.bind(1, object);
                _UPDATE_STATEMENT.bind(2, subject);
                _UPDATE_STATEMENT.bind(3, predicate);
                _UPDATE_STATEMENT.bind(4, context);
                _UPDATE_STATEMENT.step();
            }
            _ansql.commit();
        } catch (Exception e) {
            _ansql.rollback();
        }
        return previous;
    }
    /**
     * Get a statement's object or null if it does not exist.
     * 
     * @param subject
     * @param predicate
     * @param context
     * @return
     * @throws Exception
     */
    public final String get (
        String subject, String predicate, String context
        ) throws Exception {
        _SELECT_OBJECT.reset();
        _SELECT_OBJECT.bind(1, subject);
        _SELECT_OBJECT.bind(2, predicate);
        _SELECT_OBJECT.bind(3, context);
        if (_SELECT_OBJECT.step()) {
            return _SELECT_OBJECT.column_string(0);
        } else {
            return null;
        }
    }
    /**
     * Fill a <code>HashMap</code> with one or more statements' object, keyed by
     * context, for a given subject and predicate.
     * 
     * @param subject
     * @param predicate
     * @param objects
     * @throws Exception
     */
    public final void map (
        String subject, String predicate, HashMap objects
        ) throws Exception {
        _SELECT_CONTEXTS.reset();
        _SELECT_CONTEXTS.bind(1, subject);
        _SELECT_CONTEXTS.bind(2, predicate);
        if (_SELECT_CONTEXTS.step()) {
            while (_SELECT_CONTEXTS.step()) {
                objects.put(
                    _SELECT_CONTEXTS.column_string(0), 
                    _SELECT_CONTEXTS.column_string(1)
                    );
            }
        }
    }
    /**
     * Return a name's index. 
     * 
     * @param name
     * @return
     * @throws Exception
     */
    public final String indexed (String name) throws Exception {
        _SELECT_INDEX.reset();
        _SELECT_INDEX.bind(1, name);
        if (_SELECT_INDEX.step()) {
            return _SELECT_INDEX.column_string(0);
        } else {
            return null;
        }
    }
    protected final void indexes (String index, String subject, String context) 
    throws Exception {
        if (index == null) {
            _INSERT_INDEX.reset();
            _INSERT_INDEX.bind(1, subject);
            _INSERT_INDEX.bind(2, context);
            _INSERT_INDEX.step();
        } else if (!index.equals("")) {
            HashSet field = new HashSet();
            String updated = PublicNames.validate(
                index + context.length() + ':' + context + ',', field, _horizon 
                );
            _UPDATE_INDEX.reset();
            _UPDATE_INDEX.bind(1, updated);
            if (field.size() >= _horizon) {
                _UPDATE_INDEX.bind(2, "");
            } else if (!updated.equals(index)) {
                _UPDATE_INDEX.bind(2, subject);
            }
            _UPDATE_INDEX.step();
        }
        Iterator<String> articulated = Netunicode.iter(subject);
        if (articulated.hasNext()) {
            String name;
            do {
                name = articulated.next();
                indexes(indexed(name), name, subject);
            } while (articulated.hasNext());
        }
    }
}

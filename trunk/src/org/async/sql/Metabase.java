package org.async.sql;

import org.async.chat.BytesProducer;
import org.async.chat.Producer;
import org.async.protocols.Netunicode;
import org.async.protocols.PublicNames;
import org.async.protocols.PublicRDF;
import org.async.protocols.JSON;
import org.async.protocols.SHA1;
import org.async.simple.Bytes;

import SQLite.Stmt;
import SQLite.Exception;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * An SQL meta-base for the web, the implementation of a Public RDF subset backed 
 * by AnSQLite and serving JSON objects only.
 */
public class Metabase implements PublicRDF {
	private Stmt _INSERT_TOPIC;
	private Stmt _SELECT_TOPIC;
	private Stmt _INSERT_STATEMENT;
    private Stmt _UPDATE_STATEMENT;
    private Stmt _REPLACE_STATEMENT;
    private Stmt _SELECT_STATEMENT;
    private Stmt _SELECT_STATEMENTS;
    private Stmt _INSERT_INDEX;
    private Stmt _UPDATE_INDEX;
    private Stmt _SELECT_INDEX;
	private Stmt _INSERT_ROUTE;
	private Stmt _SELECT_ROUTES;
    private LinkedList<String[]> _statements = new LinkedList<String[]>();
    public AnSQLite sql;
    public int horizon;
    /**
     * Instantiates a meta-base: eventually creates the necessary tables
     * and indexes in an SQLite database and prepare statements objects.
     * 
     * @param ansql database
     * @param horizon ...
     * @throws Exception
     */
    public Metabase (AnSQLite sql, int horizon) throws Exception {
    	sql.execute("PRAGMA encoding = \"UTF-8\"");
        sql.execute(
            "CREATE TABLE IF NOT EXISTS m4topics ( "
            + "subject, context, digest, " 
            + "PRIMARY KEY (subject, context)"
            + ")"
            );
        sql.execute(
            "CREATE TABLE IF NOT EXISTS m4statements ( "
            + "topic, predicate, object, " 
            + "PRIMARY KEY (topic, predicate) "
            + ")"
            );
        sql.execute(
            "CREATE TABLE IF NOT EXISTS m4routes ( "
            + "name, context, " 
            + "PRIMARY KEY (name, context) "
            + ")"
            );
        sql.execute(
            "CREATE TABLE IF NOT EXISTS m4indexes ( "
            + "name, names, "
            + "PRIMARY KEY (name) "
            + ")"
            );
        _INSERT_TOPIC = sql.prepared(
            "INSERT INTO m4topics (subject, context, digest) VALUES (?, ?, ?)"
            );
        _SELECT_TOPIC = sql.prepared(
            "SELECT digest FROM m4topics WHERE subject = ? AND context = ?"
            );
        _UPDATE_STATEMENT = sql.prepared(
            "UPDATE m4statements SET object = ? "
            + "WHERE topic = ? AND predicate = ? "
            );
        _INSERT_STATEMENT = sql.prepared(
            "INSERT INTO m4statements (topic, predicate, object) VALUES (?, ?, ?)"
            );
        _REPLACE_STATEMENT = sql.prepared(
            "REPLACE INTO m4statements (topic, predicate, object) VALUES (?, ?, ?)"
            );
        _SELECT_STATEMENT = sql.prepared(
            "SELECT object FROM m4statements, m4topics "
            + "WHERE m4statements.topic = m4topics.digest "
            + "AND m4topics.subject = ? "
            + "AND m4statements.predicate = ? "
            + "AND m4topics.context = ?"
            );
        _SELECT_STATEMENTS = sql.prepared(
            "SELECT context, object FROM m4statements, m4topics "
            + "WHERE m4statements.topic = m4topics.digest "
            + "AND m4topics.subject = ? "
            + "AND m4statements.predicate = ?"
            );
        _INSERT_INDEX = sql.prepared(
            "INSERT INTO m4indexes (name, names) VALUES (?, ?)"
            );
        _UPDATE_INDEX = sql.prepared(
            "UPDATE m4indexes SET names = ? WHERE name = ?"
            );
        _SELECT_INDEX = sql.prepared(
            "SELECT names FROM m4indexes WHERE name = ?"
            );
        _INSERT_ROUTE = sql.prepared(
            "INSERT INTO m4routes (name, context) VALUES (?, ?)"
            );
        _SELECT_ROUTES = sql.prepared(
            "SELECT context FROM m4routes WHERE name = ?"
            );
        this.sql = sql;
        this.horizon = horizon;
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
                _SELECT_STATEMENTS.reset();
                _SELECT_STATEMENTS.bind(1, subject);
                _SELECT_STATEMENTS.bind(2, predicate);
                if (_SELECT_STATEMENTS.step()) {
                    do {
                        _statements.add(new String[]{
                            subject, 
                            predicate,
                            _SELECT_STATEMENTS.column_string(0), 
                            _SELECT_STATEMENTS.column_string(1),
                            "_"
                            });
                    } while (_SELECT_STATEMENTS.step());
                }
            } else {
            	String previous = select(subject, predicate, context);
            	if (previous == null) {
                    _statements.add(new String[]{
                		subject, predicate, "", context, "."
                		});
            	} else {
                    _statements.add(new String[]{
                		subject, predicate, previous, context, "_"
                		});
            	}
            }
        } else {
        	sql.begin();
        	try {
        		String previous = select(subject, predicate, context);
        		if (previous == null) {
        			insert(subject, predicate, context, object);
        			_statements.add(new String[]{
    					subject, predicate, object, context, "."
    					});
        		} else {
        			update(subject, predicate, context, object);
        			_statements.add(new String[]{
                		subject, predicate, previous, context, "_"
    					});
        		}
            	sql.commit();
        	} catch (Exception e) {
            	sql.rollback();
            	_statements.add(new String[]{
    				subject, predicate, object, context, e.toString()
    				});
        	}
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
    protected final String topic (String subject, String context) throws Exception {
        _SELECT_TOPIC.reset();
        _SELECT_TOPIC.bind(1, subject);
        _SELECT_TOPIC.bind(2, context);
        if (_SELECT_TOPIC.step()) {
            return _SELECT_TOPIC.column_string(0);
        }
		if (!PublicNames.validate(
    		context, new HashSet(), horizon
			).equals(context)) {
    		throw new Exception("invalid context");
		}
        Iterator<String> articulated = Netunicode.iter(subject);
        if (articulated.hasNext()) {
            String index = indexed(subject);
        	if (index == null) {
                PublicNames names = new PublicNames(subject, horizon);
                if (names.encoded.equals(subject)) {
                    _index(index, subject, context);
                } else {
                    throw new Exception("invalid subject");
                }
            } else if (!index.equals("")) {
                _index(index, subject, context);
            }
            String name;
            do {
                name = articulated.next();
                _INSERT_ROUTE.reset();
                _INSERT_ROUTE.bind(1, name);
                _INSERT_ROUTE.bind(2, context);
                _INSERT_ROUTE.step();
            } while (articulated.hasNext());
        }
    	String digest;
    	SHA1 sha1 = new SHA1();
    	sha1.update(Netunicode.encode(
			new String[]{subject,context}).getBytes()
			);
    	digest = sha1.hexdigest();
        _INSERT_TOPIC.reset();
        _INSERT_TOPIC.bind(1, subject);
        _INSERT_TOPIC.bind(2, context);
        _INSERT_TOPIC.bind(3, digest);
        _INSERT_TOPIC.step();
        return digest;
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
    
    protected final void _index (String index, String subject, String context) 
    throws Exception {
        if (index == null) {
            _INSERT_INDEX.reset();
            _INSERT_INDEX.bind(1, subject);
            _INSERT_INDEX.bind(2, context);
            _INSERT_INDEX.step();
        } else if (!index.equals("")) {
            HashSet field = new HashSet();
            String updated = PublicNames.validate(
        		Netunicode.encode(new String[]{index, context}), field, horizon 
                );
            if (field.size() >= horizon) {
                _UPDATE_INDEX.reset();
                _UPDATE_INDEX.bind(1, "");
                _UPDATE_INDEX.bind(2, subject);
                _UPDATE_INDEX.step();
            } else if (!updated.equals(index)) {
                _UPDATE_INDEX.reset();
                _UPDATE_INDEX.bind(1, updated);
                _UPDATE_INDEX.bind(2, subject);
                _UPDATE_INDEX.step();
            } 
        } else {
        	return;
        }
        Iterator<String> articulated = Netunicode.iter(subject);
        if (articulated.hasNext()) {
            String name;
            do {
                name = articulated.next();
                _index(indexed(name), name, subject);
            } while (articulated.hasNext());
        }
    }
    protected void _walk (String name, HashMap<String, ArrayList<String>> routes) 
    throws Exception {
    	String context;
    	ArrayList<String> singleton;
        _SELECT_ROUTES.reset();
        _SELECT_ROUTES.bind(1, name);
        if (_SELECT_ROUTES.step()) {
        	do {
        		context = _SELECT_ROUTES.column_string(0);
        		if (routes.containsKey(context)) {
        			routes.get(context).add(name);
        		} else {
            		singleton = new ArrayList<String>();
            		singleton.add(name);
        			routes.put(context, singleton);
        		}
        	} while (_SELECT_ROUTES.step());
        }
    }
    /**
     * ...
     * 
     * @param name
     * @return
     * @throws Exception
     */
    public final HashMap<String, ArrayList<String>> walk (String name) 
    throws Exception {
    	String index = indexed(name);
    	HashMap<String, ArrayList<String>> routes = new HashMap();
    	Iterator<String> names = Netunicode.iter((index == null) ? name: index);
    	ArrayList<String> singleton;
    	if (names.hasNext()) {
    		do {
    			_walk(names.next(), routes);
    		} while (names.hasNext()); 
    	} else {
    		singleton = new ArrayList<String>();
    		singleton.add(name);
	        _SELECT_ROUTES.reset();
	        _SELECT_ROUTES.bind(1, name);
        	while (_SELECT_ROUTES.step()){
        		routes.put(_SELECT_ROUTES.column_string(0), singleton);
	        }
    	}
    	return routes;
    }
    /**
     * Insert a statement's object in the metabase, eventually index the topic
     * and return null or the previous object if any.  
     * 
     * @param subject
     * @param predicate
     * @param context
     * @param object
     * @throws Exception
     */
    public final void insert (
        String subject, String predicate, String context, String object
        ) throws Exception {
    	String digest = topic(subject, context);
        _INSERT_STATEMENT.reset();
        _INSERT_STATEMENT.bind(1, digest);
        _INSERT_STATEMENT.bind(2, predicate);
        _INSERT_STATEMENT.bind(3, object);
        _INSERT_STATEMENT.step();
    }
	/**
	 * Insert or replace a statement
	 * 
	 * @param subject
	 * @param predicate
	 * @param objects
	 * @throws Exception
	 */
    public final void replace (
        String subject, String predicate, String context, String object
        ) throws Exception {
    	String digest = topic(subject, context);
        _REPLACE_STATEMENT.reset();
        _REPLACE_STATEMENT.bind(1, digest);
        _REPLACE_STATEMENT.bind(2, predicate);
        _REPLACE_STATEMENT.bind(3, object);
        _REPLACE_STATEMENT.step();
	}
    /**
     * Update a statement's object in the metabase and return null or the previous 
     * object if any.  
     * 
     * @param subject
     * @param predicate
     * @param context
     * @param object
     * @throws Exception
     */
    public final void update (
        String subject, String predicate, String context, String object
        ) throws Exception {
    	String topic = topic(subject, context);
        _UPDATE_STATEMENT.reset();
        _UPDATE_STATEMENT.bind(1, object);
        _UPDATE_STATEMENT.bind(2, topic);
        _UPDATE_STATEMENT.bind(3, predicate);
        _UPDATE_STATEMENT.step();
    }
    /**
	 * Get a statement's object as a <code>String</code> or return <code>null</code> 
	 * if the statement does not exist.
	 * 
	 * @param subject of the statement
	 * @param predicate of the statement
	 * @param context of the statement
	 * @return the statement's object as bytes
	 * @throws Exception if SQLite failed 
	 */
	public final String select (
	    String subject, String predicate, String context
	    ) throws Exception {
	    _SELECT_STATEMENT.reset();
	    _SELECT_STATEMENT.bind(1, subject);
	    _SELECT_STATEMENT.bind(2, predicate);
	    _SELECT_STATEMENT.bind(3, context);
	    if (_SELECT_STATEMENT.step()) {
	        return _SELECT_STATEMENT.column_string(0);
	    } else {
	        return null;
	    }
	}
	private static final byte[] _null = new byte[]{'n','u','l','l'};
	/**
	 * Get a statement's object as <code>byte[]</code> or return <code>null</code> 
	 * if the statement does not exist.
	 * 
	 * @param subject of the statement
	 * @param predicate of the statement
	 * @param context of the statement
	 * @return the statement's object as bytes
	 * @throws Exception if SQLite failed 
	 */
	public final byte[] bytes (
	    String subject, String predicate, String context
	    ) throws Exception {
	    _SELECT_STATEMENT.reset();
	    _SELECT_STATEMENT.bind(1, subject);
	    _SELECT_STATEMENT.bind(2, predicate);
	    _SELECT_STATEMENT.bind(3, context);
	    if (_SELECT_STATEMENT.step()) {
	        return _SELECT_STATEMENT.column_bytes(0);
	    } else {
	        return _null;
	    }
	}
	/**
     * Fill a <code>HashMap</code> with one or more statements' object, 
     * keyed by context, for a given subject and predicate.
     * 
     * @param subject
     * @param predicate
     * @param objects
     * @throws Exception
     */
    public final void objects (
		String subject, String predicate, HashMap<String, Object> json
		) {
    	try {
	        _SELECT_STATEMENTS.reset();
	        _SELECT_STATEMENTS.bind(1, subject);
	        _SELECT_STATEMENTS.bind(2, predicate);
	        if (_SELECT_STATEMENTS.step()) {
	            do {
	                json.put(
	                    _SELECT_STATEMENTS.column_string(0), 
	                    JSON.decode(_SELECT_STATEMENTS.column_string(1))
	                    );
	            } while (_SELECT_STATEMENTS.step());
	        }
    	} catch (Exception e) {
    		throw new RuntimeException("SQL Error " + e.getMessage());
    	} catch (JSON.Error e) {
    		throw new RuntimeException("JSON Error " + e.getMessage());
    	}
    }
    /**
     * Fill a <code>StringBuilder</code> with one or more statements' object, 
     * keyed by context, for a given subject and predicate.
     * 
     * @param subject
     * @param predicate
     * @param sb
     * @throws Exception
     */
    public final void buffer (String subject, String predicate, StringBuilder sb) 
    throws Exception {
        _SELECT_STATEMENTS.reset();
        _SELECT_STATEMENTS.bind(1, subject);
        _SELECT_STATEMENTS.bind(2, predicate);
        if (_SELECT_STATEMENTS.step()) {
        	sb.append('{');
        	JSON.strb(sb, _SELECT_STATEMENTS.column_string(0));
        	sb.append(':');
        	sb.append(_SELECT_STATEMENTS.column_string(1));
            while (_SELECT_STATEMENTS.step()) {
            	sb.append(',');
            	JSON.strb(sb, _SELECT_STATEMENTS.column_string(0));
            	sb.append(':');
            	sb.append(_SELECT_STATEMENTS.column_string(1));
            }
        	sb.append('}');
        } else {
        	sb.append("null");
        }
    }
    /**
     * ...
     * 
     * @param subject
     * @param predicate
     * @return
     * @throws Exception
     */
    public final String buffer (String subject, String predicate) throws Exception {
    	StringBuilder sb = new StringBuilder();
    	buffer(subject, predicate, sb);
    	return sb.toString();
    } 
    /**
     * Fill a <code>StringBuilder</code> with one or more statements' object, 
     * keyed by context, for a given subject and predicate.
     * 
     * @param subject
     * @param predicate
     * @param sb
     * @throws Error
     */
    private byte[] _OPEN_OBJECT = new byte[]{'{'};
    private byte[] _CLOSE_OBJECT = new byte[]{'}'};
    private byte[] _COMMA = new byte[]{':'};
    private byte[] _COLON = new byte[]{','};
    private byte[] _NULL = new byte[]{'n', 'u', 'l', 'l'};
    public final Producer produce (String subject, String predicate) 
    throws Exception {
    	LinkedList<byte[]> bytes = new LinkedList();
        _SELECT_STATEMENTS.reset();
        _SELECT_STATEMENTS.bind(1, subject);
        _SELECT_STATEMENTS.bind(2, predicate);
        if (_SELECT_STATEMENTS.step()) {
        	bytes.add(_OPEN_OBJECT);
        	bytes.add(Bytes.encode(JSON.encode(
    			_SELECT_STATEMENTS.column_string(0)
    			), "UTF-8"));
        	bytes.add(_COLON);
        	bytes.add(_SELECT_STATEMENTS.column_bytes(1));
            while (_SELECT_STATEMENTS.step()) {
            	bytes.add(_COMMA);
            	bytes.add(Bytes.encode(JSON.encode(
        			_SELECT_STATEMENTS.column_string(0)
	    			), "UTF-8"));
            	bytes.add(_COLON);
            	bytes.add(_SELECT_STATEMENTS.column_bytes(1));
            }
            bytes.add(_CLOSE_OBJECT);
        } else {
        	bytes.add(_NULL);
        }
        return new BytesProducer(bytes.iterator());
    }
}
/*

Transactions with BEGIN, COMMIT or ROLLBACK are bound to disk, at a not so fast
pace of 7200rpm in most case. That's 120 access per seconds, two by transactions
and 60 transactions per seconds. So a transacted metabase can handle at most sixty 
concurrent updates per seconds with the fraction of a single core CPU time, the
rest is available to handle more metabase queries (which are simpler in execution)
... by chunks of 1/60th of second.

To handle more of all types of requests without wait state, transactions must be 
deferred to another process either in I/O buffers and/or persistent queues.

Or do not transact, size your CPU and RAM to your I/O buffers and make sure that 
the server shuts down gracefully on error ... with a final COMMIT.

*/
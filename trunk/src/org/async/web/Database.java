package org.async.web;

import org.async.protocols.JSON;
import org.async.protocols.JSONR;
import org.async.simple.Bytes;
import org.async.simple.Strings;
import org.async.sql.AnSQLite;
import org.async.web.HttpServer.Actor;

import SQLite.Authorizer;
import SQLite.Constants;

import java.util.Iterator;

/**
 * A secured HTTP interface for AnSQLite database.
 * 
 * Authorizations are enforced accross the database by roles. To restrict
 * access rights by domains, partition your application's relations in
 * distinct authority realms (since SQLite does not enforce referential
 * integrity, you lose nothing but weight and complications).
 * 
 * There are five roles:
 * 
 * @pre reader writer editor censor root
 * 
 * They are defined as accumulations of seven rights:
 * 
 * @pre SELECT UPDATE INSERT DELETE CREATE DROP MANAGE
 * 
 * Readers can SELECT rows, writers can UPDATE rows, editors can INSERT rows, 
 * administrators can DELETE rows and the super user has all rights left on 
 * the database.
 * 
 * Note that any user with at least one right can create and drop temporary
 * data structures or begin transactions (but not necessarily update, insert
 * and delete rows because its role may limits its access to temporary 
 * data too).
 */
public class Database extends Service {
    protected static final class Role implements Authorizer {
        protected static final int SELECT = 1;
        protected static final int UPDATE = 2;
        protected static final int INSERT = 4;
        protected static final int DELETE = 8;
        protected static final int CREATE = 16;
        protected static final int DROP = 32;
        protected static final int MANAGE = 64;
        protected static final int READER = SELECT;
        protected static final int WRITER = SELECT + UPDATE;
        protected static final int EDITOR = WRITER + INSERT;
        protected static final int CENSOR = EDITOR + DELETE;
        protected static final int ROOT = CENSOR + CREATE + DROP + MANAGE;
        private int _rights;
        protected Role (int rights) {
            _rights = rights;
        }
        public final int authorize (
            int what, 
            String arg1,
            String arg2, 
            String arg3, 
            String arg4
            ) {
            switch(what) {
            case Constants.SQLITE_SELECT:
                if ((_rights & SELECT) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_UPDATE:
                if ((_rights & UPDATE) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_INSERT:
                if ((_rights & INSERT) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_DELETE:
                if ((_rights & DELETE) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_CREATE_INDEX:
            case Constants.SQLITE_CREATE_TABLE:
            case Constants.SQLITE_CREATE_VIEW:
            case Constants.SQLITE_CREATE_TRIGGER:
                if ((_rights & CREATE) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_DROP_INDEX:
            case Constants.SQLITE_DROP_TABLE:
            case Constants.SQLITE_DROP_VIEW:
            case Constants.SQLITE_DROP_TRIGGER:
                if ((_rights & DROP) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            case Constants.SQLITE_ATTACH:
            case Constants.SQLITE_DETACH:
            case Constants.SQLITE_PRAGMA:
            case Constants.SQLITE_READ:
                if ((_rights & MANAGE) == 0) {
                    return Constants.SQLITE_DENY;
                } else {
                    break;
                }
            }
            if (_rights == 0) {
                return Constants.SQLITE_DENY;
            }
            return Constants.SQLITE_OK;
        }
        protected static final Role reader = new Role(READER);
        protected static final Role writer = new Role(WRITER);
        protected static final Role editor = new Role(EDITOR);
        protected static final Role censor = new Role(CENSOR);
        protected static final Role root = new Role(ROOT);
    }
    protected static final JSON.Object _model = JSON.dict(new Object[]{
        "arg0", JSON.list(new Object[]{null})
    });
    String _resource = _model.toString();
    protected AnSQLite _sql = null; 
    protected JSONR.Type _type = null; 
    public Database (String name) {
        _sql = new AnSQLite(name, 0);
    }
    public Database (AnSQLite sql) {
        _sql = sql;
    }
    public void configure (String route) {
        _type = JSONR.compile(_model, JSONR.TYPES);
    }
    public boolean identify (Actor http) {
        if (http.identity == null) {
            http.response(401); // Unauthorized
            return false;
        }
        return true;
    }
    public JSONR.Type type(Actor http) {
        return _type;
    }
    public void resource(Actor http) {
        http.responseHeader("Cache-control", "max-age=3600;");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, Bytes.encode(_resource, Bytes.UTF8));
    }
    public void call(Actor http) {
        StringBuilder response = new StringBuilder();
        JSON.Array statement = http.state.getArray("arg0", null);
        if (statement == null) {
            JSON.strb(response, "missing payload");
        } else {
            Role role = null;
            if (http.rights != null && http.rights.length() > 0) {
                Iterator<String> rights = Strings.split(http.rights, ',');
                String right;
                while (rights.hasNext()) {
                    right = rights.next();
                    if (right.equals("reader")) {
                        role = Role.reader;
                    } else if (right.equals("writer")) {
                        role = Role.writer;
                    } else if (right.equals("editor")) {
                        role = Role.editor;
                    } else if (right.equals("censor")) {
                        role = Role.censor;
                    } else if (right.equals("root")) {
                        role = Role.root;
                    } else {
                        continue;
                    }
                    break;
                }
            }
            if (role != null) {
                _sql.db().set_authorizer(role);
                _sql.handle(statement, response);
                _sql.db().set_authorizer(null);
            } else {
                JSON.strb(response, "unauthorized");
            }
        }
        http.responseHeader("Cache-control", "no-cache");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, Bytes.encode(response.toString(), Bytes.UTF8));
    }
}

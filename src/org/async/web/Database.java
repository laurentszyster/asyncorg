package org.async.web;

import org.async.sql.AnSQLite;
import org.async.protocols.JSON;
import org.async.simple.Bytes;
import org.async.simple.Fun;

import SQLite.Authorizer;
import SQLite.Constants;

/**
 * A web service function that enforces role-based access to an SQLite database
 * 
 * It is practical to develop application with a generic SQL service, then enforce
 * validation with specific services
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
 * data structures or begin transactions, but not necessarily update, insert
 * and delete rows because its role may limits its access to temporary 
 * data too. So, practically this reduce the right of a reader to create,
 * select and drop temporary views. 
 * 
 * Authorizations are enforced across the database by roles. 
 * 
 * To restrict access rights by domains, partition your application's relations 
 * in distinct authority realms. 
 */
public class Database implements Fun {
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
    protected AnSQLite _sql = null; 
    public Database (String name) {
        _sql = new AnSQLite(name, 0);
    }
    public Database (AnSQLite sql) {
        _sql = sql;
    }
    public Object apply(Object actor) {
    	HttpServer.Actor http = (HttpServer.Actor) actor;
    	String rights = http.rights;
        Role role = null;
        if (rights != null && rights.length() > 0) {
            if (rights.equals("reader")) {
                role = Role.reader;
            } else if (rights.equals("writer")) {
                role = Role.writer;
            } else if (rights.equals("editor")) {
                role = Role.editor;
            } else if (rights.equals("censor")) {
                role = Role.censor;
            } else if (rights.equals("root")) {
                role = Role.root;
            }
        }
        StringBuilder response = new StringBuilder();
        if (role != null) {
            JSON.Array statement = http.state.getArray("arg0", null);
            if (statement == null) {
                JSON.strb(response, "missing payload");
            } else {
                _sql.db().set_authorizer(role);
                _sql.handle(statement, response);
                _sql.db().set_authorizer(null);
            }
        } else {
            JSON.strb(response, "unauthorized");
        }
        http.set("Cache-control", "no-cache");
        http.set("Content-Type", "text/javascript; charset=UTF-8");
        http.reply(200, response.toString(), Bytes.UTF8);
        return null;
    }
}

package org.async.web;

import org.async.protocols.JSON;
import org.async.protocols.JSONR;
import org.async.simple.Bytes;
import org.async.sql.AnSQLite;
import org.async.web.HttpServer.Actor;

/**
 * 
 */
public class Database extends JSONService {
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
    public void handleConfigure (String route) {
        _type = JSONR.compile(_model, JSONR.TYPES);
    }
    public boolean handleIdentify (Actor http) {
        if (http.identity == null) {
            http.response(403);
            return false;
        }
        return true;
    }
    public JSONR.Type type(Actor http) {
        return _type;
    }
    public boolean resource(Actor http) {
        http.responseHeader("Cache-control", "max-age=3600;");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, Bytes.encode(_resource, Bytes.UTF8));
        return false;
    }
    public boolean call(Actor http) {
        StringBuilder response = new StringBuilder();
        JSON.Array statement = http.state.getArray("arg0", null);
        if (statement == null) {
            JSON.strb(response, "AnSQL error: missing payload");
        } else {
            _sql.db().set_authorizer(new AnSQLite.Role(0));
            _sql.handle(statement, response);
            _sql.db().set_authorizer(null);
        }
        http.responseHeader("Cache-control", "no-cache");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, Bytes.encode(response.toString(), Bytes.UTF8));
        return false; 
    }
}

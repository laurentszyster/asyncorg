/**
 *  Copyright (C) 2007 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of version 2 of the GNU General Public License as
 *  published by the Free Software Foundation.
 *  
 *   http://www.gnu.org/copyleft/gpl.html
 *  
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA
 *  
 *  Use Rhino's JavaScript interpreter to prototype and configure the 
 *  web application of asyncorg. It will not slow down the web server's 
 *  configuration significantly and supports a fast development cycle.
 * 
 */
var asyncorg = Packages.org.async;
 
importClass(asyncorg.core.Static);
importClass(asyncorg.collect.StringCollector);
importClass(asyncorg.protocols.JSON);
importClass(asyncorg.protocols.JSONR);
importClass(asyncorg.sql.AnSQLite);
importClass(asyncorg.sql.Metabase);
importClass(asyncorg.web.HttpServer);
importClass(asyncorg.web.Authority);
importClass(asyncorg.web.FileCache);
importClass(asyncorg.web.FileSystem);
importClass(asyncorg.prototypes.Stateful);

importClass(java.lang.Runtime);


function _200_JSON_UTF8 (http, body) {
    http.responseHeader("Cache-control", "no-cache");
    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
    http.response(200, body, "UTF-8");
    return false;
}

function _500_JSON_UTF8 (http, body) {
    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
    http.response(500, body, "UTF-8");
    return false;
}

function _GET (service, model) {
	if (typeof model == 'undefined') {
		return Stateful.web({'request': service});
	}
	var resource = 'null', type = null;
	if (model != null) {
        resource = JSON.pprint(model);
        type = JSONR.compile(resource);
	}
    return Stateful.web({
        'request': function (http) {
            var query = http.uri().getRawQuery();
            if (query != null) {
                JSON.parseURLencoded(query, http.state);
                if (type != null) {
                    JSONR.validate(http.state, type);
                }
                return service(http);
            } else {
            	return _200_JSON_UTF8(http, resource);
            }
        }
    });
}

function _POST (service, model) {
    var type = null;
    if (model != null && typeof model != 'undefined') {
        type = JSONR.compile(JSON.encode(model));
    }
    return Stateful.web({
        'request': function (http) {
            http.collect(new StringCollector("UTF-8"));
            return false;
        },
        'collected': function (http) {
            var input = (
                (type == null) ? new JSON(): new JSONR(type)
                ).eval(http.requestBody().toString());
            if (input != null && typeof input == "JSON.Object") {
                input.putAll(http.state);
                http.state = input;
            } else {
                http.state.put("arg0", input);
            }
            return service(http);
        }
    });
}

var server = new HttpServer(".");

var data = function (http) {
    return _200_JSON_UTF8(http, ansql.handle(http.state.getArray("arg0")));
}

var inspect = function (http) {
    return _200_JSON_UTF8(http, JSON.pprint(eval(
        '(' + http.state.getString("arg0", "null") + ')'
        )));
}

var execute = function (http) {
    return _200_JSON_UTF8(http, JSON.pprint(eval(
        "(function(){" + http.state.getString("arg0", "null") + "})()"
        )));
}

var state = function (http) {
    var rt = Runtime.getRuntime();
    var server = http.channel().server();
    var concurrent = [];
    var channel;
    var channels = server.channels();
    while (channels.hasNext()) {
        channel = channels.next();
        concurrent.push({
            "bytesIn": channel.bytesIn,
            "bytesOut": channel.bytesOut,
            "name": channel.toString(),
            "when": channel.when,
            "whenIn": channel.whenIn,
            "whenOut": channel.whenOut
            });
    }
    return _200_JSON_UTF8(http, JSON.pprint({
        "accepted": server.channelsAccepted(),
        "accepting": server.readable(),
        "active": server.isActive(),
        "bytesIn": server.bytesIn,
        "bytesOut": server.bytesOut,
        "channels": concurrent,
        "date": server.httpDate(),
        "dispatched": server.channelsDispatched(),
        "memory free": rt.freeMemory(),
        "memory maximum": rt.maxMemory(),
        "memory total": rt.totalMemory(),
        "routes": server.httpRoutes()
        }));
}

var authority = new Authority("127.0.0.2", "/");

var login = function (http) {
    http.identity = http.state.get("user");
    var password = http.state.get("pass");
    authority.identify(http, http.when()/1000);
    return _200_JSON_UTF8(http, JSON.encode({
        "identity": http.identity, 
        "rights": http.rights
        }));
}

var logoff = function (http) {
    return _200_JSON_UTF8(http, JSON.encode(authority.unidentify(http)));
}

var ansql = new AnSQLite("met4.db");

ansql.open();

// var metabase = new Metabase(ansql, 126);

function Open(filename, address) {
    server.httpListen(
        typeof(address) == "undefined" ? "127.0.0.2:8765": address
        );
    this.hookShutdown([server]);
    var host = server.httpHost();
    var route = function (method, path, handler) {
	    server.httpRoute(method + " " + host + path, handler);
    }
    route("GET", "/", new FileSystem("www"));
    route("GET", "/state", Stateful.web({"request": state}));
    route("GET", "/login", _GET(login, {
        "user": "^[^/s]{4,40}$",
        "pass": "^[^/s]{4,40}$"
        }));
    route("GET", "/logoff", authority.identified(_GET(logoff, null)));
    route("POST", "/data", _POST(data, [null, null]));
    route("GET", "/eval", _GET(inspect, {"arg0": ""}));
    route("POST", "/execute", _POST(execute, ""));
}

// ... asynchronous loop ...

function Close() {
    ansql.close();
}

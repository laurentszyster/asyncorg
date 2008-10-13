/**
 *  Copyright (C) 2007-2008 Laurent A.V. Szyster
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
importClass(Packages.org.protocols.JSON);
importClass(Packages.org.protocols.JSONR);

var asyncorg = Packages.org.async;
 
importClass(asyncorg.core.Static);
importClass(asyncorg.chat.StringCollector);
importClass(asyncorg.sql.AnSQLite);
importClass(asyncorg.sql.Metabase);
importClass(asyncorg.web.HttpServer);
importClass(asyncorg.web.Authority);
importClass(asyncorg.web.FileCache);
importClass(asyncorg.web.FileSystem);
importClass(asyncorg.web.Meta);
importClass(asyncorg.web.Service);
importClass(asyncorg.prototypes.Stateful);

importClass(java.lang.Runtime);


function _200_JSON_UTF8 (http, body) {
    http.set("Cache-control", "no-cache");
    http.set("Content-Type", "text/javascript; charset=UTF-8");
    http.reply(200, body, "UTF-8");
    return false;
}

function _500_JSON_UTF8 (http, body) {
    http.set("Content-Type", "text/javascript; charset=UTF-8");
    http.reply(500, body, "UTF-8");
    return false;
}


function _GET (service, model) {
	if (typeof model == 'undefined') {
		return Stateful.web({
			'handleRequest': function (http) {
                if (http.method().equals("GET")) {
                	return service(http);
                } else {
	                http.error(501); // Not implemented
	                return false;
                }
			}
		});
	}
	var resource = 'null', type = null;
	if (model != null) {
        resource = JSON.pprint(model);
        type = JSONR.compile(resource);
	}
    return Stateful.web({
        'handleRequest': function (http) {
        	if (http.method().equals("GET")) {
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
        	} else {
        		http.error(501); // Not implemented
        		return false;
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
        'handleRequest': function (http) {
            if (http.method().equals("POST")) {
	            http.collect(new StringCollector("UTF-8"));
	            return false;
            } else {
                http.error(501); // Not implemented
                return false;
            }
        },
        'handleBody': function (http) {
            var input = (
                (type == null) ? new JSON.Parser(): new JSONR(type)
                ).eval(http.body().toString());
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

var data = function (http) {
    return _200_JSON_UTF8(http, ansql.handle(http.state.getArray("arg0")));
}

var metabase = new Metabase(ansql, 126);

function met4_request (http) {
    var method = http.method();
    if (method == 'POST') {
        if (http.about[3] == null) {
            http.error(404); // Not found
        } else {
            http.collect(new StringCollector("UTF-8"));
        }
    } else if (method == 'GET') {
        http.set("Cache-control", "no-cache");
        http.set("Content-Type", "text/javascript; charset=UTF-8");
        try {
	        if (http.about[3] == null) {
                http.reply(200, metabase.produce(
                    http.about[1], http.about[2]
                    ));
            } else {
                http.reply(200, metabase.bytes(
                    http.about[1], http.about[2], http.about[3]
                    ));
            }
        } catch (e) {
            http.reply(500, e.toString(), "UTF-8"); // Server error
        }
    } else {
        http.error(501); // Not implemented
    }
    return false;
}

function met4_body (http) {
    http.set("Cache-control", "no-cache");
    http.set("Content-Type", "text/javascript; charset=UTF-8");
    metabase.sql.begin();
    try {
        var previous = metabase.select(http.about[1], http.about[2], http.about[3]);
        // http.json = JSON.decode(http.requestBody.toString());
        if (previous == null) {
            metabase.insert(
                http.about[1], http.about[2], http.about[3], http.body().toString()
                );
        } else {
            metabase.update(
                http.about[1], http.about[2], http.about[3], http.body().toString()
                );
        }
    } catch (e) {
        metabase.sql.rollback();
        http.reply(500, e.toString(), "UTF-8"); // Server error
        return;
    }
    metabase.sql.commit();
    http.reply(200, "null", "UTF-8");
}

var met4 = new Meta (metabase);

met4.predicates.put("predicate", Stateful.web({
    "handleRequest": met4_request, 
    "handleBody": met4_body
    }));

function Open(filename, address) {
    server.httpListen(
        typeof(address) == "undefined" ? "127.0.0.2:8765": address
        );
    var host = server.httpHost();
    var route = function (path, handler) {
	    server.httpRoute(host + path, handler);
    }
    route("", new FileCache("www"));
    route("/state", _GET(state));
    route("/login", _GET(
        login, {"user": "^[^/s]{4,40}$", "pass": "^[^/s]{4,40}$"})
        );
    route("/eval", _GET(inspect, {"arg0": ""}));
    route("/execute", _POST(execute, ""));
    route("/data", _POST(data, [null, null]));
    route("/logoff", authority.identified(_GET(logoff, null)));
    route("/meta", met4);
    this.hookShutdown([server]);
}

// ... asynchronous loop ...

function Close() {
    ansql.close();
}

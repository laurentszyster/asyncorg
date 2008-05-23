/**
 * Use Rhino's JavaScript interpreter to prototype and configure the 
 * web application of asyncorg. It will not slow down the web server's 
 * configuration significantly and supports a fast development cycle.
 * 
 */
var asyncorg = Packages.org.async;
 
importClass(asyncorg.protocols.JSON);
importClass(asyncorg.protocols.JSONR);
importClass(asyncorg.sql.AnSQLite);
importClass(asyncorg.web.HttpServer);
importClass(asyncorg.web.Authority);
importClass(asyncorg.web.HttpServerState);
importClass(asyncorg.web.FileSystem);
importClass(asyncorg.web.Prototype);

importClass(java.lang.Runtime);

var server = new HttpServer(".");

var authority = new Authority("127.0.0.2", "/");

var db = new AnSQLite(":memory:", 0);

function _200_JSON_UTF8 (http, body) {
    http.responseHeader("Cache-control", "no-cache");
    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
    http.response(200, body, "UTF-8");
}

function Service (model, service) {
    var _resource = JSON.pprint(model);
    var _type = JSONR.compile(_resource);
    return Prototype.bind ({
        "type": function (http) {return _type;},
        "service": service,
        "resource": function (http) {
            _200_JSON_UTF8 (http, _resource);
            }
    });
}

function Resource (body) {
    return Prototype.bind ({
        "resource": function (http) {
            _200_JSON_UTF8 (http, body);
            }
    });
}

var login = function (http) {
    http.identity = http.state.get("user");
    var password = http.state.get("pass");
    authority.identify(http, http.when()/1000);
    _200_JSON_UTF8 (http, JSON.encode({
        "identity": http.identity, 
        "rights": http.rights
        }));
}

var ansql = function (http) {
    _200_JSON_UTF8 (http, ansqlite.handle(http.state.getArray("arg0")));
}

var inspect = function (http) {
    _200_JSON_UTF8 (http, JSON.pprint(eval(
        '(' + http.state.getString("arg0") + ')'
        )));
}

var execute = function (http) {
    _200_JSON_UTF8 (http, JSON.pprint(eval(
        "(function(){" + http.state.get("arg0", "null") + "})()"
        )));
}

var metabase_type = {
    "Prototype": {
        "type": function (http) {
            return 
        },
        "service": function (http) {
            
        },
        "resource": function (http) {
            
        }
    }
}

var state = {
    "resource": function (http) {
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
        _200_JSON_UTF8 (http, JSON.pprint({
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
}

function Open(filename, address) {
    db.open();
    this.exits.add(server);
    server.httpListen(
        typeof(address) == "undefined" ? "127.0.0.2:8765": address
        );
    var host = server.httpHost();
    server.httpRoute("GET " + host + "/", new FileSystem("www"));
    server.httpRoute(
        "GET " + host + "/login", 
        Service({
            "user": "^[^/s]{4,40}$",
            "pass": "^[^/s]{4,40}$"
            }, login)
        );
    server.httpRoute(
        "POST " + host + "/db", 
        authority.identified(Service([null, null], ansql))
        );
    server.httpRoute(
        "GET " + host + "/state", 
        authority.identified(Resource(state))
        );
    server.httpRoute(
        "GET " + host + "/inspect", 
        authority.identified(Service(null, inspect))
        );
    server.httpRoute(
        "POST " + host + "/execute", 
        authority.identified(Service(null, execute))
        );
}

// ... asynchronous loop ...

function Close() {
    db.close();
}

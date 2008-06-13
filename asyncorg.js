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
}

function _500_JSON_UTF8 (http, body) {
    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
    http.response(500, body, "UTF-8");
}

function Service (service, model) {
    var _resource = JSON.pprint(model);
    var _type = (model == null) ? null: JSONR.compile(_resource);
    return Stateful.web({
        "type": function (http) {return _type;},
        "service": service,
        "resource": function (http) {
            _200_JSON_UTF8 (http, _resource);
            }
    });
}

function Resource (body) {
    return Stateful.web({
        "resource": function (http) {
            _200_JSON_UTF8 (http, body);
            }
    });
}

var server = new HttpServer(".");

var login = function (http) {
    http.identity = http.state.get("user");
    var password = http.state.get("pass");
    authority.identify(http, http.when()/1000);
    _200_JSON_UTF8(http, JSON.encode({
        "identity": http.identity, 
        "rights": http.rights
        }));
}

var logoff = function (http) {
    _200_JSON_UTF8(http, JSON.encode(authority.unidentify(http)));
}

var data = function (http) {
    _200_JSON_UTF8(http, ansql.handle(http.state.getArray("arg0")));
}

var inspect = function (http) {
    _200_JSON_UTF8(http, JSON.pprint(eval(
        '(' + http.state.getString("arg0", "null") + ')'
        )));
}

var execute = function (http) {
    _200_JSON_UTF8(http, JSON.pprint(eval(
        "(function(){" + http.state.getString("arg0", "null") + "})()"
        )));
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
        _200_JSON_UTF8(http, JSON.pprint({
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

var ansql = new AnSQLite("met4.db");

ansql.open();

var metabase = new Metabase(ansql, 126);

var publicNames = function (http) {
    var responses = [];
    var requests = http.state.get("arg0").iterator();
    while (requests.hasNext()) {
        responses.push(metabase.indexed(requests.next()));
    }
    _200_JSON_UTF8(http, JSON.encode(responses));
}

var publicRDF = function (http) {
    var st, requests = http.state.get("arg0").iterator();
    while (requests.hasNext()) {
        st = requests.next();
        metabase.send(st.get(0), st.get(1), st.get(2), st.get(3));
    }
    _200_JSON_UTF8(http, JSON.encode(metabase));
}

var authority = new Authority("127.0.0.2", "/");

function Open(filename, address) {
    server.httpListen(
        typeof(address) == "undefined" ? "127.0.0.2:8765": address
        );
    this.hookShutdown([server]);
    var host = server.httpHost();
    server.httpRoute("GET " + host + "/", new FileSystem("www"));
    server.httpRoute(
        "GET " + host + "/login", 
        Service(login, {
            "user": "^[^/s]{4,40}$",
            "pass": "^[^/s]{4,40}$"
            })
        );
    server.httpRoute(
        "GET " + host + "/logoff", 
        authority.identified(Service(logoff, null))
        );
    server.httpRoute(
        "POST " + host + "/publicNames", 
        Service(publicNames, [""])
        );
    server.httpRoute(
        "POST " + host + "/publicRDF", 
        Service(publicRDF, [["", "", "", ""]])
        );
    server.httpRoute(
        "POST " + host + "/data", 
        Service(data, [null, null])
        );
    server.httpRoute(
        "GET " + host + "/state", 
        Stateful.web(state)
        );
    server.httpRoute(
        "GET " + host + "/eval", 
        Service(inspect, {"arg0": ""})
        );
    server.httpRoute(
        "POST " + host + "/execute", 
        Service(execute, "")
        );
}

// ... asynchronous loop ...

function Close() {
    ansql.close();
}

importClass(Packages.org.async.protocols.JSON);
importClass(Packages.org.async.sql.AnSQLite);
importClass(Packages.org.async.web.HttpServer);
importClass(Packages.org.async.web.Authority);
importClass(Packages.org.async.web.HttpServerState);
importClass(Packages.org.async.web.FileSystem);
importClass(Packages.org.async.web.Prototype);

function jsonResponse (http, status, object) {
    http.responseHeader("Cache-control", "no-cache");
    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
    http.response(status, JSON.encode(object), "UTF-8");
}

var authority = new Authority("127.0.0.2", "/");

var login = Prototype.bind({
    "service": function (http) {
        authority.identify(http, http.when()/1000);
        jsonResponse(http, 200, {"identity": http.identity});
    }
});

var state = authority.identified(new HttpServerState());

var sql = new AnSQLite(":memory:", 0);

var db = authority.identified(Prototype.bind({
    "service": function (http) {
        var statement = http.state.getArray("arg0");
        var response = sql.handle(statement);
        http.responseHeader("Cache-control", "no-cache");
        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, response, "UTF-8");
    }
}));

function open(address) {
    sql.open();
    var server = new HttpServer(".");
    server.httpListen(
        typeof(address) == "undefined" ? "127.0.0.2:8765": address
        );
    var host = server.httpHost();
    server.httpRoute("GET " + host + "/", new FileSystem("www"));
    server.httpRoute("POST " + host + "/db", db);
    server.httpRoute("GET " + host + "/doc", new FileSystem("doc"));
    server.httpRoute("GET " + host + "/login", login);
    server.httpRoute("GET " + host + "/state", state);
    this.exits.add(server);
}

function close() {
    sql.close();
}

import org.async.core.Static;
import org.async.web.HttpServer;
import org.async.web.FileSystem;
import org.async.web.Database;
import org.async.sql.AnSQLite;

public class HttpServerTest {
    public static void main (String[] args) throws Throwable {
        AnSQLite sql = new AnSQLite(":memory:", 0);
        Database db = new Database(sql);
        Static.loop.hookShutdown();
        try {
            HttpServer server = new HttpServer(".");
            server.httpListen((args.length > 0) ? args[0]: "127.0.0.2:8765");
            String host = server.httpHost();
            server.httpRoute("GET " + host + "/", new FileSystem("www"));
            server.httpRoute("GET " + host + "/db", db);
            server.httpRoute("POST " + host + "/db", db);
            server.httpRoute("GET " + host + "/doc", new FileSystem("doc"));
            Static.loop.exits.add(server);
            server = null;
        } catch (Throwable e) {
            Static.loop.log(e);
        }
        sql.open();
        Static.loop.dispatch();
        sql.close();
    }
}

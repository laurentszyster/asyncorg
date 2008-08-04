import org.async.core.Static;
import org.async.protocols.JSONR;
import org.async.web.HttpServer;
import org.async.web.FileSystem;
import org.async.web.Database;
import org.async.sql.AnSQLite;
import org.async.web.Service;


public class HttpServerTest {
	protected static final class Except extends Service {
		public final void configure (String route) {
			;
		}
		public final boolean identify (HttpServer.Actor http) {
			return false;
		}
		public final JSONR.Type type (HttpServer.Actor http) {
			return null;
		}
		public final void service (HttpServer.Actor http) {
			try {
				throw new Exception("except");
			} catch (Exception e) {
				http.channel().log(e);
			}
			http.response(500); // Server Error
		}
		public final void resource (HttpServer.Actor http) {
			http.response(501); // Not implemented
		}
	}
    public static void main (String[] args) throws Throwable {
        AnSQLite sql = new AnSQLite(":memory:", 0);
        Database db = new Database(sql);
        Static.loop.hookShutdown();
        try {
            HttpServer server = new HttpServer(".");
            server.httpListen((args.length > 0) ? args[0]: "127.0.0.2:8765");
            String host = server.httpHost();
            server.httpRoute(host + "/", new FileSystem("www"));
            server.httpRoute(host + "/db", db);
            server.httpRoute(host + "/doc", new FileSystem("doc"));
            server.httpRoute(host + "/except", new Except());
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

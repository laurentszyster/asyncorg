import org.async.web.HttpServer;
import org.async.web.FileSystem;
import org.async.web.Database;
import org.async.web.Service;
import org.async.sql.AnSQLite;
import org.async.core.Static;
import org.simple.Fun;


public class HttpServerTest {
	protected static final class Except implements Fun {
		public final Object apply (Object o) {
			HttpServer.Actor http = (HttpServer.Actor) o;
			try {
				throw new Exception("except");
			} catch (Exception e) {
				http.channel().log(e);
			}
			http.error(500); // Server Error
			return null;
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
            server.httpRoute(host + "/db", new Service(db, null));
            server.httpRoute(host + "/doc", new FileSystem("doc"));
            server.httpRoute(host + "/except", new Service(new Except()));
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

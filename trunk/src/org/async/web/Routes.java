package org.async.web;

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URLDecoder;

/**
 * An asynchronous HTTP <code>Handler</code> wrapper to match URI paths
 * against a regular expression and eventually collect named groups into
 * the HTTP <code>Actor</code>'s JSON state. 
 * 
 * It is a usefull convenience to allow statefull handlers to pose as
 * resources and handle sets of idempotent requests with one function. 
 * 
 * Note that it is up to the wrapped handler to add the usual headers
 * for idempotent responses. Applications that serve cacheable entity
 * may add a "proxy: cache" or "proxy: private" to prevent distribution
 * of its content in the web of HTTP/1.1 on the network.  
 * 
 * @h3 Synopsis
 * 
 * @p ...
 * 
 * @pre server.httpRoute(
 *    'GET example.com/test', 
 *    new Routes(handler, "^/(.+?)/hello-world$", 
 *    new String[]{"arg0"}
 *    );
 * 
 * @p ...
 * 
 * @pre GET /test/serge/hello-world HTTP/1.1
 *Host: example.com
 * 
 * @p ...
 * 
 * @pre {"arg0": "serge"}
 * 
 * @p ...
 * 
 * @pre new Routes(
 *    handler, 
 *    "^([0-9]{4}).([0-9]{2}).([0-9]{2})$", 
 *    new String[]{"year", "month", "day"}
 *    )
 * 
 * @p ...
 * 
 * @pre GET /2008.04.22 HTTP/1.1
 *Host: example.com
 * 
 * @p ...
 * 
 * @pre {"year": "2008", "month": "04", "day": "22"}
 * 
 * @p ...
 * 
 * @author Laurent Szyster
 *
 */
public class Routes implements Handler {
    private Handler _routed;
    private Pattern _re; 
    private String[] _names;
    public Routes (Handler routed, Pattern regular, String[] names) {
        _re = regular;
        _names = names;
        _routed = routed;
    }
    public final boolean request (Actor http) throws Throwable {
		String value;
        Matcher re = _re.matcher(http.about[1]);
        if (re.matches()) {
    		int n = re.groupCount();
            for (int i=1; i<n; i++) {
            	value = re.group(i);
            	if (value != null)  {
            		http.state.put(_names, URLDecoder.decode(value, "UTF-8"));
            	}
            } 
            http.handler = _routed;
            return _routed.request(http);
        }
        return false;
    }
    public final void collected (Actor http) throws Throwable {
		throw new Error("unexpected call");
    }

}

package org.async.web;

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
 * @pre new RegularRoutes(new HelloWorld(), "/test/(.*?)/hello-world")
 * 
 * @pre new RegularRoutes(new HelloWorld(), "/test/(.+?)/hello-world", "arg0")
 * 
 * @pre GET /test/serge/hello-world HTTP/1.0
 * 
 * @pre {"arg0": "serge"}
 * 
 * @pre new RegularRoutes(
 *    new HelloWorld(), 
 *    "/test/([0-9]{4}).([0-9]{2}).([0-9]{2})/day_of_week", 
 *    "year", "month", "day",
 *    )
 * 
 * @pre {"year": "2008", "month": "04", "day": "22"}
 * 
 * @author Laurent Szyster
 *
 */
public class RegularRoutes implements Handler {
    private Handler _routed;
    private Pattern _re; 
    private String[] _names;
    public RegularRoutes (Handler routed, Pattern regular, String ... names) {
        _re = regular;
        _names = names;
        _routed = routed;
    }
    public final void handleConfigure (String route) throws Throwable {
        _routed.handleConfigure(route);
    }
    public boolean handleIdentify(HttpServer.Actor http) throws Throwable {
        return _routed.handleIdentify(http);
    }
    public final boolean handleRequest (Actor http) throws Throwable {
        Matcher re = _re.matcher(http.uri().getPath());
        if (re.matches()) {
            for (int i=0, n=re.groupCount()-1; i<n; i++) {
                http.state.put(_names, re.group(i+1));
            } 
            return _routed.handleRequest(http);
        }
        return false;
    }
    public final void handleCollected (Actor http) throws Throwable {
        _routed.handleCollected(http);
    }

}

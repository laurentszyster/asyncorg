package org.async.web;

import org.async.sql.Metabase;
import org.async.protocols.JSON;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URLDecoder;

public class Meta implements HttpServer.Controller {
	private Metabase _metabase;
	public Meta (Metabase metabase) {
		_metabase = metabase;
	}
	public HashMap<String,HttpServer.Controller> predicates = new HashMap();
	private static final Pattern _re = Pattern.compile(
		"^/([^/]+)(?:/([^/]+)(?:/([^/]+?))?)?$"
		);
	public final boolean handleRequest (HttpServer.Actor http) throws Throwable {
        if (http.about.length < 2) {
        	http.error(404); // Not Found
            return false;
        }
        Matcher re = _re.matcher(http.about[1]);
        if (re.matches()) {
    		String value;
    		int n = re.groupCount() + 1;
			String[] about = new String[n];
			about[0] = http.about[0];
	        for (int i=1; i<n; i++) {
	        	value = re.group(i);
	        	if (value == null) {
	        		about[i] = null;
	        	} else {
	        		about[i] = URLDecoder.decode(value, "UTF-8");
	        	}
	        }
	        http.about = about;
        } else {
        	http.error(404); // Not Found
            return false;
        }
        if (http.about[2] != null) { // has a predicate
        	HttpServer.Controller predicate = predicates.get(http.about[2]);
        	if (predicate == null) {
        		http.error(404); // Not found
        	} else {
		        http.handler = predicate;
		        return predicate.handleRequest(http);
        	}
        } else {
        	if (http.method().equals("GET")) {
	            http.set("Cache-control", "no-cache");
	            http.set("Content-Type", "text/javascript; charset=UTF-8");
	            http.reply(
            		200, JSON.encode(_metabase.walk(http.about[1])), "UTF-8"
            		);
        	} else {
            	http.error(501); // Not implemented
        	}
        }
		return false;
	}
	public final void handleBody (HttpServer.Actor http) {
		throw new Error("unexpected call");
	}
}

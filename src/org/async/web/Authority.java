package org.async.web;

import org.async.web.HttpServer.Handler;
import org.async.web.HttpServer.Actor;
import org.async.protocols.IRTD2;
import org.async.simple.Strings;

/**
 * ...
 * 
 * @pre Authority authorize = new Authority("localhost", "/");
 * authorize.salts = new byte[][]{'s', 'a', 'l', 't'};
 * authorize.timeout = 600;
 * authorize.identified(handler);
 * 
 */
public class Authority {
    public byte[][] salts = new byte[][]{
        Strings.random(10, Strings.ALPHANUMERIC).getBytes()
    };
    public int timeout = 600;
    private String _qualifier;
    public Authority () {
        _qualifier = "; expires=Sun, 17-Jan-2038 19:14:07 GMT";
    }
    public Authority (String domain, String path) {
        _qualifier = (
            "; expires=Sun, 17-Jan-2038 19:14:07 GMT; path=" + path 
            + "; domain=" + domain
            );
    }
    public final void identify (Actor http, long time) {
        if (http.identity == null || http.identity.length() == 0) {
            http.identity = Strings.random(10, Strings.ALPHANUMERIC); 
        } 
        String[] irtd2 = new String[]{
            http.identity,
            http.rights,
            String.valueOf(time),
            http.digested
            };
        http.digest = IRTD2.digest(irtd2, salts[0]);
        StringBuilder sb = new StringBuilder();
        sb.append(irtd2[0]);
        sb.append(' ');
        sb.append(irtd2[1]);
        sb.append(' ');
        sb.append(irtd2[2]);
        sb.append(' ');
        if (irtd2[3] != null) {
            sb.append(irtd2[3]);
        }
        sb.append(' ');
        sb.append(http.digest);
        sb.append(_qualifier);
        http.responseCookie("IRTD2", sb.toString());
    }
    public final String unidentify (Actor http) {
        http.responseCookie("IRTD2", _qualifier);
        return http.identity;
    }
    protected static class Identified implements Handler {
        private HttpServer.Handler _wrapped;
        private Authority _authority;
        public Identified (Handler handler, Authority authority) {
            _wrapped = handler;
            _authority = authority;
        }
        public final void configure(String route) throws Throwable {
            _wrapped.configure(route);
        }
        public final boolean identify (Actor http) throws Throwable {
            http.response(401); // Unauthorized
            return false;
        }
        public final boolean request(Actor http) throws Throwable {
            String irtd2 = http.requestCookie("IRTD2");
            if (irtd2 != null) {
                String[] parsed = IRTD2.parse(irtd2);
                long time = http.when()/1000;
                int error = IRTD2.digested(
                    parsed, time, _authority.timeout, _authority.salts
                    );
                if (error == 0) {
                    http.identity = parsed[0];
                    http.rights = parsed[1];
                    http.digested = parsed[4];
                    _authority.identify(http, time);
                    return _wrapped.request(http);
                } else {
                    http.channel().log("IRTD2 error " + error);
                }
            }
            if (_wrapped.identify(http)) {
            	http.handler = _wrapped;
                return _wrapped.request(http);
            }
            return false;
        }
        public final void collected(Actor http) throws Throwable {
        	;
        }
    }
    public final Handler identified(Handler handler) { 
        return new Identified(handler, this);
    }
    protected static class Authorized implements Handler {
        private HttpServer.Handler _wrapped;
        private String _right;
        public Authorized (Handler handler, String right) {
            _wrapped = handler;
            _right = right;
        }
        public final void configure(String route) throws Throwable {
            _wrapped.configure(route);
        }
        public final boolean identify (Actor http) throws Throwable {
            http.response(401); // Unauthorized
            return false;
        }
        public final boolean request(Actor http) throws Throwable {
            if (http.rights == null || http.rights.indexOf(_right) < 0) {
                return false;
            } else {
            	http.handler = _wrapped;
                return _wrapped.request(http);
            }
        }
        public final void collected(Actor http) throws Throwable {
        	;
        }
    }
    public static final Handler authorized (Handler handler, String right) {
        return new Authorized(handler, right);
    }
}

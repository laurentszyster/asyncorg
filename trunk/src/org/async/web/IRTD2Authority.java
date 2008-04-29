package org.async.web;

import org.async.web.HttpServer.Handler;
import org.async.web.HttpServer.Actor;
import org.async.protocols.IRTD2;
import org.async.protocols.SHA1;
import org.async.simple.Strings;

/**
 * ...
 * 
 * @pre IRTD2Authority authorize = new IRTD2Authority();
 * authorize.salts = new byte[][]{'s', 'a', 'l', 't'};
 * authorize.timeout = 600;
 * authorize.identified(handler);
 * 
 */
public class IRTD2Authority {
    public byte[][] salts = new byte[][]{
        Strings.random(10, Strings.ALPHANUMERIC).getBytes()
    };
    public int timeout = 600;
    protected static class IRTD2Identified implements Handler {
        private HttpServer.Handler _wrapped;
        private IRTD2Authority _authority;
        public IRTD2Identified (Handler handler, IRTD2Authority authority) {
            _wrapped = handler;
            _authority = authority;
        }
        public final void handleConfigure(String route) throws Throwable {
            _wrapped.handleConfigure(route);
        }
        public final boolean handleIdentify (Actor http) {
            return true;
        }
        public final boolean handleRequest(Actor http) 
        throws Throwable {
            String irtd2 = http.requestCookie("IRTD2");
            if (irtd2 != null) {
                String[] parsed = IRTD2.parse(irtd2);
                long time = http.when()/1000;
                if (IRTD2.digested(
                    parsed, time, _authority.timeout, _authority.salts
                    ) == 0) {
                    http.identity = parsed[0];
                    http.rights = parsed[1];
                    http.digested = parsed[4];
                    _authority.identify(http, time);
                    return _wrapped.handleRequest(http);
                };
            } 
            if (_wrapped.handleIdentify(http)) {
                return _wrapped.handleRequest(http);
            }
            return false;
        }
        public final void handleCollected(Actor http) 
        throws Throwable {
            _wrapped.handleCollected(http);
        }
    }
    public final Handler identified(Handler handler) { 
        return new IRTD2Identified(handler, this);
    }
    public final void identify (Actor http, long time) {
        if (
                http.identity.length() == 0 || 
                http.identity.indexOf(" ") >= 0
                ) {
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
        sb.append("; expires=Sun, 17-Jan-2008 19:14:07 GMT; path=");
        sb.append(http._uri.getPath());
        sb.append("; domain=");
        sb.append(http._host);
        http.responseCookie("IRTD2", sb.toString());
    }
    public static final boolean authorized (
        Actor http, String digest, String[] rights
        ) {
        if (digest.equals(http.state.getString("digest", ""))) {
            String identity = http.state.getString("identity", null);
            if (identity == null) {
                http.identity = Strings.random(10, Strings.ALPHANUMERIC);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(http.rights);
            for (int i=0; i<rights.length; i++) {
                if (http.rights.indexOf(rights[i]) < 0) {
                    sb.append(',');
                    sb.append(rights[i]);
                }
            }
        }
        return false;
    }
}

package org.async.protocols;

import org.async.core.Dispatcher;
import org.async.core.Function;
import org.async.simple.Strings;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.SocketAddress;

public class DNSClient extends Dispatcher {

    protected static final int _skipName (byte[] datagram, int pos) {
        int ll;
        while (pos < datagram.length) {
            ll = datagram[pos];
            if ((ll & 0xc0) != 0) {
                return pos + 2;
            } else if (ll == 0) {
                pos++;
                break;
            } else {
                pos = pos + ll + 1;
            }
        }
        return pos;
    } 
    
    protected static final String _unpackName (byte[] datagram, int pos) {
        ArrayList n = new ArrayList();
        int ll;
        while (pos < datagram.length) {
            ll = datagram[pos];
            if ((ll & 0xc0) != 0) {
                pos = (ll&0x3f << 8) + (datagram[pos+1]);
            } else if (ll == 0) {
                break;
            } else {
                pos = pos + 1;
                n.add(new String(datagram, pos, pos+ll));
                pos = pos + ll;
            }
        }
        return Strings.join(".", n.iterator());
    }
    
    protected static final int _unpackTTL (byte[] datagram, int pos) {
        int ttl = 0;
        for (int i=0; i<4; i++) {
            ttl = (ttl << 8) | datagram[pos+i];
        }
        return ttl;
    }
    
    protected static final int _unpackPreference (byte[] datagram, int pos) {
        int pref = 0;
        for (int i=0; i<2; i++) {
            pref = (pref << 8) | datagram[pos+i];
        }
        return pref;
    }
    
    public static abstract class Request {
        public int failover = 0;
        public int ttl = 60;
        public long when = 0;
        public int uid = 0;
        public SocketAddress peer;
        public SocketAddress[] servers;
        public byte[] response;
        public ArrayList resources;
        public ArrayList defered;
        protected String[] _question;
        protected Request(String[] question) {
            _question = question;
        }
        protected final boolean unpack (byte[] datagram) {
            response = datagram;
            int ancount = (datagram[6]<<8) + datagram[7];
            if (ancount > 0) {
                int pos = _skipName(datagram, 12);
                resources = new ArrayList();
                for (int an=0; an<ancount; an++) {
                    pos = _skipName(datagram, pos);
                    if (collect(datagram, pos)) {
                        break;
                    }
                    pos = pos + 8;
                    pos = pos + 2 + (datagram[pos]<<8) + datagram[pos+1];
                }
                return collected(datagram, pos);
            }
            return false;
        }
        public abstract String[] template();
        public final byte[] datagram () {
            String[] t = template();
            StringBuffer sb = new StringBuffer();
            sb.append((char)(uid>>8)&0xff);
            sb.append((char)uid&0xff);
            sb.append(t[0]);
            String part;
            Iterator<String> parts = Strings.split(_question[0], '.');
            while (parts.hasNext()) {
                part = parts.next();
                sb.append((char)part.length());
                sb.append(part);
            }
            sb.append(t[1]);
            return sb.toString().getBytes();
        }
        public abstract boolean collect(byte[] datagram, int pos);
        public abstract boolean collected(byte[] datagram, int pos);
    }
    
    public static final class RequestA extends Request {
        protected RequestA(String[] question) {
            super(question);
        }
        private static final String[] _template = new String[]{
            "\001\000\000\001\000\000\000\000\000\000",
            "\000\000\001\000\001"
            };
        public String[] template() {
            return _template;
        }
        public final boolean collect(byte[] datagram, int pos) {
            if (
                datagram[pos] == 0 && 
                datagram[pos+1] == 1 && 
                datagram[pos+2] == 0 && 
                datagram[pos+3] == 1 
                ) {
                ttl = _unpackTTL(datagram, pos+4);
                resources.add(
                    Integer.toString(datagram[10]) + "." +
                    Integer.toString(datagram[11]) + "." +
                    Integer.toString(datagram[12]) + "." +
                    Integer.toString(datagram[13])
                    );
                return true;
            }
            return false;
        }
        public final boolean collected(byte[] datagram, int pos) {
            return true;
        }
    }
    
    protected long _sent = 0;
    protected int _failover = 1;
    protected HashMap _cache = new HashMap();
    protected HashMap _pending = new HashMap();
    protected SocketAddress[] _servers;
    
    public DNSClient(SocketAddress[] servers) {
        super();
        _servers = servers;
    }
    
    public void resolve(
        String[] question, Function resolved, SocketAddress[] servers
        ) throws Throwable {
        Request req = (Request) _cache.get(question);
        if (req == null) {
            if (req.defered == null) {
                if (req.when + req.ttl > _loop.now()) {
                    resolved.apply(req);
                    return;
                }
            } else {
                req.defered.add(resolved);
                return;
            }
        }
        if (question == null || question.length != 2) {
            throw new Error("parameter must be an array of two strings");
        } else if (question[1] == "A") {
            req = new RequestA(question);
        } else {
            throw new Error("unsupported DNS request type " + question[0]);
        }
        req.servers = _servers;
        req.defered = new ArrayList();
        req.defered.add(resolved);
        // send(req);
    }
    
    public boolean writable() {
        return false;
    }

    public boolean readable() {
        return !_pending.isEmpty();
    }

    public void handleAccept() throws Throwable {
        throw new Error("unexpected accept event");
    }

    public void handleConnect() throws Throwable {
    }

    public void handleWrite() throws Throwable {
        throw new Error("unexpected write event");
    }

    public void handleRead() throws Throwable {
    }

    public void handleClose() throws Throwable {
    }

    public Object apply(Object input) throws Throwable {
        return null;
    }

}

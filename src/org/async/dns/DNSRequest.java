/**
 * 
 */
package org.async.dns;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import org.async.simple.Strings;

public abstract class DNSRequest {
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
    protected DNSRequest(String[] question) {
        _question = question;
    }
    protected final boolean unpack (byte[] datagram) {
        response = datagram;
        int ancount = ((datagram[6] & 0xff) * 256) + (datagram[7] & 0xff);
        if (ancount > 0) {
            int pos = DNS._skipName(datagram, 12) + 4;
            resources = new ArrayList();
            for (int an=0; an<ancount; an++) {
                pos = DNS._skipName(datagram, pos);
                if (collect(datagram, pos)) {
                    break;
                }
                pos = pos + 8;
                pos = pos + 2 + (
            		(datagram[pos] & 0xff) * 256
            		) + (datagram[pos+1] & 0xff);
            }
            return collected(datagram, pos);
        }
        return false;
    }
    public abstract String[] template();
    public final byte[] datagram () {
        String[] t = template();
        StringBuilder sb = new StringBuilder();
        sb.append((char) ((uid >> 8) & 0xff));
        sb.append((char) (uid & 0xff));
        sb.append(t[0]);
        String part;
        Iterator<String> parts = Strings.split(_question[0], '.');
        while (parts.hasNext()) {
            part = parts.next();
            sb.append((char) part.length());
            sb.append(part);
        }
        sb.append(t[1]);
        byte[] d = sb.toString().getBytes();
        return d;
    }
    public abstract boolean collect(byte[] datagram, int pos);
    public abstract boolean collected(byte[] datagram, int pos);
}
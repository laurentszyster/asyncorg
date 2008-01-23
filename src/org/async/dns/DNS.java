package org.async.dns;

import java.util.ArrayList;

import org.async.simple.Strings;

public class DNS {

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
                pos = (ll&0x3f << 8) + datagram[pos+1];
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

}

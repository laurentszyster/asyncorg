package org.async.dns;

import org.async.simple.SIO;
import org.async.simple.Strings;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

public class DNS {

    protected static final int _skipName (byte[] datagram, int pos) {
        int ll;
        while (pos < datagram.length) {
            ll = (datagram[pos] & 0xff);
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

    protected static final ArrayList _unpackText (byte[] datagram, int pos) {
        ArrayList t = new ArrayList();
        int ll;
        while (pos < datagram.length) {
            ll = (datagram[pos] & 0xff);
            if (ll == 0 || ll == 0xc0) {
                break;
            } else {
                pos = pos + 1;
                t.add(new String(datagram, pos, ll));
                pos = pos + ll;
            }
        }
        return t;
    }

    protected static final String _unpackName (byte[] datagram, int pos) {
        ArrayList n = new ArrayList();
        int ll;
        while (pos < datagram.length) {
            ll = (datagram[pos] & 0xff);
            if ((ll & 0xc0) != 0) {
                pos = ((ll & 0x3f) * 256) + (datagram[pos+1] & 0xff);
            } else if (ll == 0) {
                break;
            } else {
                pos = pos + 1;
                n.add(new String(datagram, pos, ll));
                pos = pos + ll;
            }
        }
        return Strings.join(".", n.iterator());
    }

    protected static final int _unpackTTL (byte[] datagram, int pos) {
        int ttl = 0;
        for (int i=0; i<4; i++) {
            ttl = (ttl * 256) + (datagram[pos+i] & 0xff);
        }
        return ttl;
    }

    protected static final int _unpackPreference (byte[] datagram, int pos) {
        int pref = 0;
        for (int i=0; i<2; i++) {
            pref = (pref * 256) + (datagram[pos+i] & 0xff);
        }
        return pref;
    }

    private static final Pattern _NT = Pattern.compile (
		"^.*?Windows.*$", Pattern.CASE_INSENSITIVE
		);
    
    private static final Pattern _NT_IPCONFIG = Pattern.compile (
		"DNS[.\\s\\w]+?:\\s+?" 
		+ "([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+).+?"
		+ "([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)?",
		Pattern.DOTALL
		);
    
    private static final Pattern _NAMESERVER = Pattern.compile(
		"nameserver[ ]+(.+)"
		);
    
    public static final SocketAddress[] servers () throws IOException {
		ArrayList<String> ips = new ArrayList<String>();
    	String osName = System.getProperty("os.name"); 
    	if (_NT.matcher(osName).matches()) {
    		String input = SIO.read(
				Runtime.getRuntime().exec("ipconfig /all").getInputStream()
				);
    		Matcher match = _NT_IPCONFIG.matcher(input);
			if (match.find()) {
				if (match.group(1) != null) {
					ips.add(match.group(1));
				}
				if (match.group(2) != null && match.group(2).length() > 0) {
					ips.add(match.group(2));
				}
			}
		} else {
    		String input = SIO.read("/etc/resolv.conf");
    		int L = input.length(), pos = 0;
    		Matcher match = _NAMESERVER.matcher(input);
    		String ip;
    		while (pos < L) {
    			if (match.find(pos)) {
    				ip = match.group(1); 
    				if (ip != null && ip.length() > 0) {
    					ips.add(ip);
    				}
    				pos = match.end();
    			} else {
    				break;
    			}
    		}
		} 
    	if (ips.size() == 0) {
			ips.add("127.0.0.1");
		}
    	SocketAddress[] servers = new SocketAddress[ips.size()];
		for (int i=0; i<ips.size(); i++) {
			servers[i] = new InetSocketAddress(ips.get(i), 53);
		}
    	return servers;
    }
    
    public static final DNSClient client () throws IOException {
    	return new DNSClient(servers(), 2000, 1000);
    }
    
}

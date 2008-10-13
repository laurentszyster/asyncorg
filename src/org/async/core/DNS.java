package org.async.core;

import org.simple.Fun;
import org.simple.SIO;
import org.simple.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * A static DNS client, to resolve A, NS, MX, TXT and PTR records.
 * 
 * @h3 Synopsis
 * 
 * @p The purpose of this implementation is to break free from the synchronous
 * resolver API and develop fully non-blocking peers.
 */
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
        return (datagram[pos+1] & 0xff) * 256 + (datagram[pos+1] & 0xff);
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
        public String question;
        protected Request(String question) {
            this.question = question;
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
            Iterator<String> parts = Strings.split(question.split(" ")[1], '.');
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

    public static abstract class Resolution implements Fun {
    	public final Object apply (Object args) throws Throwable {
    		return handle ((Request) args);
    	}
    	public abstract Object handle (Request resolved) throws Throwable;
    } 
    
    protected static final class RequestA extends Request {
	    protected RequestA(String question) {
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
	        if (datagram[pos+1] == '\001' && datagram[pos+3] == '\001') {
	            ttl = _unpackTTL(datagram, pos+4);
	            resources.add(
	                ((int) datagram[pos+10] & 0xff) + "." +
	                ((int) datagram[pos+11] & 0xff) + "." +
	                ((int) datagram[pos+12] & 0xff) + "." +
	                ((int) datagram[pos+13] & 0xff)
	                );
	            return true;
	        }
	        return false;
	    }
	    public final boolean collected(byte[] datagram, int pos) {
	        return true;
	    }
	}

	protected static final class RequestNS extends Request {
		protected static final class RecordNS implements Comparable {
			public String name;
			public int ttl;
			public RecordNS (String name, int ttl) {
				this.name = name;
				this.ttl = ttl;
			}
			public final int compareTo (Object object) {
		        RecordNS compared = (RecordNS) object;
		        if (ttl < compared.ttl) {
		            return -1;
		        } else if (ttl > compared.ttl) {
		            return 1;
		        } else {
		            return 0;
		        }
		    };
		}
		private ArrayList<RecordNS> _records = new ArrayList<RecordNS>();
	    protected RequestNS(String question) {
	        super(question);
	    }
	    private static final String[] _template = new String[]{
	        "\001\000\000\001\000\000\000\000\000\000",
	        "\000\000\002\000\001"
	        };
	    public String[] template() {
	        return _template;
	    }
	    public final boolean collect(byte[] datagram, int pos) {
	        if (datagram[pos+1] == '\002' && datagram[pos+3] == '\001') {
	            _records.add(new RecordNS(
	            	_unpackName(datagram, pos+10),
	            	_unpackTTL(datagram, pos+4)
	            	));
	        }
	        return false;
	    }
	    public final boolean collected(byte[] datagram, int pos) {
	    	if (_records.isEmpty()) {
	    		return false;
	    	}
	    	RecordNS[] records = new RecordNS[_records.size()];
	    	_records.toArray(records);
	    	Arrays.sort(records);
	        ttl = records[0].ttl;
	        for (int i=0; i<records.length; i++) {
	        	resources.add(records[i].name);
	        }
	        return true;
	    }
	}

	protected static final class RequestPTR extends Request {
	    protected RequestPTR(String question) {
	        super(question);
	    }
	    private static final String[] _template = new String[]{
	        "\001\000\000\001\000\000\000\000\000\000",
	        "\000\000\014\000\001"
	        };
	    public String[] template() {
	        return _template;
	    }
	    public final boolean collect(byte[] datagram, int pos) {
	        if (datagram[pos+1] == '\014' && datagram[pos+3] == '\001') {
	            ttl = _unpackTTL(datagram, pos+4);
	            resources.add(_unpackName(datagram, pos+10));
	            return true;
	        }
	        return false;
	    }
	    public final boolean collected(byte[] datagram, int pos) {
	        return true;
	    }
	}

	protected static final class RequestTXT extends Request {
	    protected RequestTXT(String question) {
	        super(question);
	    }
	    private static final String[] _template = new String[]{
	        "\001\000\000\001\000\000\000\000\000\000",
	        "\000\000\020\000\001"
	        };
	    public String[] template() {
	        return _template;
	    }
	    public final boolean collect(byte[] datagram, int pos) {
	        if (datagram[pos+1] == '\020' && datagram[pos+3] == '\001') {
	            ttl = _unpackTTL(datagram, pos+4);
	            resources = _unpackText(datagram, pos+10);
	            return true;
	        }
	        return false;
	    }
	    public final boolean collected(byte[] datagram, int pos) {
	        return true;
	    }
	}

	protected static final class RequestMX extends Request {
		protected static final class RecordMX implements Comparable {
			public int preference;
			public String name;
			public int ttl;
			public RecordMX (int preference, String name, int ttl) {
				this.preference = preference;
				this.name = name;
				this.ttl = ttl;
			}
			public final int compareTo (Object object) {
		        RecordMX compared = (RecordMX) object;
		        if (preference < compared.preference) {
		            return -1;
		        } else if (preference > compared.preference) {
		            return 1;
		        } else {
		            return 0;
		        }
		    };
		}
		private ArrayList<RecordMX> _records = new ArrayList<RecordMX>();
	    protected RequestMX(String question) {
	        super(question);
	    }
	    private static final String[] _template = new String[]{
	        "\001\000\000\001\000\000\000\000\000\000",
	        "\000\000\017\000\001"
	        };
	    public String[] template() {
	        return _template;
	    }
	    public final boolean collect(byte[] datagram, int pos) {
	        if (datagram[pos+1] == '\017' && datagram[pos+3] == '\001') {
	            _records.add(new RecordMX(
	            	_unpackPreference(datagram, pos+10),
	            	_unpackName(datagram, pos+12),
	            	_unpackTTL(datagram, pos+4)
	            	));
	        }
	        return false;
	    }
	    public final boolean collected(byte[] datagram, int pos) {
	    	if (_records.isEmpty()) {
	    		return false;
	    	}
	    	RecordMX[] records = new RecordMX[_records.size()];
	    	_records.toArray(records);
	    	Arrays.sort(records);
	        ttl = records[0].ttl;
	        for (int i=0; i<records.length; i++) {
	        	resources.add(records[i].name);
	        }
	        return true;
	    }
	}

    protected static class Client extends Dispatcher {

        protected final class DNSTimeouts extends Timeouts {
    		public DNSTimeouts (int period, int precision) {
    			super(period, precision);
    		}
    		@Override
    		public final void stop() {
    			close();
    		}
    		@Override
    		public final void timeout(Object reference) throws Throwable {
    			Request request = _pending.get(reference);
    			if (request == null) {
    				return;
    			}
    			request.failover++;
    			int L = request.servers.length;
    			if (request.failover <  L * _failover) {
    				request.peer = request.servers[request.failover % L];
    				dnsSend(request, _loop.now());
    			} else {
    				dnsContinue(request);
    			}
    		}

    	}
    	
        protected DNSTimeouts _timeouts;
        protected long _sent = 0;
        protected int _failover = 3;
        protected HashMap<String, Request> _cache = new HashMap();
        protected HashMap<Integer, Request> _pending = new HashMap();
        protected SocketAddress[] _servers;
        
        public Client(SocketAddress[] servers, int timeout, int precision) {
            super();
            _servers = servers;
            _timeouts = new DNSTimeouts(timeout, precision);
        }
        
        public final void resolve(String question, Fun resolved) throws Throwable {
            Request request = _cache.get(question);
            if (request != null) {
                if (request.defered == null) {
                    if (request.when + request.ttl * 1000 > _loop.now()) {
                        resolved.apply(request);
                        return;
                    }
                } else {
                	request.defered.add(resolved);
                    return;
                }
            }
            if (question == null) {
                throw new Error("parameter must be an array of two strings");
            } else if (question.startsWith("A ")) {
            	request = new DNS.RequestA(question);
            } else if (question.startsWith("PTR ")) {
            	request = new DNS.RequestPTR(question);
            } else if (question.startsWith("MX ")) {
            	request = new DNS.RequestMX(question);
            } else if (question.startsWith("NS ")) {
            	request = new DNS.RequestNS(question);
            } else if (question.startsWith("TXT ")) {
            	request = new DNS.RequestTXT(question);
            } else {
                throw new Error("unsupported DNS request type " + question);
            }
            request.servers = _servers;
            request.peer = _servers[0];
            request.defered = new ArrayList();
            request.defered.add(resolved);
            dnsSend(request, _loop.now());
        }
        
        public final boolean writable() {
            return false;
        }

        public final boolean readable() {
            return !_pending.isEmpty();
        }

        public final void handleAccept() throws Throwable {
            throw new Error("unexpected accept event");
        }

        public final void handleConnect() throws Throwable {
        }

        public final void handleWrite() throws Throwable {
            throw new Error("unexpected write event");
        }

        public final void handleRead() throws Throwable {
        	byte[] datagram = new byte[2048];
        	while (true) {
    	    	SocketAddress peer = recvfrom(ByteBuffer.wrap(datagram));
    	    	if (peer == null) {
    	    		return;
    	    	} 
    	    	Integer uid = ((datagram[0] & 0xff) * 256 + (datagram[1] & 0xff));
    	    	if (!_pending.containsKey(uid)) {
    	    		log("redundant DNS response from " + peer.toString());
    	    		return;
    	    	}
    			Request request = _pending.remove(uid);
    			if (!request.peer.equals(peer)) {
    	    		log("impersonated DNS response from " + peer.toString());
    				return;
    			}
    			request.unpack(datagram);
    			dnsContinue(request);
        	}
        }

        public final void handleClose() throws Throwable {
        }

        public final Object apply(Object input) throws Throwable {
        	_timeouts = null;
            return Boolean.TRUE;
        }

        protected final boolean dnsConnect() {
        	try {
            	String ip = InetAddress.getLocalHost().getHostAddress();
            	int port = 1024 + (new Random()).nextInt(64507);
        		bind(new InetSocketAddress(ip, port));
        	} catch (Throwable e) {
        		log(e);
            	return false;
        	}
        	return true;
        }
        
        private static final Random rand = new Random();
        protected final void dnsSend (Request request, long when) throws Throwable {
        	if (_channel == null && !dnsConnect()) {
    			dnsContinue(request);
    			return;
        	}
        	request.when = when;
        	Integer uid;
        	do {
        		uid = rand.nextInt(65535);
    		} while (_pending.containsKey(uid));
        	request.uid = uid;
        	_pending.put(uid, request);
        	sendto(ByteBuffer.wrap(request.datagram()), request.peer);
        	_sent++;
        	_timeouts.push(when, uid);
        }
        
        protected final void dnsContinue (Request request) {
        	if (request.defered == null) {
        		return;
        	}
        	if (request.response != null) {
        		request.response = null;
        		_cache.put(request.question, request);
        	}
        	Iterator<Fun> defered = request.defered.iterator();
        	request.defered = null;
        	while (defered.hasNext()) {
        		try {
        			defered.next().apply(request);
        		} catch (Throwable e) {
        			log(e);
        		}
        	}
        }
        
    }
    
    public static final Client client () {
    	try {
    		return new Client(servers(), 2000, 1000);
    	} catch (IOException e) {
    		return null;
    	}
    }
    
    public static final Client resolver = client();
    public static final void resolve (String question, Fun callback) 
    throws Throwable {
    	resolver.resolve(question, callback);
    }
    // closure in plain old java ...
    protected static final class _Connect extends Resolution {
    	private Dispatcher _dispatcher;
    	private int _port;
    	private Fun _error = null;
    	public _Connect(Dispatcher dispatcher, int port, Fun error) {
    		_dispatcher = dispatcher;
    		_port = port;
    		_error = error;
    	}
    	public final Object handle (Request resolved) throws Throwable {
    		if (resolved.resources != null && resolved.resources.size() > 0) {
    			_dispatcher.connect((String) resolved.resources.get(0), _port);
    		} else if (_error != null) {
    			_error.apply(_dispatcher);
    		}
    		return null;
    	}
    }
	public static final void 
    connect (Dispatcher dispatcher, String host, int port, Fun error) 
    throws Throwable {
    	resolver.resolve(
			"A " + host, new _Connect(dispatcher, port, error)
			);
    }
    
}

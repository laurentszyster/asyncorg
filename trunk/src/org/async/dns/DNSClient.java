/*  Copyright (C) 2007 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of version 2 of the GNU General Public License as
 *  published by the Free Software Foundation.
 *  
 *   http://www.gnu.org/copyleft/gpl.html
 *  
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA
 *  
 */

package org.async.dns;

import org.async.simple.Fun;
import org.async.core.Timeouts;
import org.async.core.Dispatcher;

import java.util.Arrays;
import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * An asynchronous DNS client, to resolve A, NS, MX, TXT and PTR records.
 * 
 * @h3 Synopsis
 * 
 * @p The purpose of this implementation is to break free from the synchronous
 * resolver API and develop fully non-blocking peers.
 */
public class DNSClient extends Dispatcher {

    protected static final class RequestA extends DNSRequest {
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
                ttl = DNS._unpackTTL(datagram, pos+4);
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
    
    protected static final class RequestNS extends DNSRequest {
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
                	DNS._unpackName(datagram, pos+10),
                	DNS._unpackTTL(datagram, pos+4)
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
    
    protected static final class RequestPTR extends DNSRequest {
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
                ttl = DNS._unpackTTL(datagram, pos+4);
                resources.add(DNS._unpackName(datagram, pos+10));
                return true;
            }
            return false;
        }
        public final boolean collected(byte[] datagram, int pos) {
            return true;
        }
    }
    
    protected static final class RequestTXT extends DNSRequest {
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
                ttl = DNS._unpackTTL(datagram, pos+4);
                resources = DNS._unpackText(datagram, pos+10);
                return true;
            }
            return false;
        }
        public final boolean collected(byte[] datagram, int pos) {
            return true;
        }
    }
    
    protected static final class RequestMX extends DNSRequest {
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
                	DNS._unpackPreference(datagram, pos+10),
                	DNS._unpackName(datagram, pos+12),
                	DNS._unpackTTL(datagram, pos+4)
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
			DNSRequest request = _pending.get(reference);
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
    protected HashMap<String, DNSRequest> _cache = new HashMap();
    protected HashMap<Integer, DNSRequest> _pending = new HashMap();
    protected SocketAddress[] _servers;
    
    public DNSClient(SocketAddress[] servers, int timeout, int precision) {
        super();
        _servers = servers;
        _timeouts = new DNSTimeouts(timeout, precision);
    }
    
    public final void resolve(String question, Fun resolved) throws Throwable {
        DNSRequest request = _cache.get(question);
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
        	request = new RequestA(question);
        } else if (question.startsWith("PTR ")) {
        	request = new RequestPTR(question);
        } else if (question.startsWith("MX ")) {
        	request = new RequestMX(question);
        } else if (question.startsWith("NS ")) {
        	request = new RequestNS(question);
        } else if (question.startsWith("TXT ")) {
        	request = new RequestTXT(question);
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
			DNSRequest request = _pending.remove(uid);
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
    
    protected final void dnsSend (DNSRequest request, long when) throws Throwable {
    	if (_channel == null && !dnsConnect()) {
			dnsContinue(request);
			return;
    	}
    	request.when = when;
    	Integer uid = request.uid = (int) (_sent % 65536);
    	_pending.put(uid, request);
    	sendto(ByteBuffer.wrap(request.datagram()), request.peer);
    	_sent++;
    	_timeouts.push(when, uid);
    }
    
    protected final void dnsContinue (DNSRequest request) {
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

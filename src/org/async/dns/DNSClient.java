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

import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class DNSClient extends Dispatcher {

    public static final class RequestA extends DNSRequest {
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
                datagram[pos] == '\000' && 
                datagram[pos+1] == '\001' && 
                datagram[pos+2] == '\000' && 
                datagram[pos+3] == '\001' 
                ) {
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
    protected int _failover = 1;
    protected HashMap<String[], DNSRequest> _cache = new HashMap();
    protected HashMap<Integer, DNSRequest> _pending = new HashMap();
    protected SocketAddress[] _servers;
    
    public DNSClient(SocketAddress[] servers, int timeout, int precision) {
        super();
        _servers = servers;
        _timeouts = new DNSTimeouts(timeout, precision);
    }
    
    public final SocketAddress[] servers() {
    	return _servers;
    }
    
    public void resolve(String[] question, Fun resolved) throws Throwable {
        DNSRequest request = _cache.get(question);
        if (request != null) {
            if (request.defered == null) {
                if (request.when + request.ttl > _loop.now()) {
                    resolved.apply(request);
                    return;
                }
            } else {
            	request.defered.add(resolved);
                return;
            }
        }
        if (question == null || question.length != 2) {
            throw new Error("parameter must be an array of two strings");
        } else if (question[1] == "A") {
        	request = new RequestA(question);
        } else {
            throw new Error("unsupported DNS request type " + question[0]);
        }
        request.servers = _servers;
        request.peer = _servers[0];
        request.defered = new ArrayList();
        request.defered.add(resolved);
        dnsSend(request, _loop.now());
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
    	byte[] datagram = new byte[512];
    	SocketAddress peer = recvfrom(ByteBuffer.wrap(datagram));
    	if (peer == null) {
    		return;
    	} 
    	Integer uid = ((datagram[0] & 0xff) * 256 + (datagram[1] & 0xff));
    	if (!_pending.containsKey(uid)) {
    		log("redundant " + peer.toString());
    		return;
    	}
		DNSRequest request = _pending.remove(uid);
		if (!request.peer.equals(peer)) {
    		log("impersonate " + peer.toString());
			return;
		}
		request.unpack(datagram);
		dnsContinue(request);
    }

    public void handleClose() throws Throwable {
    }

    public Object apply(Object input) throws Throwable {
    	_timeouts = null;
        return Boolean.TRUE;
    }

    public boolean dnsConnect() {
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
    
    public void dnsSend (DNSRequest request, long when) throws Throwable {
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
    	log("sent " + uid);
    }
    
    public void dnsContinue (DNSRequest request) {
    	if (request.response != null) {
    		request.response = null;
    		_cache.put(request._question, request);
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

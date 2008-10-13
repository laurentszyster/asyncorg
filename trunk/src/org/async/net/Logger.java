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

package org.async.net;

import org.async.core.Static;
import org.async.core.Server;
import org.async.core.Pipeline;
import org.simple.SIO;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * Simple concurrent network logs, although not obvious at first: 
 * one asynchronous loop, one log file by connection and up to 
 * 512 peers (this one included). 
 * 
 * @h3 Synopsis
 * 
 * @pre java -cp asyncorg.jar org.async.net.Logger \
 *    ./log 127.0.0.2 1234 3 1
 *    
 * @p Logging netstrings to disk through the network removes a busy point
 * of contention from its applications by moving it to the network.
 * 
 * @p This peer may block writing output to the disk and therefore block 
 * reading from its buffers, then peers will start queuing log entries in
 * their buffers. 
 * 
 * @p Practically, the a lot of contention weighting on logging peers 
 * CPU and RAM is shared by the clients queing log entries, the IP routers 
 * buffering packets and the servers' OS. As for the server program itself,
 * it never contends by itself because it is fully asynchronous.
 * 
 * @p Of course the JVM may not be the ideal plateform to host a network
 * logger, but on the other hand Java is a good language to develop
 * an extensible range of network application peers.
 */
public final class Logger extends Server {
    protected static final class Channel extends NetDispatcher {
        protected Logger _server;
        protected BufferedOutputStream _output;
        public Channel (Logger server) {
            super(server._loop, 16384, 0);
            _server = server;
        }
        public final Object apply (Object value) throws Throwable {
            return null;
        }
        public final void handleConnect() throws Throwable {
            log("connected");
            _output = new BufferedOutputStream((new FileOutputStream(
                _server.path 
                + toString().replace(':', '_') 
                + "_" + _loop.now() + ".net"
                )), SIO.fioBufferSize);
        }
        public final boolean handleLength(int length) throws Throwable {
            _output.write(Integer.toString(length).getBytes());
            _output.write(':');
            return true;
        }
        public final void handleData (byte[] data) throws Throwable {
            _output.write(data);
        }
        public final boolean handleTerminator() throws Throwable {
            _output.write(',');
            return false;
        }
        private static final byte[] _ZERO = new byte[]{'0', ':', ','};
        public final void handleClose() throws Throwable {
            _output.write(_ZERO);
        }
        public final void close () {
            log("closed");
            super.close();
            _server.serverClose(this);
            if (_output != null) try {
                _output.close();
            } catch (IOException e) {
                log(e);
            }
        }
    }
    String path = ".";
    public final Pipeline serverAccept() {
        return new Channel(this);
    }
    public static final void main (String args[]) throws Throwable {
        Static.loop.hookShutdown();
        Logger server = new Logger();
        if (args.length > 0) {
            server.path = args[0];
        }
        server.listen(new InetSocketAddress(
            (args.length > 1) ? args[1]: "127.0.0.2", 
            (args.length > 2) ? Integer.parseInt(args[2]): 12345
            ));
        if (args.length > 3) {
            server.timeout = Integer.parseInt(args[4])*1000; 
        }
        if (args.length > 4) {
            server.precision = Integer.parseInt(args[4])*1000; 
        }
        Static.loop.exits.add(server);
        Static.loop.dispatch();
        System.exit(0);
    }
}

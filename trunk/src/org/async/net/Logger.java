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
import org.async.core.Dispatcher;

import java.net.InetSocketAddress;

/**
 * Simple concurrent network logs, although not obvious at first: 
 * one asynchronous loop, one log <code>OutputStream</code> and up to 
 * 512 peers (this one included). 
 * 
 * @h3 Synopsis
 * 
 * @p Start a <code>Netlogger</code> for each distinct categories of logs
 * required by your applications.
 * 
 * @p For instance, using filesystem categories and a separate tree for
 * netlogger's own error logs:
 * 
 * @pre java -cp asyncorg.jar org.async.net.Logger 127.0.0.2 1234 \
 *    &1> ./netlogs/tmp/category \ 
 *    &2> ./127.0.0.2/1234/netlogger.tmp
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
        
        public Channel () {
            super(16384, 0); 
        }
        
        public final Object apply (Object value) throws Throwable {
            return null;
        }
        
        public final void handleConnect() {
            log("connected");
        }
        
        public final boolean handleLength(int length) throws Throwable {
            System.out.write(Integer.toString(length).getBytes());
            System.out.write(':');
            return true;
        }

        public final void handleData (byte[] data) throws Throwable {
            System.out.write(data);
        }

        public final boolean handleTerminator() throws Throwable {
            System.out.write(',');
            return true;
        }
        
        public final void handleClose() {
            log("closed");
        }

    }
    
    public final Dispatcher serverAccept() throws Throwable {
        return new Channel();
    }
    public final void serverWakeUp () {
        log("wake up");
    }
    public final void serverSleep () {
        log("sleep");
    }
    public static final void main (String args[]) throws Throwable {
        Static.loop.hookShutdown();
        Logger server = new Logger();
        server.listen(new InetSocketAddress(
            (args.length > 0) ? args[0]: "127.0.0.2", 
            (args.length > 1) ? Integer.parseInt(args[1]): 12345
            ));
        Static.loop.exits.add(server);
        Static.loop.dispatch();
        System.exit(0);
    }
    
}

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

package org.async;

import org.async.core.Static;
import org.async.core.Listen;
import org.async.core.Netstring;

import org.async.net.Collector;
import org.async.net.Dispatcher;

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
 * @pre java -cp org.async.jar Netlogger 127.0.0.2 1234 \
 *    &1> ./netlogs/tmp/category \ 
 *    &2> ./127.0.0.2/1234/netlogger.tmp
 *    
 * @h3 Protocol
 *    
 * @p Integrity is guaranteed for any netlog entry fitting its netlogger 
 * input buffers (ie: between 8192 and 65356 8-bit bytes). By integrity
 * I understand that from a log consumer point of view either an entry 
 * was completely written to the log file or not, there can be no doubt
 * about it: the byte length precedes the payload. 
 * 
 * @p Note also that any entry with an abusive length for a log entry is 
 * trunked to its tail, preserving disk resources for compliant peers with 
 * minimal disruption over an established connection.
 * 
 * @p A Riddle of Asynchrony
 * 
 * @p Logging netstrings to disk through the network removes a busy point
 * of contention from its applications by moving it to the network.
 * 
 * @p This peer may block writing output to the disk and therefore block 
 * reading from its buffers. But the OS will continue to fill those buffers. 
 * And when they are full peers will start queuing log entries. 
 * 
 * @p Practically, the a lot of contention weighting on logging peers 
 * CPU and RAM is shared by the clients queing log entries, the IP routers 
 * buffering packets and the servers' OS. As for the server program itself,
 * it never contends by itself because it is fully asynchronous.
 * 
 * @p Of course the JVM may not be the ideal plateform to host a network
 * logger, but on the other hand Java is the language of choice to develop
 * an extensible range of network application peers.
 * 
*/
public final class Netlogger {

    protected static final class Server extends Listen {
        public Object apply (Object value) throws Throwable {
            log("DEBUG", "Shutdown");
            close();
            return Boolean.FALSE;
        }
        public void handleAccept() throws Throwable {
            accept(new Channel());
        }
        public void handleClose() throws Throwable {
            log("ERROR", "Unexpected close event");
        }
    }
        
    protected static final class Channel extends Dispatcher {
        public Channel () {
            super(16384, 0);
        }
        public static final class Trunk implements Collector {
            public byte[] tail = null;
            public final boolean collect (byte[] data) throws Throwable {
                return false;
            }
            public final boolean terminate (byte[] data) throws Throwable {
                tail = data;
                return false;
            }
        }
        public Trunk collector = new Trunk();
        public final Object apply (Object value) throws Throwable {
            return null;
        }
        public final Collector handleCollect(int length) throws Throwable {
            return collector;
        }
        public final void handleCollected() throws Throwable {
            Netstring.write(System.out, collector.tail);
            System.out.flush();
        }
        public final void handleClose() {
            collector = null; 
        }
    }
    public static final void main (String args[]) throws Throwable {
        Server server = new Server();
        server.listen(new InetSocketAddress(
            (args.length > 0) ? args[0]: "127.0.0.2", 
            (args.length > 1) ? Integer.parseInt(args[1]): 12345
            ));
        Static.loop.exits.add(server);
        Static.loop.dispatch();
        System.exit(0);
    }
}

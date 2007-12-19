package org.async;

import org.async.net.Collector;
import org.async.net.Dispatcher;

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
public class Netlogger extends Dispatcher {
    public Object apply (Object value) throws Throwable {
        return null;
    }
    public void handleConnect() {
    }
    public Collector handleCollect(int length) {
        return null;
    }
    public boolean handleCollected(Collector collected) {
        return false;
    }
    public void handleClose() {
    }
    public static final void main (String args[]) {
        Static.loop.dispatch();
    }
}

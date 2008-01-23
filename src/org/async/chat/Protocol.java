package org.async.chat;

/**
 * An abstract base class for collectors that implement a protocol, ie:
 * that change their collecting channel's terminator.
 * 
 * @pre if (collector instanceof Protocol) {
 *    collector = new Collect(collector) // 
 *}
 * 
 */
public abstract class Protocol implements Collector {
    public Channel channel; 
}

/**
 * 
 */
package org.async.net;

public interface Collector {
    public boolean collect (byte[] data) throws Throwable;
    public boolean terminate (byte[] data) throws Throwable;
}
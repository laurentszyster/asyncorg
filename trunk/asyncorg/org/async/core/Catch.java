package org.async.core;

/**
 * An interface to the <code>AsyncLoop</code>'s <code>exit</code> 
 * mechanism.
 */
public interface Catch {
    /**
     * Decide wether to exit or continue the loop.
     * 
     * @p Implement this method: 
     * 
     * @pre class Shutdown implements Catch {
     *    public final boolean apply (Loop loop) {
     *        return false;
     *    }
     *}
     *
     * @p Then assign an instance of its class to the loop's 
     * <code>exit</code>:
     *
     * @pre loop.exit = new Shutdown();
     * 
     * @param loop to exit or continue
     * @return <code>true</code> to continue the loop.  
     */
    public boolean apply (Loop loop);
}
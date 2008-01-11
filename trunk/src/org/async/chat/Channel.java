package org.async.chat;

public interface Channel {
    /**
     * Get the current terminator.
     * 
     * @return <code>null</code>, an <code>Integer</code> or 
     * a <code>byte[]</code> string.
     */
    public Object getTerminator();
    /**
     * Set the current terminator to <code>null</code>, collect all data
     * until the channel closes.
     */
    public void setTerminator();
    /**
     * Set a numeric terminator, collect the given number of bytes. 
     * 
     * @param terminator
     */
    public void setTerminator(int terminator);
    /**
     * Set a byte string terminator, collect data until that string of bytes
     * is found in the incoming stream.
     * 
     * @param terminator
     */
    public void setTerminator(byte[] terminator);
}

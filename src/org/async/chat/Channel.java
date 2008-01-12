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

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
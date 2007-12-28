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
 * A logger interface for unobtrusive and flexible logging.
 * 
 * @h3 Synopsis
 * 
 * @p By default all loops use the default logger implemented above,
 * logging CRLF delimited lines, uncategorized messages to STDOUT,
 * categorized messages and tracebacks to STDERR.
 * 
 * @pre private static final class _STDOE implements Loginfo {
 *    public final void out (String message) {
 *        System.out.println(message);
 *    }
 *    public final void err (String category, String message) {
 *        System.err.println(category + " " + message);
 *    }
 *    public final void traceback (Throwable error) {
 *        error.printStackTrace(System.err);
 *    }
 *}
 * 
 * @p Instances of <code>Loginfo</code> implementations are attached to a 
 * <code>Loop</code> using its <code>setLogger</code> method. 
 * 
 * @pre Static.loop.setLogger(new _STDOE());
 * 
 * @p ...
 */
public interface Loginfo {
    /**
     * Logs out an uncategorized message string.
     * 
     * @param message to log out
     */
    public void out (String message);
    /**
     * Logs a message and category strings.
     * 
     * @param category of the message
     * @param message to log
     */
    public void err (String category, String message);
    /**
     * Logs an exception traceback.
     * 
     * @param error throwed
     */
    public void traceback (Throwable error);
}
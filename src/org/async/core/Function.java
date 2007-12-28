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
 * The simplest possible first class function in Java.
 * 
 * @h3 Synopsis
 * 
 * @p ...
 * 
 * @pre Function print = new Function () {
 *    public Object apply (Object input) throws Throwable {
 *        System.out(argument.toString());
 *    }
 *};
 *print.apply("hello functional world!");
 *
 * @p ...
 * 
 */
public interface Function {
    /**
     * Apply the function.
     * 
     * @param input of the function
     * @return an <code>Object</code> or <code>null</code>
     * @throws Throwable
     */
    public Object apply (Object input) throws Throwable;
}
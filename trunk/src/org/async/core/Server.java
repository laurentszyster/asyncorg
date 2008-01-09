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

public abstract class Server extends Dispatcher {
    protected boolean _accepting = true;
    public Server () {
        super();
    }
    public Server (Loop loop) {
        super(loop);
    }
    public boolean writable () {
        return false;
    } 
    public boolean readable () {
        return _accepting;
    }
    public void handleConnect() throws Throwable {
        throw new Error("Unexpected connect event");
    }
    public void handleRead() throws Throwable {
        throw new Error("Unexpected read event");
    }
    public void handleWrite() throws Throwable {
        throw new Error("Unexpected write event");
    }
}

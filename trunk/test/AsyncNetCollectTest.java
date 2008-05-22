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

import org.async.net.NetDispatcher;

public final class AsyncNetCollectTest extends NetDispatcher {
    private StringBuilder _sb;
    public final void handleConnect() {
    }
    public final boolean handleLength(int length) throws Throwable {
        _sb = new StringBuilder();
        return true;
    }
    public final void handleData (byte[] data) {
        _sb.append(new String(data));
    }
    public final boolean handleTerminator() throws Throwable {
        _loop.log(_sb.toString());
        return false;
    }
    public final void handleClose() throws Throwable {
    }
    public final Object apply(Object input) throws Throwable {
        _bufferIn.put(((String) input).getBytes());
        _bufferIn.flip();
        collect();
        _bufferIn.compact();
        return null;
    }
    public static final void main (String[] args) throws Throwable {
        AsyncNetCollectTest net = new AsyncNetCollectTest();
        net.apply("3:one,3:two,5:th");
        net.apply("ree,");
    } 
}

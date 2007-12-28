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

package org.async.tests;

import org.async.chat.Dispatcher;
import org.async.chat.StringsProducer;
import org.async.core.Function;
import org.async.core.Static;

import java.net.InetSocketAddress;

public class AsyncChatTest extends Dispatcher {
    public Object apply (Object value) throws Throwable {
        return null;
    }
    public void handleConnect() throws Throwable {
        log("connected");
    }
    public void handleData (byte[] data) throws Throwable {
        _loop.log(new String(data, "UTF-8"));
    }
    public boolean handleTerminator () throws Throwable {
        log("terminator");
        setTerminator();
        return false;
    }
    public void handleClose() throws Throwable {
        log("closed");
    }
    public static final void main (String[] args) throws Throwable {
        try {
            AsyncChatTest chat = new AsyncChatTest();
            chat.connect(new InetSocketAddress("127.0.0.1", 8080));
            chat.push(StringsProducer.wrap(
                new String[]{"GET / HTTP/1.0\r\n\r\n"}, "UTF-8"
                ));
            chat.setTerminator("\r\n\r\n".getBytes());
            chat.continuation = new Function () {
                public final Object apply (Object input) throws Throwable {
                    Static.loop.log("finalized", (input==null) ? 
                        "null": input.toString()
                        );
                    return null;
                }
            };
            chat = null;
            Static.loop.dispatch();
        } catch (Throwable e) {
            Static.loop.log(e);
        }
    }
}

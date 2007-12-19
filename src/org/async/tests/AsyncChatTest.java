package org.async.tests;

import org.async.chat.Dispatcher;
import org.async.chat.ScheduledProducer;
import org.async.chat.StringsProducer;
import org.async.chat.producers.*;
import org.async.core.Static;

import java.net.SocketAddress;
import java.net.InetSocketAddress;

public class AsyncChatTest extends Dispatcher {
    public Object apply (Object value) throws Throwable {
        /*
        push(StringsProducer.iter(
            new String[]{"GET / HTTP/1.0\r\n\r\n"}, "UTF-8"
            ));
        */
        setTerminator("\r\n\r\n".getBytes());
        connect((SocketAddress) value);
        return null;
    }
    public void handleConnect() throws Throwable {
        log("DEBUG: ", "connected");
        ScheduledProducer defered = new ScheduledProducer(
            StringsProducer.wrap(
                new String[]{"GET / HTTP/1.0\r\n\r\n"}, "UTF-8"
                )
            );
        _loop.schedule(_loop.now() + 3000, defered);
        push(defered);
    }
    public void handleData (byte[] data) throws Throwable {
        log(new String(data, "UTF-8"));
    }
    public boolean handleTerminator () throws Throwable {
        log("DEBUG: ", "terminator");
        setTerminator();
        return false;
    }
    public void handleClose() throws Throwable {
        log("DEBUG: ", "closed");
    }
    public static final void main (String[] args) throws Throwable {
        try {
            (new AsyncChatTest()).apply(
                new InetSocketAddress("127.0.0.1", 8080)
                );
            Static.loop.dispatch();
        } catch (Throwable e) {
            Static.loop.log.traceback(e);
        }
    }
}
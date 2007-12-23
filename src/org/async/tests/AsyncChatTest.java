package org.async.tests;

import org.async.chat.Dispatcher;
import org.async.chat.StringsProducer;
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
        log("connected");
        ScheduledProducer defered = new ScheduledProducer(
            StringsProducer.wrap(
                new String[]{"GET / HTTP/1.0\r\n\r\n"}, "UTF-8"
                )
            );
        _loop.schedule(_loop.now() + 3000, defered);
        push(defered);
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
            (new AsyncChatTest()).apply(
                new InetSocketAddress("127.0.0.1", 8080)
                );
            Static.loop.dispatch();
        } catch (Throwable e) {
            Static.loop.log(e);
        }
    }
}

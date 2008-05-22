import org.async.core.Static;
import org.async.core.Dispatcher;

import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

public class DispatcherTest extends Dispatcher {

    ByteBuffer _bufferOut = ByteBuffer.wrap(new byte[8192]);
    ByteBuffer _bufferIn = ByteBuffer.wrap(new byte[8192]);
    
    public boolean writable() {
        return _bufferOut.remaining() > 0;
    }

    public boolean readable() {
        return true;
    }

    public void handleAccept() throws Throwable {
        throw new Error("Unexpected accept event");
    }

    public void handleConnect() throws Throwable {
        log("connected");
    }

    public void handleWrite() throws Throwable {
        send(_bufferOut);
    }

    public void handleRead() throws Throwable {
        if (recv(_bufferIn) > 0) {
            System.out.println(new String(
                _bufferIn.array(), 0, _bufferIn.position()
                ));
            _bufferIn.clear();
        }
    }

    public void handleClose() throws Throwable {
        log("closed");
    }

    public Object apply(Object input) throws Throwable {
        String[] args = (String[]) input;
        connect(new InetSocketAddress(args[0], Integer.parseInt(args[1])));
        _bufferOut.put(("GET " + args[2] + " HTTP/1.0\r\n\r\n").getBytes());
        _bufferOut.flip();
        return null;
    }

    public static void main (String[] args) throws Throwable {
        (new DispatcherTest()).apply(args);
        Static.loop.dispatch();
    }
    
}

package org.async.chat.producers;

import org.async.Static;
import org.async.chat.Producer;

import java.util.Iterator;

/**
 * The obvious producer of 8-bit encoded UNICODE strings iterator, with 
 * a convenience for wrapping <code>String</code> arrays.
 * 
 */
public class StringsProducer implements Producer {
    protected Iterator _strings;
    protected String _encoding;
    public StringsProducer(Iterator strings, String encoding) {
        _strings = strings;
        _encoding = encoding;
    }
    public boolean stalled() {
        return false;
    }
    public byte[] more() throws Throwable {
        if (_strings.hasNext()) {
            return ((String) _strings.next()).getBytes(_encoding);
        } else {
            return null;
        }
    }
    /**
     * ...
     * 
     * @param strings
     * @param encoding
     * @return
     * 
     * @pre dispatcher.push (StringsProducer.iter(new String[]{
     *    "GET / HTTP/1.0\r\n",
     *    "Host: 127.0.0.1:8080\r\n",
     *    "\r\n"
     *    }));
     * 
     */
    public static final 
    StringsProducer wrap (String[] strings, String encoding) {
        return new StringsProducer(Static.iter(strings), encoding);
    }
}

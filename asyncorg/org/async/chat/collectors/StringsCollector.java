package org.async.chat.collectors;

import org.async.chat.Collector;

public class StringsCollector implements Collector {
    protected StringBuffer _sb = new StringBuffer();
    protected String _encoding = "UTF-8";
    public StringsCollector (String encoding) {
        _encoding = encoding;
    }
    public void handleData(byte[] data) throws Throwable {
        _sb.append(new String(data, _encoding));
    }
    public boolean handleTerminator() throws Throwable {
        return true;
    }
    public String toString() {
        return _sb.toString();
    }
}

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

package org.async.web;

import org.async.chat.Chat;
import org.async.chat.Channel;
import org.async.chat.Collector;
import org.async.chat.Protocol;
import org.protocols.HTTP;
import org.simple.Bytes;
import org.simple.Strings;

import java.util.HashMap;

public class ChunkCollector extends Protocol {

    private static final int _COLLECT_SIZE = 0;
    private static final int _COLLECT_CHUNK = 1;
    private static final int _COLLECT_FOOTERS = 2;
    
    protected Collector _collector;
    protected HashMap _headers = null;
    protected int _state = 0;
    protected StringBuilder _buffer = new StringBuilder();

    public Channel channel;

    public ChunkCollector(Channel channel, Collector collector) {
        if (collector instanceof Protocol) {
            _collector = new Chat((Protocol) collector);
        } else {
            _collector = collector;
        }
        this.channel = channel;
        _state = _COLLECT_SIZE;
        channel.setTerminator(Bytes.CRLF);
    }
    
    public ChunkCollector(
        Channel channel, Collector collector, HashMap headers
        ) {
        _collector = collector;
        this.channel = channel;
        _headers = headers;
        _state = _COLLECT_SIZE;
        channel.setTerminator(Bytes.CRLF);
    }
    
    public void handleData(byte[] data) throws Throwable {
        switch(_state) {
        case _COLLECT_CHUNK:
            _collector.handleData(data);
            return;
        case _COLLECT_SIZE: 
        case _COLLECT_FOOTERS:
            _buffer.append(data);
            return;
        }
        throw new Error("invalid ChunkCollector state");
    }

    public boolean handleTerminator() throws Throwable {
        switch(_state) {
        case _COLLECT_SIZE:
            int size = Integer.parseInt(
                Strings.split(_buffer.toString(), ';').next(), 16
                );
            _buffer = null;
            if (size == 0) {
                _state = _COLLECT_FOOTERS;
                channel.setTerminator(Bytes.CRLFCRLF);
            } else {
                _state = _COLLECT_CHUNK;
                channel.setTerminator(size + 2);
            }
            return false;
        case _COLLECT_CHUNK:
            _state = _COLLECT_SIZE;
            _buffer = new StringBuilder();
            channel.setTerminator(Bytes.CRLF);
            return false;
        case _COLLECT_FOOTERS:
            if (_headers != null) {
                HTTP.update(_headers, _buffer.toString(), 0);
            }
            _collector = null;
            _buffer = null;
            _state = -1;
            return _collector.handleTerminator(); // end of the collector!
        }
        throw new Error("invalid ChunkCollector state");
    }

}

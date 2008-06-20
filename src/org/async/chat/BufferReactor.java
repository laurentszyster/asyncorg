package org.async.chat;

import java.util.LinkedList;

public class BufferReactor implements Producer, Collector {
	private LinkedList<byte[]> _buffer = new LinkedList<byte[]>();
	public final byte[] more() throws Throwable {
		return _buffer.removeLast();
	}
	public final boolean stalled() {
		return _buffer.isEmpty();
	}
	public final void handleData(byte[] data) throws Throwable {
		_buffer.add(data);
	}
	public final boolean handleTerminator() throws Throwable {
		_buffer.add(null);
		return true;
	}
}

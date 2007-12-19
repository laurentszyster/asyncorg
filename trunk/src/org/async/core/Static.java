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


import java.io.UnsupportedEncodingException;
import java.util.Iterator;

public final class Static {
    /**
     * Convenience for the common application case where one loop is enough.
     */
    public static Loop loop = new Loop();
    
    // TODO: move to Utils ?
    
    public static final String UTF8 = "UTF-8";
    public static final byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }
    public static final String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }
    protected static class ObjectIterator implements Iterator {
        private Object[] _objects;
        private int _index = -1;
        public ObjectIterator (Object[] objects) {
            _objects = objects;
        }
        public boolean hasNext () {
            return _index + 1 < _objects.length;
        }
        public Object next () {
            _index++; return _objects[_index];
        }
        public void remove () {
            /* optional interface? what else now ...*/
        }
    }
    public static final Iterator iter (Object[] items) {
        return new ObjectIterator(items);
    }
}

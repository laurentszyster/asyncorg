/*  Copyright (C) 2006-2008 Laurent A.V. Szyster
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

package org.async.protocols;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A protocol to encode and iterate strings without escaping characters, 
 * the fast and easy way to serialize and parse sequences of UNICODE strings 
 * (see netstrings, the 8-bit byte original).
 */
public class Netunicode {
    
    /**
     * Encode an array of <code>String</code> as netunicodes into a
     * <code>StringBuffer</code>.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>System.out.println(
     *    Netunicode.encode(
     *        new String[]{"A", "BC", "DEF"}, new StringBuffer();
     *        ).toString();
     *    );</pre>
     * 
     * <p>Prints the following to STDOUT:</p>
     * 
     * <pre>1:A,2:BC,3:DEF,</pre>
     * 
     * @param strings to encode
     * @param sb the <code>StringBuffer</code> to fill
     */
    public static final StringBuffer encode (String[] strings, StringBuffer sb) {
        String string;
        for (int i = 0; i < strings.length; i++) {
            string = strings[i];
            sb.append(string.length());
            sb.append(':');
            sb.append(string);
            sb.append(',');
        }
        return sb;
    }

    /**
     * Encode an array of <code>String</code> as netunicodes.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>System.out.println(
     *    Netunicode.encode(new String[]{"A", "BC", "DEF"};
     *    );</pre>
     * 
     * @param strings to encode
     * @return a <code>String</code> of netunicodes 
     */
    public static final String encode (String[] strings) {
        return encode(strings, new StringBuffer()).toString();
    }
    
    /**
     * 
     * @param iter
     * @param sb
     */
    public static final void encode (Iterator iter, StringBuffer sb) {
        Object item;
        String s;
        while(iter.hasNext()) {
            item = iter.next();
            if (item instanceof String)
                s = (String) item;
            else if (item instanceof String[])
                s = encode((String[])item);
            else if (item instanceof Iterator)
                s = encode((Iterator)item);
            else
                s = item.toString();
            sb.append(s.length());
            sb.append(':');
            sb.append((String)item);
            sb.append(',');
        }
    }

    /**
     * 
     * @param iter
     * @return
     */
    public static final String encode (Iterator iter) {
        StringBuffer sb = new StringBuffer();
        encode(iter, sb);
        return sb.toString();
    }

    protected static final class Netiterator implements Iterator {
        
        private String buffer;
        private String item;
        private int size;
        private int prev = 0;
        private int pos, length, next;
        private boolean nostrip;
        
        public Netiterator(String encoded, boolean strip) {
            buffer = encoded;
            size = buffer.length();
            nostrip = !strip;
            item = _next();
        }
        
        public final boolean hasNext() {
        	return item != null;
    	}
        
        private final String _next() {
            String result = null;
            while (prev < size) {
                pos = buffer.indexOf (':', prev);
                if (pos < 1) {
                	prev = size;
                } else {
                    try {
                        length = Integer.parseInt(buffer.substring (prev, pos));
                    } catch (NumberFormatException e) {
                        prev = size;
                    }
                    next = pos + length + 1;
                    if (next >= size) {
                    	prev = size;
                    } else if (buffer.charAt(next) == ',') {
                        if (nostrip || next-pos > 1) {
                        	result = buffer.substring (pos+1, next);
                        }
                        prev = next + 1;
                    	break;
                    } else {
                    	prev = size;
                    }
                }
            }
            return result;
        }
        
        public final Object next() throws NoSuchElementException {
            if (item == null) {
                throw new NoSuchElementException();
            } else {
            	Object result = (Object) item;
            	item = _next();
            	return result;
            }
        }
        
        public final void remove() {} // optional interfaces? what else now ...
        
    }
    
    /**
     * ...
     * 
     * @param encoded
     * @return
     */
    public static final Iterator iter(String encoded) {
        return new Netiterator(encoded, true);
    }
    
}
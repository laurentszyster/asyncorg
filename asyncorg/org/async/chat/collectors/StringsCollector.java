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

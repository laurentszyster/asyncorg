/*  Copyright (C) 2007-2008 Laurent A.V. Szyster
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

import java.util.Calendar;
import java.util.HashMap;

import org.async.chat.Producer;
import org.async.simple.Objects;

public final class HTTP {

    public static final HashMap<String, String> RESPONSES = Objects.dict(
        "100", "Continue",
        "101", "Switching Protocols",
        "200", "OK", 
        "201", "Created",
        "202", "Accepted",
        "203", "Non-Authoritative Information",
        "204", "No Content",
        "205", "Reset Content",
        "206", "Partial Content",
        "300", "Multiple Choices",
        "301", "Moved Permanently",
        "302", "Moved Temporarily",
        "303", "See Other",
        "304", "Not Modified",
        "305", "Use Proxy",
        "400", "Bad Request",
        "401", "Unauthorized",
        "402", "Payment Required",
        "403", "Forbidden",
        "404", "Not Found",
        "405", "Method Not Allowed",
        "406", "Not Acceptable",
        "407", "Proxy Authentication Required",
        "408", "Request Time-out",
        "409", "Conflict",
        "410", "Gone",
        "411", "Length Required",
        "412", "Precondition Failed",
        "413", "Request Entity Too Large",
        "414", "Request-URI Too Large",
        "415", "Unsupported Media Type",
        "500", "Internal Server Error", 
        "501", "Not Implemented",
        "502", "Bad Gateway",
        "503", "Service Unavailable",
        "504", "Gateway Time-out",
        "505", "HTTP Version not supported"
        );
    private static final String[] _dow = new String[]{
        null, "Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun"
    };
    private static final String[] _moy = new String[]{
        null, "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    };

    public static final String date(Calendar calendar) {
        return String.format(
            "%s, %d %s %d %d:%d:%d GMT",
            _dow[calendar.get(Calendar.DAY_OF_WEEK)],
            calendar.get(Calendar.DAY_OF_MONTH),
            _moy[calendar.get(Calendar.MONTH)],
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.HOUR),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
            );
    }
    public static final String date(long milliseconds) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(milliseconds);
        return date(c);
    }

    public static class Entity {
        public HashMap<String, String> headers = new HashMap();
        public Producer body;
    }
}

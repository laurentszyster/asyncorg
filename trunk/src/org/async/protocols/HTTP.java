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

import org.async.chat.Producer;
import org.async.produce.BytesProducer;
import org.async.simple.Objects;
import org.async.simple.SIO;

import java.util.Calendar;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

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
        null, "Sun", "Mon", "Tue", "Wen", "Thu", "Fri", "Sat"
    };
    private static final String[] _moy = new String[]{
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
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

    public static abstract class Entity {
        public HashMap<String, String> headers = new HashMap();
        public abstract Producer body() throws Throwable;
    }
    public static final class FileEntity extends Entity {
        public String absolutePath;
        public FileEntity(File file) {
            SHA1 sha1 = new SHA1();
            StringBuffer sb = new StringBuffer();
            absolutePath = file.getAbsolutePath();
            sb.append(absolutePath);
            String length = Long.toString(file.length());
            sb.append(length);
            long lastModified = file.lastModified();
            sb.append(lastModified);
            sha1.update(sb.toString().getBytes());
            String mimeType = MIME.TYPES.get("");
            if (mimeType != null) {
                headers.put("Content-type", mimeType);
            }
            headers.put("Content-Length", length);
            headers.put("Last-Modified", HTTP.date(lastModified));
            headers.put("Etag", sha1.hexdigest());
        } 
        public Producer body () throws IOException {
            return new BytesProducer(SIO.read(
                new FileInputStream(absolutePath), SIO.fioBufferSize
                ));
        }
    }
}

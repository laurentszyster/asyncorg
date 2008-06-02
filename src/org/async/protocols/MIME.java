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

import org.async.simple.Bytes;
import org.async.simple.Strings;
import org.async.simple.Objects;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public class MIME {
    /**
     * MIME types copied from Python 2.5 mimetypes module.
     */
    public static final HashMap<String, String> 
        TYPES = Objects.dict(
        ".a"      , "application/octet-stream",
        ".ai"     , "application/postscript",
        ".aif"    , "audio/x-aiff",
        ".aifc"   , "audio/x-aiff",
        ".aiff"   , "audio/x-aiff",
        ".au"     , "audio/basic",
        ".avi"    , "video/x-msvideo",
        ".bat"    , "text/plain",
        ".bcpio"  , "application/x-bcpio",
        ".bin"    , "application/octet-stream",
        ".bmp"    , "image/x-ms-bmp",
        ".c"      , "text/plain",
        // Duplicates :(
        ".cdf"    , "application/x-cdf",
        ".cdf"    , "application/x-netcdf",
        ".cpio"   , "application/x-cpio",
        ".csh"    , "application/x-csh",
        ".css"    , "text/css",
        ".dll"    , "application/octet-stream",
        ".doc"    , "application/msword",
        ".dot"    , "application/msword",
        ".dvi"    , "application/x-dvi",
        ".eml"    , "message/rfc822",
        ".eps"    , "application/postscript",
        ".etx"    , "text/x-setext",
        ".exe"    , "application/octet-stream",
        ".gif"    , "image/gif",
        ".gtar"   , "application/x-gtar",
        ".h"      , "text/plain",
        ".hdf"    , "application/x-hdf",
        ".htm"    , "text/html",
        ".html"   , "text/html",
        ".ief"    , "image/ief",
        ".jpe"    , "image/jpeg",
        ".jpeg"   , "image/jpeg",
        ".jpg"    , "image/jpeg",
        ".js"     , "application/x-javascript",
        ".ksh"    , "text/plain",
        ".latex"  , "application/x-latex",
        ".m1v"    , "video/mpeg",
        ".man"    , "application/x-troff-man",
        ".me"     , "application/x-troff-me",
        ".mht"    , "message/rfc822",
        ".mhtml"  , "message/rfc822",
        ".mif"    , "application/x-mif",
        ".mov"    , "video/quicktime",
        ".movie"  , "video/x-sgi-movie",
        ".mp2"    , "audio/mpeg",
        ".mp3"    , "audio/mpeg",
        ".mpa"    , "video/mpeg",
        ".mpe"    , "video/mpeg",
        ".mpeg"   , "video/mpeg",
        ".mpg"    , "video/mpeg",
        ".ms"     , "application/x-troff-ms",
        ".nc"     , "application/x-netcdf",
        ".nws"    , "message/rfc822",
        ".o"      , "application/octet-stream",
        ".obj"    , "application/octet-stream",
        ".oda"    , "application/oda",
        ".p12"    , "application/x-pkcs12",
        ".p7c"    , "application/pkcs7-mime",
        ".pbm"    , "image/x-portable-bitmap",
        ".pdf"    , "application/pdf",
        ".pfx"    , "application/x-pkcs12",
        ".pgm"    , "image/x-portable-graymap",
        ".pl"     , "text/plain",
        ".png"    , "image/png",
        ".pnm"    , "image/x-portable-anymap",
        ".pot"    , "application/vnd.ms-powerpoint",
        ".ppa"    , "application/vnd.ms-powerpoint",
        ".ppm"    , "image/x-portable-pixmap",
        ".pps"    , "application/vnd.ms-powerpoint",
        ".ppt"    , "application/vnd.ms-powerpoint",
        ".ps"     , "application/postscript",
        ".pwz"    , "application/vnd.ms-powerpoint",
        ".py"     , "text/x-python",
        ".pyc"    , "application/x-python-code",
        ".pyo"    , "application/x-python-code",
        ".qt"     , "video/quicktime",
        ".ra"     , "audio/x-pn-realaudio",
        ".ram"    , "application/x-pn-realaudio",
        ".ras"    , "image/x-cmu-raster",
        ".rdf"    , "application/xml",
        ".rgb"    , "image/x-rgb",
        ".roff"   , "application/x-troff",
        ".rtx"    , "text/richtext",
        ".sgm"    , "text/x-sgml",
        ".sgml"   , "text/x-sgml",
        ".sh"     , "application/x-sh",
        ".shar"   , "application/x-shar",
        ".snd"    , "audio/basic",
        ".so"     , "application/octet-stream",
        ".src"    , "application/x-wais-source",
        ".sv4cpio': 'application/x-sv4cpio",
        ".sv4crc" , "application/x-sv4crc",
        ".swf"    , "application/x-shockwave-flash",
        ".t"      , "application/x-troff",
        ".tar"    , "application/x-tar",
        ".tcl"    , "application/x-tcl",
        ".tex"    , "application/x-tex",
        ".texi"   , "application/x-texinfo",
        ".texinfo': 'application/x-texinfo",
        ".tif"    , "image/tiff",
        ".tiff"   , "image/tiff",
        ".tr"     , "application/x-troff",
        ".tsv"    , "text/tab-separated-values",
        ".txt"    , "text/plain",
        ".ustar"  , "application/x-ustar",
        ".vcf"    , "text/x-vcard",
        ".wav"    , "audio/x-wav",
        ".wiz"    , "application/msword",
        ".wsdl"   , "application/xml",
        ".xbm"    , "image/x-xbitmap",
        ".xlb"    , "application/vnd.ms-excel",
        // Duplicates :(
        ".xls"    , "application/excel",
        ".xls"    , "application/vnd.ms-excel",
        ".xml"    , "text/xml",
        ".xpdl"   , "application/xml",
        ".xpm"    , "image/x-xpixmap",
        ".xsl"    , "application/xml",
        ".xwd"    , "image/x-xwindowdump",
        ".zip"    , "application/zip"
        );
    public static final String type (String filename) {
        int dotAt = filename.lastIndexOf('.');
        if (dotAt > -1)
            return TYPES.get(filename.substring(dotAt));
        else {
            return null;
        }
    }
    private static final byte[] _COLON = new byte[]{':'}; 
    private static final byte[] _SPACE = new byte[]{' '}; 
    private static final byte[] _CRLF = "\r\n".getBytes(); 
    private static final void put (
        HashMap headers, String name, String value
        ) {
        if (headers.containsKey(name)) {
            if (headers.get(name) instanceof ArrayList) {
                ((ArrayList) headers.get(name)).add(value);
            } else {
                Object first = headers.get(name);
                ArrayList list = new ArrayList();
                list.add(first);
                list.add(value);
                headers.put(name, list);
            }
        } else {
            headers.put(name, value);
        }
    }
    public static final void update(HashMap headers, String lines, int pos) {
        int spaceAt, colonAt, crlfAt;
        String name = null, value = "";
        int length = lines.length();
        while (pos < length) {
            spaceAt = lines.indexOf(" ", pos);
            colonAt = lines.indexOf(":", pos);
            if (0 < colonAt && (spaceAt == -1 || colonAt < spaceAt)) {
                if (name != null) {
                    put(headers, name, value.trim());
                }
                name = lines.substring(pos, colonAt).toLowerCase();
                value = "";
                pos = colonAt + 1;
            }
            crlfAt = lines.indexOf("\r\n", pos);
            if (crlfAt == -1) {
                value = value + lines.substring(pos, length);
                break;
            } else {
                value = value + lines.substring(pos, crlfAt);
                pos = crlfAt + 2;
            }
        }
        if (name != null) {
            put(headers, name, value.trim());
        }
    }
    public static final void update(HashMap headers, byte[] bytes, int pos) {
        int spaceAt, colonAt, crlfAt;
        String name = null, value = "";
        while (pos < bytes.length) {
            spaceAt = Bytes.find(_SPACE, bytes, pos);
            colonAt = Bytes.find(_COLON, bytes, pos);
            if (0 < colonAt && (spaceAt == -1 || colonAt < spaceAt)) {
                if (name != null) {
                    put(headers, name, value.trim());
                }
                name = (new String(bytes, pos, colonAt-pos)).toLowerCase();
                value = "";
                pos = colonAt + 1;
            }
            crlfAt = Bytes.find(_CRLF, bytes, pos);
            if (crlfAt == -1) {
                value = value + (new String(bytes, pos, bytes.length-pos));
                break;
            } else {
                value = value + (new String(bytes, pos, crlfAt-pos));
                pos = crlfAt + 2;
            }
        }
        if (name != null) {
            put(headers, name, value.trim());
        }
    }
    public static final Iterator<String> options (
        HashMap headers, String name
        ) {
        String options = (String) headers.get(name);
        if (options == null) {
            return null;
        } else {
            return Strings.split(options, ',');
        } 
    }
}

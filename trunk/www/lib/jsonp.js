/* Copyright (C) 2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU General Public License as
published by the Free Software Foundation.

    http://www.gnu.org/copyleft/gpl.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU General Public License
along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */
 
JSON.pprint = (function () {
    var _escape = (function () {
        var _escaped = {
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
            };
        return function (a, b) {
            var c = _escaped[b];
            if (c) return c;
            c = b.charCodeAt();
            return '\\u00'+Math.floor(c/16).toString(16)+(c%16).toString(16);
        }
    })();
    return function (sb, value, indent) {
        switch (typeof value) {
        case 'string': case 'number': case 'boolean':
            return JSON.buffer(sb, value);
        case 'undefined':
            sb.push('null'); 
            return sb;
        case 'function': case 'unknown':
            return sb;
        case 'object':
            if (value === null) 
                sb.push ('null');
            else if (typeof value.length == 'undefined') { // Object
                sb.push ('{');
                if (indent) {
                    indent += '  ';
                } else
                    indent = '\n  ';
                for (k in value) {
                    if (typeof value[k] != 'function') {
                        sb.push (indent);
                        JSON.buffer (sb, k);
                        sb.push (': '); 
                        JSON.pprint (sb, value[k], indent); 
                        sb.push (',');
                    }
                }
                if (sb.pop() == '{') 
                    sb.push('{}');
                else {
                    sb.push(indent);
                    sb.push('}');
                }
            } else { // Array
                var flat = true;
                var v, i, L=value.length;
                for (i=0; i<L; i++) {
                    v = value[i];
                    if (v !== null && (typeof v) == 'object') {
                        flat = false;
                        break;
                    }
                }
                if (flat) {
                    sb.push ('[');
                    for (i=0; i<L; i++) {
                        JSON.buffer (sb, value[i]); 
                        sb.push (', ');
                    }
                    if (sb.pop() == '[') {
                        sb.push('[]');
                    } else {
                        sb.push(']');
                    }
                } else {
                    if (indent) {
                        indent += '  ';
                    } else
                        indent = '\n  ';
                    sb.push ('[');
                    for (i=0; i<L; i++) {
                        sb.push (indent);
                        JSON.pprint (sb, value[i], indent); 
                        sb.push (',');
                    }
                    if (sb.pop() == '[') {
                        sb.push('[]');
                    } else {
                        sb.push(indent);
                        sb.push(']');
                    }
                }
            }
            return sb;
        default:
            value = value.toString();
            sb.push ('"');
            if (/["\\\x00-\x1f]/.test(value)) 
                sb.push(value.replace(/([\x00-\x1f\\"])/g, _escape));
            else
                sb.push(value);
            sb.push ('"');
            return sb;
        }
    };
})();
 

/*  http://laurentszyster.be/protocols/index.html

Copyright(c) 2006-2007, Jack Slocum.

    http://extjs.com/license.txt

Copyright © 2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify it under 
the terms of version 3 of the GNU Lesser General Public License as published 
by the Free Software Foundation.

    http://www.gnu.org/licenses/lgpl-3.0.txt

This library is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU General Public License along with 
this library; if not, write to the Free Software Foundation, Inc., 59 Temple 
Place, Suite 330, Boston, MA 02111-1307 USA */

function _ie_purge(d) {
    var a = d.attributes, i, l, n;
    if (a) {
        l = a.length; for (i = 0; i < l; i += 1) {
            n = a[i].name;
            if (typeof d[n] === 'function') d[n] = null;
        }
    }
    a = d.childNodes;
    if (a) {
        l = a.length;
        for (i = 0; i < l; i += 1) _ie_purge(d.childNodes[i]);
    }
} // see http://javascript.crockford.com/memory/leak.html

function pass() {}; // noop, nada, niks, rien.
function map (fun, list) {
    var r = [];
    for (var i=0, L=list.length; i<L; i++) 
        r.push(fun(list[i]));
    return r;
};
var filter = (function () {
    if (Array.filter)
        return function (fun, list) {
            return list.filter(fun);
        };
    else
        return function (fun, list) {
            var r = [];
            for (var i=0, L=list.length; i<L; i++) 
                if (fun(list[i]))
                    r.push(list[i]);
            return r;
        };
})(); // a functional duo waiting for JavaScript 1.7 list comprehension

function $(id) {
    return document.getElementById(id);
} // the simplest implementation of Prototype's defacto standard.

function bindAsEventListener(object, fun, bubble, capture) {
    return function listener (event) {
        if (!event) { // IE
            fun.apply(object, [window.event]);
            window.event.cancelBubble = (bubble==false);
            return capture;
        } else { // W3C
            fun.apply(object, [event])
            if (bubble==false) event.stopPropagation();
            if (capture) event.preventDefault();
        }
    }
} // a different event listener binding than Prototype's.

var Protocols = function () {
    var n, f = function () {this.initialize.apply(this, arguments)};
    for (var i=0, L=arguments.length; i<L; i++)
        for (n in arguments[i]) {
            f.prototype[n] = arguments[i][n];
        }
    return f;
} // the only OO convenience you need in JavaScript
var HTTP = {requests: {}, pending: 0, timeout: 3000};
HTTP.state = function (active) {
    var hourGlass = $('hourGlass');
    if (hourGlass)
        if (active)
            CSS.add (hourGlass, ['wait']);
        else
            CSS.remove (hourGlass, ['wait']);
}
HTTP.urlencode = (function () {
    var _encode = function (s) {
    	var a = s.split("+");
    	for (var i=0, L=a.length; i<L; i++) {
    		a[i] = escape(a[i]);
    	}
    	return a.join("%2B");
    };
    return function (sb, query) {
        var value, prefix, start = sb.length;
        for (key in query) {
            value = query[key];
            if (value === null) {
                sb.push('&'); 
                sb.push(_encode(key));
                continue;
            }
            switch (typeof value) {
            case 'string': case 'number': case 'boolean':
                sb.push('&'); 
                sb.push(_encode(key));
                sb.push('='); 
                sb.push(_encode(value.toString()));
                break;
            case 'undefined': 
                sb.push('&'); 
                sb.push(_encode(key));
                break;
            case 'function': case 'unknown':
                break;
            case 'object':
                if (value.length) {
                    prefix = '&' + _encode(key) + '=';
                    for (var i=0, L=value.length; i<L; i++) {
                        sb.push(prefix);
                        sb.push(_encode(value[i].toString()))
                    }
                    break;
                }
            default:
                throw "query values must be String, Number, Boolean or Array";
            }
        }
        if (sb.length > start)
            if (start === 0)
                sb[start] = sb[start].substr(1);
            else
                sb[start] = '?' + sb[start].substr(1);
        return sb;
    };
})();
HTTP.request = function (
    method, url, headers, body, ok, error, timeout
    ) {
    var key = [method, url].join(' ');
    if (HTTP.requests[key]!=null) return null;
    var req = null;
    if (window.XMLHttpRequest) { // Mozilla, Safari, ...
        req = new XMLHttpRequest();
        if (req.overrideMimeType && (
            navigator.userAgent.match(/Gecko\/(\d{4})/) || [0,2005]
            )[1] < 2005)
            headers['Connection'] = 'close';
    } else if (window.ActiveXObject) {
        try { // IE
            req = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {req = new ActiveXObject("Microsoft.XMLHTTP");} 
            catch (e) {;}
        }
    }
    if (!req) {
        (except||pass)(key, "XMLHttpRequest not supported"); 
        return null;
    }
    HTTP.requests[key] = req;
    req.onreadystatechange = HTTP.response(key, ok, error);
    req.open(method, url, true);
    for (var name in headers) 
        req.setRequestHeader(name, headers[name]);
    if (HTTP.pending == 0) HTTP.state(true);
    HTTP.pending++;
    setTimeout('HTTP.abort("' + key + '")', timeout||HTTP.timeout);
    req.send(body);
    return key;
};
HTTP.response = function (key, ok, error) {
    return function onReadyStateChange () {
        var status = 0, req = HTTP.requests[key];
        if (!req) { // request allready aborted
            HTTP.except(key, "aborted");
        }
        var state = req.readyState;
        if (state > 1) try {
            HTTP.observe(key, state);
        } catch (e) {
            HTTP.except(key, e.toString());
        };
        if (state > 2) try {
            status = req.status; 
        } catch (e) {}; // timeout is an error with status 0
        if (state == 4) {
            HTTP.requests[key] = null;
            HTTP.pending--;
            if (HTTP.pending == 0) HTTP.state(false);
            if (status == 200 || ((status == 0) && req.responseText)) {
                try {
                    ok (req);
                } catch (e) {
                    HTTP.except(key, e.toString());
                }
            } else if (error) try {
                error (status, req);
            } catch (e) {
                HTTP.except(key, e.toString());
            }
        }
    }
} // see http://www.quirksmode.org/blog/archives/2005/09/xmlhttp_notes_r_2.html
HTTP.abort = function (key) {
    try { 
        HTTP.requests[key].abort();
        HTTP.pending--;
        if (HTTP.pending == 0) HTTP.state(false);
    } catch (e) {
        HTTP.requests[key] = null;
    }
};
HTTP.observe = function (key, state) {
    (HTTP.observe.rs[key]||pass) (state);
};
HTTP.observe.rs = {}; // yeah, that's smart ;-)
HTTP.except = function (key, message) {
    HTTP.except.ions.push(arguments);
};
HTTP.except.ions = []; // remove everything with HTTP.except = pass;

var HTML = {onload: []};
(function () {
    var _onload = function () {
        for (var i=0, L=HTML.onload.length; i<L; i++) 
            HTML.onload[i]();
        HTML.onload = null; // release those functions now!
    };
    if (document.addEventListener) { // Mozilla
        document.addEventListener("DOMContentLoaded", _onload, false);
    } else if (/WebKit/i.test(navigator.userAgent)) { // Safari
        var _timer = setInterval(function() {
            if (/loaded|complete/.test(document.readyState)) {
                clearInterval(_timer); 
                _onload();
            }
        }, 10);
    } else { // IE is somehow supported ...
        // for Internet Explorer (using conditional comments)
        document.write("<script id=__ie_onload defer src=javascript:void(0)><\/script>");
        var script = document.getElementById("__ie_onload");
        script.onreadystatechange = function() {
            if (this.readyState == "complete") {
                _onload(); // call the onload handler
            }
        };
    }
})(); // see http://dean.edwards.name/weblog/2006/06/again/
HTML._escaped = {'<': '&lt;', '>': '&gt;', '"': '&quot;', '&': '&amp;'};
HTML._escape = function (a, b) {return HTML._escaped[b];}
HTML.cdata = function (string) {
    if (/[<>"&]/.test(string)) 
        return string.replace(/([<>"&])/g, HTML._escape);
    else
        return string;
}
HTML.input = function (elements, query) {
    for (var el, i=0, L=elements.length; i<L; i++) {
        el = elements[i]; 
        if (
            el.name != null && 
            /(input)|(textarea)/.test (el.nodeName.toLowerCase()) && 
             /(text)|(password)|(checkbox)|(radio)|(hidden)/.test(
                 (el.type||'').toLowerCase()
                 )
            ) query[el.name] = el.value;
        else if (el.childNodes)
            HTML.input (el.childNodes, query)
    }
    return query;
}
HTML.text = (function () {
    if (window.XMLHttpRequest) // Mozilla, Safari, ...
        return function (element) {
            return element.textContent;
        };
    else // IE ...
        return function (element) {
            return element.innerText;
        }; 
})();
HTML.update = function (element, html) {
    if (element.innerHTML!=null) { // Everybody's fast hack
        _ie_purge (element);
        element.innerHTML = html;
    } else { // DOM standard, ...
        var range = element.ownerDocument.createRange();
        range.selectNodeContents(element);
        range.collapse(element);
        element.appendChild(range.createContextualFragment(html));
    }
}
HTML.replace = function (element, html) {
    if (element.outerHTML!=null) { // IE fast hack
        _ie_purge (element);
        element.outerHTML = html;
    } else { // DOM standard, ...
        var range = element.ownerDocument.createRange();
        range.selectNodeContents(element);
        element.parentNode.replaceChild(
            range.createContextualFragment(html), element
            );
    }
}
HTML.insert = function (element, html, adjacency) {
    var fragments, range = null;
    if (element.insertAdjacentHTML!=null) {
        try { // IE fast hack
            element.insertAdjacentHTML(adjacency, html); return;
        } catch (e) { // and its the fast catch.
            var tagName = element.tagName.toUpperCase();
            if (['TBODY', 'TR'].include(tagName)) {
                var div = document.createElement('div');
                div.innerHTML = (
                    '<table><tbody>' + html + '</tbody></table>'
                    );
                fragments = div.childNodes[0].childNodes[0].childNodes;
            } else {throw e;}
        }
    } else {  // DOM standard
        range = element.ownerDocument.createRange();
    } // Prototype's complicated Insertion untwisted ;-)
    var i, L;
    switch (adjacency) {
    case 'beforeBegin':
        if (range!=null) {
            range.setStartBefore(element); // ... Mozilla only
            fragments = [range.createContextualFragment(html)];
        }
        for (i=0, L=fragments.length; i<L; i++)
            element.parentNode.insertBefore(fragments[i], element);
        break;
    case 'afterBegin':
        if (range!=null) {
            range.selectNodeContents(element);
            range.collapse(true);
            fragments = [range.createContextualFragment(html)];
        }
        for (i=fragments.length-1; i>-1; i--)
            element.insertBefore(fragments[i], element.firstChild)
        break;
    case 'beforeEnd':
        if (range!=null) {
            range.selectNodeContents(element);
            range.collapse(element);
            fragments = [range.createContextualFragment(html)];
        }
        for (i=0, L=fragments.length; i<L; i++)
              element.appendChild(fragments[i]);
        break;
    case 'afterEnd':
        if (range!=null) {
            range.setStartAfter(element);
            fragments = [range.createContextualFragment(html)];
        }
        for (i=0, L=fragments.length; i<L; i++)
            element.parentNode.insertBefore(
                fragments[i], element.nextSibling
                );
    }
}
HTML.listen = (function () {
    if (window.XMLHttpRequest) // Mozilla, Safari, ...
        return function (element, type, listener) {
            element.addEventListener(type, listener, false);
            return listener;
        };
    else // IE ...
        return function (element, type, listener) {
            element.attachEvent("on" + type, listener);
            return listener;
        };
})();
HTML.bind = function (element, type, listener, bubble, capture) {
    return HTML.listen(
       element, type, bindAsEventListener(element, listener, bubble, capture)
       );
}
HTML.parse = function (text) {
    var el = document.createElement('div');
    HTML.update(el, text);
    return el.childNodes[0];
}

var JSON = {}
JSON._escaped = {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '"' : '\\"',
    '\\': '\\\\'
    };
JSON._escape = function (a, b) {
    var c = JSON._escaped[b];
    if (c) return c;
    c = b.charCodeAt();
    return '\\u00'+Math.floor(c/16).toString(16)+(c%16).toString(16);
}
JSON.decode = function (string) {
    try {
        if (/^("(\\.|[^"\\\n\r])*?"|[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t])+?$/.
                test(string))
            return eval('(' + string + ')');
    } catch (e) {
        throw new "JSON syntax error";
    }
}
JSON.buffer = function (sb, value) {
    switch (typeof value) {
    case 'string':
        sb.push ('"');
        if (/["\\\x00-\x1f]/.test(value)) 
            sb.push(value.replace(/([\x00-\x1f\\"])/g, JSON._escape));
        else
            sb.push(value);
        sb.push ('"');
        return sb;
    case 'number':
        sb.push (isFinite(value) ? value : "null"); 
        return sb;
    case 'boolean':
        sb.push (value); 
        return sb;
    case 'undefined': case 'function': case 'unknown':
        return sb;
    case 'object':
        if (value == null) 
            sb.push ("null");
        else if (value.length == null) { // Object
            sb.push ('{');
            for (k in value) {
                JSON.buffer (sb, k);
                sb.push (':'); 
                JSON.buffer (sb, value[k]); 
                sb.push (',');
                }
            if (sb.pop() == '{') 
                sb.push('{}');
            else {
                sb.push('}');
            }
        } else { // Array
            sb.push ('[');
            for (var i=0, L=value.length; i<L; i++) {
                JSON.buffer (sb, value[i]); 
                sb.push (',')
                }
            if (sb.pop() == '[') 
                sb.push('[]');
            else 
                sb.push(']');
        }
        return sb;
    default:
        value = value.toString();
        sb.push ('"');
        if (/["\\\x00-\x1f]/.test(value)) 
            sb.push(value.replace(/([\x00-\x1f\\"])/g, JSON._escape));
        else
            sb.push(value);
        sb.push ('"');
        return sb;
    }
}
JSON.encode = function (value) {
    return JSON.buffer([], value).join('');
}
JSON.templates = {} // {'class': ['before', 'after']}
JSON.HTML = function (sb, value, name) {
    var t = typeof value;
    var template = (JSON.templates[name] || JSON.templates[t]);
    switch (t) {
    case 'string':
        if (template) {
            sb.push(template[0]);
            sb.push(HTML.cdata(value));
            sb.push(template[1]);
        } else {
            sb.push('<span class="string">');
            sb.push(HTML.cdata(value)); 
            sb.push('</span>');
        }
        break;
    case 'number':
        if (template) {
            sb.push(template[0]);
            sb.push(isFinite(value) ? value : "null");
            sb.push(template[1]);
        } else {
            sb.push('<span class="number">');
            sb.push(isFinite(value) ? value : "null");
            sb.push('</span>');
        }
        break;
    case 'boolean':
        if (template) {
            sb.push(template[0]);
            sb.push(value); 
            sb.push(template[1]);
        } else {
            sb.push('<span class="boolean">');
            sb.push(value); 
            sb.push('</span>');
        }
        break;
    case 'undefined': case 'function': case 'unknown':
        break;
    case 'object':
        if (value == null) 
            if (template) {
                sb.push(template[0]);
                sb.push(template[1]);
            } else
                sb.push('<span class="null"/>'); 
        else if (value.length == null) { // Object
            if (template) {
                sb.push (template[0]);
                for (var k in value) JSON.HTML (sb, value[k], k); 
                sb.push (template[1]);
            } else {
                sb.push ('<div class="object">');
                for (var k in value) {
                    sb.push('<div class="property"><span class="name">');
                    sb.push(HTML.cdata (k));
                    sb.push('</span>');
                    JSON.HTML (sb, value[k], k); 
                    sb.push ('</div>');
                    }
                sb.push('</div>');
            }
        } else { // Array
            sb.push('<div class="array">');
            for (var i=0, L=value.length; i<L; i++) 
                JSON.HTML(sb, value[i], name)
            sb.push('</div>');
        }
        break;
    default:
        sb.push(HTML.cdata(value.toString())); 
        break;
    }
    if (template) 
        sb.push(template[1]);
    return sb;
}
JSON.view = function (values) {
    for (var key in values) if ($(key) != null && values[key] != null)
        HTML.update($(key), JSON.HTML([], values[key], key).join(''));
}
JSON.errors = {};
JSON.GET = function (url, query, ok, headers, timeout) {
    if (query) {
        url = HTTP.urlencode([url], query).join ('');
    }
    if (headers) {
        headers['Accept'] = 'application/json, text/javascript';
    } else {
        headers = {'Accept': 'application/json, text/javascript'};
    }
    return HTTP.request(
        'GET', url, headers, null, ok || JSON.update(), 
        function (status, req) {
            (JSON.errors[status.toString()]||pass)(url, query, req);
        }, 
        timeout
        );
}
JSON.POST = function (url, payload, ok, headers, timeout) {
    if (headers) {
        headers['Content-Type'] = 'application/json; charset=UTF-8';
        headers['Accept'] = 'application/json, text/javascript';
    } else {
        headers = {
            'Content-Type': 'application/json; charset=UTF-8', 
            'Accept': 'application/json, text/javascript'
            };
    }
    return HTTP.request(
        'POST', url, headers, JSON.buffer ([], payload).join (''), 
        ok || JSON.update(), 
        function (status, req) {
            (JSON.errors[status.toString()]||pass)(url, payload, req);
        }, 
        timeout
        );
}
JSON.update = function (id, name) {
    if (id == null)
        return function (req) {
            JSON.view(JSON.decode(req.responseText));
        }
    return function (req) {
        HTML.update($(id), JSON.HTML(
            [], JSON.decode(req.responseText), name
            ).join(''));
    }
}
JSON.insert = function (adjacency, id, name) {
    return function (req) {
        HTML.insert($(id), JSON.HTML(
            [], JSON.decode(req.responseText), name
            ).join(''), adjacency);
    }
}
JSON.extend = function (adjacency, id, name) {
    return function (req) {
        var json = JSON.decode(req.responseText);
        if (typeof json == 'object' && json.length) {
            var sb = [];
            if (adjacency == 'beforeEnd' || adjacency == 'beforeBegin') 
                for (var i=0, L=json.length; i<L; i++)
                    JSON.HTML(sb, json[i], name);
            else (adjacency == 'afterBegin' || adjacency == 'afterEnd')
                for (var i=json.length-1; i>-1; i--)
                    JSON.HTML(sb, json[i], name);
            HTML.insert($(id), sb.join(''), adjacency);
        } else 
            HTML.insert($(id), JSON.HTML([], json, name).join(''), adjacency);
    }
}


var CSS = (function(){
    /*
     * Ext - JS Library 1.0 Alpha 3 - Rev 4
     * Copyright(c) 2006-2007, Jack Slocum.
     * 
     * http://www.extjs.com/license.txt
     * 
     * This is code is also distributed under MIT license for use
     * with jQuery and prototype JavaScript libraries.
     * 
     * Modified by Laurent Szyster as a standalone CSS namespace to add
     * the power of selector to protocols.js, under LGPL 3.0 licence.
     */ 
     
    var cache = {}, simpleCache = {}, valueCache = {};
    var nonSpace = /\S/;
    var trimRe = /^\s*(.*?)\s*$/;
    var tplRe = /\{(\d+)\}/g;
    var modeRe = /^(\s?[\/>]\s?|\s|$)/;
    var tagTokenRe = /^(#)?([\w-\*]+)/;
    
    function child(p, index){
        var i = 0;
        var n = p.firstChild;
        while(n){
            if(n.nodeType == 1){
               if(++i == index){
                   return n;
               }
            }
            n = n.nextSibling;
        }
        return null;
    };
    
    function next(n){
        while((n = n.nextSibling) && n.nodeType != 1);
        return n;
    };
    
    function prev(n){
        while((n = n.previousSibling) && n.nodeType != 1);
        return n;
    };
    
    function clean(d){
        var n = d.firstChild, ni = -1;
 	    while(n){
 	        var nx = n.nextSibling;
 	        if(n.nodeType == 3 && !nonSpace.test(n.nodeValue)){
 	            d.removeChild(n);
 	        }else{
 	            n.nodeIndex = ++ni;
 	        }
 	        n = nx;
 	    }
 	    return this;
 	};

    function byClassName(c, a, v, re, cn){
        if(!v){
            return c;
        }
        var r = [];
        for(var i = 0, ci; ci = c[i]; i++){
            cn = ci.className;
            if(cn && (' '+cn+' ').indexOf(v) != -1){
                r[r.length] = ci;
            }
        }
        return r;
    };

    function attrValue(n, attr){
        if(!n.tagName && typeof n.length != "undefined"){
            n = n[0];
        }
        if(!n){
            return null;
        }
        if(attr == "for"){
            return n.htmlFor;
        }
        if(attr == "class" || attr == "className"){
            return n.className;
        }
        return n.getAttribute(attr) || n[attr];
          
    };
    
    function getNodes(ns, mode, tagName){
        var result = [], cs;
        if(!ns){
            return result;
        }
        mode = mode ? mode.replace(trimRe, "$1") : "";
        tagName = tagName || "*";
        if(typeof ns.getElementsByTagName != "undefined"){
            ns = [ns];   
        }
        if(mode != "/" && mode != ">"){
            for(var i = 0, ni; ni = ns[i]; i++){
                cs = ni.getElementsByTagName(tagName);
                for(var j = 0, ci; ci = cs[j]; j++){
                    result[result.length] = ci;
                }
            }
        }else{
            for(var i = 0, ni; ni = ns[i]; i++){
                var cn = ni.getElementsByTagName(tagName);
                for(var j = 0, cj; cj = cn[j]; j++){
                    if(cj.parentNode == ni){
                        result[result.length] = cj;
                    }
                }
            }
        }
        return result;
    };
    
    function concat(a, b){
        if(b.slice){
            return a.concat(b);
        }
        for(var i = 0, l = b.length; i < l; i++){
            a[a.length] = b[i];
        }
        return a;
    }
    
    function byTag(cs, tagName){
        if(cs.tagName || cs == document){
            cs = [cs];
        }
        if(!tagName){
            return cs;
        }
        var r = []; tagName = tagName.toLowerCase();
        for(var i = 0, ci; ci = cs[i]; i++){
            if(ci.nodeType == 1 && ci.tagName.toLowerCase()==tagName){
                r[r.length] = ci;
            }
        }
        return r; 
    };
    
    function byId(cs, attr, id){
        if(cs.tagName || cs == document){
            cs = [cs];
        }
        if(!id){
            return cs;
        }
        var r = [];
        for(var i = 0,ci; ci = cs[i]; i++){
            if(ci && ci.id == id){
                r[r.length] = ci;
                return r;
            }
        }
        return r; 
    };
    
    function byAttribute(cs, attr, value, op, custom){
        var r = [], st = custom=="{";
        var f = CSS.operators[op];
        for(var i = 0; ci = cs[i]; i++){
            var a;
            if(st){
                a = CSS.getStyle(ci, attr);
            }
            else if(attr == "class" || attr == "className"){
                a = ci.className;
            }else if(attr == "for"){
                a = ci.htmlFor;
            }else if(attr == "href"){
                a = ci.getAttribute("href", 2);
            }else{
                a = ci.getAttribute(attr);
            }
            if((f && f(a, value)) || (!f && a)){
                r[r.length] = ci;
            }
        }
        return r;
    };
    
    function byPseudo(cs, name, value){
        return CSS.pseudos[name](cs, value);
    };
    
    // This is for IE MSXML which does not support expandos.
    // IE runs the same speed using setAttribute, however FF slows way down
    // and Safari completely fails so they need to continue to use expandos.
    var isIE = window.ActiveXObject ? true : false;

    var key = 30803;

    function nodupIEXml(cs){
        var d = ++key;
        cs[0].setAttribute("_nodup", d);
        var r = [cs[0]];
        for(var i = 1, len = cs.length; i < len; i++){
            var c = cs[i];
            if(!c.getAttribute("_nodup") != d){
                c.setAttribute("_nodup", d);
                r[r.length] = c;
            }
        }
        for(var i = 0, len = cs.length; i < len; i++){
            cs[i].removeAttribute("_nodup");
        }
        return r;
    }

    function nodup(cs){
        var len = cs.length, c, i, r = cs, cj;
        if(!len || typeof cs.nodeType != "undefined" || len == 1){
            return cs;
        }
        if(isIE && typeof cs[0].selectSingleNode != "undefined"){
            return nodupIEXml(cs);
        }
        var d = ++key;
        cs[0]._nodup = d;
        for(i = 1; c = cs[i]; i++){
            if(c._nodup != d){
                c._nodup = d;
            }else{
                r = [];
                for(var j = 0; j < i; j++){
                    r[r.length] = cs[j];
                }
                for(j = i+1; cj = cs[j]; j++){
                    if(cj._nodup != d){
                        cj._nodup = d;
                        r[r.length] = cj;
                    }
                }
                return r;
            }
        }
        return r;
    }

    function quickDiffIEXml(c1, c2){
        var d = ++key;
        for(var i = 0, len = c1.length; i < len; i++){
            c1[i].setAttribute("_qdiff", d);
        }
        var r = [];
        for(var i = 0, len = c2.length; i < len; i++){
            if(c2[i].getAttribute("_qdiff") != d){
                r[r.length] = c2[i];
            }
        }
        for(var i = 0, len = c1.length; i < len; i++){
           c1[i].removeAttribute("_qdiff");
        }
        return r;
    }

    function quickDiff(c1, c2){
        var len1 = c1.length;
        if(!len1){
            return c2;
        }
        if(isIE && c1[0].selectSingleNode){
            return quickDiffIEXml(c1, c2);
        }
        var d = ++key;
        for(var i = 0; i < len1; i++){
            c1[i]._qdiff = d;
        }
        var r = [];
        for(var i = 0, len = c2.length; i < len; i++){
            if(c2[i]._qdiff != d){
                r[r.length] = c2[i];
            }
        }
        return r;
    }
    
    function quickId(ns, mode, root, id){
        if(ns == root){
           var d = root.ownerDocument || root;
           return d.getElementById(id);
        }
        ns = getNodes(ns, mode, "*");
        return byId(ns, null, id);
    }

    if (isIE) { // IE ...
        var cssSet = function (element, names) {
            element.setAttribute("className", names.join(' '));
        };
        var cssRemove = function (element, pcre) {
            var n = element.className;
            element.setAttribute("className", n.replace(pcre, ''));
        };
    } else { // Mozilla, Safari, ...
        var cssSet = function (element, names) {
            element.className = names.join(' ');
        };
        var cssRemove = function (element, pcre) {
            var n = element.className;
            element.className = n.replace(pcre, '');
        };
    }
    
    return {
        getStyle : function(el, style){
        	if (el.currentStyle)
        		var v = el.currentStyle[style];
        	else if (window.getComputedStyle)
        		var v = document.defaultView.getComputedStyle(
        		    el, null
        		    ).getPropertyValue(style);
        	return v;
        },
        /**
         * Compiles a selector/xpath query into a reusable function. The 
         * returned function takes one parameter "root" (optional), which is 
         * the context node from where the query should start. 
         * @param {String} selector The selector/xpath query
         * @param {String} type (optional) Either "select" (the default) or 
         * "simple" for a simple selector match
         * @return {Function}
         */
        compile : function(path, type){
            // strip leading slashes
            while(path.substr(0, 1)=="/"){
                path = path.substr(1);
            }
            type = type || "select";
            
            var fn = ["var f = function(root){\n var mode; var n = root || document;\n"];
            var q = path, mode, lq;
            var tk = CSS.matchers;
            var tklen = tk.length;
            var mm;
            while(q && lq != q){
                lq = q;
                var tm = q.match(tagTokenRe);
                if(type == "select"){
                    if(tm){
                        if(tm[1] == "#"){
                            fn[fn.length] = 'n = quickId(n, mode, root, "'+tm[2]+'");';
                        }else{
                            fn[fn.length] = 'n = getNodes(n, mode, "'+tm[2]+'");';
                        }
                        q = q.replace(tm[0], "");
                    }else if(q.substr(0, 1) != '@'){
                        fn[fn.length] = 'n = getNodes(n, mode, "*");';
                    }
                }else{
                    if(tm){
                        if(tm[1] == "#"){
                            fn[fn.length] = 'n = byId(n, null, "'+tm[2]+'");';
                        }else{
                            fn[fn.length] = 'n = byTag(n, "'+tm[2]+'");';
                        }
                        q = q.replace(tm[0], "");
                    }
                }
                while(!(mm = q.match(modeRe))){
                    var matched = false;
                    for(var j = 0; j < tklen; j++){
                        var t = tk[j];
                        var m = q.match(t.re);
                        if(m){
                            fn[fn.length] = t.select.replace(
                                tplRe, function(x, i){return m[i];}
                                );
                            q = q.replace(m[0], "");
                            matched = true;
                            break;
                        }
                    }
                    // prevent infinite loop on bad selector
                    if(!matched){
                        throw 'Error parsing selector, parsing failed at "' + q + '"';
                    }
                }
                if(mm[1]){
                    fn[fn.length] = 'mode="'+mm[1]+'";';
                    q = q.replace(mm[1], "");
                }
            }
            fn[fn.length] = "return nodup(n);\n}";
            eval(fn.join(""));
            return f;
        },
        
        /**
         * Selects a group of elements.
         * @param {String} selector The selector/xpath query
         * @param {Node} root (optional) The start of the query (defaults to 
         * document).
         * @return {Array}
         */
        select : function(path, root, type){
            if(!root || root == document){
                root = document;
            }
            if(typeof root == "string"){
                root = document.getElementById(root);
            }
            var paths = path.split(",");
            var results = [];
            for(var i = 0, len = paths.length; i < len; i++){
                var p = paths[i].replace(trimRe, "$1");
                if(!cache[p]){
                    cache[p] = CSS.compile(p);
                    if(!cache[p]){
                        throw p + " is not a valid selector";
                    }
                }
                var result = cache[p](root);
                if(result && result != document){
                    results = results.concat(result);
                }
            }
            return results;
        },
        
        /**
         * Selects a single element.
         * @param {String} selector The selector/xpath query
         * @param {Node} root (optional) The start of the query (defaults to 
         * document).
         * @return {Element}
         */
        selectNode : function(path, root){
            return CSS.select(path, root)[0];
        },
        
        /**
         * Selects the value of a node, optionally replacing null with the 
         * defaultValue.
         * @param {String} selector The selector/xpath query
         * @param {Node} root (optional) The start of the query (defaults to 
         * document).
         * @param {String} defaultValue
         */
        selectValue : function(path, root, defaultValue){
            path = path.replace(trimRe, "$1");
            if(!valueCache[path]){
                valueCache[path] = CSS.compile(path, "select");
            }
            var n = valueCache[path](root);
            n = n[0] ? n[0] : n;
            var v = (n && n.firstChild ? n.firstChild.nodeValue : null);
            return (v === null ? defaultValue : v);
        },
        
        /**
         * Selects the value of a node, parsing integers and floats.
         * @param {String} selector The selector/xpath query
         * @param {Node} root (optional) The start of the query (defaults to 
         * document).
         * @param {Number} defaultValue
         * @return {Number}
         */
        selectNumber : function(path, root, defaultValue){
            var v = CSS.selectValue(path, root, defaultValue || 0);
            return parseFloat(v);
        },
        
        /**
         * Returns true if the passed element(s) match the passed simple 
         * selector (e.g. div.some-class or span:first-child)
         * @param {String/HTMLElement/Array} el An element id, element or 
         * array of elements
         * @param {String} selector The simple selector to test
         * @return {Boolean}
         */
        is : function(el, ss){
            if(typeof el == "string"){
                el = document.getElementById(el);
            }
            var isArray = (el instanceof Array);
            var result = CSS.filter(isArray ? el : [el], ss);
            return isArray ? (result.length == el.length) : (result.length > 0);
        },
        
        /**
         * Filters an array of elements to only include matches of a simple 
         * selector (e.g. div.some-class or span:first-child)
         * @param {Array} el An array of elements to filter
         * @param {String} selector The simple selector to test
         * @param {Boolean} nonMatches If true, it returns the elements that 
         * DON'T match the selector instead of the ones that match
         * @return {Array}
         */
        filter : function(els, ss, nonMatches){
            ss = ss.replace(trimRe, "$1");
            if(!simpleCache[ss]){
                simpleCache[ss] = CSS.compile(ss, "simple");
            }
            var result = simpleCache[ss](els);
            return nonMatches ? quickDiff(result, els) : result;
        },
        
        /**
         * Collection of matching regular expressions and code snippets. 
         */
        matchers : [{
                re: /^\.([\w-]+)/,
                select: 'n = byClassName(n, null, " {1} ");'
            }, {
                re: /^\:([\w-]+)(?:\(((?:[^\s>\/]*|.*?))\))?/,
                select: 'n = byPseudo(n, "{1}", "{2}");'
            },{
                re: /^(?:([\[\{])(?:@)?([\w-]+)\s?(?:(=|.=)\s?['"]?(.*?)["']?)?[\]\}])/,
                select: 'n = byAttribute(n, "{2}", "{4}", "{3}", "{1}");'
            }, {
                re: /^#([\w-]+)/,
                select: 'n = byId(n, null, "{1}");'
            },{
                re: /^@([\w-]+)/,
                select: 'return {firstChild:{nodeValue:attrValue(n, "{1}")}};'
            }
        ],
        
        /**
         * Collection of operator comparison functions. The default operators 
         * are =, !=, ^=, $=, *= and %=. New operators can be added as long as 
         * the match the format <i>c</i>= where <i>c<i> is any character other 
         * than space, &gt; &lt;.
         */
        operators : {
            "=" : function(a, v){
                return a == v;
            },
            "!=" : function(a, v){
                return a != v;
            },
            "^=" : function(a, v){
                return a && a.substr(0, v.length) == v;
            },
            "$=" : function(a, v){
                return a && a.substr(a.length-v.length) == v;
            },
            "*=" : function(a, v){
                return a && a.indexOf(v) !== -1;
            },
            "%=" : function(a, v){
                return (a % v) == 0;
            }
        },
        
        set: cssSet,

        add: function (element, names) {
            var current = element.className;
            if (current) {
                var sb = [current];
                for (var i=0, L=names.length; i<L; i++)
                    if (current.indexOf(names[i]) == -1) sb.push(names[i]);
                cssSet(element, sb);
            } else
                cssSet(element, names);
        },
        
        remove: cssRemove,
        
        /**
         * Collection of "pseudo class" processors. Each processor is passed 
         * the current nodeset (array) and the argument (if any) supplied in 
         * the selector.
         */
        pseudos : {
            "first-child" : function(c){
                var r = [], n;
                for(var i = 0, ci; ci = n = c[i]; i++){
                    while((n = n.previousSibling) && n.nodeType != 1);
                    if(!n){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "last-child" : function(c){
                var r = [];
                for(var i = 0, ci; ci = n = c[i]; i++){
                    while((n = n.nextSibling) && n.nodeType != 1);
                    if(!n){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "nth-child" : function(c, a){
                var r = [];
                if(a != "odd" && a != "even"){
                    for(var i = 0, ci; ci = c[i]; i++){
                        var m = child(ci.parentNode, a);
                        if(m == ci){
                            r[r.length] = m;
                        }
                    }
                    return r;
                }
                var p;
                // first let's clean up the parent nodes
                for(var i = 0, l = c.length; i < l; i++){
                    var cp = c[i].parentNode;
                    if(cp != p){
                        clean(cp);
                        p = cp;
                    }
                }
                // then lets see if we match
                for(var i = 0, ci; ci = c[i]; i++){
                    var m = false;
                    if(a == "odd"){
                        m = ((ci.nodeIndex+1) % 2 == 1);
                    }else if(a == "even"){
                        m = ((ci.nodeIndex+1) % 2 == 0);
                    }
                    if(m){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "only-child" : function(c){
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    if(!prev(ci) && !next(ci)){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "empty" : function(c){
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    var cns = ci.childNodes, j = 0, cn, empty = true;
                    while(cn = cns[j]){
                        ++j;
                        if(cn.nodeType == 1 || cn.nodeType == 3){
                            empty = false;
                            break;
                        }
                    }
                    if(empty){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "contains" : function(c, v){
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    if(ci.innerHTML.indexOf(v) !== -1){
                        r[r.length] = ci;
                    }
                }
                return r;
            },

            "nodeValue" : function(c, v){
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    if(ci.firstChild && ci.firstChild.nodeValue == v){
                        r[r.length] = ci;
                    }
                }
                return r;
            },

            "checked" : function(c){
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    if(ci.checked == true){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "not" : function(c, ss){
                return CSS.filter(c, ss, true);
            },
            
            "odd" : function(c){
                return this["nth-child"](c, "odd");
            },
            
            "even" : function(c){
                return this["nth-child"](c, "even");
            },
            
            "nth" : function(c, a){
                return c[a-1];
            },
            
            "first" : function(c){
                return c[0];
            },
            
            "last" : function(c){
                return c[c.length-1];
            },
            
            "has" : function(c, ss){
                var s = CSS.select;
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    if(s(ss, ci).length > 0){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "next" : function(c, ss){
                var is = CSS.is;
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    var n = next(ci);
                    if(n && is(n, ss)){
                        r[r.length] = ci;
                    }
                }
                return r;
            },
            
            "prev" : function(c, ss){
                var is = CSS.is;
                var r = [];
                for(var i = 0, ci; ci = c[i]; i++){
                    var n = prev(ci);
                    if(n && is(n, ss)){
                        r[r.length] = ci;
                    }
                }
                return r;
            }
        }
    };
})();

/**
 * Selects an array of DOM nodes by CSS/XPath selector. Shorthand 
 * of {@link CSS#select}
 * @param {String} path The selector/xpath query
 * @param {Node} root (optional) The start of the query (defaults to document).
 * @return {Array}
 */
function $$(selector) {return CSS.select(selector);}

HTML.onload.push(
    function () {
        var templates = $('Protocols.JSON.templates');
        if (templates != null && templates.childNodes != null) {
            var child, template, names, i, L, j, K;
            var children = templates.childNodes;
            for (i=0, L=children.length; i<L; i++) {
                child = children[i];
                if ((child.nodeType == 1) && child.className) {
                    template = child.innerHTML.split(
                        /<json\s*\/>|<json\s*><\/json>/i
                        );
                    names = child.className.split(' ');
                    for (j=0, K=names.length; j<K; j++) {
                        JSON.templates[names[j]] = template;
                    }
                }
            }
        }
    });

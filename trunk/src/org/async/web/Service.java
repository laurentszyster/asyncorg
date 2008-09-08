/*  Copyright(c) 2008 Laurent A.V. Szyster
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

import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Controller;
import org.async.chat.GuardCollector;
import org.async.chat.StringCollector;
import org.async.protocols.JSONR;
import org.async.protocols.JSON;
import org.async.simple.Fun;

/**
 * The redefinition of a web service, using JSON in place of SOAP and
 * JSONR instead of WSDL, a simple RPC for the standard of object 
 * notation set by JavaScript web browsers. 
 */
public class Service implements Controller {
	private Fun _function;
    private JSONR.Type _type = null;
	private int _limit = 0;
    public Service (Fun function) {
    	_function = function;
    }
    public Service (Fun function, int limit) {
    	_function = function;
    	_limit = limit;
    }
    public Service (Fun function, JSONR.Type type) {
    	_function = function;
    	_type = type;
    }
    public Service (Fun function, JSONR.Type type, int limit) {
    	_function = function;
    	_type = type;
    	_limit = limit;
    }
    public final boolean handleRequest(Actor http) throws Throwable {
        String method = http.method();
        if (method.equals("GET")) {
            String query = http.uri().getRawQuery();
            if (query != null) {
                JSON.parseURLencoded(query, http.state);
                if (_type != null) {
                    JSONR.validate(http.state, _type);
                }
            }
            _function.apply(http);
        } else if (method.equals("POST")) {
    		http.collect(new GuardCollector(
    				new StringCollector("UTF-8"), 
					(_limit == 0) ? http.channel().bufferInSize(): _limit					
					));
        } else {
            http.error(501); // Not implemented
        }
        return false;
    }

    public final void handleBody(Actor http) throws Throwable {
        Object input = null;
        String body = ((StringCollector) http.body()).toString();
        if (_type == null) {
            input = (new JSON()).eval(body);
        } else {
            input = (new JSONR(_type)).eval(body);
        }
        if (input != null && input instanceof JSON.Object) {
            ((JSON.Object) input).putAll(http.state);
            http.state = (JSON.Object) input;
        } else {
            http.state.put("arg0", input);
        }
        _function.apply(http);
    }
}

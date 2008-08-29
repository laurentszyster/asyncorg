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

import org.async.simple.Fun;
import org.async.protocols.JSON;
import org.async.protocols.JSONR;
import org.async.collect.GuardCollector;
import org.async.collect.StringCollector;
import org.async.sql.Metabase;
import org.async.web.HttpServer.Actor;
import org.async.web.HttpServer.Handler;

/*
 * A class to decouple storage, semantics and access control from the 
 * application's processes.
 * 
 * Predicate handles GET and POST requests routed by Meta for a given predicate, 
 * enforce access control based on the identity and rights of the HTTP actor, 
 * then either return the selected JSON objects or update the metabase with the
 * collected object. Note that Predicate also enforces a limit on POST request 
 * bodies, set by default to the size of its channel input buffer (ie: 16KB).
 * 
 * Predicate's implementation enables developers to focus on application features
 * rather than figuring out why Object Relational Mapping sucks, which simple
 * URL routing is enough and how role based access control fits in a contextual
 * resource description framework.
 * 
 * Using predicates buys you a linear and risk-free development cycle for auditable,
 * state-full and persistent semantic web applications.
 * 
 */
public class Predicate implements Handler {
	protected static final class Pass implements Fun {
		public final Object apply (Object input) {
			return null;
		}
	}
	public static final Fun PASS = new Pass();
	public static final class Recode implements Fun {
		private Fun _fun = PASS;
		public Recode(Fun fun) {
			_fun = fun;
		}
		public final Object apply (Object input) throws Throwable {
			Object[] args = (Object[]) input; 
			args[3] = JSON.decode((String) args[2]);
			Object response = _fun.apply(args);
			args[3] = JSON.encode(args[2]);
			return response;
		}
	}
	public static final class Validate implements Fun {
		private Fun _fun = PASS;
		private JSONR.Type _type;
		public Validate(Fun fun, JSONR.Type type) {
			_fun = fun;
			_type = type;
		}
		public final Object apply (Object input) throws Throwable {
			Object[] args = (Object[]) input; 
			args[3] = _type.eval((String) args[2]);
			Object response = _fun.apply(args);
			args[3] = JSON.encode(args[2]);
			return response;
		}
	}
	private Metabase _metabase;
	private Fun _function = PASS;
	private int _limit = 0;
    public Predicate (Metabase metabase) {
		_metabase = metabase;
    }
    public Predicate (Metabase metabase, Fun function) {
		_metabase = metabase;
		_function = function;
    }
    public Predicate (Metabase metabase, Fun function, int limit) {
		_metabase = metabase;
		_function = function;
    	_limit = limit;
    }
	public final boolean request(Actor http) throws Throwable {
	    String method = http.method();
        String subject = http.about[1];
        String predicate = http.about[2];
        String context = http.about[3];
	    if (method.equals("GET")) {
	    	String object = "null";
	        try {
		    	if (context == null) { // GET subject/predicate
		    		if (http.rights.contains(predicate)) {
		    			object = _metabase.buffer(subject, predicate);
		    		} else {
			            http.response(403); // Forbidden
			            return false;
		    		}
		    	} else if (
					context.equals(http.identity) || http.rights.contains(predicate) 
					) { // GET subject/predicate/context
	                object = _metabase.select(subject, predicate, context);
	            } else {
		            http.response(403); // Forbidden
		            return false;
	            }
	        } catch (Exception e) {
	        	http.channel().log(e);
	            http.response(500); // Server Error
	            return false;
	        }
	        http.responseHeader("Cache-control", "no-cache");
	        http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
	        http.response(200, object, "UTF-8");
		    return false;
	    } else if (method.equals("POST")) {
	    	if (context == null) {
	    		http.response(404); // Not found
	    		return false;
	    	} 
            if (!(http.identity.equals(subject))) {
                http.response(403); // Forbidden
                return false;
            }
            http.collect(new GuardCollector(
				new StringCollector("UTF-8"), 
				(_limit == 0) ? http.channel().bufferInSize(): _limit
				));
    		return false;
	    } else if (method.equals("PUT")) {
	    	if (context == null) {
	    		http.response(404); // Not found
	    		return false;
	    	} 
            if (!http.rights.contains(predicate)) {
                http.response(403); // Forbidden
                return false;
            }
            http.collect(new GuardCollector(
				new StringCollector("UTF-8"), 
				(_limit == 0) ? http.channel().bufferInSize(): _limit
				));
    		return false;
	    } else {
	        http.response(501); // Not implemented
	    }
		return false;
	}
	public final void collected(Actor http) throws Throwable {
		String returned;
        String subject = http.about[1];
        String predicate = http.about[2];
        String context = http.about[3];
        String object = http.requestBody().toString();
	    _metabase.sql.begin();
	    try {
	    	Object[] statement = new Object[]{
    			subject, predicate, object, context, http.digest
    			};
        	returned = (String) _function.apply(statement);
	        if (http.method().equals("PUT")) {
	            _metabase.insert(subject, predicate, context, (String) statement[2]);
	        } else {
	        	_metabase.update(subject, predicate, context, (String) statement[2]);
	        }
	    } catch (Exception e) {
	        _metabase.sql.rollback();
        	http.channel().log(e);
	        http.response(500);
	        return;
	    }
        _metabase.sql.commit();
	    http.responseHeader("Cache-control", "no-cache");
	    http.responseHeader("Content-Type", "text/javascript; charset=UTF-8");
        http.response(200, returned, "UTF-8");
	}
}

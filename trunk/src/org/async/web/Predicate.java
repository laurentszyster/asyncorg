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

import org.simple.Fun;
import org.protocols.JSON;
import org.protocols.JSONR;
import org.async.chat.GuardCollector;
import org.async.chat.StringCollector;
import org.async.sql.Metabase;
import org.async.web.HttpServer;

/*
 * A class to decouple storage, semantics and access control from the 
 * application's data processing.
 * 
 * Predicate handles GET and POST requests routed by Meta for a given predicate, 
 * enforce access control based on the identity and rights of the HTTP actor, 
 * then either return the selected JSON objects or update the metabase with the
 * collected object. Note that Predicate also enforces a limit on POST request 
 * bodies, set by default to the size of its channel input buffer (ie: 16KB).
 * 
 * Predicate's implementation enables developers to focus on application features
 * rather than figuring out how much Object Relational Mapping can suck, which 
 * simple URL routing is enough and how role based access control fits in a 
 * contextual resource description framework.
 * 
 * Using predicates buys you a linear and risk-free development cycle for 
 * audited, state-full and persistent semantic web applications.
 * 
 */
public class Predicate implements HttpServer.Controller {
	/**
	 * What an application of <code>Predicate</code> handles.
	 * 
	 * Applications may alter the object and relation of a statement, to further 
	 * validate a resource description and record facts about it that can be used 
	 * to establish functional relations between resources.
	 *  
	 * Note that the <code>relation</code> map is <code>null</code> by default,
	 * which translates in no relation update.
	 */
	public static final class Statement extends Metabase.Statement {
		protected Statement (HttpServer.Actor http) {
			super (http.digest, http.about[0], http.about[2]);
			object = http.body().toString();
		}
	}
	protected static final class Pass implements Fun {
		public final Object apply (Object input) {
			return null;
		}
	}
	public static final Fun PASS = new Pass();
	protected static final class ApplyJSON implements Fun {
		private Fun _fun = PASS;
		public ApplyJSON(Fun fun) {
			_fun = fun;
		}
		public final Object apply (Object input) throws Throwable {
			Statement statement = (Statement) input; 
			statement.object = JSON.decode((String) statement.object);
			Object response = _fun.apply(statement);
			statement.object = JSON.encode(statement.object);
			return response;
		}
	}
	public static final Fun applyJSON (Fun fun) {
		return new ApplyJSON(fun);
	}
	protected static final class ApplyJSONR implements Fun {
		private Fun _fun = PASS;
		private JSONR.Type _type;
		public ApplyJSONR(Fun fun, JSONR.Type type) {
			_fun = fun;
			_type = type;
		}
		public final Object apply (Object input) throws Throwable {
			Statement statement = (Statement) input; 
			statement.object = _type.eval((String) statement.object);
			Object response = _fun.apply(statement);
			statement.object = JSON.encode(statement.object);
			return response;
		}
	}
	public static final Fun applyJSONR (Fun fun, JSONR.Type type) {
		return new ApplyJSONR(fun, type);
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
	public final boolean handleRequest(HttpServer.Actor http) throws Throwable {
	    String method = http.method();
        String subject = http.about[1];
        String predicate = http.about[2];
        String context = http.about[3];
        Metabase.Predicate column = _metabase.predicates.get(predicate);
        if (column == null) {
            http.error(404); // Not Found
            return false;
        }
	    if (method.equals("GET")) {
	    	String object = "null";
	        try {
		    	if (context == null) { // GET subject/predicate
		    		if (http.rights.contains(predicate)) {
		    			object = column.buffer(
	    					subject, new StringBuilder()
	    					).toString();
		    		} else {
			            http.error(403); // Forbidden
			            return false;
		    		}
		    	} else if (
					context.equals(http.identity) || http.rights.contains(predicate) 
					) { // GET subject/predicate/context
	                object = column.select(subject, context);
	            } else {
		            http.error(403); // Forbidden
		            return false;
	            }
	        } catch (Exception e) {
	        	http.channel().log(e);
	            http.error(500); // Server Error
	            return false;
	        }
	        http.set("Cache-control", "no-cache");
	        http.set("Content-Type", "text/javascript; charset=UTF-8");
	        http.reply(200, object, "UTF-8");
		    return false;
	    } else if (method.equals("POST")) {
	    	if (context == null) {
	    		http.error(404); // Not found
	    		return false;
	    	} 
            if (!(http.identity.equals(subject))) {
                http.error(403); // Forbidden
                return false;
            }
            http.collect(new GuardCollector(
				new StringCollector("UTF-8"), 
				(_limit == 0) ? http.channel().bufferInSize(): _limit
				));
    		return false;
	    } else if (method.equals("PUT")) {
	    	if (context == null) {
	    		http.error(404); // Not found
	    		return false;
	    	} 
            if (!http.rights.contains(predicate)) {
                http.error(403); // Forbidden
                return false;
            }
            http.collect(new GuardCollector(
				new StringCollector("UTF-8"), 
				(_limit == 0) ? http.channel().bufferInSize(): _limit
				));
    		return false;
	    } else {
	        http.error(501); // Not implemented
	    }
		return false;
	}
	public final void handleBody(HttpServer.Actor http) throws Throwable {
		String returned;
        Metabase.Predicate column = _metabase.predicates.get(http.about[2]);
		Statement statement = new Statement (http);
    	try {
    		returned = (String) _function.apply(statement); // .toString() ?
    	} catch (Throwable t) {
        	http.channel().log(t);
	        http.error(500);
	        return;
    	}
	    try {
	        if (http.method().equals("PUT")) {
	            column.insert(statement);
	        } else {
	        	column.update(statement);
	        }
	    } catch (Exception e) {
	        // TODO: this *may* also be a major failure (disk crash, etc ...)
        	http.channel().log(e);
	        http.error(500);
	        return;
	    }
	    http.set("Cache-control", "no-cache");
	    http.set("Content-Type", "text/javascript; charset=UTF-8");
        http.reply(200, returned, "UTF-8");
	}
}

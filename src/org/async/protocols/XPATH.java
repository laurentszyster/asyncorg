/* Copyright (C) 2006-2008 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU Lesser General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/lesser.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.async.protocols;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Locale;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;

import com.jclark.xml.parse.ApplicationException;
import com.jclark.xml.parse.CharacterDataEvent;
import com.jclark.xml.parse.DocumentParser;
import com.jclark.xml.parse.EndElementEvent;
import com.jclark.xml.parse.EntityManagerImpl;
import com.jclark.xml.parse.OpenEntity;
import com.jclark.xml.parse.StartElementEvent;
import com.jclark.xml.parse.base.ApplicationImpl;

import org.async.protocols.XML;

/**
 * A minimal subset of XPATH to support an event parser for XML data, plus path 
 * outlining and collection convenience, just enough to develop fast and lean 
 * XML data pipelining processors.
 * 
 * XPATH feeds suite well state-full applications with asynchronous I/O, as they
 * enable an asynchronous network peer to produce chunks of processed data. 
 * 
 */
public class XPATH {
	public static final class Expression {
		public HashMap<String,String> qualifiers;
		public int length;
		public String[] paths;
		public String[] values;
		public Expression (HashMap<String,String> qualifiers) {
			length = qualifiers.size();
			paths = new String[length];
			qualifiers.keySet().toArray(paths);
			Arrays.sort(paths);
			values = new String[length];
			for (int i=0; i<length; i++) {
				values[i] = qualifiers.get(paths[i]);
			}
		}
		public final String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			sb.append(paths[0]);
			if (values[0] == null || values[0].equals("")) {
				sb.append("=\"");
				sb.append(values[0]);
				sb.append("\"");
			}
			for (int i=1; i<length; i++) {
				sb.append(" and ");
				sb.append(paths[i]);
				if (values[i] == null || values[i].equals("")) {
					sb.append("=\"");
					sb.append(values[i]);
					sb.append("\"");
				}
			}
			sb.append("]");
			return sb.toString();
		}
		public final boolean eval (String[] values) {
			for (int i=0; i<length; i++) {
				if (values[i] == null || !values[i].equals(this.values[i])) {
					return false;
				}
			}
			return true;
		}
	}
    public static abstract class Feed {
    	protected String path;
    	protected StringWriter cdata = null;
    	public String[] attributes = null;
    	public Feed (String path) {
    		this.path = path;
    	}
    	public abstract void handleStart ();
    	public abstract void handleAttribute (String name, String value);
    	public abstract void handleText (String text);
    	public abstract void handleEnd (Feed[] stack, int depth);
    }
    protected static class FeedNull extends Feed {
    	public FeedNull (String path) {
    		super(path);
    	}
    	public final void handleStart () {};
    	public final void handleAttribute (String name, String value) {};
    	public final void handleText (String text) {};
    	public final void handleEnd (Feed[] stack, int depth) {};
    }
    public static abstract class FeedBase extends Feed {
    	public HashMap<String,Integer> paths = null;
    	public String[] data;
    	public String[] qualifiers;
    	public FeedBase (String path, String[] paths) {
    		super(path);
    		cdata = new StringWriter();
    		this.path = path;
    		this.paths = new HashMap();
    		data = new String[paths.length];
    		for (int i=0; i<data.length; i++) {
    			this.paths.put(paths[i], i);
    		}
    	}
    	public final void handleStart () {
    		for (int i=0, L=data.length; i<L; i++) {
    			data[i] = null;
    		}
    	};
    	public final void handleAttribute (String name, String value) {
    		data[paths.get(name)] = value;
    	};
    	public final void handleText (String text) {
    		data[paths.get("")] = text;
    	};
    }
    protected static final class FeedRelative extends Feed {
    	public FeedBase base;
    	public FeedRelative (String path, FeedBase base) {
    		super(path);
    		cdata = new StringWriter();
    		this.base = base;
    	}
    	public final void handleStart () {}
    	public final void handleAttribute (String name, String value) {
    		String relative = path + "/@" + name;
    		if (base.paths.containsKey(relative)) {
    			base.handleAttribute(relative, value);
    		}
    	}
    	public final void handleText (String text) {
    		if (base.paths.containsKey(path)) {
    			base.handleAttribute(path, text);
    		}
    	}
    	public final void handleEnd (Feed[] stack, int depth) {}
    }
    public static final class FeedParser extends ApplicationImpl {
        protected HashMap<String,Feed> feeds = new HashMap();
        protected int depth = -1;
        protected Feed[] stack = new Feed[1024];
        protected String[] paths = new String[1024];
        public FeedParser (HashMap<String,Feed> feeds) {
            this.feeds = feeds;
            };
        public final void startElement(StartElementEvent event) {
            String name = event.getName();
            String path;
            if (depth < 0) {
            	path = "";
            } else if (depth < 1024){
            	path = paths[depth] + "/" + name;
            } else {
            	throw new RuntimeException("XML FeedParser stack overflow");
            }
        	Feed feed = feeds.get(path);
            depth++;
        	if (feed == null) {
        		feed = new FeedNull(path);
        		feeds.put(path, feed);
        	}
        	stack[depth] = feed;
        	paths[depth] = path;
        	feed.handleStart();
        	if (feed.attributes == null) {
        		return;
        	}
            if (feed.attributes != null) {
            	int L = event.getAttributeCount();
                for (int i=0; i < L; i++) { 
                	feed.handleAttribute(
                		event.getAttributeValue(i), 
                        event.getAttributeValue(i)
                        );
                }
            }
        }
        public final void characterData (CharacterDataEvent event) 
        throws IOException {
        	if (stack[depth] == null) {
        		return;
        	}
        	StringWriter cdata = stack[depth].cdata;
        	if (cdata != null) {
        		event.writeChars(stack[depth].cdata);
        	}
        }
        public final void endElement(EndElementEvent event) {
        	if (stack[depth] == null) {
            	depth--;
        		return;
        	}
        	Feed feed = stack[depth];
        	StringWriter cdata = feed.cdata;
        	if (cdata != null) {
	            cdata.flush();
	            cdata = null;
	        	feed.handleText(cdata.toString());
        	}
        	feed.handleEnd(stack, depth);
        	stack[depth] = null;
        	depth--;
        }
    }
    public static final class FeedParserNS extends ApplicationImpl {
        protected HashMap<String,String> ns = new HashMap();
        protected HashMap<String,String> prefixes = new HashMap();
        protected HashMap<String,Feed> feeds = new HashMap();
        protected int depth = -1;
        protected Feed[] stack = new Feed[1024];
        protected String[] paths = new String[1024];
        public FeedParserNS (HashMap<String,Feed> feeds) {
            this.feeds = feeds;
            };
        public final void startElement(StartElementEvent event) {
            String name = XML.fqn(event.getName(), prefixes);
            String path;
            if (depth < 0) {
            	path = "";
            } else if (depth < 1024){
            	path = paths[depth] + "/" + name;
            } else {
            	throw new RuntimeException("XML FeedParser stack overflow");
            }
        	Feed feed = feeds.get(path);
            depth++;
        	if (feed == null) {
        		feed = new FeedNull(path);
        		feeds.put(path, feed);
        	}
        	stack[depth] = feed;
        	feed.path = paths[depth] = path;
        	feed.handleStart();
            String[] attributeNames = XML.xmlns (event, ns, prefixes);
            if (!(attributeNames == null || feed.attributes == null)) {
                for (int i=0; i < attributeNames.length; i++) { 
                    if (attributeNames[i] != null) {
                    	feed.handleAttribute(
                    		XML.fqn(attributeNames[i], prefixes), 
                            event.getAttributeValue(i)
                            );
                    }
                }
            }
        }
        public final void characterData (CharacterDataEvent event) 
        throws IOException {
        	if (stack[depth] == null) {
        		return;
        	}
        	StringWriter cdata = stack[depth].cdata;
        	if (cdata != null) {
        		event.writeChars(stack[depth].cdata);
        	}
        }
        public final void endElement(EndElementEvent event) {
        	if (stack[depth] == null) {
            	depth--;
        		return;
        	}
        	Feed feed = stack[depth];
        	StringWriter cdata = feed.cdata;
        	if (cdata != null) {
	            cdata.flush();
	            cdata = null;
	        	feed.handleText(cdata.toString());
        	}
        	feed.handleEnd(stack, depth);
        	stack[depth] = null;
        	depth--;
        }
    }
    public static final void feed (
		HashMap<String,Feed> feeds, InputStream is, String path, URL baseURL
		) throws XML.Error, IOException {
    	FeedParser fp = new FeedParser(feeds);
        try {
            DocumentParser.parse(new OpenEntity(
                is, path, baseURL
                ), new EntityManagerImpl(), fp, Locale.US);
        } catch (ApplicationException e) {
        	throw new XML.Error(e);
        }
    }
    public static final void feed (HashMap<String,Feed> feeds, File file) 
    throws XML.Error, IOException {
    	feed(feeds, new FileInputStream(file), file.getAbsolutePath(), file.toURL());
    }
}

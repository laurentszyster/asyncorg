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

import java.net.URL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.HashSet;
import java.util.NoSuchElementException;

import com.jclark.xml.parse.ProcessingInstructionEvent;
import com.jclark.xml.parse.StartElementEvent;
import com.jclark.xml.parse.EndElementEvent;
import com.jclark.xml.parse.CharacterDataEvent;
import com.jclark.xml.parse.DocumentParser;
import com.jclark.xml.parse.OpenEntity;
import com.jclark.xml.parse.EntityManagerImpl;
import com.jclark.xml.parse.base.ApplicationImpl;
import com.jclark.xml.parse.ApplicationException;
import com.jclark.xml.output.UTF8XMLWriter;
import com.jclark.xml.sax.ReaderInputStream;

import org.async.simple.Bytes;
import org.async.simple.Objects;
import org.async.protocols.JSON;

/**
 * An enhanced port of Greg Stein's <a 
 * href="http://www.lyra.org/greg/python/qp_xml.py"
 * >qp_xml.py</a> for Java, enhancing James Clark's <a 
 * href="http://www.jclark.com/xml/xp/index.html"
 * >XP</a> to build an element tree model more practical than the W3C's DOM
 * for XML data.
 * 
 * It also provides a convenient API to develop extensible interpreters and
 * comes with a convenience to map XML string to JSON arrays and objects. 
 *
 * @h3 Simple Object Model
 *
 * @p The document object model made of <code>XML.Document</code> and 
 * <code>XML.Element</code> is the pythonic element tree introduced by 
 * Greg Stein.
 * 
 * @h3 Agile Interpreters
 *
 * @p The <code>XML.Element</code> class supports the reuse of derived 
 * element in the development of agile XML interpreters.
 * 
 * @h3 From XML To JSON
 * 
 * @p ... <code>XML.Regular</code>
 *
 */
public final class XML {

	public final static String fqn (
		String name, HashMap<String,String> prefixes
		) {
	    int colon = name.indexOf(':');
	    if (colon > -1) {
	        String prefix = name.substring(0, colon);
	        if (prefix.equals(_xml)) {
	            return name.substring(colon+1);
	        } else if (prefixes.containsKey(prefix)) {
	            return (
	                ((String)prefixes.get(prefix)) 
	                + ' ' + name.substring(colon+1)
	                );
	        }
	        throw new RuntimeException (_namespace_prefix_not_found);
	    } else if (prefixes.containsKey(_no_prefix)) {
	        return (
	            prefixes.get(_no_prefix) + ' ' + name.substring(colon+1)
	            );
	    }
	    return name;
	}
	public static final String[] xmlns (
		StartElementEvent event, 
		HashMap<String,String> ns,
		HashMap<String,String> prefixes
		) {
	    String name;
	    String[] attributeNames = null;
	    int L = event.getAttributeCount();
	    int A = 0;
	    if (L > 0) {
	        attributeNames = new String[L]; 
	        for (int i=0; i<L; i++) {
	            name = event.getAttributeName(i);
	            if (name.equals(_xmlns)) {
	                ns.put(event.getAttributeValue(i), _no_prefix);
	                prefixes.put(_no_prefix, event.getAttributeValue(i));
	            } else if (name.startsWith(_xmlns_colon)) {
	                ns.put(event.getAttributeValue(i), name.substring(6));
	                prefixes.put(
	                    name.substring(6), event.getAttributeValue(i)
	                    );
	            } else {
	                attributeNames[i] = name; 
	                A++;
	            }
	        }
	    }
	    return (A > 0) ? attributeNames: null;
	}
    
    public static final String localName(String name) {
        int local = name.indexOf(' ');
        if (local > -1)
            return name.substring(local+1);
        else
            return name;
    }
    
    public static final String MIME_TYPE = "text/xml";

    protected static final String _utf8 = "UTF-8";
    
    /**
     * An error class derived from <code>Exception</code>, throwed
     * by the <code>XP</code> parser when the XML 1.0 processed is 
     * not-well formed, a namespace prefix is not declared or an
     * exception is raised by its application.
     */
    public static class Error extends Exception {
        /**
         * instantiate a new exception with the given message.
         * 
         * @param message the error(s information <code>String</code>
         */
        public Error (Exception e) {super(e);}
    };
    
    /**
     * A base class for all nodes in an XML element tree, with only five
     * properties, just enough to fully represent an XML string (without
     * comments) as a compact tree of Java objects with the smallest
     * memory footprint.
     */
    public static class Element {
        /**
         * The name of this element.
         * 
         * @p Note that qualified names are represented as a string joining 
         * the name space and the local name with a single space character, 
         * while unqualified names (a.k.a tags) are represented as a string 
         * without white spaces.
         */
        public String name = null;
        /**
         * A pointer to this element's parent.
         */
        public Element parent = null;
        /**
         * The first CDATA (ie: text) after this element's opening tag
         * and before this element's first child.
         */
        public String first = null;
        /**
         * The CDATA after this element's closing tag and before this 
         * element's next sibling.
         */
        public String follow = null;
        /**
         * A list of this element's children. 
         */
        public ArrayList<Element> children = null;
        /**
         * A map of this element's fully qualified attributes.
         */
        public HashMap<String, String> attributes = null;
        /**
         * instantiate a new empty <code>Element</code> with name.
         * 
         * @param name the fully qualified name of this element
         * 
         * @test return (new XML.Element("tag")).toString().equals(
         *    "<tag></tag>"
         *    );
         */
        public Element (String name) {
            this.name = name;
        }
        /**
         * instantiate a new empty <code>Element</code> with name and 
         * attributes.
         * 
         * @param name the fully qualified name of this element
         * @param attributes a <code>HashMap</code> of named attributes
         * 
         * @test importPackage(Packages.org.less4j); 
         *var e = new XML.Element("tag", Simple.dict(["name", "value"]));
         *return e.toString().equals(
         *    "<tag name=\"value\"></tag>"
         *    );
         */
        public Element (String name, HashMap attributes) {
            this.name = name;
            this.attributes = attributes;
        }
        /**
         * instantiate a new child-less <code>Element</code> with attributes,
         * first and following CDATA.
         * 
         * @param name the fully qualified name of this element
         * @param attributes as an even array of names and values
         * @param first text after the opening tag 
         * @param follow text after the closing tag
         * 
         * @pre new XML.Element("a", new String[]{
         *    "href", "#", "name", "top"
         *    }, "go to top", "\r\n");
         *    
         * @div This constructor is usefull when building element trees
         * from scratch.
         * 
         * @test importPackage(Packages.org.less4j); 
         *var e = new XML.Element("tag", ["name", "value"], "first", "follow");
         *return e.toString().equals(
         *    "<tag name=\"value\">first</tag>follow"
         *    );
         */
        public Element (
            String name, String[] attributes, String first, String follow
            ) {
            this.name = name;
            if (attributes != null)
                this.attributes = (HashMap<String, String>) Objects.update(
                    new HashMap<String, String>(), (Object[]) attributes
                    );
            this.first = first;
            this.follow = follow;
        }
        /**
         * ...
         */
        public Element newElement (String name) {
            return new Element(name);
        }
        /**
         * Add a child element, creates the <code>children</code> array list
         * if it does not exist yet.
         * 
         * @param child to append
         * @return the child appended.
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild(new XML.Element("child"));
         *return e.toString().equals(
         *    "<tag><child></child></tag>"
         *    );
         */
        public XML.Element addChild (Element child) {
            if (children == null) 
                children = new ArrayList();
            children.add(child);
            child.parent = this;
            return child;
        }
        /**
         * Add a new named and empty child element, creates the 
         * <code>children</code> array list if it does not exist yet.
         * 
         * @param name of the child to append
         * @return the child appended.
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("child");
         *return e.toString().equals(
         *    "<tag><child></child></tag>"
         *    );
         */
        public XML.Element addChild (String name) {
            return addChild(newElement(name));
        }
        /**
         * Add a new named child element with text, creates the 
         * <code>children</code> array list if it does not exist yet.
         * 
         * @param name of the child to append
         * @param first CDATA after the child opening tag 
         * @return the child appended.
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("child", "first");
         *return e.toString().equals(
         *    "<tag><child>first</child></tag>"
         *    );
         */
        public XML.Element addChild (String name, String first) {
            XML.Element child = addChild(newElement(name));
            child.first = first;
            return child;
        }
        /**
         * Add a new named child element with attributes, creates the 
         * <code>children</code> array list if it does not exist yet.
         * 
         * @param name of the child to append
         * @param attributes of the child element 
         * @return the child appended.
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("child", ["name", "value"]);
         *return e.toString().equals(
         *    "<tag><child name=\"value\"></child></tag>"
         *    );
         */
        public XML.Element addChild (String name, String[] attribute) {
        	Element e = newElement(name);
        	e.attributes = Objects.dict((Object[])attribute);
            return addChild(e);
        }
        /**
         * Add a new named child element, creates the <code>children</code> 
         * array list if it does not exist yet.
         * 
         * @param name of the child to append
         * @param attributes of the child element 
         * @param first CDATA after the child opening tag 
         * @param follow CDATA after the child closing tag 
         * @return the child appended.
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("child", ["name", "value"], "first", "follow");
         *return e.toString().equals(
         *    "<tag><child name=\"value\">first</child>follow</tag>"
         *    );
         */
        public XML.Element addChild (
            String name, String[] attrs, String first, String follow
            ) {
            XML.Element child = addChild(newElement(name));
            child.attributes = (HashMap) Objects.update(
        		new HashMap(), (Object[])attrs
        		);
            child.first = first;
            child.follow = follow;
            return child;
        }
        /**
         * ...
         * 
         * @param cdata
         * 
         * @test var e = new XML.Element("tag");
         *e.addCdata("first");
         *e.addChild("child");
         *e.addCdata("follow ...");
         *e.addCdata(" more");
         *return e.toString().equals(
         *    "<tag>first<child></child>follow ... more</tag>"
         *); 
         */
        public void addCdata (String cdata) {
            if (children == null || children.size() == 0) {
                if (first == null)
                    first = cdata;
                else
                    first = first + cdata;
            } else {
                Element child = getChild(children.size()-1);

                if (child.follow == null)
                    child.follow = cdata;
                else
                    child.follow = child.follow + cdata;
            }
        }
        /**
         * Returns the element's local name, i.e.: without name space.
         * 
         * @return the element's unqualified name
         * 
         * @test var e = new XML.Element("urn:NameSpace tag");
         *return e.getLocalName().equals("tag");
         * 
         */
        public String getLocalName () {
            int local = name.indexOf(' ');
            if (local > -1)
                return name.substring(local+1);
            else
                return name;
        }
        /**
         * A convenience to access <code>XML.Element</code> children by index. 
         * 
         * @param index of the child in this element's children.
         * @return an <code>XML.Element</code> of null
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("zero");
         *e.addChild("one");
         *e.addChild("two");
         *return (
         *    e.getChild(1).toString().equals("<one></one>") &&
         *    e.getChild(3) == null
         *    );
         */
        public Element getChild (int index) {
            if (children == null)
                return null;
            else
                return (Element) children.get(index);
        }
        /**
         * A convenience to access a first <code>XML.Element</code>'s child 
         * by name. 
         * 
         * @param name of the child.
         * @return an <code>XML.Element</code> or null
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("zero");
         *e.addChild("one");
         *e.addChild("two");
         *return (
         *    e.getChild("one").toString().equals("<one></one>") &&
         *    e.getChild("three") == null
         *    );
         */
        public Element getChild (String name) {
            Element child;
            if (children != null) for (int i=0, L=children.size(); i<L; i++) {
                child = (Element) children.get(i);
                if (child.name.equals(name)) 
                    return child;
            }
            return null;
        }
        
        /**
         * A convenience to access the last child of an 
         * <code>XML.Element</code>, if any. 
         * 
         * @return an <code>XML.Element</code> or null
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("zero");
         *e.addChild("one");
         *e.addChild("two");
         *return (
         *    e.getLastChild().toString().equals("<two></two>") &&
         *    e.getChild(0).getLastChild() == null
         *    );
         */
        public Element getLastChild () {
            if (children == null)
                return null;
            
            return getChild(children.size()-1);
        }
        
        protected class ChildrenIterator implements Iterator<Element> {
            private Iterator _children = null;
            private Element _next = null;
            private HashSet _names = null;
            public ChildrenIterator (Element element, HashSet names) {
                if (element.children == null)
                    return;
                
                _children = element.children.iterator();
                _names = names;
                try {
                    next();
                } catch (NoSuchElementException e) {
                    _next = null;
                }
            };
            public boolean hasNext() {
                return (_next == null);
            }
            public Element next() {
                Element result = _next;
                while (_children.hasNext()) {
                    _next = (Element) _children.next();
                    if (_names.contains(_next.name))
                        return result;
                }
                throw new NoSuchElementException();
            }
            public void remove() {} // wtf?
        }

        /**
         * A convenience to iterate through named children.
         * 
         * @param names set of the children to iterate.
         * @return an iterator of <code>XML.Element</code>
         * 
         * @test var e = new XML.Element("tag");
         *e.addChild("zero");
         *e.addChild("one");
         *e.addChild("two");
         *var named = e.getChildren(
         *    Simple.set(["zero", "two", "four"])
         *    );
         *return (
         *    named.next().toString().equals("<zero></zero>") &&
         *    named.next().toString().equals("<two></two>") &&
         *    named.hasNext() == false
         *    );
         */
        public Iterator<Element> getChildren (HashSet names) {
            return new ChildrenIterator(this, names);
        }
        /**
         * A convenience to access an attribute <code>String</code> value
         * by name, returns null if the named attribute does not exists. 
         * 
         * @param name of the attribute
         * @return the value of the named attribute as a string
         * 
         * @test var e = new XML.Element("tag", Simple.dict(["name", "value"]));
         *return (
         *    e.getAttribute("name").equals("value") &&
         *    e.getAttribute("whatever") == null 
         *    );
         */
        public String getAttribute (String name) {
            if (attributes == null)
                return null;
            else
                return (String) attributes.get(name);
        }
        /**
         * Set the named attribute's value, creating the attributes' map if
         * it does not exist yet.
         * 
         * @param name of the attribute
         * @param value of the named attribute
         * 
         * @test var e = new XML.Element("tag");
         *e.setAttribute("name", "value");
         *return e.getAttribute("name").equals("value");
         */
        public void setAttribute (String name, String value) {
            if (attributes == null) 
                attributes = new HashMap();
            attributes.put(name, value);
        }
        /**
         * Break all circular reference for this element and all its children.
         * 
         * @p ...
         */
        public void collect () {
            parent = null;
            if (children != null) {
                Iterator child = children.iterator();
                while (child.hasNext()) {
                    ((Element) child.next()).collect();
                }
            }
        }
        /**
         * Remove this element and all its children from the tree, folding
         * up the following CDATA and collecting circular references in its
         * branch.
         * 
         * @test var doc = new XML.Document();
         *doc.parse(
         *    "<tag><one>first</one>follow<two></two><three></three></tag>"
         *    );
         *doc.root.getChild("one").remove();
         *return doc.root.toString().equals(
         *    "<tag>follow<two></two><three></three></tag>"
         *    );
         */
        public void remove () {
            int index = parent.children.indexOf(this);
            if (index == 0) {
                if (parent.first == null)
                    parent.first = follow;
                else
                    parent.first = parent.first + follow; 
            } else {
                XML.Element previous = (
                    (Element) parent.children.get(index-1)
                    );
                if (previous.follow == null)
                    previous.follow = follow;
                else
                    previous.follow = previous.follow + follow;
            }
            parent.children.remove(index);
            collect();
        }
        public void open (Document doc) {
            ;
        }
        /**
         * This is a method called by <code>QP</code> when an element has 
         * been parsed without errors.
         * 
         * @param doc the Document currently parsed
         * 
         * @div The purpose of this interface is to allow derived classes of
         * <code>Element</code> to implement "Object Oriented Pull Parsers"
         * (OOPP). The benefit of this pattern is to avoid the need to walk
         * the element tree and instead process elements as they are
         * validated by the parser, using this simple DOM as an abstract
         * syntax tree.
         * 
         */
        public void close (Document doc) {
            ;
        }
        /**
         * Serialize an XML element as an UTF-8 encoded byte string using 
         * the given map of namespace prefixes.
         * 
         * @param ns the map of namespace prefixes
         * @return an UTF-8 encoded byte string
         */
        public byte[] encodeUTF8(Map ns) {
            return XML.encodeUTF8(this, ns);
        }
        /**
         * Serialize an XML element as a UNICODE string, eventually generates 
         * namespace prefixes.
         * 
         * @return an XML string
         */
        public String toString() {
            return Bytes.decode(XML.encodeUTF8(this, new HashMap()), _utf8);
        }
    }
    
    /**
     * A class with just enough properties to fully represent an XML
     * document with name spaces and processing instructions. 
     */
    public static class Document {
        /**
         * This document's root <code>XML.Element</code>.
         */
        public Element root = null;
        /**
         * A <code>HashMap</code> of processing instructions, mapping
         * instruction names to lists of processing parameter strings.
         */
        public HashMap<String,ArrayList<String>> pi;
        /**
         * A map of prefix strings to namespace strings.
         */
        public HashMap<String,String> ns;
        /**
         * instantiate a new document without a root element.
         * 
         * @param doctype ...
         */
        public Document () {
            pi = new HashMap();
            ns = new HashMap();
        } 
        /**
         * instantiate a new document with a root element named after the
         * document type.
         * 
         * @param doctype ...
         */
        public Document (Element root) {
            pi = new HashMap();
            ns = new HashMap();
            this.root = root;
        } 
        /**
         * Try to parse an XML <code>InputStream</code> with path and base URL 
         * using the extension <code>types</code>. 
         * 
         * @param is InputStream to parse
         * @param path locating the parsed entity
         * @param baseURL for external entity resolution
         * @param types to use as extensions
         * @throws Error
         * @throws IOException
         */
        public void read (
    		InputStream is, String path, URL baseURL, Map types, Element type
    		) throws Error, IOException {
            QuickParser qp = new QuickParser(this, types, type);
            try {
	            DocumentParser.parse(new OpenEntity(
	                is, path, baseURL
	                ), new EntityManagerImpl(), qp, Locale.US);
            } catch (ApplicationException e) {
            	throw new Error(e);
            }
        }
        /**
         * Try to parse an XML <code>file</code> using the extension 
         * <code>types</code>. 
         * 
         * @param file to parse
         * @param types to use as extensions
         * @throws Error
         * @throws IOException
         */
        public void read(File file, Map types, Element type) 
        throws Error, IOException {
            read (
                new FileInputStream(file), 
                file.getAbsolutePath(), 
                file.toURL(),
                types,
                type
                );
        }
        /**
         * Try to parse an XML <code>file</code> using the extension 
         * <code>types</code>. 
         * 
         * @param file to parse
         * @throws Error
         * @throws IOException
         */
        public void read(File file) throws Error, IOException {
            read(
                new FileInputStream(file), 
                file.getAbsolutePath(), 
                file.toURL(), 
                null,
                null
                );
        }
        public static final String PROLOGUE = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        public void write (OutputStream os, String prologue) 
        throws IOException {
            UTF8XMLWriter writer = new UTF8XMLWriter(os);
            writer.markup(prologue);
            // TODO: processing instructions
            writeUTF8(writer, root, ns);
            writer.flush();
        }
        public void write (OutputStream os) throws IOException {
            write(os, PROLOGUE);
        }
        public void write (File file, String prologue) throws IOException {
            FileOutputStream os = new FileOutputStream(file);
            write(os, prologue);
            os.close();
        }
        public void write (File file) throws IOException {
            write(file, PROLOGUE);
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @param path locating the parsed entity
         * @param baseURL for external entity resolution
         * @param types to use as extensions
         * @throws XML or I/O exceptions
         */
        public void parse(
    		String string, String path, URL baseURL, Map types, Element type
    		) throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                path, baseURL, types, type
                );
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @param types to use as extensions
         * @throws XML or I/O exceptions
         */
        public void parse(String string, Map types, Element type) 
        throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                "", null, types, type
                );
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @throws XML or I/O exceptions
         */
        public void parse(String string) 
        throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                "", null, null, null
                );
        }
        /**
         * 
         * @return
         */
        public String toString () {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                this.write(os, "");
                return os.toString(Bytes.UTF8); // it's always an UTF8 writer ,-)
            } catch (Exception e){ 
            	throw new RuntimeException(e); // checked exceptions suck ...
            }
        }
    }    
    // The Quick Parser, in Java (a "little bit" slower).
    private static final Element _ELEMENT = new Element("");
    private static final String _xml = "xml";
    private static final String _xmlns = "xmlns";
    private static final String _xmlns_colon = "xmlns:";
    private static final String _no_prefix = "";
    private static final String 
    _namespace_prefix_not_found = "namespace prefix not found";
    public static class QuickParser extends ApplicationImpl {
        protected Document doc;
        protected Element type;
        protected Map<String,Element> types = null;
        protected HashMap<String,String> prefixes = new HashMap();
        protected Element curr = null;
        public QuickParser (
    		Document doc, Map<String,Element> types, Element type
    		) {
            this.doc = doc;
            this.types = types;
            this.type = (type == null) ? _ELEMENT: type;
            };
        public final void processingInstruction(ProcessingInstructionEvent event) {
            String name = event.getName();
            if (doc.pi.containsKey(name))
                doc.pi.get(name).add(event.getInstruction());
            else {
                ArrayList<String> pi = new ArrayList<String>();
                pi.add(event.getInstruction());
                doc.pi.put(name, pi);
            }
        }
        public final void startElement(StartElementEvent event) {
            String[] attributeNames = xmlns (event, doc.ns, prefixes);
            String name = fqn(event.getName(), prefixes);
            Element e;
            // hook element instantiation here
            if (types != null && types.containsKey(name)) {
                e = types.get(name).newElement(name);
            } else {
                e = type.newElement(name);
            }
            e.parent = curr;
            if (attributeNames != null) {
                for (int i=0; i < attributeNames.length; i++) { 
                    if (attributeNames[i] != null) {
                        e.setAttribute( // hook attribute setters here ;-)
                            fqn(attributeNames[i], prefixes), 
                            event.getAttributeValue(i)
                            );
                    }
                }
            }
            if (curr == null) {
                doc.root = e;
            } else {
                curr.addChild(e);
            }
            curr = e;
            e.open(doc); // hook element initialization here
        }
        public final void endElement(EndElementEvent event) {
            Element parent = curr.parent;
            curr.close(doc);
            curr = parent;
        }
        public final void characterData (CharacterDataEvent event) 
        throws IOException {
            StringWriter sw = new StringWriter();
            event.writeChars(sw);
            sw.flush();
            curr.addCdata(sw.toString()); // hook CDATA setter here
        }
    }
    
    /**
     * Map any XML tag soup into a tree of JSON types that is as flat as 
     * possible, using local names only as identifiers.
     * 
     * @div This class extends XML.Element with a JSON object and overrides
     * the valid method to map the XML names and structures to a flatter,
     * and less ambiguous object model made of JSON objects, arrays and
     * strings. Its purpose is to avoid the complications of DOM walks
     * through XML data and instead provide practical JSON types that can
     * be validated by a regular JSON expression.
     * 
     * @pre import org.less4j.XML;
     *import org.less4j.JSON;
     *import org.less4j.JSONR;
     * 
     *public class MyXMLObject extends XML.Regular {
     *    private static final JSONR.Type model = JSONR.compile(
     *        "{\"item\": \"[0-9]{13}\", \"quantity\": [320.01]}"
     *        );
     *    public validate (XML.Tree application) {
     *        try {
     *            model.validate(this.json);
     *            // ... 
     *            // handle this element's data for the application
     *            // ... 
     *        } catch (JSONR.Error e) {
     *            // ... invalid JSON input, handle the error ...
     *            JSON.pprint(System.err, this.json)
     *        }
     *        this.delete(); // eventually release some memory ... 
     *    }
     *}
     * 
     * @div Programming an XML data processor becomes a lot less tedious
     * and error-prone ...
     * 
     */
    public static class Regular extends Element {
        public static Element TYPE = new Regular(null, null);
        public JSON.Object json = null;
        public Regular (String name) {
            super(name);
        }
        public Regular (String name, HashMap attributes) {
            super(name, attributes);
        }
        public Regular (
    		String name, String[] attributes, String first, String follow
    		) {
            super(name, attributes, first, follow);
        }
        public final Element newElement (String name) {
            return new Regular(name);
        }
        protected final void update(
            String name, Object value, Regular container
            ) {
            JSON.Object map = container.json; 
            if (map == null) {
                map = new JSON.Object();
                container.json = map;
                map.put(name, value);
            } else if (map.containsKey(name)) {
                Object o = map.get(name);
                if (o instanceof JSON.Array) {
                    ((JSON.Array) o).add(value);
                } else {
                    JSON.Array list = new JSON.Array();
                    list.add(o);
                    list.add(value);
                    map.put(name, list);
                }
            } else
                map.put(name, value);
        }
        private static final void _putAllLocal(HashMap attributes, JSON.Object json) {
        	String name;
        	Iterator names = attributes.keySet().iterator();
        	while (names.hasNext()) {
        		name = (String) names.next();
        		json.put("@" + localName(name), attributes.get(name));
        	}
        }
        public final Regular getContainer() {
            Element container = this.parent;
            while (container != null) {
                if (container instanceof Regular)
                    return (Regular) container;

                container = container.parent;
            }
            return null;
        }
        public final void close(Document document) {
            Object value = null;
            Regular container = getContainer();
            if (container == null) // root 
                ;
            else if (children == null) { // leaf
                if (attributes != null) { // complex type of attributes
                    if (first != null) {
                        attributes.put("", first);
                    }
                	value = new JSON.Object();
                	_putAllLocal(attributes, (JSON.Object) value);
                } else if (first != null) { // simple types
                    value = first;
                }
            } else { // branch, complex type of elements
                if (json != null) {
                    if (attributes != null) {
                        _putAllLocal(attributes, json);
                    }
                    value = json;
                } else if (attributes != null) {
                	value = new JSON.Object();
                	_putAllLocal(attributes, (JSON.Object) value);
                }
            }
            this.validate (value, container, document);
        }
        /**
         * Applications of XML.Regular should override this method for each
         * relevant type. The default implementation is to update the container's 
         * JSON object and delete the validated element from the XML tree if 
         * there is a container, leaving only the JSON structures.    
         * 
         * @param value
         * @param container
         * @param document
         */
        public void validate(
            Object value, Regular container, Document document
            ) {
            if (container != null) { 
                if (value != null) {
                    update(getLocalName(), value, container);
                }
                remove();
            }
        }
    } 
    /**
     * Read an XML file and return a JSON object.
     * 
     * @param xml
     * @return
     * @throws Error
     * @throws IOException
     */
    public static final JSON.Object regular (File xml) throws Error, IOException {
    	Document doc = new Document();
    	doc.read(xml, null, Regular.TYPE);
    	return ((Regular) doc.root).json;
    }
    /**
     * Read XML from an input stream and return a JSON object.
     * 
     * @param is
     * @return
     * @throws Error
     * @throws IOException
     */
    public static final JSON.Object regular (InputStream is) 
    throws Error, IOException {
    	Document doc = new Document();
    	doc.read(is, null, null, null, Regular.TYPE);
    	return ((Regular) doc.root).json;
    }
    
    private static final String _prefix = "ns";
    
    /**
     * ...
     * 
     * @param name
     * @param ns
     * @return
     */
    public static final String prefixed (String name, Map<String,String> ns) {
        int fqn = name.indexOf(' ');
        if (fqn > -1) {
            String namespace = name.substring(0, fqn);
            String prefix = ns.get(namespace);
            if (prefix == null) {
                prefix = _prefix + Integer.toString(ns.size());
                ns.put(namespace, prefix);
                name = prefix + ':' + name.substring(fqn+1);
            } else if (prefix == _no_prefix) {
                name = name.substring(fqn+1);
            } else {
                name = prefix + ':' + name.substring(fqn+1);
            }
        }
        return name;
    }
    /**
     * ...
     * 
     * @param os
     * @param element
     * @param ns
     */
    public static final void writeUTF8 (
        UTF8XMLWriter writer, Element element, Map ns
        )
    throws IOException {
        String tag = prefixed (element.name, ns);
        writer.startElement(tag);
        if (element.parent == null) { // root, declare namespaces now
            String namespace;
            Iterator<String> namespaces = ns.keySet().iterator();
            while (namespaces.hasNext()) {
                namespace = namespaces.next();
                writer.attribute(
                    _xmlns_colon + ns.get(namespace), namespace
                    );
            }
        }
        if (element.attributes != null) {
            String name;
            Iterator<String> names = element.attributes.keySet().iterator();
            while (names.hasNext()) {
                name = names.next();
                writer.attribute(
                    prefixed(name, ns), element.attributes.get(name)
                    );
            }
        }
        if (element.first != null)
            writer.write(element.first);
        if (element.children != null) {
            Iterator _children = element.children.iterator();
            while (_children.hasNext()) {
                writeUTF8(writer, (Element) _children.next(), ns);
            }
        }
        writer.endElement(tag);
        if (element.follow != null) {
            writer.write(element.follow);
        }
    }
    /**
     * 
     * @param element
     * @param ns
     * @return
     */
    public static final byte[] encodeUTF8 (Element element, Map ns) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            UTF8XMLWriter writer = new UTF8XMLWriter(os);
            writeUTF8(writer, element, ns);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e); // checked exceptions suck.
        }
        return os.toByteArray();
    }
    // TODO: an UTF-8 bytes iterator, using a stack of children iterators
    //       and connecting a PipedOutputStream and a PipedInputStream to
    //       write XML by chunks ... when needed ;-)
    //
    //       XML is out there, with high-latency HTTP/1.0 SOAP clients 
    //       requesting verbose responses over sluggish VPN enterprise
    //       network ... it should be serialized when needed, not before.
    //       
    //       Rationing bandwith through the usual TCP/IP windows of 16KB
    //       a network server can limit each client share of the CPU for
    //       XML serialization ... when encoding the element tree is done
    //       on socket a read event, when the client is ready to receive
    //       it, avoiding to buffer more than needed and let other clients
    //       have their turn.

}

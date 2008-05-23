/* Copyright (C) 2006-2007 Laurent A.V. Szyster

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

package org.async.protocols; // less java for more applications

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.io.OutputStream;
import java.io.IOException;

import org.async.simple.Bytes;
import org.async.simple.SIO;
import org.async.simple.Objects;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.NativeJavaObject;

/**
 * A relatively strict JSON intepreter to evaluate a UNICODE string 
 * as a tree of basic Java instances with maximum limits on the number
 * of containers and iterations, plus static methods to serialize java 
 * objects as JSON strings.
 * 
 * @h3 Synopsis
 * 
 * @p Conveniences JSON static methods allow to evaluate strings as an untyped
 * <code>Object</code>, a <code>JSON.Object</code> map or a 
 * <code>JSON.Array</code> list: 
 * 
 * @pre JSON json = new JSON() 
 *try {
 *    Object value = json.eval("null");
 *    JSON.Object map = json.object("{\"pass\": true}");
 *    JSON.Array list = json.array("[1,2,3]");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}
 * 
 * @p Access them simply and practically:
 * 
 * @pre try {
 *    if (map.bool("pass"))
 *        Integer i = list.intg(2);
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}
 * 
 * @p Serialize java instances as JSON strings:
 * 
 * @pre System.out.println(JSON.encode(value));
 *System.out.println(JSON.encode(map));
 *System.out.println(JSON.encode(list));
 * 
 * @p Note that you can serialize not just the types instanciated by JSON
 * but also any <code>Map</code> or <code>List</code> of many other
 * java object. 
 * 
 * @p Also, JSON object are serialized with their properties sorted by names,
 * allowing to compare two objects for equality by comparing such string.
 * That's handy in many case, most remarkably in order to sign and check
 * digest for JSON objects.
 * 
 * @h4 Interpreter
 * 
 * @p Direct instanciation of an Interpreter is usefull to evaluate many 
 * strings under the same global constraints on their cumulated numbers 
 * of containers and iterations.
 * 
 * @p With limits set to their maximum it is practical to evaluate distinct 
 * JSON values:
 * 
 * @pre JSON json = new JSON();
 *try {
 *    Object one = json.eval("{\"size\": 0}");
 *    Object two = json.eval("[1.0, true, null]");
 *    Object three = json.eval("1.0");
 *    Object four = json.eval("true");
 *    Object five = json.eval("null");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage());
 *}
 * 
 * @p To update any instance of <code>Map</code> with the members of many 
 * JSON objects:
 * 
 * @pre JSON.Error e;
 *JSON json = new JSON();
 *HashMap map = new HashMap(); 
 *e = json.update(map, "{\"width\": 200}");
 *if (e != null)
 *    System.out.println(e.str());
 *e = json.update(map, "{\"pass\": 1, fail: true}");
 *if (e != null)
 *    System.out.println(e.str());
 * 
 * @p To extend any <code>List</code> with the collection of many 
 * JSON arrays:
 * 
 * @pre JSON json = new JSON();
 *ArrayList list = new ArrayList(); 
 *e = json.extend(list, "[1,2,3]");
 *if (e != null)
 *    System.out.println(e.str());
 *e = json.extend(list, "[null, true, 1.0]");
 *if (e != null)
 *    System.out.println(e.str());
 * 
 * @h4 JSON Types
 * 
 * @p A convenience with static methods to serialize java objects as JSON
 * strings and to evaluate a strict JSON expression as a limited tree of
 * the five Java types 
 * 
 * <code>String</code>, 
 * <code>Double</code>, 
 * <code>BigDecimal</code>, 
 * <code>Integer</code>, 
 * <code>Boolean</code>,
 *
 * two convenience extending <code>HashMap</code> and <code>ArrayList</code>, 
 * plus the untyped <code>null</code> value.
 * 
 * @p Note that the additional distinction between JSON number types is made 
 * by considering numbers with an exponent as Doubles, the ones with decimals 
 * as BigDecimal and the others as Integer.
 * 
 * @h4 Safety Limits
 * 
 * @p Lower limits than the defaults maximum of 65355 can be set for the
 * number of objects and arrays and the count of values, for containers
 * and iterations, making JSON evaluation safe for public interfaces:
 * 
 * @pre try {
 *    JSON.Array list = (new JSON(1, 4)).array("[1,2,3,4,5]");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}
 * 
 * @h4 Serialization
 * 
 * @p To append distinct values into a <code>StringBuilder</code> using 
 * templates for constants:
 * 
 * @pre StringBuilder sb = new StringBuilder();
 *sb.append("{\"size\":");
 *JSON.strb(sb, value);
 *sb.append(",\"map\": ");
 *JSON.strb(sb, map);
 *sb.append(",\"list\": ");
 *JSON.strb(sb, list.iterator());
 *sb.append("}");
 *System.out.println(sb.toString());
 *
 * @h4 Pretty Print
 * 
 * @p To pretty print an indented representation of a java instance in JSON:
 * 
 * @pre System.out.println(JSON.outline(value));
 *System.out.println(JSON.outline(map));
 *System.out.println(JSON.outline(list));
 * 
 * @p ...
 */
public class JSON {
    
    public static final String MIME_TYPE = "application/json"; 
    
    /**
     * A simple JSON exception throwed for any syntax error found by the
     * interpreter.
     * 
     * @div @h3 Synopsis
     * 
     * There is just enough in a JSON.Error to identify the error 
     * 
     * @pre String string = "{\"test\": fail}";
     *try {
     *    Object value = (new JSON()).eval(string)
     *} catch (JSON.Error e) {
     *    System.out.println(e.toString());
     *}
     */
    public static class Error extends Exception {
        
        /**
         * The position of the JSON syntax error, -1 by default.
         */
        public int jsonIndex = -1;
        
        /**
         * The path to the JSON error value, if any.
         */
        public ArrayList jsonPath = new ArrayList();
        
        /**
         * Instanciate a JSON error with an error message.
         * 
         * @param message the error message
         */
        public Error(String message) {super(message);}
        
        /**
         * Instanciate a JSON error with an error message and the index 
         * in the JSON string at which the error occured.
         * 
         * @param message the error message
         * @param index position at which the error occured
         */
        public Error(String message, int index) {
            super(message);
            jsonIndex = index;
            }
        
        /**
         * Buffers the JSON representation of a <code>JSON.Error</code>.
         * 
         *@pre StringBuilder sb = new StringBuilder();
         *try {
         *    String model = (new JSON()).eval("{fail}");
         *} catch (JSONR.Error e) {
         *    e.jsonError(sb);
         *}
         *System.out.println(sb.toString());
         *
         * @p ...
         * 
         * @return the updated StringBuilder
         */
        public StringBuilder strb(StringBuilder sb) {
            sb.append('[');
            JSON.strb(sb, getMessage());
            sb.append(',');
            sb.append(jsonIndex);
            sb.append(',');
            JSON.strb(sb, jsonPath);
            sb.append(']');
            return sb;
        }
        
        /**
         * ...
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("JSON error ");
            strb(sb);
            return sb.toString(); 
        }
        
    }
    
    protected static final String OBJECT_TYPE_ERROR = 
        "Object type error";
    protected static final String ARRAY_TYPE_ERROR = 
        "Array type error";
    protected static final String STRING_TYPE_ERROR = 
        "String type error";
    protected static final String BOOLEAN_TYPE_ERROR = 
        "Boolean type error";
    protected static final String DOUBLE_TYPE_ERROR = 
        "Double type error";
    protected static final String BIGDECIMAL_TYPE_ERROR = 
        "BigDecimal type error";
    protected static final String INTEGER_TYPE_ERROR = 
        "Integer type error";
    protected static final String NUMBER_TYPE_ERROR = 
        "Number type error";
    protected static final String NULL_JSON_STRING = 
        "null JSON string";
    
    protected static final char _done = CharacterIterator.DONE;
    
    protected static final Number N (java.lang.Object value) throws Error {
        if (value instanceof Number) {return (Number) value;} 
        else throw new Error(NUMBER_TYPE_ERROR);
    }   
    protected static final Integer I (java.lang.Object value) throws Error {
        if (value instanceof Integer) {return (Integer) value;} 
        else throw new Error(INTEGER_TYPE_ERROR);
    }   
    protected static final BigDecimal D(java.lang.Object value) throws Error {
        if (value instanceof BigDecimal) {return (BigDecimal) value;}
        else throw new Error(BIGDECIMAL_TYPE_ERROR);
    }
    protected static final Double F(java.lang.Object value) throws Error {
        if (value instanceof Double) {return (Double) value;}
        else throw new Error(DOUBLE_TYPE_ERROR);
    }
    protected static final Boolean B(java.lang.Object value) throws Error {
        if (value instanceof Boolean) {return (Boolean) value;}
        else throw new Error(BOOLEAN_TYPE_ERROR);
    }
    protected static final String S(java.lang.Object value) throws Error {
        if (value instanceof String) {return (String) value;}
        else throw new Error(STRING_TYPE_ERROR);
    }
    protected static final Array A(java.lang.Object value) throws Error {
        if (value instanceof Array) {return (Array) value;} 
        else throw new Error(ARRAY_TYPE_ERROR);
    }
    protected static final Object O(java.lang.Object value) throws Error {
        if (value instanceof Object) {return (Object) value;} 
        else throw new Error(OBJECT_TYPE_ERROR);
    }
    
    /**
     * An extension of HashMap with type-casting convenience methods
     * that throw <code>JSON.Error</code> or return a typed object.
     * 
     * @h3 Synopsis
     * 
     * @pre try {
     *    JSON.Object map = (new JSON()).object("{" +
     *        "\"nothing\": null," +
     *        "\"a boolean\": true," +
     *        "\"an integer\": 2," +
     *        "\"a big decimal\": +1234567.89," +
     *        "\"a double float\": -123456789e-4," +
     *        "\"unicode string"\: \"hello world!\", "+
     *        "\"a list\": [null,true,1,2.0,3e+3]" +
     *        "\"another map\": {}" +
     *        "}");
     *    Boolean b = map.getBoolean("test");
     *    Integer i = map.getInteger("an integer");
     *    BigDecimal d = map.getDecimal("a big decimal");
     *    Double r = map.getDouble("a double float");
     *    String s = map.getString("unicode string");
     *    JSON.Array a = map.getArray("a list");
     *    JSON.Object o = map.getObject("another map");
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr());
     *}
     * 
     * The convenience is double. At runtime it distinguishes a JSON
     * type value error from other type casting allow programs to be
     * executed like a scripting language against to access a dynamic  
     * object model and continue in Java.
     * 
     * @p The second advantage for developper is trivial but practical:
     * most java IDE support autocompletion and namespace browsing by
     * type. Not having to break the chain for "manual" type casting
     * helps a lot and make the whole a lot clearer to read and debug. 
     */
    public static class Object extends HashMap {
        public final void add (String name, java.lang.Object value) {
            if (containsKey(name)) {
                java.lang.Object curr = get(name);
                if (curr instanceof JSON.Array) {
                    ((JSON.Array) curr).add(value);
                } else {
                    put(name, JSON.list(curr, value));
                }
            } else {
                put(name, value);
            }
        }
        /**
         * Access an <code>Number</code> value by name.
         * 
         * @param name of the value to access
         * @return an numeric value
         * @throws if there is no <code>Number</code> value by that named
         */
        public final Number getNumber(String name) throws Error {
            return JSON.N(get(name));
        }
        /**
         * Access an <code>Integer</code> value by name.
         * 
         * @param name of the value to access
         * @return an integer value
         * @throws if there is no <code>Integer</code> value by that named
         */
        public final Integer getInteger(String name) throws Error {
            return JSON.I(get(name));
        }
        /**
         * Access a value by name and cast it to an <code>int</code> or return 
         * the given default.
         * 
         * @param name of the value to cast
         * @param def the default value if none is named
         * @return an integer value
         */
        public final int intValue(String name, int def) {
            if (!containsKey(name)) return def;
            try {return JSON.N(get(name)).intValue();} 
            catch (Error e) {return def;}
        }
        /**
         * Access a value by name and cast it to an <code>long</code> or 
         * return the given default.
         * 
         * @param name of the value to cast
         * @param def the default value if none is named
         * @return an long value
         */
        public final long longValue(String name, long def) {
            if (!containsKey(name)) return def;
            try {return JSON.N(get(name)).longValue();} 
            catch (Error e) {return def;}
        }
        /**
         * Access a value by name and cast it to an <code>BigDecimal</code>.
         * 
         * @param name of the value
         * @return a decimal value
         * @throws if there is no <code>BigDecimal</code> value by that named
         */
        public final BigDecimal getDecimal(String name) throws Error {
            return JSON.D(get(name));
        }
        /**
         * Access a value by name and cast it to an <code>BigDecimal</code> 
         * or return the given default.
         * 
         * @param name of the value to cast
         * @param def the default value if none is named
         * @return an decimal value
         */
        public final BigDecimal getDecimal(String name, BigDecimal def) {
            if (!containsKey(name)) return def;
            try {return JSON.D(get(name));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param name
         * @return
         * @throws Error
         */
        public final Double getDouble(String name) throws Error {
            return (JSON.F(get(name)));
        }
        /**
         * ...
         * 
         * @param name
         * @param def
         * @return
         */
        public final double doubleValue(String name, double def) {
            if (!containsKey(name)) return def;
            try {return JSON.N(get(name)).doubleValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param name
         * @return
         * @throws Error
         */
        public final Boolean getBoolean(String name) throws Error {
            return (JSON.B(get(name)));
        }
        /**
         * ...
         * 
         * @param name
         * @param def
         * @return
         */
        public final boolean booleanValue(String name, boolean def) {
            if (!containsKey(name)) return def;
            try {return JSON.B(get(name)).booleanValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param name
         * @return
         * @throws Error
         */
        public final String getString(String name) throws Error {
            return (JSON.S(get(name)));
        }
        /**
         * ...
         * 
         * @param name
         * @param def
         * @return
         */
        public final String getString(String name, String def) {
            if (!containsKey(name)) return def;
            try {return JSON.S(get(name));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param name
         * @return
         * @throws Error
         */
        public final Array getArray(String name) throws Error {
            return (JSON.A(get(name)));
        }
        /**
         * ...
         * 
         * @param name
         * @param def
         * @return
         */
        public final Array getArray(String name, JSON.Array def) {
            if (!containsKey(name)) return def;
            try {return (JSON.A(get(name)));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param name
         * @return
         * @throws Error
         */
        public final Object getObject(String name) throws Error {
            return (JSON.O(get(name)));
        }
        /**
         * ...
         * 
         * @param name
         * @param def
         * @return
         */
        public final Object getObject(String name, JSON.Object def) {
            if (!containsKey(name)) return def;
            try {return (JSON.O(get(name)));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         */
        public final String toString() {
            return JSON.strb(new StringBuilder(), this).toString();
        }

    }
    
    /**
     * An extension of ArrayList with type-casting convenience methods
     * that throw <code>JSON.Error</code> or return a typed object.
     * 
     * @h3 Synopsis
     * 
     * @pre try {
     *    JSON.Array list = (new JSON()).array(
     *        "[null, true, 1, 3.0, 1234e-2, \"test\", [], {}]"
     *    );
     *    Object o = list.getObject(0); 
     *    Boolean b = list.getBoolean(1);
     *    Integer i = list.getInteger(2);
     *    BigDecimal d = list.getDecimal(3);
     *    Double r = list.getDouble(4);
     *    JSON.Array a = list.getArray(5);
     *    JSON.Object o = list.getObject(6);
     *    Boolean b = list.getArray(5).getBoolean(2);
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr());
     *}
     * 
     * @p ...
     */
    public static class Array extends ArrayList {
        /**
         * Access an <code>Number</code> value by index.
         * 
         * @param index of the value to access
         * @return an numeric value
         * @throws if there is no <code>Number</code> value at that index
         */
        public final Number getNumber(int index) throws Error {
            return JSON.N(get(index));
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final Integer getInteger(int index) throws Error {
            return (JSON.I(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final int intValue(int index, int def) {
            if (index >= size()) return def;
            try {return JSON.N(get(index)).intValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final long longValue(int index, long def) {
            if (index >= size()) return def;
            try {return JSON.N(get(index)).longValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final BigDecimal getDecimal(int index) throws Error {
            return JSON.D(get(index));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final BigDecimal getDecimal(int index, BigDecimal def) {
            if (index >= size()) return def;
            try {return JSON.D(get(index));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final Double getDouble(int index) throws Error {
            return (JSON.F(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final double doubleValue(int index, double def) {
            if (index >= size()) return def;
            try {return JSON.N(get(index)).doubleValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final Boolean getBoolean(int index) throws Error {
            return (JSON.B(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final boolean booleanValue(int index, boolean def) {
            if (index >= size()) return def;
            try {return JSON.B(get(index)).booleanValue();} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final String getString(int index) throws Error {
            return (JSON.S(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final String getString(int index, String def) {
            if (index >= size()) return def;
            try {return JSON.S(get(index));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final Array getArray(int index) throws Error {
            return (JSON.A(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final Array getArray(int index, JSON.Array def) {
            if (index >= size()) return def;
            try {return (JSON.A(get(index)));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         * 
         * @param index
         * @return
         * @throws Error
         */
        public final JSON.Object getObject(int index) throws Error {
            return (JSON.O(get(index)));
        }
        /**
         * ...
         * 
         * @param index
         * @param def
         * @return
         */
        public final JSON.Object getObject(int index, JSON.Object def) {
            if (index >= size()) return def;
            try {return (JSON.O(get(index)));} 
            catch (Error e) {return def;}
        }
        /**
         * ...
         */
        public final String toString() {
            return JSON.strb(new StringBuilder(), this).toString();
        }
    }

    // The Interpreter
    
    protected static final String ILLEGAL_UNICODE_SEQUENCE = 
        "illegal UNICODE sequence";
    protected static final String ILLEGAL_ESCAPE_SEQUENCE = 
        "illegal escape sequence";
    protected static final String COLON_EXPECTED = 
        "colon expected";
    protected static final String VALUE_EXPECTED = 
        "value expected";
    protected static final String STRING_EXPECTED = 
        "string expected ";
    protected static final String UNEXPECTED_CHARACTER = 
        "unexpected character";
    protected static final String UNEXPECTED_END = 
        "unexpected end";
    protected static final String NULL_EXPECTED = 
        "null expected";
    protected static final String FALSE_EXPECTED = 
        "false expected";
    protected static final String TRUE_EXPECTED = 
        "true expected";
    protected static final String CONTAINERS_OVERFLOW = 
        "containers overflow";
    protected static final String ITERATIONS_OVERFLOW = 
        "iterations overflow";
    
    protected char c;
    protected CharacterIterator it;
    protected StringBuilder buf;
    
    /**
     * The maximum number of containers left to instanciate by this parser. 
     */
    public int containers = 65355;
    
    /**
     * The maximum number of iterations left to for this parser. 
     */
    public int iterations = 65355;
    
    /**
     * Instanciate a JSON interpreter with limits set to 65355 on 
     * the number of containers and iterations.
     */
    public JSON() {}
    
    /**
     * Instanciate a JSON interpreter with the given limits on 
     * the number of both containers and iterations.
     *
     * @param containers a limit on the number of objects and arrays
     * @param iterations a limit on the total count of values
     */
    public JSON(int containers, int iterations) {
        this.containers = (containers > 0 ? containers: 1);
        this.iterations = (iterations > 0 ? iterations: 1);
    }
    
    /**
     * Evaluates a JSON <code>String</code> as an untyped value, returns a 
     * <code>JSON.Object</code>, 
     * <code>JSON.Array</code>, 
     * <code>String</code>,
     * <code>BigDecimal</code>, 
     * <code>Integer</code>, 
     * <code>Double</code>,
     * <code>Boolean</code>,
     * <code>null</code> or throws a <code>JSON.Error</code> if a syntax 
     * error occured.
     * 
     * @param json string to evaluate
     * @return an untyped Object
     * @throws JSON.Error
     */
    public java.lang.Object eval(String json) throws Error {
        buf = new StringBuilder();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            if (c == _done)
                throw error(NULL_JSON_STRING);
            else
                return value();
        } finally {
            buf = null;
            it = null;
        }
    }
    
    /**
     * Evaluates a JSON object, returns a new <code>JSON.O</code>   
     * or throws a JSON.Error if the string does not represent a 
     * valid object or if it exceeds the limits set on the number
     * of containers and iterations. 
     * 
     * @param json string to evaluate
     * @return a new <code>JSON.O</code>
     * @throws Error
     */
    public JSON.Object object(String json) throws Error {
        JSON.Object o = new JSON.Object();
        Error e = update(o, json);
        if (e == null)
            return o;
        else
            throw e;
    }

    /**
     * Evaluates a JSON array, returns a new <code>JSON.Array</code> or throws   
     * a <code>JSON.Error</code> if the string does not represent a valid 
     * array or if it exceeds the limits on containers and iterations. 
     * 
     * @param json <code>String</code> to evaluate
     * @return a new <code>JSON.Array</code> array
     * @throws JSON.Error
     */
    public JSON.Array array(String json) throws Error {
        JSON.Array a = new JSON.Array();
        Error e = extend(a, json);
        if (e == null)
            return a;
        else
            throw e;
    }
            
    /**
     * Evaluates a JSON <code>String</code> and update a <code>Map</code>, 
     * return <code>null</code> or a <code>JSON.Error</code> if the string 
     * does not represent a valid object. 
     * 
     * @param map the <code>Map</code> to update
     * @param json <code>String</code> to evaluate
     * @return <code>null</code> or a <code>JSON.Error</code>
     */
    public Error update(Map map, String json) {
        buf = new StringBuilder();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '{') {
                c = it.next(); object(map); return null;
            } else
                return error(OBJECT_TYPE_ERROR);
        } catch (Error e) {
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    /**
     * Evaluates a JSON <code>String</code> and extends a <code>List</code>,
     * return <code>null</code> or a <code>JSON.Error</code> if the string 
     * does not represent a valid array. 
     * 
     * @param list the <code>List</code> to extend
     * @param json <code>String</code> to evaluate
     * @return <code>null</code> or a <code>JSON.Error</code>
     */
    public Error extend(List list, String json) {
        buf = new StringBuilder();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '[') {
                c = it.next(); array(list); return null;
            } else
                return error(ARRAY_TYPE_ERROR);
        } catch (Error e) {
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    protected final JSON.Error error(String message) {
        return new JSON.Error(message, it.getIndex());
    }
    
    protected final boolean next(char test) {
        c = it.next();
        return c == test;
    }
    
    protected static final java.lang.Object OBJECT = new java.lang.Object();
    protected static final java.lang.Object ARRAY = new java.lang.Object();
    protected static final java.lang.Object COLON = new java.lang.Object();
    protected static final java.lang.Object COMMA = new java.lang.Object();
    
    protected final java.lang.Object value() throws Error {
        while (Character.isWhitespace(c)) c = it.next();
        switch(c){
        case '{': {c = it.next(); return object(new JSON.Object());}
        case '[': {c = it.next(); return array(new JSON.Array());}
        case '"': {c = it.next(); return string();}
        case '0': case '1': case '2': case '3': case '4':  
        case '5': case '6': case '7': case '8': case '9': 
        case '-': {
            return number();
            }
        case 't': {
            if (next('r') && next('u') && next('e')) {
                c = it.next(); return Boolean.TRUE;
            } else
                throw error(TRUE_EXPECTED);
        }
        case 'f': {
            if (next('a') && next('l') && next('s') && next('e')) {
                c = it.next(); return Boolean.FALSE;
            } else
                throw error(FALSE_EXPECTED);
        }
        case 'n': {
            if (next('u') && next('l') && next('l')) {
                c = it.next(); return null;
            } else
                throw error(NULL_EXPECTED);
        }
        case ',': {c = it.next(); return COMMA;} 
        case ':': {c = it.next(); return COLON;}
        case ']': {c = it.next(); return ARRAY;} 
        case '}': {c = it.next(); return OBJECT;}
        case _done:
            throw error(UNEXPECTED_END);
        default: 
            throw error(UNEXPECTED_CHARACTER);
        }
    }
    
    protected final java.lang.Object value(String name) throws Error {
        try {
            return value();
        } catch (JSON.Error e) {
            e.jsonPath.add(0, name);
            throw e;
        }
    }
    
    protected final java.lang.Object value(int index) throws Error {
        try {
            return value();
        } catch (JSON.Error e) {
            e.jsonPath.add(0, new Integer(index));
            throw e;
        }
    }
    
    protected final java.lang.Object object(Map o) throws Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        String name; 
        java.lang.Object val;
        java.lang.Object token = value();
        while (token != OBJECT) {
            if (!(token instanceof String))
                throw error(STRING_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            name = (String) token;
            if (value() == COLON) {
                val = value(name);
                if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                    throw error(VALUE_EXPECTED);
                
                o.put(name, val);
                token = value();
                if (token == COMMA)
                    token = value();
            } else {
                throw error(COLON_EXPECTED);
            }
        }
        return o;
    }
    
    protected final java.lang.Object array(List a) throws Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        int i = 0;
        java.lang.Object token = value(i++);
        while (token != ARRAY) {
            if (token==COLON || token==COMMA || token==OBJECT)
                throw error(VALUE_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            a.add(token);
            token = value(); 
            if (token == COMMA) 
                token = value(i++);
        }
        return a;
    }
    
    protected final java.lang.Object number() {
        buf.setLength(0);
        if (c == '-') {
            buf.append(c); c = it.next();
        }
        digits();
        if (c == '.') {
            buf.append(c); c = it.next();
            digits();
            if (c == 'e' || c == 'E') {
                buf.append(c); c = it.next();
                if (c == '+' || c == '-') {
                    buf.append(c); c = it.next();
                }
                digits();
                return new Double(buf.toString());
            } else {
                return new BigDecimal(buf.toString()); 
            }
        } else if (c == 'e' || c == 'E') {
            buf.append(c); c = it.next();
            if (c == '+' || c == '-') {
                buf.append(c); c = it.next();
            }
            digits();
            return new Double(buf.toString());
        } else {
            return new Integer(buf.toString());
        }
    }
    
    protected final java.lang.Object string() throws Error {
        buf.setLength(0);
        while (c != '"') {
            if (c == '\\') {
                c = it.next(); 
                switch(c) {
                    case 'u': buf.append(unicode(4)); break;
                    case 'x': buf.append(unicode(2)); break;
                    case '\\': buf.append('\\'); break;
                    case '"': buf.append('"'); break;
                    case '/': buf.append('/'); break;
                    case 'b': buf.append('\b'); break;
                    case 'f': buf.append('\f'); break;
                    case 'n': buf.append('\n'); break;
                    case 'r': buf.append('\r'); break;
                    case 't': buf.append('\t'); break;
                    default: 
                        throw error(ILLEGAL_ESCAPE_SEQUENCE);
                }
            } else if (c == _done) {
                throw error(UNEXPECTED_END);
            } else {
                buf.append(c); 
            }
            c = it.next();
        }
        c = it.next();
        return buf.toString();
    }
    
    protected final void digits() {
        while (Character.isDigit(c)) {buf.append(c); c = it.next();}
    }
    
    protected final char unicode(int length) throws Error {
        int val = 0;
        for (int i = 0; i < length; ++i) {
            c = it.next();
            switch (c) {
            case '0': case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9':
                val = (val << 4) + c - '0';
                break;
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                val = (val << 4) + c - 'k';
                break;
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                val = (val << 4) + c - 'K';
                break;
            case _done:
                throw error(UNEXPECTED_END);
            default:
                throw error(ILLEGAL_UNICODE_SEQUENCE);
            }
        }
        return (char) val;
    }

    /**
     * Instanciate a new <code>JSON.Object</code> with an even sequence
     * of name and value pairs, a convenient way to express JSON literals
     * in java.
     * 
     * @pre JSON.dict(
     *     "hello", "world",
     *     "one", new Integer(1),
     *     "test", Boolean.TRUE 
     *     );
     * 
     * @param pairs of key and values
     * @return a new <code>JSON.Object</code>
     */
    public static final JSON.Object dict (java.lang.Object ... pairs) {
        JSON.Object result = new JSON.Object();
        for (int i=0; i< pairs.length; i=i+2)
            if (pairs[i] instanceof String)
                result.put(pairs[i], pairs[i+1]);
            else
                result.put(pairs[i].toString(), pairs[i+1]);
        return result;
    };
    
    /**
     * Instanciate a new <code>JSON.Object</code> with an sequence
     * of name and value String pairs, a convenience to map a 
     * <code>String[][]</code> array of options to a dictionary.
     * 
     * @pre JSON.dict(new String[][]{
     *     {"hello", "world"},
     *     {"one", "1"},
     *     {"test", "true"} 
     *     });
     * 
     * @param pairs of key and values
     * @return a new <code>JSON.Object</code>
     */
    public static final JSON.Object options (String[][] pairs) {
        JSON.Object result = new JSON.Object();
        for (int i=0; i<pairs.length; i++) {
            if (pairs[i].length > 1)
                result.put(pairs[i][0], pairs[i][1]);
            else
                result.put(pairs[i][0], null);
        }
        return result;
    };
    
    /**
     * Instanciate a new <code>JSON.Array</code> with a sequence of values, 
     * a convenient way to express JSON literals in java.
     * 
     * @pre JSON.list("world", new Integer(1), Boolean.TRUE, null);
     * 
     * @param items
     * @result a new <code>JSON.Array</code>
     */
    public static final JSON.Array list (java.lang.Object ... items) {
        return (JSON.Array) Objects.extend(new JSON.Array(), items);
    };
    
    /**
     * Decode a JSON string.
     * 
     * @param encoded JSON string
     * @return a JSON type
     * @throws JSON syntax error
     */
    public static final java.lang.Object decode(String encoded) 
    throws Error {
        return (new JSON()).eval(encoded);
    };
    
    /**
     * Try to reflect all public fields of <code>value</code> as a 
     * <code>JSON.Object</code>.
     * 
     * @param value to reflect
     * @return a <code>JSON.Object</code>
     */
    public static final java.lang.Object reflect(
        java.lang.Object value
        ) {
        JSON.Object proxy = new JSON.Object();
        java.lang.reflect.Field[] fields = value.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                proxy.put(fields[i].getName(), fields[i].get(value));
            } catch (Throwable e) {
                continue;
            }
        }
        return proxy;
    }
    
    protected static final String _quote = "\\\"";
    protected static final String _back = "\\\\";
    protected static final String _slash = "\\/";
    protected static final String _ctrl_b = "\\b";
    protected static final String _ctrl_f = "\\f";
    protected static final String _ctrl_n = "\\n";
    protected static final String _ctrl_r = "\\r";
    protected static final String _ctrl_t = "\\t";
    
    protected static final StringBuilder strb(StringBuilder sb, String s) {
        if (s==null) {sb.append(_null); return sb;}
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (char c = it.first(); c != _done; c = it.next()) {
            switch(c) {
            case '"':  sb.append(_quote); break;
            case '\\': sb.append(_back); break;
            case '/': sb.append(_slash); break;
            case '\b': sb.append(_ctrl_b); break;
            case '\f': sb.append(_ctrl_f); break;
            case '\n': sb.append(_ctrl_n); break;
            case '\r': sb.append(_ctrl_r); break;
            case '\t': sb.append(_ctrl_t); break;
            default: 
                if (Character.isISOControl(c))
                    unicode(sb, c);
                else
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb;
    }
    
    protected static final String _unicode = "\\u";
    protected static final char[] _hex = "0123456789ABCDEF".toCharArray();
    
    protected static final StringBuilder unicode(StringBuilder sb, char c) {
        sb.append(_unicode);
        int n = c;
        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;
            sb.append(_hex[digit]);
            n <<= 4;
        }
        return sb;
    }

    protected static final String _object = "{}";
    protected static final String _array = "[]";
    protected static final String _null = "null";
    protected static final String _true = "true";
    protected static final String _false = "false";
    
    protected static final StringBuilder strb(StringBuilder sb, byte[] bytes) {
        if (bytes.length > 0) { 
            sb.append('[');
            sb.append(bytes[0]);
            for (int i=1; i<bytes.length; i++) {
                sb.append(','); sb.append(bytes[i]);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final StringBuilder strb(StringBuilder sb, int[] integers) {
        if (integers.length > 0) { 
            sb.append('[');
            sb.append(integers[0]);
            for (int i=1; i<integers.length; i++) {
                sb.append(','); sb.append(integers[i]);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final StringBuilder strb(StringBuilder sb, short[] shorts) {
        if (shorts.length > 0) { 
            sb.append('[');
            sb.append(shorts[0]);
            for (int i=1; i<shorts.length; i++) {
                sb.append(','); sb.append(shorts[i]);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final StringBuilder strb(StringBuilder sb, long[] longs) {
        if (longs.length > 0) { 
            sb.append('[');
            sb.append(longs[0]);
            for (int i=1; i<longs.length; i++) {
                sb.append(','); sb.append(longs[i]);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final 
    StringBuilder strb(StringBuilder sb, double[] doubles) {
        if (doubles.length > 0) { 
            sb.append('[');
            sb.append(doubles[0]);
            for (int i=1; i<doubles.length; i++) {
                sb.append(','); sb.append(doubles[i]);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final 
    StringBuilder strb(StringBuilder sb, boolean[] bools) {
        if (bools.length > 0) { 
            sb.append('[');
            sb.append(bools[0]);
            for (int i=1; i<bools.length; i++) {
                sb.append(','); sb.append((bools[i])?_true:_false);
            }
            sb.append(']');
        } else
            sb.append(_array);
        return sb;
    }
    
    protected static final StringBuilder strb(StringBuilder sb, char[] chars) {
        return strb(sb, chars.toString());
    }
    
    protected static final StringBuilder strb(
        StringBuilder sb, java.lang.Object value, Class component
        ) {
        if (component.isArray()) {
            Class type = component.getComponentType();
            java.lang.Object[] values = (java.lang.Object[]) value;
            if (values.length > 0) {
                sb.append('[');
                strb(sb, values[0], type);
                for (int i=1; i<values.length; i++) {
                    sb.append(','); strb(sb, values[i], type);
                }
                sb.append(']');
            } else
                sb.append(_array);
        } else if (component == Byte.TYPE)
            strb(sb, (byte[]) value);
        else if (component == Integer.TYPE)
            strb(sb, (int[]) value);
        else if (component == Long.TYPE)
            strb(sb, (long[]) value);
        else if (component == Short.TYPE)
            strb(sb, (short[]) value);
        else if (component == Double.TYPE)
            strb(sb, (double[]) value);
        else if (component == Boolean.TYPE)
            strb(sb, (boolean[]) value);
        else if (component == Character.TYPE)
            strb(sb, (char[]) value);
        return sb;
    }
    
    protected static final StringBuilder strb(
        StringBuilder sb, Map map, Iterator it
        ) {
        java.lang.Object key; 
        if (!it.hasNext()) {
            sb.append(_object);
            return sb;
        }
        sb.append('{');
        key = it.next();
        strb(sb, key);
        sb.append(':');
        strb(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            strb(sb, key);
            sb.append(':');
            strb(sb, map.get(key));
        }
        sb.append('}');
        return sb;
    }
    
    protected static final StringBuilder strb(
        StringBuilder sb, Map map, java.lang.Object[] names
        ) {
        java.lang.Object key; 
        if (names.length == 0)
            sb.append(_object);
        else {
            sb.append('{');
            key = names[0];
            strb(sb, key);
            sb.append(':');
            strb(sb, map.get(key));
            for (int i = 1; i < names.length; i++) {
                sb.append(',');
                key = names[i];
                strb(sb, key);
                sb.append(':');
                strb(sb, map.get(key));
            }
            sb.append('}');
        }
        return sb;
    }
    
    protected static final StringBuilder strb(StringBuilder sb, Iterator it) {
        if (!it.hasNext()) {
            sb.append(_array);
            return sb;
        }
        sb.append('[');
        strb(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            strb(sb, it.next());
        }
        sb.append(']');
        return sb;
    }
    
    protected static final String trimZero (String untrimmed) {
        int last = untrimmed.length();
        char c;
        while (last > 0) { 
            last--;
            c = untrimmed.charAt(last);
            switch (c) {
            case '0': 
                continue;
            case '.':
                return untrimmed.substring(0, last);
            default: 
                return untrimmed.substring(0, last + 1);
            }
        }
        return "0";
    }
    
    /**
     * Serialize a JSON type in a <code>StringBuilder</code>
     * 
     * @param sb to fill
     * @param value to serialize
     * @return
     */
    public static final StringBuilder strb(
        StringBuilder sb, java.lang.Object value
        ) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(trimZero(((Number) value).toString()));
        else if (value instanceof String) 
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Iterator) 
            strb(sb, (Iterator) value);
        else if (value instanceof Map) {
            Map object = (Map) value;
            java.lang.Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            strb(sb, object, Objects.iter(names));
        } else if (value instanceof Iterable) {
            strb(sb, ((Iterable) value).iterator());
        } else if (value instanceof Object[]) {
            strb(sb, Objects.iter((java.lang.Object[]) value));
        } else if (value instanceof NativeArray) {
            NativeArray array = (NativeArray) value;
            java.lang.Object[] ids = (array).getIds();
            if (ids.length == 0)
                sb.append(_array);
            else {
                java.lang.Object[] list = new java.lang.Object[ids.length]; 
                for (int i=0; i < ids.length; i++) {
                    list[i] = array.get(i, array);
                }
                strb(sb, Objects.iter(list));
            }
        } else if (value instanceof NativeObject) {
            NativeObject object = (NativeObject) value;
            java.lang.Object[] ids = (object).getIds();
            if (ids.length == 0)
                sb.append(_object);
            else {
                Arrays.sort(ids);
                JSON.Object map = new JSON.Object();
                String key;
                for (int i=0; i < ids.length; i++) {
                    key = (String) ids[i];
                    map.put(key, object.get(key, object));
                }
                strb(sb, map, Objects.iter(ids));
            }
        } else if (value instanceof NativeJavaObject) {
            strb(sb, ((NativeJavaObject) value).unwrap());
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type == null)
                sb.append(value);
            else if (type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    strb(sb, value, component);
                else
                    strb(sb, Objects.iter((java.lang.Object[]) value));
            } else
                strb(sb, value.toString());
        }
        return sb;
    }
    
    /**
     * Encode an untyped value as a JSON string, return a <code>String</code>
     * that can safely be encoded in UTF-8.
     * 
     * @param value to encode
     * @return a JSON <code>String</code>
     */
    public static final String encode(java.lang.Object value) {
        return strb(new StringBuilder(), value).toString();
    }
    
    protected static final StringBuilder xjson(StringBuilder sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (char c = it.first(); c != _done; c = it.next()) {
            switch(c) {
            case '"':  sb.append(_quote); break;
            case '\\': sb.append(_back); break;
            case '/': sb.append(_slash); break;
            case '\b': sb.append(_ctrl_b); break;
            case '\f': sb.append(_ctrl_f); break;
            case '\n': sb.append(_ctrl_n); break;
            case '\r': sb.append(_ctrl_r); break;
            case '\t': sb.append(_ctrl_t); break;
            default: 
                if (c > 126 || c < 32)
                    unicode(sb, c);
                else
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb;
    }
    
    protected static final StringBuilder xjson(
        StringBuilder sb, Map map, Iterator it
        ) {
        java.lang.Object key; 
        if (!it.hasNext()) {
            sb.append(_object);
            return sb;
        }
        sb.append('{');
        key = it.next();
        xjson(sb, key);
        sb.append(':');
        xjson(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            xjson(sb, key);
            sb.append(':');
            xjson(sb, map.get(key));
        }
        sb.append('}');
        return sb;
    }
    
    protected static final StringBuilder xjson(
        StringBuilder sb, Iterator it
        ) {
        if (!it.hasNext()) {
            sb.append(_array);
            return sb;
        }
        sb.append('[');
        xjson(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            xjson(sb, it.next());
        }
        sb.append(']');
        return sb;
    }
    
    protected static final StringBuilder xjson(
        StringBuilder sb, java.lang.Object value
        ) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(trimZero(((Number) value).toString()));
        else if (value instanceof String) 
            xjson(sb, (String) value);
        else if (value instanceof Character) 
            xjson(sb, ((Character) value).toString());
        else if (value instanceof Iterator) 
            xjson(sb, (Iterator) value);
        else if (value instanceof Map) {
            Map object = (Map) value;
            java.lang.Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            xjson(sb, object, Objects.iter(names));
        } else if (value instanceof Iterable) {
            xjson(sb, ((Iterable) value).iterator());
        } else if (value instanceof Object[]) {
            xjson(sb, Objects.iter((java.lang.Object[]) value));
        } else if (value instanceof NativeArray) {
            NativeArray array = (NativeArray) value;
            java.lang.Object[] ids = (array).getIds();
            if (ids.length == 0)
                sb.append(_array);
            else {
                java.lang.Object[] list = new java.lang.Object[ids.length]; 
                for (int i=0; i < ids.length; i++) {
                    list[i] = array.get(i, array);
                }
                xjson(sb, Objects.iter(list));
            }
        } else if (value instanceof NativeObject) {
            NativeObject object = (NativeObject) value;
            java.lang.Object[] ids = (object).getIds();
            if (ids.length == 0)
                sb.append(_object);
            else {
                Arrays.sort(ids);
                JSON.Object map = new JSON.Object();
                String key;
                for (int i=0; i < ids.length; i++) {
                    key = (String) ids[i];
                    map.put(key, object.get(key, object));
                }
                xjson(sb, map, Objects.iter(ids));
            }
        } else if (value instanceof NativeJavaObject) {
            xjson(sb, ((NativeJavaObject) value).unwrap());
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type == null)
                sb.append(value);
            else if (type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    strb(sb, value, component);
                else
                    xjson(sb, Objects.iter((java.lang.Object[]) value));
            } else
                xjson(sb, value.toString());
        }
        return sb;
    }
    
    /**
     * Encode an untyped value as a JSON string, return a <code>String</code>
     * that can safely be encoded in 7bit ASCII.
     * 
     * @param value to encode
     * @return an X-JSON <code>String</code>
     */
    public static final String xjson(java.lang.Object value) {
        return xjson(new StringBuilder(), value).toString();
    }
    
    protected static final String _crlf = "\r\n";
    protected static final String _indent = "  ";
    
    protected static final StringBuilder outline(
        StringBuilder sb, Map map, Iterator it, String indent
        ) {
        java.lang.Object key; 
        if (!it.hasNext()) {
            sb.append("{}");
            return sb;
        }
        indent += _indent;
        sb.append('{');
        sb.append(indent);
        key = it.next();
        outline(sb, key, indent);
        sb.append(": ");
        outline(sb, map.get(key), indent);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            key = it.next();
            outline(sb, key, indent);
            sb.append(": ");
            outline(sb, map.get(key), indent);
        }
        sb.append(indent);
        sb.append('}');
        return sb;
    }
    
    protected static final StringBuilder outline(
        StringBuilder sb, Iterator it, String indent
        ) {
        if (!it.hasNext()) {
            sb.append("[]");
            return sb;
        }
        sb.append('[');
        indent += _indent;
        sb.append(indent);
        outline(sb, it.next(), indent);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            outline(sb, it.next(), indent);
        }
        sb.append(indent);
        sb.append(']');
        return sb;
    }
    
    public static final StringBuilder outline(
        StringBuilder sb, java.lang.Object value, String indent
        ) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(trimZero(((Number) value).toString()));
        else if (value instanceof String)
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Iterator) 
            outline(sb, (Iterator) value, indent);
        else if (value instanceof Map) {
            Map object = (Map) value;
            java.lang.Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            outline(sb, object, Objects.iter(names), indent);
        } else if (value instanceof Iterable) {
            outline(sb, ((Iterable) value).iterator(), indent);
        } else if (value instanceof Object[]) {
            outline(sb, Objects.iter((java.lang.Object[]) value), indent);
        } else if (value instanceof NativeArray) {
            NativeArray array = (NativeArray) value;
            java.lang.Object[] ids = (array).getIds();
            if (ids.length == 0)
                sb.append(_array);
            else {
                java.lang.Object[] list = new java.lang.Object[ids.length]; 
                for (int i=0; i < ids.length; i++) {
                    list[i] = array.get(i, array);
                }
                outline(sb, Objects.iter(list), indent);
            }
        } else if (value instanceof NativeObject) {
            NativeObject object = (NativeObject) value;
            java.lang.Object[] ids = (object).getIds();
            if (ids.length == 0)
                sb.append(_object);
            else {
                Arrays.sort(ids);
                JSON.Object map = new JSON.Object();
                String key;
                for (int i=0; i < ids.length; i++) {
                    key = (String) ids[i];
                    map.put(key, object.get(key, object));
                }
                outline(sb, map, Objects.iter(ids), indent);
            }
        } else if (value instanceof NativeJavaObject) {
            outline(sb, ((NativeJavaObject) value).unwrap(), indent);
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type == null)
                sb.append(value);
            else if (type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    strb(sb, value, component);
                else
                    outline(sb, Objects.iter((java.lang.Object[]) value), indent);
            } else
                outline(sb, value.toString(), indent);
        }
        return sb;
    }
    
    /**
     * Encode an untyped value as a pretty-printed JSON string with
     * CRLF line delimiters and a two space wide indentation, return 
     * a <code>String</code> that can safely be encoded in UTF-8.
     * 
     * @param value to represent
     * @return an X-JSON <code>String</code>
     */
    public static final String pprint(java.lang.Object value) {
        return outline(new StringBuilder(), value, _crlf).toString();
    }
    
    protected static final StringBuilder pprint(
        StringBuilder sb, Map map, Iterator it, String indent, OutputStream os 
        ) throws IOException {
        java.lang.Object key; 
        if (!it.hasNext()) {
            sb.append("{}");
            return sb;
        }
        indent += _indent;
        sb.append('{');
        sb.append(indent);
        key = it.next();
        sb = pprint(sb, key, indent, os);
        sb.append(": ");
        sb = pprint(sb, map.get(key), indent, os);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            key = it.next();
            sb = pprint(sb, key, indent, os);
            sb.append(": ");
            sb = pprint(sb, map.get(key), indent, os);
            if (sb.length() > SIO.netBufferSize) {
                os.write(Bytes.encode(sb.toString(), "UTF-8"));
                os.flush();
                sb = new StringBuilder();
            }
        }
        sb.append(indent);
        sb.append('}');
        return sb;
    }
    
    protected static final StringBuilder pprint(
        StringBuilder sb, Iterator it, String indent, OutputStream os 
        ) throws IOException {
        if (!it.hasNext()) {
            sb.append("[]");
            return sb;
        }
        sb.append('[');
        indent += _indent;
        sb.append(indent);
        sb = pprint(sb, it.next(), indent, os);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            sb = pprint(sb, it.next(), indent, os);
            if (sb.length() > SIO.netBufferSize) {
                os.write(Bytes.encode(sb.toString(), "UTF-8"));
                os.flush();
                sb = new StringBuilder();
            }
        }
        sb.append(indent);
        sb.append(']');
        return sb;
    }
    
    protected static final StringBuilder pprint(
        StringBuilder sb, java.lang.Object value, String indent, OutputStream os 
        ) throws IOException {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(trimZero(((Number) value).toString()));
        else if (value instanceof String)
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Iterator) 
            sb = pprint(sb, (Iterator) value, indent, os);
        else if (value instanceof Map) {
            Map object = (Map) value;
            java.lang.Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            sb = pprint(sb, object, Objects.iter(names), indent, os);
        } else if (value instanceof Iterable) {
            sb = pprint(sb, ((Iterable) value).iterator(), indent, os);
        } else if (value instanceof Object[]) {
            sb = pprint(sb, Objects.iter(
                (java.lang.Object[]) value
                ), indent, os);
        } else if (value instanceof NativeArray) {
            NativeArray array = (NativeArray) value;
            java.lang.Object[] ids = (array).getIds();
            if (ids.length == 0)
                sb.append(_array);
            else {
                java.lang.Object[] list = new java.lang.Object[ids.length]; 
                for (int i=0; i < ids.length; i++) {
                    list[i] = array.get(i, array);
                }
                pprint(sb, Objects.iter(list), indent, os);
            }
        } else if (value instanceof NativeObject) {
            NativeObject object = (NativeObject) value;
            java.lang.Object[] ids = (object).getIds();
            if (ids.length == 0)
                sb.append(_object);
            else {
                Arrays.sort(ids);
                JSON.Object map = new JSON.Object();
                String key;
                for (int i=0; i < ids.length; i++) {
                    key = (String) ids[i];
                    map.put(key, object.get(key, object));
                }
                pprint(sb, map, Objects.iter(ids), indent, os);
            }
        } else if (value instanceof NativeJavaObject) {
            pprint(sb, ((NativeJavaObject) value).unwrap(), indent, os);
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type == null)
                sb.append(value);
            else if (type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    strb(sb, value, component);
                else
                    sb = pprint(sb, Objects.iter(
                        (java.lang.Object[]) value
                        ), indent, os);
            } else
                sb = pprint(sb, value.toString(), indent, os);
        }
        return sb;
    }
    
    /**
     * Encode an untyped value as a pretty-printed JSON string with
     * CRLF line delimiters and a two space wide indentation, buffer
     * chunks of 16KB before writing them to an <code>OutputStream</code>.
     * 
     * @param value to represent
     * @return an X-JSON <code>String</code>
     */
    public static final void pprint(
        java.lang.Object value, OutputStream os
        ) throws IOException {
        StringBuilder sb = pprint(new StringBuilder(), value, "\r\n", os);
        os.write(Bytes.encode(sb.toString(), "UTF-8"));
        os.flush();
    }
}
package org.async.simple;

import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conveniences to split, join and generate strings.
 */
public class Strings {
    /**
     * A convenient constant: "\r\n".
     */
    public static final String CRLF = "\r\n";
    protected static class CharSplitIterator implements Iterator<String> {
        private String _splitted;
        private char _splitter;
        private int _current = 0;
        private int _next;
        public CharSplitIterator (String splitted, char splitter) {
            _splitted = splitted;
            _splitter = splitter;
            _next = splitted.indexOf(splitter);
            }
        public boolean hasNext () {
            return !(_next == -1 && _current == -1);
        }
        public String next () {
            String token;
            if (_current == -1)
                return null;
            else if (_next == -1) {
                token = _splitted.substring(_current);
                _splitted = null; // free willy-memory!
                _current = -1;
            } else {
                if (_next > _current)
                    token = _splitted.substring(_current, _next);
                else
                    token = "";
                _current = _next + 1;
                _next = _splitted.indexOf(_splitter, _current);
            }
            return token;
        }
        public void remove () {/* optional interface? what else now ...*/}
    }
    /**
     * Returns an <code>Iterator</code> that splits a string with a single
     * character as fast an lean as possible in Java (without a PCRE and
     * for a maybe too simple use case).
     * 
     * @pre Iterator<String> strings = Strings.split("one two three", ' ');
     * 
     * @test importPackage(Packages.org.async.simple);
     *strings = Strings.split("one two three", ' ');
     *return (
     *    strings.next() == "one" &&
     *    strings.next() == "two" &&
     *    strings.next() == "three"
     *    );
     * 
     * @param text to split
     * @param pattern used to split input
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static final Iterator<String> split (String text, char splitter) {
        return new CharSplitIterator (text, splitter);
    }
    protected static class ReSplitIterator implements Iterator<String> {
        private String _splitted;
        private Matcher _matcher;
        private int _current = 0;
        private int _group = 0;
        private int _groups = 0;
        public ReSplitIterator (String splitted, Pattern splitter) {
            _splitted = splitted;
            _matcher = splitter.matcher(splitted);
        }
        public boolean hasNext () {
            return _matcher != null;
        }
        public String next () {
            String token;
            if (_matcher == null) {
                return null;
            } else if (_group < _groups) { // groups
                _group++;
                token = _matcher.group(_group);
            } else {
                _group = 0;
                if (_matcher.find(_current)) {
                    token = _splitted.substring(_current, _matcher.start());
                    _current = _matcher.end();
                    _groups = _matcher.groupCount();
                } else {
                    token = _splitted.substring(_current);
                    _matcher = null;
                    _splitted = null;
                    _groups = 0;
                }
            }
            return token;
        }
        public void remove () {/* optional interface? what else now ...*/}
    }
    /**
     * Returns an <code>Iterator</code> that splits a string with a regular
     * expression but - unlike the standard Java API and like Python's re - 
     * does the right thing and also iterates through the expression groups.
     * 
     * @pre Iterator<String> strings = Strings.split(
     *    "one\t  and  \r\n three", Pattern.compile("\\s+(and|or)\\s+")
     *    );
     * 
     * @test importPackage(Packages.org.async.simple);
     *importClass(Packages.java.util.regex.Pattern)
     *strings = Strings.split(
     *    "one\t  and  \r\n three", Pattern.compile("\\s+(and|or)\\s+")
     *    );
     *return (
     *    strings.next() == "one" &&
     *    strings.next() == "and" &&
     *    strings.next() == "three"
     *    );
     * 
     * @param text to split
     * @param pattern used to split input
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static final Iterator<String> split (String text, Pattern pattern) {
        return new ReSplitIterator (text, pattern);
    }
    /**
     * Join the serialized objects produced by an <code>Iterator</code> in a 
     * <code>StringBuilder</code>, using another serializable 
     * <code>Object</code> as separator between items.
     * 
     * @pre StringBuilder buffer = new StringBuilder(); 
     *Iterator objects = Strings.iter(new Object[]{"A", "B", "C"});
     *Strings.join(", ", objects, buffer);
     * 
     * @param separator between joined strings
     * @param objects to join as strings
     * @param buffer to append strings and separators to
     * @return the appended buffer
     */
    public static final StringBuilder join (
        Object separator, Iterator objects, StringBuilder buffer
        ) {
        if (objects.hasNext()) {
            buffer.append(objects.next());
            while (objects.hasNext()) {
                buffer.append(separator);
                buffer.append(objects.next()); 
            }
        }
        return buffer;
    }
    /**
     * Join object's strings with any other type joinable in a 
     * <code>StringBuffer</code>. 
     * 
     * @test importPackage(Packages.org.async.simple);
     *return Strings.join(
     *    ", ", Objects.iter(["A", "B", "C"])
     *    ).equals("A, B, C");
     * 
     * @param separator between joined objects
     * @param objects to join as strings
     * @return the joined string
     */
    public static final String join (Object separator, Iterator objects) {
        return join(separator, objects, new StringBuilder()).toString();
    }
    /**
     * The strictly alphanumeric set of ASCII characters, usefull for
     * ubiquitous identifiers on any devices and in any languages, including
     * American English and all phone slangs.
     */
    public static final char[] ALPHANUMERIC = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'        
    };
    /**
     * @p Generate a random string of a given <code>length</code> composed
     * from the given character set.
     * 
     * @param length of the string to generate
     * @param set of character to compose from
     * @return a string of <code>length</code> characters
     */
    public static final String random (int length, char[] set) {
        Random random = new Random();
        char[] characters = new char[length];
        for (int i=0; i<length; i++) 
            characters[i] = set[random.nextInt(set.length)];
        return String.copyValueOf(characters);
    }
}

/*  Copyright (C) 2008 Laurent A.V. Szyster
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.async.simple.Strings;

/**
 * Articulate UNICODE text as Public Names and make Public RDF statements.
 * 
 * Note that the <code>articulate</code> methods recurse and do not scale 
 * linearly. Applications should therefore be carefully set their limits on 
 * text size and Public Names horizon. Big text chunks and large semantic 
 * fields will yield deeply nested articulations and consume a lot of CPU
 * and RAM for results of little practical values.
 */
public class SAT {
    
    /**
     * A convenience to produce a regular expression matching one of
     * the pattern.
     * 
     * @pre String expression = SAT.separators(new String[]{
     *    "his", "her", "its", "our", "your", "their"
     *    });
     * 
     * @param patterns
     * @return
     */
    public static final String separators(String[] patterns) {
        int L = patterns.length;
        if (L < 2) {
            throw new RuntimeException(
                "The input array must contain at leas two patterns."
                );
        }
        StringBuffer sb = new StringBuffer();
        sb.append("(?:^|/s+)((?:");
        sb.append(patterns[0]);
        for (int i=1; i<L; i++) {
            sb.append(")|(?:");
            sb.append(patterns[i]);
        }
        sb.append("))(?:$|/s+)");
        return sb.toString();
    }
    
    /**
     * Compile an array of strings into patterns. 
     * 
     * @param expressions to compile
     * @return an array of patterns
     */
    public static final Pattern[] compile(String[] expressions) {
        Pattern[] patterns = new Pattern[expressions.length];
        for (int i=0; i<expressions.length; i++) {
            patterns[i] = Pattern.compile(expressions[i]);
        }
        return patterns;
    }
    
    /**
     * An array of PCRE patterns, used as a stack to articulate ASCII text.
     * 
     * @pre  SAT.articulate(
     *     "Some articulated text.", SAT.ASCII, 126
     *     );
     */
    public static Pattern[] ASCII = compile(new String[]{
        "\\s*[?!.](?:\\s+|$)", // point, split sentences
        "\\s*[:;](?:\\s+|$)", // split head from sequence
        "\\s*,(?:\\s+|$)", // split the sentence articulations
        "(?:(?:^|\\s+)[({\\[]+\\s*)|(?:\\s*[})\\]]+(?:$|\\s+))", // parentheses
        "\\s+[-]+\\s+", // disgression
        "[\"]", // citation
        "(?:^|\\s+)(?:(?:([A-Z]+[\\S]*)(?:$|\\s+)?)+)", // private names
        "\\s+", // white spaces
        "['\\\\\\/*+\\-#]" // common hyphens
        });
    
    /**
     * Split text by moving down a stack of PCRE patterns from a given depth
     * until list of <code>articulated</code> non-empty strings can be filled
     * with more than one token or the bottom of the stack is reached, returns 
     * the depth reached in the stack.
     * 
     * @param text to split
     * @param articulators stack of PCRE expressions
     * @param articulators stack of patterns used to split text
     * @param depth to start from in the articulators stack
     * @param articulated list of non-empty strings
     * @return the depth reached in the stack
     */
    public static final int split (
        String text, Pattern[] articulators, int depth, ArrayList articulated
        ) {
        int bottom = articulators.length;
        Iterator<String> texts;
        String t;
        int L;
        while (depth < bottom) {
            texts = Strings.split(text, articulators[depth]); 
            depth++;
            while (texts.hasNext()) {
                t = texts.next();
                if (t.length() > 0) {
                    articulated.add(t);
                }
            }
            L = articulated.size();
            if (L > 1) {
                break; 
            } else if (L == 1) {
                text = (String) articulated.get(0);
                articulated.clear();
            } else {
                articulated.add(text);
                break;
            }
        }
        return depth;
    }
    
    /**
     * Articulate text as a list of Public Names under a given semantic
     * horizon, walking down a stack of PCRE patterns from a given depth. 
     * 
     * @param text to articulate
     * @param articulators stack of patterns used to split text
     * @param depth to start from in the articulators stack
     * @param horizon limit
     * @return a list of Public Names strings
     */
    public static final ArrayList articulate (
        String text, Pattern[] articulators, int depth, int horizon
        ) {
        ArrayList articulated = new ArrayList();
        depth = split(text, articulators, depth, articulated);
        if (depth == articulators.length) {
            if (articulated.size() == 0) {
                articulated.add(text);
            }
            return articulated;
        } else {
            HashSet field = new HashSet();
            ArrayList names = new ArrayList();
            Iterator iter = articulated.iterator();
            while (iter.hasNext()) {
                names.add(PublicNames.validate (articulate (
                    (String) iter.next(), articulators, depth, horizon
                    ), field, horizon));
            }
            return names;
        }
    }
    
    /**
     * The context of the Public RDF statement articulated.
     */
    public String context;
    /**
     * A limit on the size of text chunks articulated as the 
     * statements subject.
     */
    public int chunk;
    /**
     * The Public RDF consumer for statements articulated.
     */
    public PublicRDF consumer;
    /**
     * The statements' predicate.
     */
    public String predicate = "SAT";
    /**
     * The articulators' stack.
     */
    public Pattern[] articulators = SAT.ASCII;
    /**
     * A limit on the articulated Public Names' horizon.
     */
    public int horizon = 126;
    
    /**
     * 
     * @param context
     * @param chunk
     * @param consumer
     */
    public SAT (String context, int chunk, PublicRDF consumer) {
        this.context = context;
        this.chunk = chunk;
        this.consumer = consumer;
    }
    
    /**
     * 
     * @param context
     * @param chunk
     * @param consumer
     * @param predicate
     * @param articulators
     * @param horizon
     */
    public SAT (
        String context, int chunk, PublicRDF consumer,  
        String predicate, Pattern[] articulators, int horizon
        ) {
        this.context = context;
        this.chunk = chunk;
        this.consumer = consumer;
        this.predicate = predicate;
        this.articulators = articulators;
        this.horizon = horizon;
    }
    
    public void articulate (String text, int depth) throws Throwable {
        ArrayList articulated = new ArrayList();
        depth = split(text, articulators, depth, articulated);
        HashSet field = new HashSet();
        if (depth == articulators.length) {
            consumer.send(PublicNames.validate (articulate (
                text, articulators, depth, horizon
                ), field, horizon), predicate, text, context);
        } else {
            Iterator iter = articulated.iterator();
            while (iter.hasNext()) {
                text = (String) iter.next();
                if (text.length() > chunk) {
                    this.articulate (text, depth);
                } else {
                    consumer.send(PublicNames.validate (articulate (
                        text, articulators, depth, horizon
                        ), field, horizon), predicate, text, context);
                } 
            } 
        }
    }
    
}

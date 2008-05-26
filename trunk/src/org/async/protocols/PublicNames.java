/*  Copyright (C) 2006-2008 Laurent A.V. Szyster
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

package org.async.protocols; // less java for more applications

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A protocol to uniformely encode and validate well-articulated 
 * context graphs of UNICODE strings (may bring some intelligent order to 
 * the semantic chaos of network application interfaces names).
 */
public class PublicNames {
    /**
     * ...
     * 
     * @param encoded
     * @param field
     * @param horizon
     * @return
     */
    public static final String validate (
        String encoded, HashSet field, int horizon
        ) {
        Iterator names = Netunicode.iter(encoded);
        if (!names.hasNext()) {
            if (field.contains(encoded)) {
                return null;
            }
            field.add(encoded); return encoded;
        } else {
            String name; ArrayList valid = new ArrayList();
            do {
                name = validate((String)names.next(), field, horizon);
                if (name != null) {
                    valid.add(name); 
                    if (field.size() >= horizon) {
                        break;
                    }
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return Netunicode.encode(valid.iterator());
            }
            if (valid.size() > 0) {
                return (String)valid.get(0);
            }
            return null;
        }
    }
        
    /**
     * 
     * @param encoded
     * @param field
     * @param horizon
     * @param tree
     * @return
     */
    public static final String validate (
        String encoded, HashSet field, int horizon, ArrayList tree
        ) {
        Iterator names = Netunicode.iter(encoded);
        if (names.hasNext()) {
            String name;
            ArrayList valid = new ArrayList();
            ArrayList branch = new ArrayList();
            do {
                name = validate((String)names.next(), field, horizon, branch);
                if (name != null) {
                    if (branch.size() > 1) {
                        tree.add(branch);
                    } else {
                        tree.add(name);
                    }
                    branch = new ArrayList(); 
                    valid.add(name);
                    if (field.size() >= horizon) {
                        break;
                    }
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return Netunicode.encode(valid.iterator());
            }
            if (valid.size() > 0) {
                return (String)valid.get(0);
            }
            return null;
        } else {
            if (field.contains(encoded)) {
                return null;
            }
            field.add(encoded); 
            return encoded;
        }
    }
    
    /**
     * 
     * @param articulated
     * @param field
     * @param horizon
     * @return
     */
    public static final String validate (
        ArrayList articulated, HashSet field, int horizon
        ) {
        Iterator names = articulated.iterator();
        if (!names.hasNext()) {
            return null;
        } else {
            Object item;
            String name;
            ArrayList valid = new ArrayList();
            do {
                item = names.next(); 
                if (item instanceof String) {
                    name = (String)item;
                    if (!field.contains(name)) {
                        valid.add(name); 
                        field.add(name);
                        if (field.size() >= horizon) {
                            break; 
                        }
                    }
                } else if (item instanceof ArrayList) {
                    name = validate((ArrayList)item, field, horizon);
                    if (name != null) {
                        valid.add(name);
                    }
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return Netunicode.encode(valid.iterator());
            }
            if (valid.size() > 0) {
                return (String)valid.get(0);
            }
            return null;
        }
    }
    
    /**
     * ...
     */
    public String encoded;
    /**
     * ...
     */
    public ArrayList articulated;
    /**
     * ...
     */
    public HashSet field = new HashSet();
    
    /**
     * ...
     * 
     * @param encoded
     * @param horizon
     */
    public PublicNames(String encoded, int horizon) {
        articulated = new ArrayList();
        this.encoded = validate(encoded, field, horizon, articulated);
    }
    
    /**
     * ...
     * 
     * @param articulated
     * @param horizon
     */
    public PublicNames(ArrayList articulated, int horizon) {
        this.articulated = articulated;
        this.encoded = validate(articulated, field, horizon);
    }
    
    /**
     * ...
     */
    public static int HORIZON = 126;
    
    /**
     * ...
     * 
     * @param encoded
     */
    public PublicNames(String encoded) {
        articulated = new ArrayList();
        this.encoded = validate(encoded, field, HORIZON, articulated);
    }
    
    /**
     * ...
     * 
     * @param articulated
     */
    public PublicNames(ArrayList articulated) {
        this.articulated = articulated;
        encoded = validate(articulated, field, HORIZON);
    }
    
    /**
     * ...
     */
    public String toString() {return encoded;}
    
} // that's all folks.
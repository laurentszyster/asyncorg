package org.async.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Functional conveniences to extend <code>List</code>, update <code>Map</code> 
 * and iterate through list and map values using <code>Object</code> arrays.
 */
public final class Objects {

    protected static final class ObjectIterator implements Iterator {
        private Object[] _objects;
        private int _index = -1;
        public ObjectIterator (Object[] objects) {
            _objects = objects;
        }
        public final boolean hasNext () {
            return _index + 1 < _objects.length;
        }
        public final Object next () {
            _index++; return _objects[_index];
        }
        public final void remove () {
            /* optional interface? what else now ...*/
        }
    }

    /**
     * A convenience to extend a <code>List</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre List sequence = Simple.extend(
     *     new ArrayList(), new Object[]{"a", "b", "c"}
     *     );
     * 
     * @param items to add in the sequence
     * @return the extended <code>List</code>
     */
    public static final List extend (List sequence, Object[] items) {
        for (int i=0; i<items.length; i++)
            sequence.add(items[i]);
        return sequence;
    }
    
    /**
     * A convenience to create an <code>ArrayList</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre ArrayList sequence = Simple.list(
     *     new Object[]{"a", "b", "c"}
     *     );
     * 
     * @param items to add in the sequence
     * @return a new <code>ArrayList</code>
     */
    public static final ArrayList list (Object[] items) {
        ArrayList sequence = new ArrayList();
        for (int i=0; i<items.length; i++)
            sequence.add(items[i]);
        return sequence;
    }
    
    /**
     * A convenience to build a <code>HashSet</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre HashSet set = Simple.set(new String[]{"a", "b", "c"});
     * 
     * @param items to add in the set 
     * @return a <code>HashSet</code>
     */
    public static final HashSet set (Object[] items) {
        HashSet result = new HashSet();
        for (int i=0; i<items.length; i++)
            result.add(items[i]);
        return result;
    }
    
    /**
     * A convenience to iterate around a "primitive" array. 
     * 
     * @pre Iterator iter = Simple.iter(new Object[]{x, y, z});
     *     
     * @p Usefull to iterate through final arrays, a prime construct in web
     * application controllers where check lists and filter sets made of
     * primitive array abound (usually to enforce business rules).
     * 
     * @param objects the array to iterate through
     * @return iterator yields all objects in the array
     */
    public static final Iterator iter (Object[] objects) {
        return new ObjectIterator(objects);
        }
    
    protected static class MapIterator implements Iterator {
        private Map _map;
        private Iterator _iter;
        public MapIterator (Map map, Iterator iter) {
            _map = map; _iter = iter;
            }
        public boolean hasNext () {return _iter.hasNext();}
        public Object next () {return _map.get(_iter.next());}
        public void remove () {/* optional interface? what else now ...*/}
    }

    /**
     * Update a <code>Map</code> with the keys and values sequence found in
     * an even <code>Object</code> array.
     * 
     * @pre HashMap map = Simple.update(new HashMap(), new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": new Integer(1), 
     *     "D", false
     *     });
     * 
     * @p This method is convenient to instanciate or update dictionaries 
     * with a clearer syntax than using a sequence of <code>Map.put</code>.
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>Map</code>
     */
    public static final Map update (Map map, Object[] pairs) {
        for (int i=0; i<pairs.length; i=i+2)
            map.put(pairs[i], pairs[i+1]);
        return map;
    }
    
    /**
     * Update a new <code>HashMap</code> with the keys and values sequence 
     * found in an even <code>Object</code> array.
     * 
     * @pre HashMap map = Simple.dict(new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": new Integer(1), 
     *     "D", false
     *     });
     * 
     * @p This method is convenient to instanciate unsynchronized dictionaries 
     * with a clearer syntax than using a sequence of <code>HashMap.put</code>.
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>HashMap</code>
     */
    public static final HashMap dict (Object[] pairs) {
        HashMap map = new HashMap();
        for (int i=0; i<pairs.length; i=i+2)
            map.put(pairs[i], pairs[i+1]);
        return map;
    }
    
    /**
     * Iterate through arbitrary values in a <code>Map</code>.
     * 
     * @pre HashMap map = Simple.dict(new Object[]{
     *    "A", "test", 
     *    "B", true, 
     *    "C": new Integer(1), 
     *    "D", false
     *    });
     * Iterator values = Simple.iter(map, new Object[]{
     *    "A", "C", "E"
     *    });
     * 
     * @p This method is convenient to extract an ordered set of named
     * values from a dictionary using a <code>Object</code> array.
     * 
     * @test var map = Simple.dict([
     *    "A", "test", 
     *    "B", true, 
     *    "C", 1, 
     *    "D", false
     *    ]);
     *var values = Simple.iter(map, ["A", "C", "E"]);
     *return (
     *    values.next() == "test" &&
     *    values.next() == 1 &&
     *    values.next() == null &&
     *    values.hasNext() == false
     *    );
     * 
     * @param map the <code>Map</code> to iterate through
     * @param keys and array of keys to iterate through 
     * @return an <code>Iterator</code>
     */
    public static final Iterator iter (Map map, Object[] keys) {
        return new MapIterator(map, iter(keys));
    }
    
}

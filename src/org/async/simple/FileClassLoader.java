/*
 *  Copyright (C) 2007 Laurent A.V. Szyster
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

package org.async.simple;

import java.io.File;
import java.io.FileInputStream;

/**
 * A really simple class file loader, for applications that need to reload
 * simple parts or restrict loadable classes to a filesystem path.
 * 
 * @h3 Note
 * 
 * @p Don't even think about glorifying this into some kind of mighty
 * be-all-do-all uberClassLoader. Production Java applications are not 
 * actually expected to reload their classes bytecodes, they are supposed 
 * to JIT that once for all. And when they do demand reloadable classes, 
 * a configurable filesystem restriction is all you will ever practically 
 * use. Loading classes from the network, from a signed and compressed
 * archive may sound cool but have little practical use in case of reload.
 * 
 * @p Reloading large JARs with many dependicies is a recipe for worse than 
 * failure. Restart the JVM instead.
 * 
 * @p Loading bytecode from the network open the door for too many exploit.
 * Instead, use SVN to update the application's components.
 * 
 */
public class FileClassLoader extends ClassLoader {
    private String _path;
    /**
     * Create a new loader that reads byte class definition files from
     * a given file system path. 
     * 
     * @param path to prefix class file names
     * 
     * @pre public final class ClassLoaderTest {
     *    public static final void main (String[] args) throws Throwable {
     *        Class cl = (new FileClassLoader("bin/")).findClass(
     *            "org.async.tests.DispatcherTest"
     *            ); 
     *        ((Function) cl.newInstance()).apply(args);
     *        Static.loop.dispatch();
     *    }
     *}
     * 
     */
    public FileClassLoader (String path) {
        super();
        if (path==null || !path.endsWith("/")) {
            throw new Error("Invalid or null path");
        }
        _path = path;
    }
    /**
     * Find and load a class by its fully qualified name.
     * 
     * @param name of the class
     * 
     * @p For instance:
     * 
     * @pre Class cl = (new FileClassLoader("bin/")).findClass(
     *    "org.async.tests.DispatcherTest"
     *    );
     *    
     * @p will load the binary class file found at:
     * 
     * @pre bin/org/async/tests/DispatcherTest
     * 
     * @p Note that this implementation will fail to load enclosed classes. 
     * 
     */
    public final Class findClass(String name) throws ClassNotFoundException {
        byte[] bytes = null;
        try {
            FileInputStream fis = new FileInputStream(new File(
                _path + name.replaceAll("\\.", "/") + ".class"
                ));
            try {
                bytes = new byte[fis.available()];
                fis.read(bytes);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(
                "Class " + name + " not found, " + e.getMessage()
                );
        }
        return defineClass(name, bytes, 0, bytes.length);
    }
}
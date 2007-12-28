package org.async.tests;

import org.async.core.Static;
import org.async.core.Function;
import org.async.simple.FileClassLoader;

public final class ClassLoaderTest {
    public static final void main (String[] args) throws Throwable {
        Class cl = (new FileClassLoader("bin/")).findClass(
            "org.async.tests.DispatcherTest"
            );
        ((Function) cl.newInstance()).apply(args);
        Static.loop.dispatch();
    }
}

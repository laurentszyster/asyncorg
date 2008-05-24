import org.async.core.Static;
import org.async.simple.FileClassLoader;
import org.async.simple.Fun;

public final class ClassLoaderTest {
    public static final void main (String[] args) throws Throwable {
        Class cl = (new FileClassLoader("bin/")).findClass(
            "org.async.tests.DispatcherTest"
            );
        ((Fun) cl.newInstance()).apply(args);
        Static.loop.dispatch();
    }
}

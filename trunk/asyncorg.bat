SET JVM=java -Xmx64M -Xms64M -XX:MaxHeapFreeRatio=20 -XX:MinHeapFreeRatio=10
SET DLLS=-Djava.library.path=lib 
SET CLASSPATH=-cp lib/js.jar;lib/xp.jar;lib/protocols.jar;lib/sqlite.jar;asyncorg.jar
SET MAIN=org.async.prototypes.Stateful
%JVM% %CLASSPATH% %DLLS% %MAIN% %1 1>log\http.out
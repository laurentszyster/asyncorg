package org.async.core;

/**
 * Functional conveniences to support 8-bit bytes protocols.
 */
public class Bytes {

    public static final int find (byte[] what, byte[] in, int from) {
        // so faster in C, so why does the JVM came without this?
        int i;
        int limit = in.length - what.length;
        for (; from < limit; from++) {
            if (in[from]==what[0]) {
                for (i=1; i<what.length; i++) {
                    if (in[from+i]!=what[i]) {
                        break;
                    }
                }
                if (i==what.length) {
                    return from;
                }
            }
        }
        return -1;
    }

}

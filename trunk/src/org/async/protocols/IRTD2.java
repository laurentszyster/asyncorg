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

package org.async.protocols;

import java.util.Iterator;

import org.async.simple.Objects;
import org.async.simple.Strings;

/**
 * An implementation of IRTD2, a protocol to support distributed 
 * authentication of user agents' identity and rights in time and
 * sequence.
 * 
 * @p There are four benefits to expect from IRTD2 cookies for
 * J2EE public applications:
 * 
 * <ol>
 * <li>Supply an alternative solution to statefull sessions.</li>
 * <li>Distribute the load of authorization on a cluster of servers 
 * without adding the contention and latency implied by statefull
 * session synchronization.</li>
 * <li>Audit identifed and authorized interactions in sequence and time.</li>
 * <li>Detect impersonation exploit by a man-in-the-middle.</li>
 * </ol>
 * 
 * @p Note that IRTD2 does <em>not</em> prevent impersonation when its 
 * transport protocol is not encrypted. Instead, it supports a torough audit 
 * of user interaction and the detection of fraudulent actions. Which is
 * a much safer way to protect a network service from malicious users.
 * 
 */
public class IRTD2 {
    
    /**
     * Text description of the four IRTD2 status code: 0, 1, 2 and 3,
     * litteraly:
     * 
     * @pre ["Ok", "Invalid", "Timed-out", "Fraudulent"]
     * 
     */
    public static final String[] errors = new String[]{
        "Ok", "Invalid", "Timed-out", "Fraudulent"
    };
    
    /**
     * Parse any string as an IRTD2 vector of five strings.
     * 
     * @param irtd2 the string to parse
     * @return a vector of five strings
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("")
     *    ) == '["","","","",""]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I")
     *    ) == '["I","","","",""]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I R")
     *    ) == '["I","R","","",""]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I R T")
     *    ) == '["I","R","T","",""]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I R T D")
     *    ) == '["I","R","T","D",""]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I R T D 2")
     *    ) == '["I","R","T","D","2"]';
     * 
     * @test return JSON.encode(
     *    IRTD2.parse("I R T D 2 X Y Z")
     *    ) == '["I","R","T","D","2"]';
     */
    public static final String[] parse (String irtd2) {
        Iterator tokens = Strings.split(irtd2, ' ');
        return new String[] {
            (tokens.hasNext()) ? (String) tokens.next() : "", // identity
            (tokens.hasNext()) ? (String) tokens.next() : "", // rights
            (tokens.hasNext()) ? (String) tokens.next() : "", // time
            (tokens.hasNext()) ? (String) tokens.next() : "", // digested
            (tokens.hasNext()) ? (String) tokens.next() : ""  // digest
        };
    }
    
    /**
     * Test an IRTD2 string vector to have been digested with one of the
     * given salts and before the specified timeout, return an error code. 
     * 
     * @param irtd2 a vector of at least 4 strings
     * @param time of digestion
     * @param timeout limit set as the maximul interval between two digests
     * @param salts to digest
     * @return 0 in case of success; 1, 2 or 3 in case of failure
     * 
     * @test return IRTD2.digested([
     *    'Identity', 'Rights', '1195809876810',
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b',
     *    '6a05ffad8c723420058085c9723ef90284c94382'
     *    ], 1195809876830, 3600, [[1,2,3]]) == 0; 
     * 
     * @test return IRTD2.digested([
     *    'Identity', 'Rights', '1195809876810',
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b',
     *    '6a05ffad8c723420058085c9723ef90284c94382'
     *    ], 1195809876830, 3600, [[4,5,6], [1,2,3]]) == 0; 
     * 
     * @test return IRTD2.digested([
     *    'Identity', 'Rights', 'Not a Number',
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b',
     *    '6a05ffad8c723420058085c9723ef90284c94382'
     *    ], 1195809876830, 3600, [[1,2,3]]) == 1; 
     *    
     * @test return IRTD2.digested([
     *    'Identity', 'Rights', '1195809876810',
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b',
     *    '6a05ffad8c723420058085c9723ef90284c94382'
     *    ], 1195809888888, 3600, [[1,2,3]]) == 2; 
     *    
     * @test return IRTD2.digested([
     *    'Identity', 'Rights', '1195809876810',
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b',
     *    '6a05ffad8c723420058085c9723ef90284c94382'
     *    ], 1195809876830, 3600, [[4,5,6]]) == 3; 
     *    
     */
    public static final int digested (
        String[] irtd2, long time, int timeout, byte[][] salts
        ) {
        long t;
        try {
            t = Long.parseLong(irtd2[2]);
        } catch (Exception e) {
            return 1;
        }
        int interval = (int)(time - t);
        if (interval > timeout) {
            return 2;
        } 
        byte[] irtd = Strings.join(" ", Objects.iter(
            irtd2[0], irtd2[1], irtd2[2], irtd2[3]
            )).getBytes();
        String digest = null;
        for (int i=0; i<salts.length; i++) {
            SHA1 md = new SHA1();
            md.update(irtd);
            md.update(salts[i]);
            digest = md.hexdigest();
            if (digest.equals(irtd2[4])) {
                return 0;
            }
        }
        return 3;
    }
    
    /**
     * Digest a vector of four strings using the salt provided. 
     * 
     * @test return IRTD2.digest([
     *    'Identity', 
     *    'Rights', 
     *    '1195809876810', 
     *    ''
     *    ], [1,2,3]
     *    ) == '91a5b70dd590b60275f89e46f4eb140852e85d5b';
     * 
     * @test return IRTD2.digest([
     *    'Identity', 
     *    'Rights', 
     *    '1195809876810', 
     *    '91a5b70dd590b60275f89e46f4eb140852e85d5b'
     *    ], [1,2,3]
     *    ) == '6a05ffad8c723420058085c9723ef90284c94382';
     * 
     * @param irtd2 a vector of at least 4 strings
     * @param salt the bytes appended to the digested string
     * @return a SHA1 hexdigest
     */
    public static final String digest (String[] irtd2, byte[] salt) {
        StringBuffer sb = new StringBuffer();
        sb.append(irtd2[0]);
        sb.append(' ');
        sb.append(irtd2[1]);
        sb.append(' ');
        sb.append(irtd2[2]);
        sb.append(' ');
        if (irtd2[3] != null) {
            sb.append(irtd2[3]);
        }
        String irtd = sb.toString();
        SHA1 md = new SHA1();
        md.update(irtd.getBytes());
        md.update(salt);
        return md.hexdigest();
    }
    
}

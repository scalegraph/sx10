/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */

import harness.x10Test;


/**
 * Check lazy, per-field, per-place initialization semantics of static fields.
 * Valid for X10 2.2.3 and later.
 * 
 * @author mtake 7/2012
 */
public class InitStaticField3d extends x10Test {

    static val a2 = null as Any;
    static val b = "abc" as String;
    static val b3 = "abc" as Any;
    static val c = 1 as Int;
    static val c2 = 1 as Any;

    static val xa2 = null as Any as Any;
    static val xb = "abc" as String as String;
    static val xb3 = "abc" as Any as Any;
    static val xc = 1 as Int as Int;
    static val xc2 = 1 as Any as Any;
    static val xc3 = 1 as Int as Any;

    public def run():Boolean {
        chk(a2 == null);
        chk(b.equals("abc"));
        chk(b3.equals("abc"));
        chk(c == 1);
        chk(c2 == 1);

        chk(xa2 == null);
        chk(xb.equals("abc"));
        chk(xb3.equals("abc"));
        chk(xc == 1);
        chk(xc2 == 1);
        chk(xc3 == 1);

        return true;
    }

    public static def main(Array[String](1)) {
        new InitStaticField3d().execute();
    }

}

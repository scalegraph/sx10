/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

import harness.x10Test;

/**
 * @author bdlucas 10/2008
 */

class XTENLANG_22 extends x10Test {

    class B {
        public def m() = 0;
    }
    
    interface I {
        def m(): int;
    }
    
    class A extends B implements I {}
    
    class X {
        val a = new A();
        val x = a.m();
    }

    public def run(): boolean {
        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_22().execute();
    }
}

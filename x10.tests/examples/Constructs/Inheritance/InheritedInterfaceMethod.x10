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
 * Inherited methods should satisfy interface dependences.
 * Adapted from the test case for XTENLANG-22.
 * @author igor 06/2009
 */
class InheritedInterfaceMethod extends x10Test {

    class A {
        public def m() = 0;
    }

    interface I {
        def m(): int;
    }

    class B extends A implements I {}

    public def run(): boolean {
        val v = new B();
        return v.m() == 0;
    }

    public static def main(Array[String](1)) {
        new InheritedInterfaceMethod().execute();
    }
}


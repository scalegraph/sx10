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
 * Test for X10 arrays -- tests arrays passed as parameters and stored in fields.
 */
public class Array4 extends x10Test {

    var ia: Array[int](2);

    public def this(): Array4 = {}

    public def this(var ia: Array[int](2)): Array4 = {
        this.ia = ia;
    }

    private def runtest(): boolean = {
        ia(1, 1) = 42;
        return 42 == ia(1, 1);
    }

    /**
     * Run method for the array. Returns true iff the test succeeds.
     */
    public def run(): boolean = {
        return (new Array4(new Array[int]((1..10)*(1..10), 0))).runtest();
    }

    public static def main(var args: Array[String](1)): void = {
        new Array4().execute();
    }
}

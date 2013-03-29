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
import x10.array.*;


/**
 * Check that a single statement in an if, subject to flattening, is handled
 * correctly.
 */

public class FlattenConditional extends x10Test {

    var a: Array[int](2);

    public def this(): FlattenConditional = {
        a = new Array[int](Region.make([1..10, 1..10]), ([i,j]: Point) => { return i+j;});
    }
    
    def m(a: int): int = {
        if (a == 2) throw new Exception();
        return a;
    }

    // m(a[1,1]) should not be executed. If the conditional is flattened
    // so that the body is moved out before the if, then it will be executed
    // and the test will fail.
    public def run(): boolean = {
        var b: int = 0;
        if (a(2, 2) == 0)
            b = m(a(1, 1));
        return b==0;
    }

    public static def main(Rail[String]) {
        new FlattenConditional().execute();
    }
    
}

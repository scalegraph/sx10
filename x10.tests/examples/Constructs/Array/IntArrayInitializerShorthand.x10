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
 * Test the shorthand syntax for an array initializer.
 */

public class IntArrayInitializerShorthand extends x10Test {

    public def run(): boolean {
        val r = (1..10)*(1..10);
        val ia = new Array[int](r, ([i,j]:Point) => i+j);

        for (val p[i,j]: Point(2) in r)
            chk(ia(p) == i+j);

        return true;
    }

    public static def main(Rail[String]) {
        new IntArrayInitializerShorthand().execute();
    }
}

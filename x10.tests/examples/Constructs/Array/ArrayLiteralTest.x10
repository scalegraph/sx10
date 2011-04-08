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
 * Array Literal test.
 */

public class ArrayLiteralTest extends x10Test {

    public def run(): boolean = {

        val e = 0..9;
        val r = [1, 2, 3];
        val a = [1 as Int,2 as Int,3 as Int];
        var sumr:int=0, suma:int=0;
        for (i in a.values()) suma += i;
        for (i in r.values()) sumr += i;
        return suma==6 && sumr==6;
    }

    public static def main(Array[String](1)): void = {
        new ArrayLiteralTest().execute();
    }
}

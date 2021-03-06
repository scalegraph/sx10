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

public class ArrayPlusEqual extends x10Test {

    val v = new Rail[int](2, (x:int)=>0);

    public def run() {
        for ([i] in 0..1) v(i) += 5;
        for ([i] in 0..1) chk(v(i) == 5);
        return true;
    }

    public static def main(Array[String](1)) {
        new ArrayPlusEqual().execute();
    }
}

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
 * Test serialization of arrays between places.
 */
public class ArraySerialization extends x10Test {

    public def run():boolean {
        val a1 = new Array[int](0..20, (p:Point(1))=>p(0));
        at (here.next()) {
	    for ([i] in a1) {
	        chk(a1(i) == i);
            }
        }

        return true;
    }

    public static def main(Array[String](1)) {
        new ArraySerialization().execute();
    }
}

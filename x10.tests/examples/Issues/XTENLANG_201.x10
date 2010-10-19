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
 * @author bdlucas 12/2008
 */

class XTENLANG_201 extends x10Test {

    public def run(): boolean {
        try {
            val r = Rail.make[Rail[int]](100);
            for (var i:int=0; i<100; i++)
                r(i) = Rail.make[int](1000*1000*1000);
        } catch (Error) {
            return true;
        }
        return false;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_201().execute();
    }
}

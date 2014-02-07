/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

public class XTENLANG_643 extends x10Test {

    public static def test() {
        type Ome(x:Long) = String;
        var result:Boolean = "some string " instanceof Ome(5);
        return result;
    }
    public def run() = test();


    public static def main(Rail[String]) {
        new XTENLANG_643().execute();
    }

}


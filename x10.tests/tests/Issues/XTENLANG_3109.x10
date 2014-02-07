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

class XTENLANG_3109[T] extends x10Test {


    public def reduce(unit:T) {
        class Inner {
            public def m():T = unit;
        };
    }
    
    public def run(): boolean {
        return true;
    }

    public static def main(Rail[String]) {
        new XTENLANG_3109[String]().execute();
    }
}

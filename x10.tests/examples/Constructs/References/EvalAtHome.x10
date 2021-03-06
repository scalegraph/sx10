/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright Australian National University 2011.
 */

import harness.x10Test;

/**
 * Tests evaluating a closure over the referenced object
 * at the home place of a GlobalRef.
 * @author milthorpe 06/2011
 */
class EvalAtHome extends x10Test {
    public def run(): boolean {
        val x = new Cell[Int](1);
        val globalX = new GlobalRef[Cell[Int]](x);
        at (here.next()) {
            val y = globalX.evalAtHome[Int]((a:Cell[Int]) => (a()+1));
            chk(y == 2);
        }
        return true;
    }

    public static def main(Array[String](1)) {
        new EvalAtHome().execute();
    }
}

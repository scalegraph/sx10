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

import x10.util.Box;
import harness.x10Test;

/**
 * Testing a variable scope problem.
 * The second q's scope does not overlap with the first q.
 */
public class VariableScope extends x10Test {

    public def run(): boolean = {

        val N: int = 10;
        var e: Region(1){rect} = 1..N;
        var r: Region(2){rect} = e*e;
        var d: Dist = r->here;
        var n: int = 0;

        for (val p: Point in e)
            for (val q: Point in e) {
                n++;
            }

        for (val p: Point in d) {
            var q: Box[Point] = null;
            n++;
        }

        return n == 2 * N * N;
    }

    public static def main(var args: Array[String](1)): void = {
        new VariableScope().execute();
    }
}

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
 * Test for for ... async.
 *
 * @author kemal, 12/2004
 */
public class Foreach2 extends x10Test {

    public static N: int = 100;
    var nActivities: int = 0;

    public def run(): boolean = {
        val P0 = here; // save current place
        val r = 0..N-1;
        val d = r->P0;

        finish
            for (p in d.region) async {
                // Ensure each activity spawned by for .. async
                // runs at P0
                // and that the hasbug array was
                // all false initially
                if (P0 != d(p) || P0 != here)
                    throw new Error("Test failed.");
                atomic { nActivities++; }
            }
        return nActivities == N;
    }

    public static def main(var args: Array[String](1)): void = {
        new Foreach2().execute();
    }
}

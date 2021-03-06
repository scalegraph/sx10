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
 * Minimal test for dists.
 */

public class DistributionTest extends x10Test {

    public def run(): boolean = {
        val r = 0..100; //(low, high)
        val R = r*r;
        val d  = R->here;
        return ((d.rank == 2) &&
                (R.rank == 2) &&
                (R.max(1) - R.min(1) + 1 == 101));
    }

    public static def main(var args: Array[String](1)): void = {
        new DistributionTest().execute();
    }
}

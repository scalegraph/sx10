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
 * Minimal test for the empty region.
 */

public class RegionTest2 extends x10Test {

    public def run(): boolean = {

        var reg: Region = 0..-1;

        var sum: int = 0;
        for (val p: Point in reg) sum++;

        return sum == 0;
    }

    public static def main(var args: Array[String](1)): void = {
        new RegionTest2().execute();
    }
}

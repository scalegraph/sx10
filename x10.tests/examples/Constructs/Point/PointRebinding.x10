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
 * Must allow binding components to an existing point.
 *
 * @author igor, 1/2006
 */

public class PointRebinding extends x10Test {

    public def run(): boolean = {

        val p: Point = [1, 2] as Point;
        val [i, j]: Point = p;

        return (i == 1 && j == 2);
    }

    public static def main(var args: Array[String](1)): void = {
        new PointRebinding().execute();
    }
}

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
 * Minimal test for distribution restriction.
 */

public class Restrict extends x10Test {

    public def run(): boolean = {
        val r = 0..100;
        val R = r*r;
        val d = R->here;
        val R2  = (d | here).region;
        x10.io.Console.OUT.println("R " + R);
        x10.io.Console.OUT.println("R2 " + R2);
        x10.io.Console.OUT.println("R.size() " + R.size());
        x10.io.Console.OUT.println("R2.size() " + R2.size());
        x10.io.Console.OUT.println("d " + d);
        x10.io.Console.OUT.println("(d|here) " + (d|here));
        return (R.size() == R2.size());
    }

    public static def main(var args: Array[String](1)): void = {
        new Restrict().execute();
    }
}

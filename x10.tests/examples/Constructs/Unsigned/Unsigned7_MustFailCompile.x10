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
 * Simple unsigned test.
 */
public class Unsigned7_MustFailCompile extends x10Test {

    public def run(): boolean = {
        val a: ubyte = 0;
        val b: ubyte = 1;
        val c: ubyte = -1; // ERR
        val d: ubyte = 127;
        val e: ubyte = 128;
        val f: ubyte = 255;
        val g: ubyte = 256; // ERR
        return true;
    }

    public static def main(Array[String](1)) = {
        new Unsigned7_MustFailCompile().execute();
    }
}

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
 * The x10-style initializer for java arrays should work.
 *
 * @author kemal 8/2005
 */

public class JavaArrayWithInitializer extends x10Test {

    static N: int = 25;

    public def run(): boolean = {

        val foo1  = new Array[int](0..(N-1), ([i]: Point)=> i);

        x10.io.Console.OUT.println("1");

        for (val [i]: Point in 0..(N-1)) chk(foo1(i) == i);
        val foo2  = new Array[int](0..(N-1), ([i]: Point)=>i);

        x10.io.Console.OUT.println("2");

        for (val [i]: Point(1) in 0..(N-1)) chk(foo2(i) == i);

        return true;
    }

    public static def main(Rail[String]) {
        new JavaArrayWithInitializer().execute();
    }
}

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

//LIMITATION: overloading is by erasure

import harness.x10Test;



/**
 * Two or more methods of a class or interface may have the same name
 * if they have a different number of type parameters, or they have
 * value parameters of different types.
 *
 * @author bdlucas 8/2008
 */

public class GenericOverloading05 extends GenericTest {

    def m(String) = 0;
    def m(int) = 1;
    def m(Array[String]) = 2;
    def m(Array[Int]) = 3;

    public def run(): boolean = {

        genericCheck("m(\"1\")", m("1"), 0);
        genericCheck("m(1)", m(1), 1);
        genericCheck("m([\"0\"])", m(["0" as String]), 2);
        genericCheck("m([0])", m([0 as Int]), 3);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new GenericOverloading05().execute();
    }
}

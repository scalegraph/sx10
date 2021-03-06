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
 * @author bdlucas 8/2008
 */

public class GenericOverriding06 extends GenericTest {

    static class A[T] {
        def m(): int = 0;
        def m(T): int = 1;
        def m(int,T): int = 2;
    }

    static class B[T] extends A[T] {
        def m(T): int = 4;
    }

    val a = new A[int]();
    val b = new B[int]();

    public def run(): boolean = {

        genericCheck("a.m()", a.m(), 0);
        genericCheck("a.m(0)", a.m(0), 1);
        genericCheck("a.m(0,0)", a.m(0,0), 2);
        genericCheck("b.m()", b.m(), 0);
        genericCheck("b.m(0)", b.m(0), 4);
        genericCheck("b.m(0,0)", b.m(0,0), 2);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new GenericOverriding06().execute();
    }
}

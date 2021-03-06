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
 * STATIC SEMANTICS RULE: If a class C overrides a method of a class or
 * interface B, the guard of the method in B must entail the guard of the
 * method in C.
 *
 * @author bdlucas 8/2008
 */

public class GenericOverriding13 extends GenericTest {

    static class A[T] {
        def m[U](T): int = 0;
    }

    static class B[T] extends A[T] {
        def m[U](T): int = 1;
    }

    val a = new A[int]();
    val b = new B[int]();

    public def run() = {

        genericCheck("a.m[String](0)", a.m[String](0), 0);
        genericCheck("b.m[String](0)", b.m[String](0), 1);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new GenericOverriding13().execute();
    }
}

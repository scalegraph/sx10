/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

/**
 * @author bdlucas 10/2008
 */

class XTENLANG_112 extends x10Test {

    static class D {}
    
    static class A[T](d:D) {
        static type A[T](d:D) = A[T]{self.d==d};
        static def make[T](d:D): A[T](d) {return null;}
        def this(d:D) = property(d);
    }
    
    static val d: D = new D();
    static val a: A[int] = A.make[int](d);

    public def run(): boolean {
        return true;
    }

    public static def main(Rail[String]) {
        new XTENLANG_112().execute();
    }
}

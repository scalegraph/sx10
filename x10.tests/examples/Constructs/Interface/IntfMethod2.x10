/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */

/*
 * Check the implementation of interface method that returns primitive types.
 * cf. XTENLANG-2993.
 */

import harness.x10Test;

public class IntfMethod2 extends x10Test {
    interface I[T] {}
    interface J[T] { def compare(I[T]):Any; }
    abstract static class S implements J[Int] {
        public abstract def compare(I[Int]):Any;                
    }
        
    interface K[T] { def compare(I[T]):Int; }
    interface L[T] { def compare(I[T]):Double; }
    interface M[T] { def compare(I[T]):UInt; }
    interface N[T] { def compare(I[T]):Int; }
    interface O[T] { def compare(I[T]):void; }
    interface P[T] { def compare(I[T]):Any; }
    static class C extends S implements K[Int], L[S], M[Float], N[Any], O[UInt], P[ULong] {
        public def compare(I[Int]) = 1;
        public def compare(I[S]) = 2.0;
        public def compare(I[Float]) = 3u;
        public def compare(I[Any]) = 4;
        public def compare(I[UInt]) {}
        public def compare(I[ULong]):Any = null;
    }
        
    public def run():Boolean {
        val c = new C();
        val s:S = c;
        val j:J[Int] = c;
        val k:K[Int] = c;
        val l:L[S] = c;
        val m:M[Float] = c;
        val o:O[UInt] = c;

        val vs = s.compare(null);
        val vj = j.compare(null);
        val vk = k.compare(null);
        val vl = l.compare(null);
        val vm = m.compare(null);
        o.compare(null);
        
        val vck = c.compare(null as I[Int]);
        val vcl = c.compare(null as I[S]);
        val vcm = c.compare(null as I[Float]);
        c.compare(null as I[UInt]);

        return true;
    }

    public static def main(Array[String](1)) {
        new IntfMethod2().execute();
    }
}

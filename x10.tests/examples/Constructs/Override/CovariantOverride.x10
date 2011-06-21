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
 * Test compilation of methods with covariant override.
 */
public class CovariantOverride extends x10Test {

    static class A[T] {
	def f() : Any = 1;
	def g() : Any = "abc";
	def h() : T = 1 as T;
    }

    static class B extends A[UInt] {
	def f() : Int = 2;
	def g() : String = "efg";
	def h() : UInt = 3u;
    }

    public def run(): boolean = {
	val b = new B();
	chk(b.f() == 2);
	chk(b.g() == "efg");
	chk(b.h() == 3u);
	return true;
    }

    public static def main(Array[String](1)) = {
        new CovariantOverride().execute();
    }
}

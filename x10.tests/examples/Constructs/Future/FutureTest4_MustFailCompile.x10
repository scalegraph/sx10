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

import x10.util.concurrent.Future;
/**
 * Tests that free variables used in e in future { e }
 * are declared final.
 *
 * Expected result: must fail at compile time.
 *
 * @author kemal 4/2005
 */
public class FutureTest4_MustFailCompile extends x10Test {

	public static N: int = 8;

	/**
	 * testing free variables in future expression
	 */
	public def run(): boolean = {
		val A = DistArray.make[int](Dist.makeBlock((0..(N-1))*(0..(N-1))),
		  ([i,j]: Point): int =>  N*i+j);
		var x: int=0;
		var s: int=0;
		for ([i] in 0..(N-1)) {
			s += i;
			//=== >compiler error: s not final  (i is final!)
			x += Future.make[int](() => at(A.dist([i,
			        s // ERR: Local variable "s" is accessed from an inner class or a closure, and must be declared final.
			        %N] as Point(A.rank)))  A(i,
			        s // ERR: Local variable "s" is accessed from an inner class or a closure, and must be declared final.
			        %N) ).force();
		}
		x10.io.Console.OUT.println("x = "+x);
		if (x != 252) return false;
		x = 0;
		s = 0;
		for ([i]  in 0..(N-1)) {
			s += i;
			val I: int = i; val S: int = s;
				// no compiler error
				x += Future.make[int](() => at(A.dist([I, S%N] as Point(A.rank))) A(I, S%N) ).force();
			
		}
		x10.io.Console.OUT.println("x = "+x);
		return (x == 252);
	}

	public static def main(var args: Array[String](1)): void = {
		new FutureTest4_MustFailCompile().execute();
	}
}

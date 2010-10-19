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
 * Check that a cast is created for a constructor call with a simple dep clause.
 * @author vj
 */
public class ConConstructor1Arg extends x10Test {
	static class A(i:Int) {}
	def this() {}
	def this(A{self.i==2}){}
	def this(i:Int) {
		// This call will compile only if -strictCalls is not set.
		this(new A(i)); // DYNAMIC_CHECK   with -STATIC_CALLS we get ERR: Constructor this(id$328: ConConstructor1Arg.A{self.i==2}): ConConstructor1Arg cannot be invoked with arguments    (ConConstructor1Arg.A{self.i==i, self!=null}).
	}
	
	public def run(): boolean {
		try {
			val x = new ConConstructor1Arg(3);
			return false;
		} catch (ClassCastException) {
			return true;
		}
	}

	public static def main(Array[String](1)) {
		new ConConstructor1Arg().execute();
	}


}

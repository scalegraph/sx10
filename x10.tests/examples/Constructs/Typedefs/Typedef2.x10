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
 * Test a generics class with an invariant parameter.
 *
 * @author nystrom 8/2008
 */
public class Typedef2 extends x10Test {
	public def run(): boolean = {
                type foo[T] = T;
                type bar(x: int) = int{self==x};
                val x: foo[int] = 3;
                val y: bar(5) = 5;
		return x == 3 && y == 5;
	}

	public static def main(var args: Array[String](1)): void = {
		new Typedef2().execute();
	}
}


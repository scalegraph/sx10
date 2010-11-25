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
 * Expression parentheses test.
 */
public class ExpressionParentheses extends x10Test {

	var x: Int = 0x80000000;
	var n: Int = 16;
	var z: Int;

	public def run(): boolean = {
		z = ((x ^ (x>>8) ^ (x>>31)) & (n-1));
		if (z != 1) return false;
		z = ((x | (n-1)) & 1);
		if (z != 1) return false;
		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new ExpressionParentheses().execute();
	}
}

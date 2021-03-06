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
 * Purpose: Checks the numeric expression is not evaluated several time while checking for constraint
 * Note: The cast should not be inlined to avoid several execution of ++j
 * @author vcave
 **/
public class NumericExpressionToPrimitiveDepType_5 extends x10Test {

	public def run(): boolean = {
		var j: int = -1;
		var i: int{self == 0} = 0;
		i = (++j) as int{self == 0};
		return ((j == 0) && (i==0));
	}

	public static def main(var args: Array[String](1)): void = {
		new NumericExpressionToPrimitiveDepType_5().execute();
	}

}

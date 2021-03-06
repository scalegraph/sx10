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
 * Check that dep clauses are cheked when checking statically if a cast can be valid at runtime.
 */
public class IntToInt1 extends x10Test {
	public def run(): boolean = {
		var zero: int{self==0} = 0;
		var one: int{self==1} = 1;
		var i: int = one as int;
		one = i as int{self==1};
		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new IntToInt1().execute();
	}


}

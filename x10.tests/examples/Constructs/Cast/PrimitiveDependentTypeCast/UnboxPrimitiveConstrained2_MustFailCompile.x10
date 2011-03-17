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


// If we do not run with STATIC_CALLS it generates:
// Warning: Generated a dynamic check for the method call.
// OPTIONS: -STATIC_CALLS

import harness.x10Test;

import x10.util.Box;
/**
 * Purpose: Checks Unboxing from a primitive constrained cast works.
 * Issue: deptype type information helps detects wrong cast at compile time.
 * @author vcave
 **/
 public class UnboxPrimitiveConstrained2_MustFailCompile extends x10Test {

	public def run(): boolean = {
		var res4: boolean = false;
		
      var ni: int(4) = 4;
		
		try {
			// (nullable<int(:self==3)>) <-- int(:c)
         var case4b: Box[int(3)] = new Box[int(3)](ni); // ERR deptype check
		} catch (e: ClassCastException) {
			res4 = true;
		}
		
		return res4;
	}
	

	public static def main(var args: Array[String](1)): void = {
		new UnboxPrimitiveConstrained2_MustFailCompile().execute();
	}
}

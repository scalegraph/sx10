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
 * Purpose: Cast's dependent type constraint must be satisfied by the primitive.
 * Issue: Value to cast does not meet constraint requirement of target type.
 * @author vcave
 **/
public class CastPrimitiveConstrainedToPrimitiveConstrained2_MustFailCompile extends x10Test {

	public def run(): boolean = {
		
		try { 
			var i: int{self == 1n} = 1n;
			var j: int{self == 0n} = i as int{self == 0n}; // ERR
		}catch(e: Exception) {
			return false;
		}

		return true;
	}

	public static def main(var args: Rail[String]): void = {
		new CastPrimitiveConstrainedToPrimitiveConstrained2_MustFailCompile().execute();
	}

}

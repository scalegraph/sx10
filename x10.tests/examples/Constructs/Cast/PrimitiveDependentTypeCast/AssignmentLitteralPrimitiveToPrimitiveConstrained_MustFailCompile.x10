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
 * Purpose: Cast's dependent type constraint must be satisfied by the primitive.
 * Issue: assignment is illegal as constraint is not meet.
 * @author vcave
 **/
public class AssignmentLitteralPrimitiveToPrimitiveConstrained_MustFailCompile extends x10Test {

	public def run(): boolean = {
		
		try { 
			var i: int{self == 0} = 0;
			i = 1;
		}catch(e: Throwable) {
			return false;
		}

		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new AssignmentLitteralPrimitiveToPrimitiveConstrained_MustFailCompile().execute();
	}

}

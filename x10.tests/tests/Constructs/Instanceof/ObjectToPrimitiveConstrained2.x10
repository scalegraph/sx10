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
 * Purpose: Checks boxed integer value is checks against primtive dependent type.
 * Issue: Constraint on self is not meet.
 * @author vcave
 **/
public class ObjectToPrimitiveConstrained2 extends x10Test {
	 
	public def run(): boolean = {
		var primitive: x10.lang.Any = 3;
		return !(primitive instanceof Long(4));
	}
	
	public static def main(Rail[String]) {
		new ObjectToPrimitiveConstrained2().execute();
	}
}

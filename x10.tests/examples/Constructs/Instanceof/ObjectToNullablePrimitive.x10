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
 * Purpose: Checks a boxed struct is an instanceof of the same type.
 * @author vcave
 **/
public class ObjectToNullablePrimitive extends x10Test {
	 
	public def run(): boolean = {
		var primitive:Any = 3;
		// Type to check is transformed to BoxedInt
		return (primitive instanceof Int);
	}
	
	public static def main(var args: Array[String](1)): void = {
		new ObjectToNullablePrimitive().execute();
	}
}

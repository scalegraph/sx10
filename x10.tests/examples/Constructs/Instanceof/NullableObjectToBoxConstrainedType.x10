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

import x10.util.Box;
/**
 * Purpose: 
 * @author vcave
 **/
public class NullableObjectToBoxConstrainedType extends x10Test {
	 
	public def run(): boolean = {
		var nullableVarNotNull: Box[ValueClass] = 
			ValueClass(1)
                  as Box[ValueClass{p==1}];
		return nullableVarNotNull instanceof Box[ValueClass{p==1}];
	}
	
	public static def main(Array[String](1)) {
		new NullableObjectToBoxConstrainedType().execute();
	}
}

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

import x10.util.Box;
import harness.x10Test;

/**
 * Purpose: 
 * @author vcave
 Converted as Box by vj.
 **/
public class BoxObjectToBoxConstrainedType extends x10Test {
	 
	public def run(): boolean = {
                val n = ValueClass(1);
		return n instanceof ValueClass{p==1};
	}
	
	public static def main(var args: Array[String](1)): void = {
		new BoxObjectToBoxConstrainedType().execute();
	}
}

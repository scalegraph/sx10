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
 **/
public class PrimitiveToNullablePrimitiveConstrained1 extends x10Test {
	val three:Box[Int(3)] = new Box[int(3)](3);
	public def run()= three instanceof Box[Int(3)];
	
	public static def main(var args: Array[String](1)): void = {
		new PrimitiveToNullablePrimitiveConstrained1().execute();
	}
}

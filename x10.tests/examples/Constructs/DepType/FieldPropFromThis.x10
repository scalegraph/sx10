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
 * Check that info from the real clause of a type is propagated down
 * to the use of the type.
 */
public class FieldPropFromThis extends x10Test {
	class  Foo(i:int(0)) {
		public def this():Foo {
			property(0);
		}
	}
	
	                   
	public def run(): boolean = {
		var f: Foo = new Foo();
		var s: int(0) = f.i;
		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new FieldPropFromThis().execute();
	}


}

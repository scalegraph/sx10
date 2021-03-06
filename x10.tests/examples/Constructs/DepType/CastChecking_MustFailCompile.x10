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
 * Check that a cast involving types which are not related by supertype or subtype
 * relation fails
 *
 * @author pvarma
 */
public class CastChecking_MustFailCompile extends x10Test {
	class Test( i:int,  j:int) {
		def this(val i: int, val j: int): Test{self.i==i && self.j==j} = {
			property(i,j);
		}
		}
	class Test2(k:int) extends Test{i==j} {
		def this(val k: int): Test2{self.i==k&&self.j==k&&self.k==k} = {
			super(k,k);
			property(k);
		}
		}
	class Test3 (l:int) {
		def this(var l: int): Test3 = { property(l);}
	}
	
	public def run(): boolean = {
		var a: Test2{k==1&&i==j} = new Test2(1);
		var b: Test{i==j} = a;
		var d: Test =  new Test3(1) as Test;  // ERR must fail compile
	   return true;
	}
	public static def main(var args: Array[String](1)): void = {
		new CastChecking_MustFailCompile().execute();
	}
}

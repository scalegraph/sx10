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

/** Tests that a constructor invocation satisfies the parameter types and constraint
 * of the constructor declaration.
 *@author pvarma
 *
 */

import harness.x10Test;

public class ConstructorInvocation_MustFailCompile extends x10Test { 

	class Test(i:int, j:int) {
	    def this(i:int, j:int{self==i}):Test{self.i==self.j} = {
		property(i,j);
	    }
	}
		
	class Test2(k:int) extends Test {
	    def this(k:int):Test2 = { 
		// the call to super below violates the constructor parameters constraint i == j
		super(0,1);
		property(k);
	    }
	}
	public def run()=true;
	
    public static def main(a: Array[String](1)) = {
        new ConstructorInvocation_MustFailCompile().execute();
    }
   

		
}

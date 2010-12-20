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
import x10.compiler.*; // @Uncounted @NonEscaping @NoThisAccess
import x10.compiler.tests.*; // err markers
import x10.util.*;


/**
 * Check that in the return type of a closure call actuals are substituted for formals.
 * @author vj
 */

public class ClosureCall3 extends ClosureTest {

    public def run(): boolean = {
        val y = (x:Int)=> x; // todo: we should infer type (x:Int)=>Int{self==x},
		// but closures have a bug:
		//Semantic Error: Cannot assign expression to target.
		//	 Expression:  (val x: x10.lang.Int){}: x10.lang.Int{self==x} => { return x; }
		//	 Type: (a1:x10.lang.Int)=> x10.lang.Int{self==x}
		//	 Expected type: (a1:x10.lang.Int)=> x10.lang.Int{self==x}
        @ShouldBeErr val z :Int(1) = y(1);  // todo: this is an error only with -STATIC_CALLS (with DYNAMIC_CALLS there is no warning!)
        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new ClosureCall3().execute();
    }
}

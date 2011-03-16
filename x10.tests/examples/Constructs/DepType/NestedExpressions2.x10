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
* Checking that the type-checker can correctly handle boolean expressions as the values
* boolean properties. Check that an expression of type C(x.a&&y.a) can
* be assigned to a variable of type C{self.a==(y.a&&x.a)}.
 */
	
public class NestedExpressions2 extends x10Test {
	class C(a:boolean) {
		static type C(b:boolean) = C{self.a==b};
		def this(b:boolean):C{self.a==b}{property(b);}
		def and(x:C, y:C): C{self.a== (x.a && y.a)} = new C(x.a&&y.a);
		def n() {
			val x = new C(true);
			val y = new C(true);
			// todo: this constraint is too complicated to the type system and should be flaged as an error!
			val z: C{self.a==(y.a&&x.a)} = and(x,y); // ERR: Warning: Expression 'NestedExpressions2.C.this.and(x, y)' was cast to type NestedExpressions2.C{self.NestedExpressions2.C#a==y.NestedExpressions2.C#a, x.NestedExpressions2.C#a}.
		}
	}
    public def run() = true;

    public static def main(Array[String](1))  {
        new NestedExpressions2().execute();
    }
}

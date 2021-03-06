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


/**
 * STATIC SEMANTICS RULE: In an expression (x1: T1, . . ., xn: Tn) =>
 * e, any outer local variable accessed by e must be final or must be
 * declared as shared (�14.9).
 *
 * @author bdlucas 8/2008
 */

public class ClosureEnclosingScope2c_MustFailCompile extends ClosureTest {

    val a = 1;

    public def run(): boolean = {
        
        val b = 1;

        class C {
            def foo(): void {
              var c:int = 1;
              (()=>a+b+
                @ERR c)
                ();
            }
        }

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new ClosureEnclosingScope2c_MustFailCompile().execute();
    }
}

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
 * An instance of a defined type with no type parameters and no value
 * parameters may be used to instantiate an instance of a type. The type
 * has the same constructors with the same signature as its defining
 * type; however, a constructor may not be invoked using a given defined
 * type name if the constructor return type is not a subtype of the
 * defined type.
 *
 * @author bdlucas 9/2008
 */

public class TypedefNew11_MustFailCompile extends TypedefTest {

    public def run(): boolean = {
        
        class X {}
        class Y {}

        class A[C] {
            def this() = {};
            def this(i:int):A[C]{C<:X} = {};
        }
        
        type T[C] = A[C];
        type TX = A[X];
        type TY = A[Y];

        // not allowed
        val t6 = new TY(0);  // A[Y] !<: A[Y]{Y<:X}

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new TypedefNew11_MustFailCompile().execute();
    }
}

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

public class TypedefNew06_MustFailCompile extends TypedefTest {

    public def run(): boolean = {
        
        class A(k:int) {
            def this():A{self.k==0} = property(0);
            def this(i:int):A{self.k==i} = property(i);
        }
        
        type T(i:int) = A{self.k==i};
        type T0 = A{self.k==0};
        type T1 = A{self.k==1};

        // not allowed
        val t = new T1();  // ERR A(1) !<: A(0)

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new TypedefNew06_MustFailCompile().execute();
    }
}

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

// COMPILER_CRASHES: the compiler now crashes on this file.
//java.lang.AssertionError: TypedefOverloading08_MustFailCompile.A->TypedefOverloading08_MustFailCompile.A x10.types.X10ParsedClassType_c is already in the cache; cannot replace with TypedefOverloading08_MustFailCompile.A x10.types.MacroType_c
//	at polyglot.types.CachingResolver.install(CachingResolver.java:147)

/**
 * It is illegal for a package, class, or interface to contain a type
 * definition with no type or value parameters and also a member class
 * or interface with the same name.
 *
 * @author bdlucas 9/2008
 */

public class TypedefOverloading08_MustFailCompile extends TypedefTest {

    static class A(i:int) {def this() = property(0);}
    static type A = int;

    public def run(): boolean = {
        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new TypedefOverloading08_MustFailCompile().execute();
    }
}

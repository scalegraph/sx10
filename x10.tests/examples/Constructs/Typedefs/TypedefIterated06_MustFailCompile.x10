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
 * Basic typdefs and type equivalence.
 *
 * Type definitions are applicative, not generative; that is, they define
 * aliases for types and do not introduce new types.
 *
 * @author bdlucas 9/2008
 */

public class TypedefIterated06_MustFailCompile extends TypedefTest {

    public def run(): boolean = {

        type A = A[int]; // ERR
        type A[T] = A;

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new TypedefIterated06_MustFailCompile().execute();
    }
}

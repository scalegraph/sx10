/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;



/**
 * @author makinoy 4/2010
 */

public class GenericInstanceof16 extends GenericTest {

    class A[T1, T2] {}
    class B[U1, U2] extends A[U2, U1] {}
    
    public def run() = {
        return new B[Long, Double]() instanceof A[Double, Long] && !((new B[Long, Double]() as Any) instanceof A[Long, Double]);
    }
    
    public static def main(var args: Rail[String]): void = {
        new GenericInstanceof16().execute();
    }
}

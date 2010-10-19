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
 * Checks that an inner class within a generic can actually use
 * the captured type parameters of the outer instance.
 */
public class GenericInnerClassParameterCapture[A] extends GenericTest {

    public class Inner[B] {
        public def m(): Inner[A] = make[A]();
    }
    public def make[G]() = new Inner[G]();

    public static def foo():GenericInnerClassParameterCapture[Int].Inner[Int] = new GenericInnerClassParameterCapture[Int]().make[Double]().m();

    public def run() = {
        return true;
    }

    public static def main(var args: Array[String](1)): void = {
        new GenericInnerClassParameterCapture[Object]().execute();
    }
}

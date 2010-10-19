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
 * For a type T, a list of types (T1, . . . , Tn), a method name m and an
 * expression e of type T, e.m.(T1, . . ., Tn) denotes the function, if
 * any, bound to the instance method named m at type T whose argument
 * type is (T1, . . ., Tn), with this the method body bound to
 * the value obtained by evaluating e. The return type of the function is
 * specified in the method declaration.  Thus, the method selector
 * 
 * e.m.[X1, . . ., Xm](T1, . . ., Tn)
 * 
 * behaves as if it were the closure
 * 
 * (X1, . . ., Xm](x1: T1, . . ., xn: Tn) => e.m[X1, . . ., Xm](x1, . . ., xn)
 *
 * @author bdlucas 8/2008
 */

public class MethodSelector3 extends ClosureTest {

    def foo() = 10;
    def foo(i:int) = i*10;

    static class C3 {
        static def foo() = 1;
        static def foo(i:int) = i;
    }

    public def run(): boolean = {

        val c = new C3();
        val f1 = c.foo.();
        val f2 = c.foo.(int);
        val f3 = C3.foo.();
        val f4 = C3.foo.(int);
        check("f1()", f1(), 1);
        check("f2(2)", f2(2), 2);
        check("f3()", f3(), 1);
        check("f4(2)", f4(2), 2);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new MethodSelector3().execute();
    }
}

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
import x10.compiler.tests.*;

/**
 * See XTENLANG-2389.
 *
 * @author vj 5/2011
 */
public class DynamicOuterTests extends x10Test {
    static class A (i:Int) {
        class X(j:String) {
            def m(Int{self==A.this.i}){}
            def n(a:A, x:A{self.i==a.i}.X{self.j=="x"}){}
        }
    }

    public def run(): boolean = {
        val a = new A(3);
        val x = a.new X("x");
        x.m(a.i);
        x.m(3);
        // @ERR { x.m(4);  }
        var b:A=a;
        x.n(b, x);
        return true;
    }

    public static def main(Array[String](1)) {
        new DynamicOuterTests().execute();
    }
}

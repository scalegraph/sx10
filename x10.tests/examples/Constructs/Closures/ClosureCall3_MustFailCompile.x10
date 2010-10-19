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
 * It is a static error if a call may resolve to both a closure call or
 * to a method call.
 *
 * @author bdlucas 8/2008
 */

public class ClosureCall3_MustFailCompile extends x10Test {

    def f(x:int) = "floor wax";
    val f = (x:int) => "desert topping";

    public def run(): boolean = {
        check("f(1)", f(1), "comedic hilarity");
        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new ClosureCall3_MustFailCompile().execute();
    }
}

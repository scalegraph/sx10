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
 * Java and X10 permit a call to a method which returns a value to occur as a statement.
 * The returned value is discarded. However, Java does not permit a variable standing alone
 * as a statement. Thus the x10 compiler must check that as a result of flattening it does
 * not produce a variable standing alone. 
 * In an earlier implementation this would give a t0 not reachable error.
 */

public class FlattenAsyncExpr2 extends x10Test {

    val a: DistArray[int](1);

    public def this(): FlattenAsyncExpr2 = {
        a = DistArray.make[int](1..10 -> here, ([j]: Point): int => { return 2*j;});
    }

    static def m(x: int) = x;
        
    
    public def run(): boolean = {
        finish async at(a.dist(1)) {
            m(50000);
            atomic { a(1) = (a(1)^2);}
        }
        return a(1)== (2^2);
    }

    public static def main(Array[String](1)) {
        new FlattenAsyncExpr2().execute();
    }
}

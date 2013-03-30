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
import x10.array.*;
import x10.util.concurrent.Future;

/**
 * Testing arrays of Future<T>.
 *
 * @author kemal, 5/2005
 */

public class ArrayFutureFlatten extends x10Test {

    public def run(): boolean = { 
        val A = new Array[int](Region.make(1..10,1..10), (Point)=>0);
        val B = new Array[int](Region.make(1..10, 1..10), (Point)=>0);
        val b = (Future.make[int](()=>3))();
        chk(0 == (Future.make[int](()=>B(1,1))()));
        return true;
    }

    public static def main(var args: Rail[String]): void = {
        new ArrayFutureFlatten().execute();
    }
}

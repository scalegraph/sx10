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
 * Test that a can be accessed through point p if p ranges over b.dist
 and a.rank=b.rank. Here a and b are defined over two distributions,
 each of whose regions has rank 1.

 * @author vj 03/17/09 -- fails compilation.

 */

public class ArrayAccessEqualRank2 extends x10Test {

    public def arrayEqual(A: DistArray[int], B: DistArray[int](A.rank)) {
        finish
            ateach (p in A.dist) {
            val v = at (B.dist(p)) B(p);
            chk(A(p) == v);
	}
    }

    public def run(): boolean = {

	val R = Region.make(0,9);
        val S = Region.make(0,9); 
	// R and S should both be rank 1.
	val D = Dist.make(R);
	val E = Dist.make(S);
        val a = DistArray.make[Int](D, (Point)=>0);
        val b = DistArray.make[Int](E, (Point)=>0);
	arrayEqual(a,b);
        return true;
    }

    public static def main(Array[String](1)) = {
        new ArrayAccessEqualRank2().execute();
    }
}

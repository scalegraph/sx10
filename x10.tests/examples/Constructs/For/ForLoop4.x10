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

import x10.util.Box;
import harness.x10Test;

/**
 * Test for for loop with for (Point p:[1:N,1:N,1:N]) syntax.
 *
 * @author kemal, 1/2005
 */
public class ForLoop4 extends x10Test {

	public static N: int = 3;

	public def run(): boolean = {
		//Ensure iterator works in lexicographic order
		var n: int = 0;
		var prev: Box[Point] = null;
		for (p in 0..(N-1)*0..(N-1)*0..(N-1)->here) {
			if (!successor(prev, p)) return false;
			prev = new Box[Point](p);
			n++;
		}
		if (n != N*N*N) return false;
		return true;
	}

	/**
	 * return true iff p is the lexicographic successor of prev
	 * For example for a [0..2,0..2,0..2] region
	 * i.e. we expect the order (0,0,0), (0,0,1),(0,0,2)
	 *  (0,1,0) ... (2,2,2) (row-major order)
	 */
	static def successor(var prev: Box[Point], var p: Point): boolean = {
		if (prev == null) return true;
                val v = prev();
		var i: int = v(0);
		var j: int = v(1);
		var k: int = v(2);
		//x10.io.Console.OUT.println("Prev:"+i+" "+j+" "+k);
		//x10.io.Console.OUT.println("Actual:"+ p.get(0)+" "+p.get(1)+" "+p.get(2));
		k++;
		if (k == N) {
			k = 0;
			j++;
			if (j == N) {
				j = 0;
				i++;
			}
		}
		//x10.io.Console.OUT.println("Expected:"+i+" "+j+" "+k);
		if (i != p(0)) return false;
		if (j != p(1)) return false;
		if (k != p(2)) return false;
		return true;
	}

	public static def main(Array[String](1)) {
		new ForLoop4().execute();
	}
}

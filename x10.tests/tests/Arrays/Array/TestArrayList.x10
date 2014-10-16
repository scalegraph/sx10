/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2014.
 */

import harness.x10Test;
import x10.util.ArrayList;

/**
 * Functional test of x10.util.ArrayList
 */
public class TestArrayList extends x10Test {

	public def run(): Boolean = {
        testAccess();
        testResize();

        return true;
	}

    public def testAccess() {
        val N = 20;
        val a = new ArrayList[Double](N);
        for (i in 0..(N-1)) {
            a.add(i as Double);
        }

        for (i in 0..(N-1)) {
            chk(a(i) == i as Double);
        }
    }

    public def testResize() {
        val N = 20;
        val a = new ArrayList[Double]();

        a.resize(N);
        // new elements should be zero-initialized
        chk(a.size() == N);
        for (i in 0..(N-1)) {
            chk(a(i) == 0.0);
        }

        a.resize(N+10, 1.0);
        // new elements should be initialized to 1.0
        chk(a.size() == N+10);
        for (i in N..(a.size()-1)) {
            chk(a(i) == 1.0);
        }

        a.resize(10);
        chk(a.size() == 10);
        // original elements should be unchanged
        for (i in 0..9) {
            chk(a(i) == 0.0);
        }
    }

	public static def main(args: Rail[String]): void = {
		new TestArrayList().execute();
	}
}


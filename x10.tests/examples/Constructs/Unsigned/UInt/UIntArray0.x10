/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2011.
 */

import harness.x10Test;

/**
 * Test Array[UInt]
 *
 * @author Salikh Zakirov 5/2011
 */
public class UIntArray0 extends x10Test {
    public def run(): boolean = {
	if (!test_simple()) return false;
	if (!test_range()) return false;
	return true;
    }

    public def test_range():boolean {
	val aaa = new Array[UInt](1..10);
	for (i in 1..10) aaa(i) = i as UInt;

	var s : UInt = 0;
	for (i in aaa.region) s += aaa(i);

	if (s != 55u) return false;
	return true;
    }

    public def test_simple():boolean {
	val aaa = new Array[UInt](10);
	for (i in 0..9) aaa(i) = i as UInt;

	var s : UInt = 0;
	for (i in aaa.region) s += aaa(i);

	if (s != 45u) return false;

	return true;
    }

    public static def main(Array[String]) {
        new UIntArray0().execute();
    }
}

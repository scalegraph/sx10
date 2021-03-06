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
 * Inference for return types.
 *
 * @author nystrom 8/2008
 */
public class Inference3 extends x10Test {
        def m() = 3;

	public def run(): boolean = {
                val x = m();
                val y: int = x;
		return x == y;
	}

	public static def main(var args: Array[String](1)): void = {
		new Inference3().execute();
	}
}


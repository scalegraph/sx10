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
 * Exposes a possible parsing bug with
 * if (true) S1 else finish S2; .
 * As of 11/2005, this was executing the finish (it should not)
 * @author vj
 * @author kemal 11/2005
 */
public class IfElseFinishBug extends x10Test {

	public def run(): boolean = {
		if (true) x10.io.Console.OUT.println("True branch");
		else finish for (val [i]: Point in [0..1]) async { throw new Error("Throwing "+i); }
		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new IfElseFinishBug().execute();
	}
}

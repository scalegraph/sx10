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
import x10.util.Future;

/**
 * Checks force for grand-children.
 * @author Christoph von Praun
 */
public class FutureForce extends x10Test {

	var flag: Boolean;
	var foo: Int;

	public def bar(): Int = {
		x10.io.Console.OUT.print("waiting ...");
		Activity.sleep(2000);
		x10.io.Console.OUT.println("done.");
		atomic flag = true;
		return 42;
	}

	public def foo(): Int = {
		var r2: Future[Int] = Future.make( () => bar() );
		return 42;
	}

	public def run(): Boolean = {
		atomic flag = false;
		var r1: Future[Int] = Future.make( () => foo() );
		r1();
		var b: Boolean;
		atomic b = flag;
		x10.io.Console.OUT.println("The flag is b=" + b + " (should be true).");
		return (b == true);
	}

	public static def main(var args: Array[String](1)): void = {
		new FutureForce().execute();
	}
}

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
 * Region algebra.
 *
 * @author nystrom 8/2008
 */
public class Subclassing4 extends x10Test {
        class Get[T] {
                val x: T;
                def this(y: T) = { x = y; }
                def get(): T = x; }
        class Getint extends Get[int] {
                def this(y: int) = { super(y); } }

	public def run(): boolean = {
                new Get[int](0);
                new Get[int](1);
                new Getint(2);
                new Getint(3);
		return true;
	}

	public static def main(var args: Array[String](1)): void = {
		new Subclassing4().execute();
	}
}

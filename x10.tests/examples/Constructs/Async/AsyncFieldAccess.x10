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
import x10.compiler.Pinned;
import x10.compiler.SuppressTransientError;

/**
 * Testing an async spawned to a field access.
 */
public class AsyncFieldAccess extends x10Test {
    private val root = GlobalRef[AsyncFieldAccess](this);
	@SuppressTransientError transient var t: GlobalRef[T] = GlobalRef[T](null);
	public def run(): boolean = {
		var Second: Place = Place.FIRST_PLACE.next();
		val r  = 0..0;
		val D: Dist = r->Second;
		val root = this.root;
		finish ateach (val p: Point in D) {
			val NewT = (new T()).root;
			async at (root) { root().t = NewT; }
		}
		val tt = this.t;
		at (tt) { atomic tt().i = 3; }
		return 3 == (at (tt) tt().i);
	}

	public static def main(Array[String](1)){
		new AsyncFieldAccess().execute();
	}

	static class T {
		private val root = GlobalRef[T](this);
		transient public var i: int;
	}
}

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

/**
 * An ateach statement cannot occur in a method called from within an atomic.
 * @vj
 */
public class NoAtEachInAtomicMethod extends x10Test {
	
	public def run(): boolean = {
        try {
			atomic 
			  m();
		} catch (IllegalOperationException) {
			return true;
		} 
		return false;
	}
	def m() {
		ateach(p in Region.make(1,10)->here) 
			Console.OUT.println("This should not print.");
	}

	public static def main(Rail[String]){
		new NoAtEachInAtomicMethod().execute();
	}
}

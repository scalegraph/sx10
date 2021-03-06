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
 * Clock test for barrier functions.
 * @author kemal 3/2005
 */
public class ClockTest3 extends x10Test {

	var value: int = 0;
	static N: int = 32;

	public def run(): boolean = {
	 clocked finish
		for ([i] in 0..(N-1)) clocked async {
			clocked async   
			   finish async { 
			       async { 
			          atomic value++; 
			       } 
			   }
			Clock.advanceAll();
			var temp: int;
			atomic { temp = value; }
			if (temp != N) {
				throw new Exception();
			}
			Clock.advanceAll();
			clocked async finish async { async { atomic value++; } }
			Clock.advanceAll();
		}
		Clock.advanceAll(); Clock.advanceAll(); Clock.advanceAll();
		var temp2: int;
		atomic { temp2 = value; }
		if (temp2 != 2*N) {
			throw new Exception();
		}
		return true;
	}

	public static def main(Array[String](1)) {
		new ClockTest3().executeAsync();
	}
}

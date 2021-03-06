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

public class ArrayStaticPlusEqual extends x10Test {

    val v  = GlobalRef[Rail[int]](new Rail[int](2, (x:int)=>0));

    public def run() {
	    at (v) {
	    	val myV = (v as GlobalRef[Rail[int]]{self.home==here})();
            for ([i]:Point(1) in 0..1) myV(i) += 5;
            for ([i]:Point(1) in 0..1) chk(myV(i) == 5);
        }
        return true;
    }

    public static def main(var args: Array[String](1)): void = {
        new ArrayStaticPlusEqual().execute();
    }
}

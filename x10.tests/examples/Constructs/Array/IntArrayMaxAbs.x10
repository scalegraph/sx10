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
 * Testing the maxAbs function on arrays.
 */

public class IntArrayMaxAbs extends x10Test {

    public def run(): boolean = {
        val ia  = new Array[int]((1..10)*(1..10), (p:Point)=>-p(0));

	    val absMax = ia.reduce((a:Int, b:Int):Int => {
            val ma = Math.abs(a), mb =Math.abs(b);
            ma <= mb? mb : ma
        }, 0);

	    println("ABSmax=" + absMax);
	    return absMax==10;
    }

    public static def main(Array[String](1)) {
        new IntArrayMaxAbs().execute();
    }
}

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

 */

class XTENLANG_732 extends x10Test {

	class A {
		def this(x:Int){this.x=x;}
		var x:Int=0;
		operator this()=x;
		operator this()=(v:Int) { this.x=v;}
	}

	public def run(): boolean {
		val v = new A(1);
		v() = 2;
		return v()==2;
	
	}

    public static def main(Array[String](1)) {
        new XTENLANG_732().execute();
    }
}

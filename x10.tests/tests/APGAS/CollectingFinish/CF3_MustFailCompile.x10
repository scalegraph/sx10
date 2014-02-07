/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

/**
 *  Check that you cannot offer a type that is not a subtype of the offer type.
 * @author vj
 */
public class CF3_MustFailCompile extends x10Test {

    static struct Reducer implements Reducible[Long] {
     	public  def zero()=0;
     	public  operator this(a:Long,b:Long)=a+b;
    }
    def m() offers Long : void {
    	offer "a"; // ERR
    }
	public def run() {

		val x = finish (Reducer()){
			m();
		};
		return 6==x;
	}

	public static def main(Rail[String]) {
		new CF3_MustFailCompile().execute();
	}
}

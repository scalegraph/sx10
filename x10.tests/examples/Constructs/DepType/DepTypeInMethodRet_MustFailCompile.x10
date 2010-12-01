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
 * Cannot return a value of type boolean from a method whose return type is boolean(:self==true).
 *
 * @author vj
 */
public class DepTypeInMethodRet_MustFailCompile extends x10Test {
    
   public def m(var t: boolean): boolean(true) = t; // ERR
	public def run()=m(false);
	public static def main(var args: Array[String](1)): void = {
		new DepTypeInMethodRet_MustFailCompile().execute();
	}
}

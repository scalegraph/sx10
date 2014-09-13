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

 public class DepTypeInMethod1 extends x10Test {
	 static class C[T] {
		 public def f1(){T==Int}:C[Int] {
			 val ci:C[Int] = this;
			 return ci;
		 }
		 public def f2(){T==Int}:C[Int] {
			 return this;
		 }
	 }
	 
     public def run():boolean = {
    	 return true;
     }
     
     public static def main(var args: Rail[String]): void = {
    	 new DepTypeInMethod1().execute();
     }
 }

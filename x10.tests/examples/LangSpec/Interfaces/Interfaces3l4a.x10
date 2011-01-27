/* Current test harness gets confused by packages, but it would be in package Interfaces3l4a;
*/
// Warning: This file is auto-generated from the TeX source of the language spec.
// If you need it changed, work with the specification writers.


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



public class Interfaces3l4a extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Interfaces3l4a().execute();
    }


// file Interfaces line 257
 // 
 static class A {
  static def example(n: Int) {
 static     class B {
 static       interface I { val V = n*n; }
    }
   return B.I.V + 1;
  }
}
 static  class Hook{ def run() { return A.example(5) == 26; }}

}
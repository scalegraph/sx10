/* Current test harness gets confused by packages, but it would be in package Classes3g2h;
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



public class Classes3g2h extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes3g2h().execute();
    }


// file Classes line 1794
 static class Numbered(n:Int) {
  public static operator (x:Numbered) as Int = x.n;
  public static def example(){
     val n3 = new Numbered(3);
     assert n3 as Int == 3; // ShouldNotBeERR
  }
}
 static  class Hook{ def run() {Numbered.example(); return true;}}

}
/* Current test harness gets confused by packages, but it would be in package Statements2j1d;
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



public class Statements2j1d extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Statements2j1d().execute();
    }


// file Statements line 751
 static  class Example {
def method(x:Int) {
  val closure = (y:Int) => {return x+y;};
  val res = closure(0);
  assert res == x;
  return res == x;
}
}

 static class Hook {
   def run():Boolean = true;
}

}
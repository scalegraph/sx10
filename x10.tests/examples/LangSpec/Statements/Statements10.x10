/* Current test harness gets confused by packages, but it would be in package statements_emptystatement;
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



public class Statements10 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Statements10().execute();
    }


// file Statements line 18
 static  class EmptyStatementExample {
static def search[T](a: Array[T](1), v: T):Int {
  var i : Int;
  for(i = a.region.min(0); a(i) != v; i++)
     ;
  return i;
}
}

 static class Hook {
   def run():Boolean = true;
}

}

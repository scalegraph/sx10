/* Current test harness gets confused by packages, but it would be in package ObjInit_at;
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



public class ThisEscapingViaAt_MustFailCompile extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new ThisEscapingViaAt_MustFailCompile().execute();
    }


// file ObjectInitialization line 81
 
 static class Example {
  val a: Int;
  def this() {
    at(here.next()) {
      // Recall that 'this' is a copy of 'this' outside 'at'.
      Console.OUT.println("this.a = " + this.a);
    }
    this.a = 1;
  }
}

 static class Hook {
   def run():Boolean = true;
}

}
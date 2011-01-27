/* Current test harness gets confused by packages, but it would be in package Activities_Atsome_Globref2;
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



public class Activities60 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Activities60().execute();
    }


// file Activities line 430
 static  class GR2 {
  public static def main(argv: Array[String](1)) {
    val argref = GlobalRef[Array[String](1)](argv);
    at(here.next()) use(argref);
  }
  static def use(argref : GlobalRef[Array[String](1)]) {
    at(argref.home) {
      val argv = argref();
      argv(0) = "Hi!";
    }
  }
}
 static  class Hook{ def run() { GR2.main(["what, me weasel?"]); }}

}
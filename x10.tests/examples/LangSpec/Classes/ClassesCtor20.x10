/* Current test harness gets confused by packages, but it would be in package ClassesCtor20;
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



public class ClassesCtor20 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new ClassesCtor20().execute();
    }


// file Classes line 1240
 static class C(x:Int) {
  static def example() {
    val c : C = new C(4);
    assert c.x == 4;
  }
}
 static  class Hook{ def run() {C.example(); return true;}}

}

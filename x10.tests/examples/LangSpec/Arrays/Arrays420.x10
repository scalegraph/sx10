/* Current test harness gets confused by packages, but it would be in package genexpsome_Arrays45;
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



public class Arrays420 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Arrays420().execute();
    }


// file Arrays line 735

 static class Arrays45GenexpTest{
  def check[T](R:Region, v:T)  = new Array[T](R, v);  }

 static class Hook {
   def run():Boolean = true;
}

}
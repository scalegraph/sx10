/* Current test harness gets confused by packages, but it would be in package Expressions4f4y;
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
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

 import x10.regionarray.*;

public class Expressions4f4y extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(args:Rail[String]):void {
        new Expressions4f4y().execute();
    }


// file Expressions line 1663
 static  class Example { def example() {

val a = new Array[String](Region.make(2..3, 4..5), "hi!");
a([2,4]) = "converted!";
} }

 static class Hook {
   def run():Boolean = true;
}

}

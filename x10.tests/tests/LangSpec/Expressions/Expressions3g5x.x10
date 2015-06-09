/* Current test harness gets confused by packages, but it would be in package Expressions3g5x;
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



public class Expressions3g5x extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(args:Rail[String]):void {
        new Expressions3g5x().execute();
    }


// file Expressions line 1202
 static interface Snack {}
 static interface Crunchy {}
 static class Pretzel implements Snack, Crunchy{}
 static class Apricot implements Snack{}
 static class Gravel  implements Crunchy{}
 static class Example{
  def example(crunchy : Crunchy) {
    if (crunchy instanceof Snack) {
       val snack = crunchy as Snack;
    } } }

 static class Hook {
   def run():Boolean = true;
}

}

/* Current test harness gets confused by packages, but it would be in package Clocks_For_Jocks;
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



public class Clocks30 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(args:Rail[String]):void {
        new Clocks30().execute();
    }


// file Clocks line 155
 static class Qlocked{
static def S():void{}
static def flock() {
 val c1 = Clock.make(); val c2 = Clock.make(); val c3 = Clock.make();
  async clocked (c1, c2, c3) S
();
}}

 static class Hook {
   def run():Boolean = true;
}

}

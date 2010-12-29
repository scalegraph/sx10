/* Current test harness gets confused by packages, but it would be in package exp_loc_al_varia_ble;
*/

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



public class Expressions20 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Expressions20().execute();
    }


// file Expressions line 84
 static  class Example {
 def example() {
val n = 22;
val m = n + 56;
} }

 static class Hook {
   def run():Boolean = true;
}

}
/* Current test harness gets confused by packages, but it would be in package Vars_In_Snares;
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



public class Vars30 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Vars30().execute();
    }


// file Vars line 137
 static  class ABitTedious{
 def example() {
val a : Int = 10;
val b = (a+1)*(a-1);
}}

 static class Hook {
   def run():Boolean = true;
}

}
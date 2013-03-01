/* Current test harness gets confused by packages, but it would be in package Vars_Local_not_the_express_plz;
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



public class Vars_Var_Bounded extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Rail[String]): void = {
        new Vars_Var_Bounded().execute();
    }


// file Vars line 503
 static  class Tub(p:Int){
   def this(pp:Int):Tub{self.p==pp} {property(pp);}
   def example() {
     val t : Tub = new Tub(3);
   }
 }
 static  class TubBounded{
 def example() {
   val t <: Tub = new Tub(3);
   // ERROR: val u <: Int = new Tub(3);
}}

 static class Hook {
   def run():Boolean = true;
}

}

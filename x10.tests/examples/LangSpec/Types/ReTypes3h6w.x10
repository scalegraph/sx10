/* Current test harness gets confused by packages, but it would be in package ReTypes3h6w;
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



public class ReTypes3h6w extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new ReTypes3h6w().execute();
    }


// file Types line 262
 static  class Example{ static def example() {
val f : (x:Int)=>Int{self!=x} = (x:Int) => (x+1) as Int{self!=x};
val g : (y:Int)=>Int{self!=y} = f;
val t : (x:Int)=>Int          = (x:Int) => x;
val u : ( Int )=>Int          = t;
}}

 static class Hook {
   def run():Boolean = true;
}

}

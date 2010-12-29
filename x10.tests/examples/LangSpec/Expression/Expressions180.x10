/* Current test harness gets confused by packages, but it would be in package Types_Coercions;
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



public class Expressions180 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Expressions180().execute();
    }


// file Expressions line 1022
 static   class Person {}
 static   class Child extends Person{}
 static   class Exampllllle {
    def example(rhys:Child) =
  rhys as Person
;}

 static class Hook {
   def run():Boolean = true;
}

}
/* Current test harness gets confused by packages, but it would be in package ObjectInitialization_ShowingSegments;
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



public class ObjectInitialization20 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new ObjectInitialization20().execute();
    }


// file ObjectInitialization line 252
 static class Overlord(x:Int) {
  def this(x:Int) { property(x); }
}//Overlord
 static class Overdone(y:Int) extends Overlord  {
  val a : Int;
  val b =  y * 9000;
  def this(r:Int) {
    super(r);                      // (1)
    x10.io.Console.OUT.println(r); // (2)
    property(r+1);                 // (2)
    // field initializations here  // (3)
    a = r + 2;                     // (4)
  }
}//Overdone

 static class Hook {
   def run():Boolean = true;
}

}
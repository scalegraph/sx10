/* Current test harness gets confused by packages, but it would be in package Clocks_For_Spock;
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



public class Clocks10 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Clocks10().execute();
    }


// file Clocks line 33
 static class ClockEx {
  static def say(s:String) =
     { atomic{x10.io.Console.OUT.println(s);} }
  public static def main(argv:Rail[String]) {
    finish async{
      val cl = Clock.make();
      async clocked(cl) {// Activity A
        say("A-1");
        next;
        say("A-2");
        next;
        say("A-3");
      }// Activity A

      async clocked(cl) {// Activity B
        say("B-1");
        next;
        say("B-2");
        next;
        say("B-3");
      }// Activity B
    }
  }
 }

 static class Hook {
   def run():Boolean = true;
}

}
package  Classes.Make.Asses.Of.Girls.With.Fake.Passes;


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

// file Classes line 475
 class Example {var f : String = ""; def example(x:Object){x != null} = {this.f = x.toString();}}
 class Eyample {
  def exam(e:Example, x:Object) {
    if (x != null)
       e.example(x as Object{x != null});
    // WRONG: if (x != null) e.example(x);
  }
}

class Hook {
   def run():Boolean = true;
}


public class Classes9 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes9().execute();
    }
}    

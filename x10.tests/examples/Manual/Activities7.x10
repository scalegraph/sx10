package  Activities.Atsome.Globref2;


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

// file Activities line 424
 class GR2 {
  public static def main(argv: Array[String](1)) {
    val argref = GlobalRef[Array[String](1)](argv);
    at(here.next()) use(argref);
  }
  static def use(argref : GlobalRef[Array[String](1)]) {
    at(argref.home) {
      val argv = argref();
      argv(0) = "Hi!";
    }
  }
}

class Hook {
   def run():Boolean = true;
}


public class Activities7 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Activities7().execute();
    }
}    

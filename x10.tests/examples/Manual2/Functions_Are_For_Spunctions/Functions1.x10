package  Functions_Are_For_Spunctions;


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

// file Functions line 39
 class Examplllll {
 static
val sq: (Int) => Int
      = (n:Int) => {
           var s : Int = 0;
           val abs_n = n < 0 ? -n : n;
           for ([i] in 1..abs_n) s += abs_n;
           s
        };
}

class Hook {
   def run():Boolean = true;
}


public class Functions1 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Functions1().execute();
    }
}    

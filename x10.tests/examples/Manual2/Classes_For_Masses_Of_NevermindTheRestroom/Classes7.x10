package  Classes_For_Masses_Of_NevermindTheRestroom;


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

// file Classes line 308
 class Exampllll[T] {
 public val x : T;
 def this(y:T) { x=y; }
public final def x()=x;
}

class Hook {
   def run():Boolean = true;
}


public class Classes7 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes7().execute();
    }
}    

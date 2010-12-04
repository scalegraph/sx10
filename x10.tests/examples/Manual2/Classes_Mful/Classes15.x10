package  Classes_Mful;


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

// file Classes line 647
class Mful{
   def m() = 1;
   def m[T]() = 2;
   def m(x:Int) = 3;
   def m[T](x:Int) = 4;
}

class Hook {
   def run():Boolean = true;
}


public class Classes15 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes15().execute();
    }
}    

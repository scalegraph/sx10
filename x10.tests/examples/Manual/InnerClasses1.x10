package  Classes.StaticNested;


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

// file InnerClasses line 15
class Outer {
  private static val priv = 1;
  private static def special(n:Int) = n*n;
  public static class StaticNested {
     static def reveal(n:Int) = special(n) + priv;
  }
}

class Hook {
   def run():Boolean = true;
}


public class InnerClasses1 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new InnerClasses1().execute();
    }
}    

package  Types.Core.Bore.Lore;


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

// file Types line 181
class LinkedList[X] {
  val head : X;
  val tail : LinkedList[X];
  def this(head:X, tail:LinkedList[X]) {
     this.head = head; this.tail = tail;
  }
}

class Hook {
   def run():Boolean = true;
}


public class Types8 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Types8().execute();
    }
}    

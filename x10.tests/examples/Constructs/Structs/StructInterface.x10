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

interface StructInterface_Sum {
  def sum():int;
}

struct StructInterface_S1 implements StructInterface_Sum {
  val x:int;
  val y:int;
  static FF:int = StructInterface_S1(100,200).sum();

  public def this(a:int, b:int) { x = a; y = b; }

  public final def sum() = x + y;
}

class StructInterface_C implements StructInterface_Sum {
  val f1:StructInterface_S1;
  
  public def this(a:StructInterface_S1) { f1 = a; }

  public def sum() = f1.sum() + 3;
}

public class StructInterface extends x10Test {
  public def run(): boolean {
    val a:StructInterface_S1 = StructInterface_S1(3,4);
    chk(a.sum() == 7, "a.sum() == 7");
    return true;
  }

  public static def main(Array[String](1)) {
    new StructInterface().execute();
  }
}

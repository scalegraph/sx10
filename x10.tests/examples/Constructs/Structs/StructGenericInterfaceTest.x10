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

struct S[T] implements (Int,Int)=>T {
  private val data:Array[T](2);

  public def this(array:Array[T](2)) {
    data = array;
  }

  public def apply(i:Int, j:Int) = data(i, j);
}

public class StructGenericInterfaceTest extends x10Test {
  public def run(): boolean {
    val r = (1..5)*(1..5);
    val a = new Array[Int](r, (p:Point(2)) => p(0)+p(1));
    val s = S[Int](a);

    chk(s(1,1) == 2);
    chk(s(4,4) == 8);

    return true;
  }

  public static def main(Array[String](1)) {
    new StructGenericInterfaceTest().execute();
  }

}


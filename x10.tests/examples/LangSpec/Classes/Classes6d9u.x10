/* Current test harness gets confused by packages, but it would be in package Classes6d9u;
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



public class Classes6d9u extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes6d9u().execute();
    }


// file Classes line 2498
 static class Colorized[T] {
  private var thing:T;
  private var color:String;
  def this(thing:T, color:String) {
     this.thing = thing;
     this.color = color;
  }
  public def thing():T = thing;
  public def color():String = color;
  public static def example() {
    val colInt  : Colorized[Int]
                = new Colorized[Int](3, "green");
    assert colInt.thing() == 3
                && colInt.color().equals("green");
    val colTrue : Colorized[Boolean]
                = new Colorized[Boolean](true, "blue");
    assert colTrue.thing()
                && colTrue.color().equals("blue");
  }
}
 static class Hook{ def run() {Colorized.example(); return true;}}

}

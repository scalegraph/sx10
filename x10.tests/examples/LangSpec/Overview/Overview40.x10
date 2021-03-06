/* Current test harness gets confused by packages, but it would be in package Overview_Mat2;
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



public class Overview40 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Overview40().execute();
    }


// file Overview line 167
abstract static  class Mat(rows:Int, cols:Int) {
 static type Mat(r:Int, c:Int) = Mat{rows==r&&cols==c};
 abstract operator this + (y:Mat(this.rows,this.cols))
                 :Mat(this.rows, this.cols);
 abstract operator this * (y:Mat) {this.cols == y.rows}
                 :Mat(this.rows, y.cols);
  static def makeMat(r:Int,c:Int) : Mat(r,c) = null;
  static def example(a:Int, b:Int, c:Int) {
    val axb1 : Mat(a,b) = makeMat(a,b);
    val axb2 : Mat(a,b) = makeMat(a,b);
    val bxc  : Mat(b,c) = makeMat(b,c);
    val axc  : Mat(a,c) = (axb1 +axb2) * bxc;
  }
}

 static class Hook {
   def run():Boolean = true;
}

}

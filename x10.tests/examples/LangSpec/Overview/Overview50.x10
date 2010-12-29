/* Current test harness gets confused by packages, but it would be in package Overview_Mat1;
*/

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



public class Overview50 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Overview50().execute();
    }


// file Overview line 190
abstract static  class Mat(rows:Int, cols:Int) {
  static type Mat(r:Int, c:Int) = Mat{rows==r&&cols==c};
  public def this(r:Int, c:Int) : Mat(r,c) = {property(r,c);}
  static def makeMat(r:Int,c:Int) : Mat(r,c) = null;
  abstract  operator this + (y:Mat(this.rows,this.cols)):Mat(this.rows, this.cols);
  abstract  operator this * (y:Mat) {this.cols == y.rows} : Mat(this.rows, y.cols);
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
package  Classes.In.Poly102;


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

// file Classes line 1381
class Poly {
  public val coeff : Array[Int](1);
  public def this(coeff: Array[Int](1)) { this.coeff = coeff;}
  public def degree() = coeff.size()-1;
  public def  a(i:Int) = (i<0 || i>this.degree()) ? 0 : coeff(i);

  public operator this + (p:Poly) =  new Poly(
     new Array[Int](
        Math.max(this.coeff.size(), p.coeff.size()),
        (i:Int) => this.a(i) + p.a(i)
     ));
  // ...
   public operator (n : Int) + this = new Poly([n]) + this;
   public operator this + (n : Int) = new Poly([n]) + this;

   def makeSureItWorks() {
      val x = new Poly([0,1]);
      val p <: Poly = x+x+x;
      val q <: Poly = 1+x;
      val r <: Poly = x+1;
   }

 }

class Hook {
   def run():Boolean = true;
}


public class Classes18 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes18().execute();
    }
}    

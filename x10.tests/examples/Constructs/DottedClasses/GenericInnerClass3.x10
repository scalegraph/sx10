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



// /*
//   Doing something twisty with inner classes: 
//   Inner <: GenericInnerClass3[Int], 
//   as well as 
//   Inner is inside of GenericInnerClass3[T].
// */




public class GenericInnerClass3[T] extends harness.x10Test {
  public static def main(Array[String](1)){
     val p = new GenericInnerClass3[Int](818);
     p.execute();
  }
  public def run():Boolean {
    val gicString = new GenericInnerClass3[String]("hum?");
    val innerString : GenericInnerClass3[String].Inner = gicString.new Inner("ow");
    innerString.test("ow");
    
    val gicInt = new GenericInnerClass3[Int](181);
    val innerInt : GenericInnerClass3[Int].Inner = gicInt.new Inner(34543);
    innerInt.test(34543);
    
    return true;    
  }
  val outerVal : T;
  var outerVar : T;
  def this(t:T) {
    outerVal = t;
    outerVar = t;
  }
  
  
  class Inner extends GenericInnerClass3[Int] {
    /*  a Inner is (a) inside one GenericInnerClass3[T], and 
        (b) extends GenericInnerClass[Int];
    */
    val innerVal : T; 
    val innerVar : T;
    
    def this(t :T) { super(888); innerVal = t; innerVar = t;
       this.outerVar = 10; // allowed because Inner extends 
       }
       
    def test(x:T): Boolean = {
       chk(this.outerVar == 10, "outerVar==10");
       chk(this.outerVal.equals(888), "this.outerVal");
       chk(this.outerVar.equals(888), "this.outerVar");
       chk(this.innerVal.equals(x), "this.innerVal");
       chk(this.innerVar.equals(x), "this.innerVar");
       chk(GenericInnerClass3.this.outerVal.equals(x), "GIC.outerVal");
       chk(GenericInnerClass3.this.outerVar.equals(x), "GIC.outerVar");
       return true;
    }
  }// Inner
  
  
  
}

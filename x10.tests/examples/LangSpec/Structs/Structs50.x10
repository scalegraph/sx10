/* Current test harness gets confused by packages, but it would be in package Structs_Converting_And_Conniving;
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



public class Structs50 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Structs50().execute();
    }


// file Structs line 275
 static  class Class2Struct {
   val a : Int;
   def this(a:Int) { this.a = a; }
   def m() = a;
 }
 static  struct Struct2Class {
   val a : Int;
   def this(a:Int) { this.a = a; }
   def m() = a;
 }
 static class Example {
 static def Examplle() {
val a = new Class2Struct(2);
val b = new Class2Struct(2);
assert a != b;
val c = Struct2Class(3);
val d = Struct2Class(3);
assert c==d;
}}

 static class Hook {
   def run():Boolean = true;
}

}
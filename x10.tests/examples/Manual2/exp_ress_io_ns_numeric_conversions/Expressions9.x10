package  exp_ress_io_ns_numeric_conversions;


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

// file Expressions line 395
 class ExampleOfConversionAndStuff {
 def example() {
val n : Byte = 123 as Byte; // explicit
val f : (Int)=>Boolean = (Int) => true;
val ok = f(n); // implicit
 } }

class Hook {
   def run():Boolean = true;
}


public class Expressions9 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Expressions9().execute();
    }
}    

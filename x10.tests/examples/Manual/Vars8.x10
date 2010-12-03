package  Vars.For.Glares;


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

// file Vars line 293
 class DestructuringEx1 {
 def whyJustForLocals() {
val [i] : Point = Point.make(11);
val p[j,k] = Point.make(22,33);
val q[l,m] = [44,55]; // coerces an array to a point.
}}

class Hook {
   def run():Boolean = true;
}


public class Vars8 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Vars8().execute();
    }
}    

package  classes_not_ferrets;


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


 class Waif(rect:Boolean, onePlace:Place, zeroBased:Boolean) {
   def this(rect:Boolean, onePlace:Place, zeroBased:Boolean)
          :Waif{self.rect==rect, self.onePlace==onePlace, self.zeroBased==zeroBased}
          = {property(rect, onePlace, zeroBased);}
   property rail: Boolean = rect && onePlace == here && zeroBased;
   static def zoink() {
      val w : Waif{
w.rail()
 }= new Waif(true, here, true);
 }}

class Hook {
   def run():Boolean = true;
}


public class Classes14 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Classes14().execute();
    }
}    

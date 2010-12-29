/* Current test harness gets confused by packages, but it would be in package expsome_Arrays11;
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



public class Arrays100 extends x10Test {
   public def run() : boolean = (new Hook()).run();
   public static def main(var args: Array[String](1)): void = {
        new Arrays100().execute();
    }


// file Arrays line 197

 static class Arrays11TestExp{
  def check()  = 1..10 && 2..20;  }

 static class Hook {
   def run():Boolean = true;
}

}
/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

import harness.x10Test;

/**
 * Purpose: Checks cast leading to primitive unboxing works.
 * @author vcave
 **/
 public class CastIntToAnyAndBack extends x10Test {

   public def run()  {
      val i  = mth() as Int;
      return true;
   }
   
   public def mth() = 3n as Any;
   public static def main(Rail[String]) {
      new CastIntToAnyAndBack().execute();
   }
}

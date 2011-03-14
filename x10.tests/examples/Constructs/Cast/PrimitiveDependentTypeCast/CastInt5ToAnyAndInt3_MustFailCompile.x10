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

/**
 * Purpose: Check you can cast a primitive to Any and back to a constrained type, 
 *  and the constraint is checked.
 * @author vcave
 *  @author vj  -- Moved to X10 2.0 and renamed.
 **/
 public class CastInt5ToAnyAndInt3_MustFailCompile extends x10Test {

   public def run() {
      try {
         val i = mth() as Int(3); // ERR
      } catch(e: ClassCastException) {
         return true;
      }
      return false;
   }
   
   public def mth() = 5 as Any{self==5};
   
   public static def main(Array[String](1)) {
      new CastInt5ToAnyAndInt3_MustFailCompile().execute();
   }
}

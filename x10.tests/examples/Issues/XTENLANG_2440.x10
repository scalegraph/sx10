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

import x10.compiler.tests.*; // err markers
import harness.x10Test;

/**
 * @author bardb 1/2011
 * This is one of our basic idioms.  It really ought to compile, but, as of rev. 
 * 20126, did not.
 */

// OPTIONS: -STATIC_CALLS 

public class XTENLANG_2440(x:Int) extends x10Test  { 

  public static def oughttowork() {
    val result : Int; // Uninitialized
    val start = here;
    at(here.next()) {
      val heavyMath = 1+1;
      at(start) {
        result = heavyMath;
      }
    }
    return result == 2; // ShouldNotBeERR (it will be fixed with athome proposal)
  }

    public def run()= oughttowork();

    public static def main(Array[String](1)) {
        new XTENLANG_2440(5).execute();
    }
}


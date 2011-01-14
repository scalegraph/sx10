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
 * @author bardb 1/2011
 */

// X10 should infer a correct type for f below.
// It compiles fine (as of now) with the proper type definition,
// but fails to compile if it is asked to infer the type

public class XTENLANG_2297 extends x10Test {

    public def run(): boolean {
        return true;
    }
    
    public static def main(Array[String](1)) {
        new XTENLANG_2297().execute();
    }
}

class clogua {
  public static def main(argv:Array[String](1)) {
    val n = 3;
// : (x:Int){x != n} => Int  
    val f 
          = (x:Int){x != n} => (12/(n-x)); // ShouldNotBeERR
    Console.OUT.println("f(5)=" + f(5));    
  }
}

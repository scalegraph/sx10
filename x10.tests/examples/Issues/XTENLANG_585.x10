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
 *Test that ž is permiited in an X10 identifier. 
 */

public class XTENLANG_585  extends x10Test {
    public static def main(Array[String](1)) {
        new XTENLANG_547_MustFailCompile().execute();
    }
    public def run()  {
         Console.OUT.println("Hello, X10 world!");
         val hž= ()=> 3;
         val myThree = hž();
         Console.OUT.println("The answer is: "+myThree);
         return 3==myThree;
    }
}
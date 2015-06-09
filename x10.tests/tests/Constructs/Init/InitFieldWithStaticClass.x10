
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
 * Check that it is ok to initialize a field with an instance of a nested static class
 * Yoav: it's not ok under the new initialization rules to let "this" escape as the receiver of "this. new InnerClass() "
 */

public class InitFieldWithStaticClass extends x10Test {

    static class A { 
    }
    val a = new A(); 
  
    public def run() =true;

    public static def main(Rail[String]) {
        new InitFieldWithStaticClass().execute();
    }
}

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
import x10.compiler.NonEscaping;



/**
 * Check that it is ok to initialize an instance field with an instance method call.
 */

public class InitFieldWithCall  extends x10Test {

    @NonEscaping final def m()=1;
    val a = m();
  
    public def run() =true;

    public static def main(Rail[String]) {
        new InitFieldWithCall().execute();
    }
}

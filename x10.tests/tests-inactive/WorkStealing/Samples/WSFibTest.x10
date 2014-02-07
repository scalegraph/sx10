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
//OPTIONS: -WORK_STEALING=true

public class WSFibTest {
    public def run():boolean {
        val res = Fib.fib(20);
        return (res == 10946);
    }

    public static def main(args:Rail[String]) {
        val r = new WSFibTest().run();
        if(r){
             x10.io.Console.OUT.println("++++++Test succeeded.");
        }
    }
}

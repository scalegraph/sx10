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
 * 
 *
 * @author vj 09/2008
 */

public class DefaultValueTypeDefTest extends x10Test {

    static struct Foo[T](n:int, s:T) {
       def this(n:int, s:T):Foo[T]{self.n==n,self.s==s} {
         property(n,s);
       }
    }
    public def run() = {
        val x:Foo[String]{self.n==2 && self.s=="a"} = Foo[String](2,"a");
        return true;
    }

    public static def main(var args: Array[String](1)): void = {
        new DefaultValueTypeDefTest().execute();
    }
}

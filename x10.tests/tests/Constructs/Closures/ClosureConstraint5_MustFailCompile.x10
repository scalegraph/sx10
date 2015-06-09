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

//OPTIONS: -STATIC_CHECKS 

import harness.x10Test;

/**
 * As with methods, a closure may declare a where clause to constraint
 * the actual parameters with which it may be invoked.
 *
 * @author bdlucas 8/2008
 */
public class ClosureConstraint5_MustFailCompile extends x10Test {

    public def run(): boolean = {
        
        val g = (x:long,y:long){x==1 && y==-1} => x+y;

        g(1,1); // ERR

        return true;
    }


    public static def main(var args: Rail[String]): void = {
        new ClosureConstraint5_MustFailCompile().execute();
    }
}

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
 * Closure expressions have zero or more formal parameters
 *
 * @author bdlucas 8/2008
 */

public class ClosureFormalParameters1c extends ClosureTest {

    public def run(): boolean = {
        
        check("((i:int,j:int)=>i+j)(1,1)", ((i:int,j:int)=>i+j)(1,1), 2);

        return result;
    }

    public static def main(var args: Array[String](1)): void = {
        new ClosureFormalParameters1c().execute();
    }
}

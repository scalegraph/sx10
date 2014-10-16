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
import x10.regionarray.*;


public class ConstDist extends x10Test {

    public def run(): boolean = {
    
        val R = Region.make(0..9, 0..9);
        val D = Dist.makeConstant(R, here);
        val a = DistArray.make[double](Dist.makeConstant(R, here));
        val b = DistArray.make[double](Dist.makeConstant(R, here));
        
        x10.io.Console.OUT.println("results are " + a + " " + b);

        return true;
    }

    public static def main(var args: Rail[String]): void = {
        new ConstDist().execute();
    }
}

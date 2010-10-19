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
 * Array operations and points must be type checked.
 *
 * @author kemal 4/2005
 */

public class ArrayTypeCheck extends x10Test {

    public def run(): boolean = {

        var a1: DistArray[int] = DistArray.make[int](Dist.makeConstant((0..2)*(0..3), here), (var p[i]: Point): int => { return i; });

        x10.io.Console.OUT.println("1");

        val E: Dist = Dist.makeConstant(-1..-2, here);

        try {
            x10.io.Console.OUT.println("a1.dist " + a1.dist);
            x10.io.Console.OUT.println("E " + E);
            x10.io.Console.OUT.println("== " + (a1.dist==E));
            // x10.io.Console.OUT.println(".equals " + a1.dist.equals(E)); XXXX
            var a2: DistArray[int] = a1 as DistArray[int](E);
            x10.io.Console.OUT.println("did not get exception");
            return false;
        } catch (var z: ClassCastException) {
            x10.io.Console.OUT.println("2");
        }

        try {
            val D: Dist = Dist.makeUnique();
            var a3: DistArray[int] = a1 as DistArray[int](D);
            return false;
        } catch (var z: ClassCastException) {
            x10.io.Console.OUT.println("3");
        }
        
        var i: int = 1;
        var j: int = 2;
        var k: int = 0;
        val p = [i, j, k] as Point;
        val q = [i, j] as Point;
        val r = [i] as Point;

        if (p == q) return false;
        
        return true;
    }

    public static def main(var args: Array[String](1)): void = {
        new ArrayTypeCheck().execute();
    }
}

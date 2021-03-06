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
 * Tests point (p[i,j]) notation.
 */

public class Array1Exploded extends x10Test {

    public def select(p[i,j]: Point, [k,l]: Point)=i+k;

    public def run(): boolean = {

        val r = (1..10)*(1..10);
        val ia = new Array[int](r);

        for (val p[i,j]: Point(2) in (1..10)*(1..10)) {
            chk(ia(p) == 0);
            ia(p) = i+j;
        }

        for (val p[i,j]: Point(2) in r) {
            val q1[m,n]  = [i, j] as Point;
            chk(i == m);
            chk(j == n);
            chk(ia(i, j) == i+j);
            chk(ia(i, j) == ia(p));
            chk(ia(q1) == ia(p));
        }

        chk(4 == select([1, 2], [3, 4]));

        return true;
    }

    public static def main(var args: Array[String](1)): void = {
        new Array1Exploded().execute();
    }
}

/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright Australian National University 2011.
 */
import harness.x10Test;

/**
 * Tests performance of clearing arrays of different base types
 * @author milthorpe 06/2011
 */
public class BenchmarkClearArray extends x10Test {
    static val REPS = 1000;
    public val N : Int;
    public def this(N : Int) {
        this.N = N;
    }
	public def run(): Boolean = {
        val a = new Array[Char](N+1);
        var start : Long = System.nanoTime();
        for (i in 0..REPS) {
            a.clear();
        }
        var stop : Long = System.nanoTime();
        Console.OUT.printf("clear Array[Char]: %g ms\n", ((stop-start) as Double) / REPS / 1e6);

        val b = new Array[Int](N+1);
        start = System.nanoTime();
        for (i in 0..REPS) {
            b.clear();
        }
        stop = System.nanoTime();
        Console.OUT.printf("clear Array[Int]: %g ms\n", ((stop-start) as Double) / REPS / 1e6);

        val c = new Array[Double](N+1);
        start = System.nanoTime();
        for (i in 0..REPS) {
            c.clear();
        }
        stop = System.nanoTime();
        Console.OUT.printf("clear Array[Double]: %g ms\n", ((stop-start) as Double) / REPS / 1e6);

        val d = new Array[Complex](N+1);
        start = System.nanoTime();
        for (i in 0..REPS) {
            d.clear();
        }
        stop = System.nanoTime();
        Console.OUT.printf("clear Array[Complex]: %g ms\n", ((stop-start) as Double) / REPS / 1e6);

        return true;
	}

	public static def main(var args: Rail[String]): void = {
        var n : Int = 1000000;
        if (args.size > 0) {
            n = Int.parseInt(args(0));
        }
		new BenchmarkClearArray(n).execute();
	}

}

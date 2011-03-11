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

import x10.io.Console;

/**
 * A simple illustration of loop parallelization within a single place.
 * Converted to X10 2.1 vj 9/1/2010
 */
public class ArraySum {

    var sum:Int;
    val data:Array[Int](1);

    public def this(n:Int) {
	// Create an Array of rank 1 with n elements (0..(n-1)), all initialized to 1.
        data = new Array[Int](n, 1);
        sum = 0;
    }

    def sum(a:Array[Int](1), start:Int, last:Int) {
        var mySum: Int = 0;
        for (i in start..(last-1)) { 
        	mySum += a(i);
        }
        return mySum;
    }

    def sum(numThreads:Int) {
        val mySize = data.size/numThreads;
        finish for (p in 0..(numThreads-1)) async {
            val mySum = sum(data, p*mySize, (p+1)*mySize);
            // Multiple activities will simultaneously update
            // this location -- so use an atomic operation.
            atomic sum += mySum;
        }
    }
    
    public static def main(args: Array[String](1)) {
        var size:Int = 5*1000*1000;
        if (args.size >=1)
            size = Int.parse(args(0));

        Console.OUT.println("Initializing.");
        val a = new ArraySum(size);
        val P = [1,2,4];

        //warmup loop
        val R = 0..(P.size-1);
        Console.OUT.println("Warming up.");
        for (i in R)
            a.sum(P(i));
        
        for (i in R) {
            Console.OUT.println("Starting with " + P(i) + " threads.");
            a.sum=0;
            var time: long = - System.nanoTime();
            a.sum(P(i));
            time += System.nanoTime();
            Console.OUT.println("For p=" + P(i) 
                    + " result: " + a.sum 
                    + ((size==a.sum)? " ok" : "  bad") 
                    + " (time=" + (time/(1000*1000)) + " ms)");
        }
        
        
    }
}

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

import x10.util.Timer;
import x10.io.Console;

/**
 * Version of Stream with a collection of local arrays implementing a
 * global array.
 * Converted to 2.1.
 */
public class FSSimpleDist {

    static MEG = 1024*1024;
    static alpha = 3.0D;

    static NUM_TIMES = 10;

    static DEFAULT_SIZE = MEG / 8;

    static NUM_PLACES = Place.MAX_PLACES;

    public static def main(args:Array[String](1)) {
        val verified = new Cell[Boolean](true);
        val times = GlobalRef[Array[double](1)](new Array[double](NUM_TIMES));
        val N0 = args.size > 0 ? int.parse(args(0)) : DEFAULT_SIZE;
        val N = N0 * NUM_PLACES;
        val localSize =  N0;

        Console.OUT.println("localSize=" + localSize);

        finish {

            for (var pp:int=0; pp<NUM_PLACES; pp++) {

                val p = pp;
                
                async at(Place.place(p)) {
                    
                    val a = new Array[double](localSize);
                    val b = new Array[double](localSize);
                    val c = new Array[double](localSize);
                    
                    for (var i:int=0; i<localSize; i++) {
                        b(i) = 1.5 * (p*localSize+i);
                        c(i) = 2.5 * (p*localSize+i);
                    }
                    
                    for (var j:int=0; j<NUM_TIMES; j++) {
                        if (p==0) {
                        	val t = times as GlobalRef[Array[double](1)]{self.home==here};
                        	t()(j) = -now(); 
                        }
                        for (var i:int=0; i<localSize; i++)
                            a(i) = b(i) + alpha*c(i);
                        if (p==0) {
                        	val t = times as GlobalRef[Array[double](1)]{self.home==here};
                        	t()(j) += now();
                        }
                    }
                    
                    // verification
                    for (var i:int=0; i<localSize; i++)
                        if (a(i) != b(i) + alpha*c(i)) 
                            verified.set(false);
                }
            }
        }

        var min:double = 1000000;
        for (var j:int=0; j<NUM_TIMES; j++)
            if (times()(j) < min)
                min = times()(j);
        printStats(N, min, verified());
    }

    static def now():double = Timer.nanoTime() * 1e-9;

    static def printStats(N:int, time:double, verified:boolean) {
        val size = (3*8*N/MEG);
        val rate = (3*8*N) / (1.0E9*time);
        Console.OUT.println("Number of places=" + NUM_PLACES);
        Console.OUT.println("Size of arrays: " + size +" MB (total)" + size/NUM_PLACES + " MB (per place)");
        Console.OUT.println("Min time: " + time + " rate=" + rate + " GB/s");
        Console.OUT.println("Result is " + (verified ? "verified." : "NOT verified."));
    }                                
}

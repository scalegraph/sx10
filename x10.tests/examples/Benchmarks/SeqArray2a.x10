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


/**
 * Basic array, c-style loop.
 *
 * @author bdlucas
 */
public class SeqArray2a extends Benchmark {

    //
    // parameters
    //

    val N = 2000;
    def expected() = 1.0*N*N*(N-1);
    def operations() = 2.0*N*N;

    //
    // the benchmark
    //

    val a = new Array[double]((0..N-1) * (0..N-1), (Point)=>0.0);

    def once() {
        for (var i:int=0; i<N; i++)
            for (var j:int=0; j<N; j++)
                a(i,j) = (i+j) as double;
        var sum:double = 0.0;
        for (var i:int=0; i<N; i++)
            for (var j:int=0; j<N; j++)
                sum += a(i,j);
        return sum;
    }

    //
    // boilerplate
    //

    public static def main(Array[String](1)) {
        new SeqArray2a().execute();
    }
}

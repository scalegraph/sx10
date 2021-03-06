/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2011.
 */

import x10.io.Console;
import x10.util.Timer;
import x10.compiler.Ifdef;
import x10.compiler.Ifndef;

import x10.matrix.Matrix;
import x10.matrix.Debug;
import x10.matrix.DenseMatrix;
import x10.matrix.DenseMultXTen;
import x10.matrix.VerifyTools;

import x10.matrix.block.Grid;
import x10.matrix.dist.DistDenseMatrix;
import x10.matrix.dist.summa.SummaDense;

/**
   <p>

   <p>
 */
public class DistDenseBench {
	public static def main(args:Array[String](1)) {
		val M = args.size > 0 ?Int.parse(args(0)):50;
		val K = args.size > 1 ?Int.parse(args(1)):50;
		val N = args.size > 2 ?Int.parse(args(2)):50;
		val iter = args.size > 3 ? Int.parse(args(3)):1;
		val ps = args.size > 4 ? Int.parse(args(4)):0;
		
		val tc = new RunDistDenseBench(M, K, N, iter, ps);
		tc.run();
	}
}

class RunDistDenseBench{

	public val iter:Int;
	public val testps:Int, lastps:Int;
	public val M:Int, N:Int, K:Int;
	public val nplace:Int = Place.MAX_PLACES;

	public val aPart:Grid, bPart:Grid, btPart:Grid, cPart:Grid;
		
	val A:DistDenseMatrix(aPart.M, aPart.N);
	val B:DistDenseMatrix(bPart.M, bPart.N);
	val tB:DistDenseMatrix(btPart.M, btPart.N);
	val C:DistDenseMatrix(cPart.M, cPart.N);
	
	
	public def this(m:Int, k:Int, n:Int, it:Int, p:Int) {
		M = m; N = n; K=k; iter=it; 
		aPart  = Grid.make(M, K);
		bPart  = Grid.make(K, N);
		btPart = Grid.make(N, K);
		cPart  = Grid.make(M, N);
		
		A  = DistDenseMatrix.make(aPart);
		B  = DistDenseMatrix.make(bPart);
		tB = DistDenseMatrix.make(btPart);
		C  = DistDenseMatrix.make(cPart);
		
		if (p != 0) {
			testps = p;	lastps = p;
		} else {
			testps = 1;	
			val lps = Math.min(aPart.getMinColSize(), bPart.getMinRowSize()); 
			lastps=Math.min(lps, 256);
		}
	}

	public def compMFPS(t:Double) = 2.0*M*N*K/(t*1000*1000*aPart.size);
    public def run(): void {
		Console.OUT.println("Starting dist dense multiply benchamrks tests on "+
							M+"x"+K+" * "+K+"x"+N+" matrices over "+nplace+" places");

		Debug.flushln("Start init dist matrices");
		A.initRandom();
		B.initRandom();
		tB.initRandom();

		testDenseMult();
		testDenseMultTrans();
	}
    
    public def testDenseMult():Boolean {

    	Console.OUT.printf("\nTest dist dense multiply (%dx%d) * (%dx%d) over %dx%d place\n",
    			 			M, K, K, N, aPart.numRowBlocks, aPart.numColBlocks);
 
    	Debug.flushln("Start dist computation");
    	for (var ps:Int=testps; ps <= lastps; ps*=2) {
    		C.init(0.1/7); 
    		
    		C.distBs(here.id()).calcTime=0; 
    		C.distBs(here.id()).commTime=0; 
    		
    		val stt = Timer.milliTime();
    		
    		for (1..iter) 
    			SummaDense.mult(ps, 1.0, A, B, C);
 
    		val avgt= (1.0*Timer.milliTime()-stt) /1000/iter;
    		val cmut= 1.0*C.getCommTime()/1000/iter;
    		val cmpt= 1.0*C.getCalcTime()/1000/iter;
    		Console.OUT.printf("Benchmark dist dense mult --- Panelsize:%4d, Time:%9.3f Sec, Mfps:%8.3f per place (cmu:%8.3f cmp:%8.3f)\n",
    							ps, avgt, compMFPS(avgt), cmut, cmpt);
    	}
    	return true;
    }

    public def testDenseMultTrans():Boolean {
    	Console.OUT.printf("\nTest dist dense multTrans: (%dx%d) * (%dx%d)^T over %d places\n",
    						M, K, N, K, nplace);

    	Debug.flushln("Start computation");
    	for (var ps:Int=testps; ps <= lastps; ps*=2) {
    		C.init(0.1/7);
    		C.distBs(here.id()).calcTime=0; 
    		C.distBs(here.id()).commTime=0; 
    		
    		val stt = Timer.milliTime();
    		for (1..iter) {
    			SummaDense.multTrans(ps, 1.0, A, tB, C);
    		}
    	   	val avgt= (1.0*Timer.milliTime()-stt) /1000/iter;
    	   	val cmut= 1.0*C.getCommTime()/1000/iter;
    	   	val cmpt= 1.0*C.getCalcTime()/1000/iter;
    		Console.OUT.printf("Benchmark dist dense multTrans --- Panelsize:%4d, Time:%9.3f Sec, Mfps:%8.3f per place (cmu:%8.3f cmp:%8.3f)\n", 
    							ps, avgt,  compMFPS(avgt), cmut, cmpt);
    	}
    	return true;
    }
    
}

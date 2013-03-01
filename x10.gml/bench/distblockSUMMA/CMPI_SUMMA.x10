/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011.
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
import x10.matrix.dist.summa.mpi.SummaMPI;


/**
   <p>
 * This benchmark test only compiles with native-C++ backend for MPI transport
   <p>
 */

public class CMPI_SUMMA {
	public static def main(args:Rail[String]) {
		val M   = args.size > 0 ?Int.parse(args(0)):100;
		val K   = args.size > 1 ?Int.parse(args(1)):100;
		val N   = args.size > 2 ?Int.parse(args(2)):100;
		val nzd = args.size > 3 ?Double.parse(args(3)):1.0;//Ignore
		val pnl = args.size > 4 ?Int.parse(args(4)):64;
		val bMN = args.size > 5 ?Int.parse(args(5)):1; //Ignore
		val it  = args.size > 6 ?Int.parse(args(6)):4;
		
		val tc = new RunSummaMPIBench(M, K, N, it, pnl);
		tc.run();
	}
} 

class RunSummaMPIBench{

	public val iter:Int;
	public val M:Int, N:Int, K:Int;
	public val testps:Int, lastps:Int;
	public val nplace:Int = Place.MAX_PLACES;
	public val aPart:Grid, bPart:Grid, btPart:Grid, cPart:Grid;

	val A:DistDenseMatrix(aPart.M, aPart.N);
	val B:DistDenseMatrix(bPart.M, bPart.N);
	val tB:DistDenseMatrix(btPart.M, btPart.N);
	val C:DistDenseMatrix(cPart.M, cPart.N);
	
	public def this(m:Int, k:Int, n:Int, it:Int, p:Int) {
		M = m; N = n; K=k; iter=it;
		aPart = Grid.make(M, K);
		bPart = Grid.make(K, N);
		btPart = Grid.make(N, K);
		cPart = Grid.make(M, N);
		
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

		testSummaMultMPI();
		testSummaMultTransMPI();
	}
    
    public def testSummaMultMPI():Boolean {
    	@Ifdef("MPI_COMMU") {
    		Console.OUT.printf("\nTest SUMMA C-MPI implementation (%dx%d) * (%dx%d) over %dx%d place\n",
    							M, K, K, N, aPart.numRowBlocks, aPart.numColBlocks);
    		for (var ps:Int=testps; ps <= lastps; ps*=2) {
    			C.init(0.1/7);
    			C.distBs(here.id()).calcTime=0; 
    			C.distBs(here.id()).commTime=0; 
    			
    			val stt = Timer.milliTime();
    			for (1..iter) 
    				SummaMPI.mult(ps, 1.0, A, B, C);
    			val avgt= (1.0*Timer.milliTime()-stt) /1000/iter;
    			val cmut= 1.0*C.getCommTime()/1000/iter; 
    			val cmpt= 1.0*C.getCalcTime()/1000/iter;
    			Console.OUT.printf("SUMMA mult total run time: %8.1f ms, ", avgt);
    			Console.OUT.printf("commun: %8.1f ms( %2.1f percent), comput: %8.1f ms( %2.1f percent)\n",
    					cmut, 100.0*cmut/avgt, cmpt, 100.0*cmpt/avgt);
    			//Console.OUT.printf("Benchmark SUMMA C-MPI mult --- Panelsize:%4d, Time:%9.3f Sec, Mfps:%f per place (cmu:%8.3f cmp:%8.3f)\n", 
    			//			ps, avgt, compMFPS(avgt), cmut, cmpt);
    		}
    	}
    	return true;
    }
    
    public def testSummaMultTransMPI():Boolean {
    	@Ifdef("MPI_COMMU") {
    		Console.OUT.printf("\nTest SUMMA C-MPI implementation multTrans: (%dx%d) * (%dx%d)^T over %d places\n",
    				M, K, N, K, nplace);
    		for (var ps:Int=testps; ps <=lastps; ps*=2) {
    			C.init(0.1/7);
    			C.distBs(here.id()).calcTime=0; 
    			C.distBs(here.id()).commTime=0; 
    			
    			val stt = Timer.milliTime();
    				for (1..iter) 
    					SummaMPI.multTrans(ps, 1.0, A, tB, C);
    			val avgt= (1.0*Timer.milliTime()-stt) /1000/iter;
    			val cmut= 1.0*C.getCommTime()/1000/iter;
    			val cmpt= 1.0*C.getCalcTime()/1000/iter;
    			Console.OUT.printf("SUMMA multTrans total run time: %8.1f ms, " , avgt);
    			Console.OUT.printf("commun: %8.1f ms( %2.1f percent), comput: %8.1f ms( %2.1f percent)\n",
    					cmut, 100.0*cmut/avgt, cmpt, 100.0*cmpt/avgt);
    			//Console.OUT.printf("Benchmark SUMMA C-MPI multTrans --- Panelsize:%4d, Time: %9.3f Sec, Mfps:%f per place (cmu:%8.3f cmp:%8.3f)\n", 
    			//					ps, avgt, compMFPS(avgt), cmut, cmpt);
    		}
    	}
    	return true;
    }
}

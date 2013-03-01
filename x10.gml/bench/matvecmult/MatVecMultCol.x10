/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011.
 */

import x10.io.Console;
import x10.util.Timer;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.blas.DenseMatrixBLAS;
import x10.matrix.block.Grid;

import x10.matrix.dist.DistDenseMatrix;
import x10.matrix.dist.DistSparseMatrix;
import x10.matrix.dist.DupDenseMatrix;

import x10.matrix.dist.DistMultDupToDist;
import x10.matrix.dist.DistMultDistToDup;

/**
 * This class contain a different partitioning of Matrix * Vector.
 * Matrx A and vector V are partitioned column-wise and row-wise in the same way. 
   <p>

   <p>
 */

public class MatVecMultCol{
	
    public static def main(args:Rail[String]) {
    	
    	val M   = args.size > 0 ?Int.parse(args(0)):100;
    	val nnz = args.size > 1 ?Double.parse(args(1)):0.5;
    	val it  = args.size > 2 ?Int.parse(args(2)):3;
    	val vrf = args.size > 3 ?Int.parse(args(3)):0;
   	
		val testcase = new DVMultColwise(M, nnz, it, vrf);
		testcase.run();
	}
}

class DVMultColwise {
	val it:Int;
	val vrf:Int;
	
	//--------------
	val M:Int;
	val partA:Grid;
	val partV:Grid;
	
	val dstA:DistSparseMatrix(M,M);
	val dstV:DistDenseMatrix(M,1);
	val V:DenseMatrix(M,1);
	val dupP:DupDenseMatrix(M,1);
	
	//---------------------
	public var st:Double;
	public var ed:Double;
	public var cmpt:Double = 0.0;
	public var comt:Double = 0.0;

	public def this(m:Int, nnz:Double, i:Int, v:Int) {
		M=m;
		it = i; vrf=v;
		
		val numP = Place.numPlaces();//Place.MAX_PLACES;
		Console.OUT.printf("\nTest Dist sparse mult dist dense over %d places\n", numP);
		
		partA = new Grid(M, M, 1, numP);
		dstA  = DistSparseMatrix.make(partA, nnz) as DistSparseMatrix(M,M);
		dstA.initRandom(nnz);
		//dstA.init(1.0);
		dstA.printRandomInfo();
		
		V     = DenseMatrix.make(M,1);
		partV = new Grid(M, 1, numP, 1); // Vector is partitioned row-wise
		                                 // but using the same partitioning geometry
		dstV = DistDenseMatrix.make(partV) as DistDenseMatrix(M,1);
		//dstV.init(1.0);
		dstV.initRandom();

		dupP  = DupDenseMatrix.make(M, 1);
	}
	
	public def run(): void {
		var ret:Boolean = true;
		if (vrf > 0) 
			dstV.copyTo(V);
		
		// Set the matrix function
		runMultParallel();
		if (vrf > 0)
			runVerify();
	}
	//------------------------------------------------
	//------------------------------------------------
	public def runMultParallel():void {
		var ct:Long=0;
		st = Timer.milliTime();		
		for (1..it) {
			/* Timer */ ct = Timer.milliTime();
			//dstA.print(); dstV.print();
			DistMultDistToDup.comp(dstA, dstV, dupP, false);
			//dupP.print();
			/* Timer */ cmpt += Timer.milliTime()-ct;
			
			/* Timer */ ct = Timer.milliTime();
			dstV.copyFrom(dupP.local()); 
			/* Timer */ comt += Timer.milliTime() -ct;
		}
		ed = Timer.milliTime();
		Console.OUT.printf("\nDone Dist*Dist->Dup MatVecMult for %d iteration\n", it);

		val tt = (ed-st) / it;
		comt = comt/it;
		cmpt = comt/it;
		Console.OUT.printf("MatVecMult Time:%9.3f ms, communication:%8.3f computation:%8.3f\n", 
				tt, comt, cmpt);		
	}

	public def runVerify():Boolean {
		Console.OUT.printf("Starting converting sparse matrix to dense\n");
		val ma = dstA.toDense() as DenseMatrix(M,M);
		val mb = V;//dstV.toDense() as DenseMatrix(M,1);
		val mc = DenseMatrix.make(ma.M, mb.N);
		Console.OUT.printf("Starting verification on dense matrix\n");
		
		for (1..it) {
			DenseMatrixBLAS.comp(ma, mb, mc, false);
			mc.copyTo(mb);
		}
		
		val ret = mc.equals(dupP.local() as Matrix(mc.M, mc.N));
		if (ret)
			Console.OUT.println("Dist*Dist->Dup MatVecMult test passed!");
		else
			Console.OUT.println("-----Dist*Dist->Dup MatVecMult test failed!-----");
		return ret;
	}
	
}


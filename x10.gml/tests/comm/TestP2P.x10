/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011-2014.
 */

import x10.util.Timer;
import x10.regionarray.DistArray;
import x10.compiler.Ifdef;
import x10.compiler.Ifndef;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.sparse.SparseCSC;
import x10.matrix.dist.DupDenseMatrix;
import x10.matrix.dist.DupSparseMatrix;
import x10.matrix.comm.MatrixRemoteCopy;

/**
   This class contains test cases P2P communication for matrix over different places.
 */
public class TestP2P{
    public static def main(args:Rail[String]) {
		val m = args.size > 0 ? Long.parse(args(0)):50;
		val n = args.size > 1 ? Long.parse(args(1)):50;
		val d = args.size > 2 ? Double.parse(args(2)):0.5;
		val i = args.size > 3 ? Long.parse(args(3)):1;
		val testcase = new TestMatrixCopy(m, n, d, i);
		testcase.run();
	}
}

class TestMatrixCopy {
	public val M:Long;
	public val N:Long;
	public val iter:Long;
	public val nzdensity:Double;

	public val numplace:Long;

	public val dmat:DupDenseMatrix;
	public val smat:DupSparseMatrix;

    public def this(m:Long, n:Long, d:Double, i:Long) {
		M=m; N=n; iter=i;
		nzdensity = d;
		
		dmat = DupDenseMatrix.make(m, n);
	@Ifdef("MPI_COMMU") { // TODO Deadlocks!
        smat = null;
    }
	@Ifndef("MPI_COMMU") { // TODO Deadlocks!
		smat = DupSparseMatrix.make(m, n, nzdensity);
    }
		numplace  = Place.numPlaces();
	}
	
	public def run(): void {
	@Ifndef("MPI_COMMU") { // TODO Deadlocks!
 		// Set the matrix function
		var retval:Boolean = true;
		retval &= testCopyTo();
		retval &= testCopyFrom();
		retval &= testSparseCopyTo();
		retval &= testSparseCopyFrom();

		if (!retval) 
			Console.OUT.println("------------Matrix communication test P2P failed!-----------");
    }
	}


	public def testCopyTo():Boolean {
		val ret:Boolean;
		var ds:Long = 0L;
		dmat.local().initRandom();
		
		Console.OUT.println("\nTest P2P copyTo dense matrix("+M+"x"+N+") in double over "
							+ numplace+" places");

		var st:Long =  Timer.milliTime();
		val ddm:DistArray[DenseMatrix](1) = dmat.dupMs;
		for (var i:Long=0; i<iter; i++) {
			for (var p:Long=0; p<numplace; p++) {
				if (p != here.id()) {
					ds=MatrixRemoteCopy.copyTo(ddm, p);
				}
			}
		}
		val tt =  Timer.milliTime() - st;
		
		val avgt = 1.0*tt/iter/(numplace-1);
		Console.OUT.printf("P2P copyTo %d bytes : %.3f ms, thput: %2.2f MB/s per iteration\n", 
						   ds*8, avgt, 8000.0*ds/avgt/1024/1024);

		ret = dmat.syncCheck();
		if (!ret)
			Console.OUT.println("--------P2P CopyTo failed, sync check failed!--------");
		
		return ret;
	}

	public def testCopyFrom() : Boolean{
		val ret:Boolean;
		var ds:Long = 0L;
		var st:Long =  Timer.milliTime();
		
		dmat.local().initRandom();
		
		Console.OUT.println("\nTest P2P copyFrom dense matrix("+M+"x"+N+") in double over "
							+ numplace+" places");
		val root = here.id();
		for (var i:Long=0; i<iter; i++) {
			for (var p:Long=0; p<numplace; p++) {
				if (p != root) {
					ds = at(dmat.dupMs.dist(p)) {
						val ddm : DistArray[DenseMatrix](1) = dmat.dupMs;
						MatrixRemoteCopy.copyFrom(ddm, root)
					};
				}
			}
		}
		val tt =  Timer.milliTime() - st;
		
		val avgt = 1.0*tt/iter/(numplace-1);
		Console.OUT.printf("P2P copyFrom %d bytes: %.3f ms, thput: %2.2f MB/s per iteration\n", 
						   ds*8, avgt, 8000.0*ds/avgt/1024/1024);
				
		ret=dmat.syncCheck();
		if (!ret) 
			Console.OUT.println("--------P2P CopyFrom failed, sync check not pass!--------");
		
		return ret;
	}

	public def testSparseCopyTo() :Boolean{
		val ret:Boolean;
		var ds:Long = 0L;
		var st:Long =  Timer.milliTime();
		 
		smat.local().initRandom(nzdensity);
		//smat.local().printSparse();

		val dsm : DistArray[SparseCSC](1) = smat.dupMs;
		Console.OUT.println("\nTest P2P copyTo sparse matrix("+M+"x"+N+") in double over "
							+ numplace+" places");
		for (var i:Long=0; i<iter; i++) {
			for (var p:Long=0; p<numplace; p++) {
				if (p != here.id()) {
					ds=MatrixRemoteCopy.copyTo(dsm, p);
				}
			}
		}
		//smat.printAll("Copy result");
		val tt =  Timer.milliTime() - st;
		
		val avgt = 1.0*tt/iter/(numplace-1);
		Console.OUT.printf("P2P sparse copyTo %d bytes : %.3f ms, thput: %2.2f MB/s per iteration\n", 
						   ds*8, avgt, 8000.0*ds/avgt/1024/1024);

		ret = smat.syncCheck();
		if (!ret) 
			Console.OUT.println("--------P2P CopyTo sparse matrix failed, sync check not pass!--------");
		return ret;
	}

	public def testSparseCopyFrom() :Boolean{
		val ret:Boolean;
		var ds:Long = 0L;
		var st:Long =  Timer.milliTime();
		 
		smat.local().initRandom(nzdensity);
		smat.sync();
		//smat.local().printSparse();

		Console.OUT.println("\nTest P2P copyFrom sparse matrix("+M+"x"+N+") in double over "
							+ numplace+" places");
		
		val root = here.id();
		//val dsm : DistArray[SparseCSC](1) = smat.dupMs;
		val dsm = smat.dupMs;
		for (var i:Long=0; i<iter; i++) {
			for (var p:Long=0; p<numplace; p++) {
				if (p != here.id()) {
					ds = at(smat.dupMs.dist(p)) {
						MatrixRemoteCopy.copyFrom(dsm, root)
					};
				}
			}
		}
		val tt =  Timer.milliTime() - st;
		
		val avgt = 1.0*tt/iter/(numplace-1);
		Console.OUT.printf("P2P copyFrom %d bytes : %.3f ms, thput: %2.2f MB/s per iteration\n", 
						   ds*8, avgt, 8000.0*ds/avgt/1024/1024);

		ret = smat.syncCheck();
		if (!ret) 
			Console.OUT.println("--------P2P CopyFrom sparse matrix failed, sync check not pass!--------");
		
		return ret;
	}

}

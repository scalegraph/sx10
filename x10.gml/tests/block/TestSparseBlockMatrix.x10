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

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.sparse.SparseCSC;
import x10.matrix.block.Grid;
import x10.matrix.block.SparseBlockMatrix;

public class TestSparseBlockMatrix {
    public static def main(args:Rail[String]) {
		val testcase = new TestSBMatrix(args);
		testcase.run();
	}
}

class TestSBMatrix {
	public val M:Long;
	public val N:Long;
	public val R:Long;
	public val C:Long;
	public val grid:Grid;
	public val nzd:Double;

    public def this(args:Rail[String]) {
		M = args.size > 0 ? Long.parse(args(0)):40;
		N = args.size > 1 ? Long.parse(args(1)):(M as Int)+2;
		R = args.size > 2 ? Long.parse(args(2)):2;
		C = args.size > 3 ? Long.parse(args(3)):3;
		nzd =  args.size > 4 ?Double.parse(args(4)):0.9;
		grid = new Grid(M, N, R, C);
	}

    public def run (): void {
		Console.OUT.println("Starting sparse block matrix tests");
		Console.OUT.printf("Matrix M:%d N:%d Row block:%d Column block:%d, nzd:%f\n", 
						   M, N, R, C, nzd);

		var ret:Boolean = true;
 		// Set the matrix function
 		ret &= (testClone());
 		ret &= (testCopyTo());
 		ret &= (testCopyFrom());
		ret &= (testScale());

		if (ret)
			Console.OUT.println("Sparse block matrix test passed!");
		else
			Console.OUT.println("----------------Sparse block matrix test failed!----------------");
	}

	public def testClone():Boolean{
		var ret:Boolean = true;
		Console.OUT.println("Starting sparse block matrix clone test");
		val sbm = SparseBlockMatrix.make(grid, nzd);
		sbm.initRandom();

		val sbm1 = sbm.clone();
		ret = sbm.equals(sbm1 as Matrix(M,N));

		val dm = DenseMatrix.make(grid.M, grid.N);
		sbm.copyTo(dm);
		ret &= sbm.equals(dm as Matrix(M,N));

		if (ret)
			Console.OUT.println("Sparse block matrix Clone and dense conversion test passed!");
		else
			Console.OUT.println("--------Sparse block matrix Clone test failed!--------");

                sbm(1, 1) = sbm1(2,2) = 10.0;

                if ((sbm(1,1)==sbm1(2,2)) && (sbm(1,1)==10.0)) {
                        ret &= true;
                        Console.OUT.println("Sparse block Matrix chain assignment test passed!");
                } else {
                        ret &= false;
                        Console.OUT.println("---------- Sparse block Matrix chain assignment test failed!-------");
                }

		return ret;
	}

	public def testCopyTo():Boolean{
		var ret:Boolean = true;
		Console.OUT.println("Starting sparse block matrix copy To test");
		val sbm = SparseBlockMatrix.make(grid, nzd);
		sbm.initRandom();

		val sm = SparseCSC.make(grid.M, grid.N, nzd);
		sbm.copyTo(sm);
		ret = sm.equals(sbm);

		if (ret)
			Console.OUT.println("Sparse block matrix copy to test passed!");
		else
			Console.OUT.println("--------Sparse block matrix copy to test failed!--------");
		return ret;
	}

	public def testCopyFrom():Boolean{
		var ret:Boolean = true;
		Console.OUT.println("Starting sparse block matrix copy From test");
		val sm = SparseCSC.make(grid.M, grid.N, nzd);
		sm.initRandom();

		val sbm = SparseBlockMatrix.make(grid, nzd);
		sbm.copyFrom(sm);

		ret &= sm.equals(sbm);

		if (ret)
			Console.OUT.println("Sparse block matrix copy from test passed!");
		else
			Console.OUT.println("--------Sparse block matrix copy from test failed!--------");
		return ret;
	}

	public def testScale():Boolean{
		Console.OUT.println("Starting sparse block matrix scaling test, nzd:"+nzd);
		val dm = SparseBlockMatrix.make(grid, nzd);
		dm.initRandom(nzd);
		val dm1  = dm * 2.5;
		val m = dm.toDense();
		
		m.scale(2.5);
		val ret = m.equals(dm1 as Matrix(M,N));
		if (ret)
			Console.OUT.println("Sparse block matrix scaling test passed!");
		else
			Console.OUT.println("--------Sparse block matrix Scaling test failed!--------");	
		return ret;
	}
} 

/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2014.
 */

package x10.matrix.dist;

import x10.util.Timer;
//
import x10.matrix.util.MathTool;
import x10.matrix.util.Debug;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.blas.DenseMatrixBLAS;
//
//import x10.matrix.mpi.TeamAllBcast; //wait for perf improvement

/**
 * Implemenation of multiplication methods between duplicated matrices.
 * Result is updated in all copies.
 */

public class DupMultToDup {


	// Multiplication operations 


	/**
	 * Multiplication method. All copies are updated.
	 */
	public static def comp(
			A:DupDenseMatrix, 
			B:DupDenseMatrix{self.M==A.N},
			C:DupDenseMatrix(A.M,B.N),
			plus:Boolean) : DupDenseMatrix(C){
				
		Debug.assure(C.M==A.M&&A.N==B.M&&B.N==C.N);
		/* Timing */ val st= Timer.milliTime();
		val dms = C.dupMs;
		finish ateach(val [p]:Point in dms.dist) {
			val mA = A.local();
			val mB = B.local();
			val mC = C.local();
			DenseMatrixBLAS.comp(mA, mB, mC, plus);
		}
		/* Timeing */ C.calcTime += Timer.milliTime() - st;
		   return C;
	}
			
	public static def comp(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.M==A.N}, 
			C:DupDenseMatrix(A.M,B.N) 
	) = comp(A, B, C, false);

	public static def comp(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.M==A.N}
	) : DupDenseMatrix(A.M,B.N) {
		val C = DupDenseMatrix.make(A.M, B.N);
		comp(A, B, C, false);
		return C;
	}


	/**
	 * Multiplication method by using BLAS driver.All copies are updated
	 */
	public static def comp(
			A:DenseMatrix, 
			B:DenseMatrix{self.M==A.N},
			C:DupDenseMatrix(A.M,B.N),
			plus:Boolean
	):DupDenseMatrix(C) {
		Debug.assure(C.M==A.M&&A.N==B.M&&B.N==C.N);
		DenseMatrixBLAS.comp(A, B, C.local(), plus);
		C.sync();
		return C;
	}
	public static def comp(
			A:DenseMatrix,
			B:DenseMatrix{self.M==A.N}, 
			C:DupDenseMatrix(A.M,B.N)
	) = comp(A, B, C, false);

	public static def comp(
			A:DenseMatrix, 
			B:DenseMatrix{self.M==A.N}
	) : DupDenseMatrix(A.M,B.N) {
		val C = DupDenseMatrix.make(A.M, B.N);
		comp(A, B, C, false);
		return C;
	}



	// Transposed A

	/**
	 * Multiplication method. All copies are updated.
	 */
	public static def compTransMult(
			A:DupDenseMatrix, 
			B:DupDenseMatrix{self.M==A.M},
			C:DupDenseMatrix(A.N,B.N),
			plus:Boolean
	) : DupDenseMatrix(C){
				
		Debug.assure(C.M==A.N&&A.M==B.M&&B.N==C.N);
		/* Timing */ val st= Timer.milliTime();
		finish ateach(val [p]:Point in C.dupMs.dist) {
			val mA = A.local();
			val mB = B.local();
			val mC = C.local();
			DenseMatrixBLAS.compTransMult(mA, mB, mC, plus);
		}
		/* Timeing */ C.calcTime += Timer.milliTime() - st;
		   return C;
	}
	public static def compTransMult(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.M==A.M}, 
			C:DupDenseMatrix(A.N,B.N)
	) = compTransMult(A, B, C, false);

	public static def compTransMult(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.M==A.M}
	) : DupDenseMatrix(A.N,B.N) {
		val C = DupDenseMatrix.make(A.N, B.N);
		compTransMult(A, B, C, false);
		return C;
	}

	/**
	 * Multiplication method by using BLAS driver.All copies are updated
	 */
	public static def compTransMult(
			A:DenseMatrix, 
			B:DenseMatrix{self.M==A.M},
			C:DupDenseMatrix(A.N,B.N),
			plus:Boolean
	) : DupDenseMatrix(C) {
		Debug.assure(C.M==A.N&&A.M==B.M&&B.N==C.N);
		DenseMatrixBLAS.compTransMult(A, B, C.local(), plus);
		C.sync();
		return C;
	}
			
	public static def compTransMult(
			A:DenseMatrix,
			B:DenseMatrix{self.M==A.M}, 
			C:DupDenseMatrix(A.N,B.N)
	) : DupDenseMatrix(C) = compTransMult(A, B, C, false);

	public static def compTransMult(
			A:DenseMatrix, 
			B:DenseMatrix{self.M==A.M}
	) : DupDenseMatrix(A.N,B.N) {
		val C = DupDenseMatrix.make(A.N, B.N);
		compTransMult(A, B, C, false);
		return C;
	}


	
	

	// Transposed B


	/**
	 * Multiplication method. All copies are updated.
	 */
	public static def compMultTrans(
			A:DupDenseMatrix, 
			B:DupDenseMatrix{self.N==A.N},
			C:DupDenseMatrix(A.M, B.M),
			plus:Boolean
	) : DupDenseMatrix(C) {
				
		Debug.assure(C.M==A.M&&A.N==B.N&&B.M==C.N);
		/* Timing */ val st= Timer.milliTime();
		finish ateach(val [p]:Point in C.dupMs.dist) {
			val mA = A.local();
			val mB = B.local();
			val mC = C.local();
			DenseMatrixBLAS.compMultTrans(mA, mB, mC, plus);
		}
		/* Timeing */ C.calcTime += Timer.milliTime() - st;
		return C;
	}
	public static def compMultTrans(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.N==A.N}, 
			C:DupDenseMatrix(A.M, B.M)) : void {
		compMultTrans(A, B, C, false);
	}

	public static def compMultTrans(
			A:DupDenseMatrix,
			B:DupDenseMatrix{self.N==A.N}
	) : DupDenseMatrix(A.M, B.M) {
		val C = DupDenseMatrix.make(A.M, B.M);
		compMultTrans(A, B, C, false);
		return C;
	}


	/**
	 * Multiplication method by using BLAS driver. All copies are updated
	 */
	public static def compMultTrans(
			A:DenseMatrix, 
			B:DenseMatrix{self.N==A.N},
			C:DupDenseMatrix(A.M,B.M),
			plus:Boolean
	) : DupDenseMatrix(C) {
		Debug.assure(C.M==A.M&&A.N==B.N&&B.M==C.N);
		DenseMatrixBLAS.compMultTrans(A, B, C.local(), plus);
		C.sync(); //Update all copies
		return C;
	}
	public static def compMultTrans(
			A:DenseMatrix,
			B:DenseMatrix{self.N==A.N}, 
			C:DupDenseMatrix(A.M,B.M)) =
		compMultTrans(A, B, C, false);

	public static def compMultTrans(
			A:DenseMatrix, 
			B:DenseMatrix{self.N==A.N}
	) : DupDenseMatrix(A.M,B.M) {
		val C = DupDenseMatrix.make(A.M, B.M);
		compMultTrans(A, B, C, false);
		return C;
	}

}

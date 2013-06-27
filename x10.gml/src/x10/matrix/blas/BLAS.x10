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

package x10.matrix.blas;

import x10.compiler.NoInline;

/**
 * The class provides direct calls to BLAS routines. Matrix data inputs are stored in Rail[Double], whose
 * memory layout complies with BLAS specification of column-mayor storage.  The leading dimension
 * of matrix (size of memory in double to store one column data) MUST be same as the matrix rows.
 * 
 * <p> NoInline is enforced to avoid WrapBLAS methods inlined in other packages in managed backend 
 */
public class BLAS {
	public static @NoInline def compScale(n:Long, a:Double, x:Rail[Double]):void {
		DriverBLAS.scale(n, a, x);
	}
	
	public static @NoInline def doCopy(n:Long, src:Rail[Double], dst:Rail[Double]):void {
		DriverBLAS.copy(n, src, dst);
	}
	
	public static @NoInline def compDotProd(n:Long, X:Rail[Double], Y:Rail[Double]):Double {
		val ret = DriverBLAS.dot_prod(n, X, Y);
		return ret;
	}
	
	public static @NoInline def compNorm(n:Long, X:Rail[Double]):Double {
		return DriverBLAS.norm(n, X);
	}
	
	public static @NoInline def compAbsSum(n:Long, X:Rail[Double]):Double {
		return DriverBLAS.abs_sum(n, X);
	}
	
	public static @NoInline def compMatMultMat(mA:Rail[Double], mB:Rail[Double],mC:Rail[Double], 
			dim:Rail[Long], scale:Rail[Double], trans:Rail[Int]):void {
		DriverBLAS.matrix_matrix_mult(mA,mB, mC, dim, scale, trans); 
	}

	public static @NoInline def compSymMultMat(mA:Rail[Double], mB:Rail[Double], mC:Rail[Double],
			dim:Rail[Long], scale:Rail[Double]):void {
		DriverBLAS.sym_matrix_mult(mA, mB, mC, dim, scale);
	}

	public static @NoInline def compMatMultSym(mB:Rail[Double],mA:Rail[Double],mC:Rail[Double],
			dim:Rail[Long], scale:Rail[Double]):void {
		DriverBLAS.matrix_sym_mult(mB, mA, mC, dim, scale);
	}

 
	public static @NoInline def compTriMultMat(mA:Rail[Double], mB:Rail[Double], 
			dim:Rail[Long], tranA:Int):void {
		DriverBLAS.tri_matrix_mult(mA, mB, dim, tranA);
	}

	public static @NoInline def compMatMultTri(mB:Rail[Double], mA:Rail[Double],
			dim:Rail[Long],tranB:Int):void {
		DriverBLAS.matrix_tri_mult(mB, mA, dim, tranB);
	}

	public static @NoInline def compMatMultVec(mA:Rail[Double], x:Rail[Double],y:Rail[Double],
			dim:Rail[Long], scale:Rail[Double], transA:Int):void {
		DriverBLAS.matrix_vector_mult(mA, x, y, dim, scale, transA);
	}
	
	public static @NoInline def compSymMultVec(mA:Rail[Double], x:Rail[Double], y:Rail[Double],
			dim:Rail[Long], scale:Rail[Double]):void {
		DriverBLAS.sym_vector_mult(mA, x, y, dim, scale);
	}
	
	public static @NoInline def compTriMultVec(mA:Rail[Double], uplo:Boolean, bx:Rail[Double], 
			lda:Long, tA:Int):void {
		DriverBLAS.tri_vector_mult(mA, uplo?1:0, bx, lda, tA);
	}

	public static @NoInline def solveTriMultVec(mA:Rail[Double], bx:Rail[Double], 
			dim:Rail[Long], transA:Int):void {
		DriverBLAS.tri_vector_solve(mA, bx, dim, transA);
	}
	
	public static @NoInline def solveTriMultMat(mA:Rail[Double], BX:Rail[Double], 
			dim:Rail[Long], transA:Int):void {
		DriverBLAS.tri_matrix_solve(mA, BX, dim, transA);
	}

	public static @NoInline def solveMatMultTri(BX:Rail[Double], mA:Rail[Double], 
			dim:Rail[Long], transA:Int):void {
		DriverBLAS.matrix_tri_solve(BX, mA, dim, transA);
	}
}

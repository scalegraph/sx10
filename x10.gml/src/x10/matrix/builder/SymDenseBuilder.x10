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

package x10.matrix.builder;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.SymDense;
import x10.matrix.Debug;
import x10.matrix.RandTool;

public type SymDenseBuilder(blder:SymDenseBuilder)=SymDenseBuilder{self==blder};
public type SymDenseBuilder(m:Int)=SymDenseBuilder{self.M==m,self.N==m};

/**
 * Builder/Initializer of symmetric dense matrix.
 */
public class SymDenseBuilder extends DenseBuilder{self.M==self.N} implements MatrixBuilder {
	
	public def this(sym:SymDense) {
		super(sym as DenseMatrix{self.M==self.N});
	}
	
	public def this(den:DenseMatrix{self.M==self.N}) {
		super(den);
	}
	
	/**
	 * Creat dense builder and dense matrix
	 * @param m   rows, leading dimension
	 * @param n   columns
	 */
	public static def make(m:Int): SymDenseBuilder(m) {
		val bdr = new SymDenseBuilder(SymDense.make(m));
		return bdr as SymDenseBuilder(m);
	}
	
	//==============================================
	/**
	 * Initial dense matrix with initial function.
	 */
	public def init(initFunc:(Int,Int)=>Double):SymDenseBuilder(this) {
		var stt:Int=0;
		for (var c:Int=0; c<this.N; c++, stt+=1+dense.M ) {
			var i:Int = stt;
			var j:Int = stt+dense.M;
			dense.d(i++) = initFunc(c, c);
			for (var r:Int=c+1; r<this.M; r++, i++, j += dense.M) 
				dense.d(i) = dense.d(j) = initFunc(r, c);
		}
		return this;
	}
	
	/**
	 * Initial symmetric dense matrix with initial function and location in dense matrix is generated randomly.
	 * @param nzDensity    nonzero sparsity.
	 * @param initFunc     nonzero value generating function.
	 */
	public def initRandom(nzDensity:Double, initFunc:(Int,Int)=>Double):SymDenseBuilder(this) {
		val maxdst:Int = ((1.0/nzDensity) as Int) * 2 - 1;
		var i:Int= RandTool.nextInt(maxdst/2);
		var stt:Int=0;
		for (var c:Int=0; c<this.N; c++, stt+=dense.M, i+=c  ) {
			var r:Int = i - stt;
			var j:Int = r*dense.M+c;

			//if (r==c) dense.d(i) = initFunc(c, c);
			for (;r<this.M; i+= RandTool.nextInt(maxdst)+1, r=i-stt, j=r*dense.M+c) {
				dense.d(i) = dense.d(j) = initFunc(r, c);
			}
		}
		return this;
	}

	
	public def initRandom(nzDensity:Double): SymDenseBuilder(this) =
		initRandom(nzDensity, (Int,Int)=>RandTool.getRandGen().nextDouble());
	
	//===============================================
	public def set(r:Int, c:Int, dv:Double) : void {
		dense(r, c) = dv;
		dense(c, r) = dv;
	}
	
	public def reset(r:Int, c:Int) : Boolean {
		dense(r, c) =0.0;
		dense(c, r) =0.0;
		return true;
	}
	//===============================================
	/**
	 * copy from upper or lower triangular and its mirror part.
	 */
	public def init(upper:Boolean, src:DenseMatrix):SymDenseBuilder(this) {
		if (upper) {
			TriDenseBuilder.copyUpper(src, this.dense as DenseMatrix(src.N));
			mirrorToLower();
		} else {
			TriDenseBuilder.copyLower(src, this.dense as DenseMatrix(src.M));
			mirrorToUpper();
		}
		return this;
	}
	/**
	 * Copy lower triangular part to upper
	 */
	public def mirrorToUpper() {
		var i:Int=1;
		var j:Int=M;
		for (var c:Int=0; c<M; c++, i+=c+1, j=i+M-1) {
			for (var r:Int=c+1; r<M; r++, i++, j+=M) 
				dense.d(j)= dense.d(i);
		}
	}
	
	/**
	 * Copy upper triangular part to lower
	 */
	public def mirrorToLower() {
		var i:Int=M;
		var j:Int=1;
		for (var c:Int=1; c<M; c++, i=c*M, j=c) {
			for (var r:Int=0; r<c; r++, i++, j+=M) 
				dense.d(j)= dense.d(i);
		}
	}
	
	//===============================================

	public def checkSymmetric():Boolean =
		SymDense.test(dense);
	
	public def toSymDense() : SymDense(M) {
		val sym = new SymDense(M, dense.d);
		return sym as SymDense(M);
	}
	
	public def toMatrix():Matrix = dense as Matrix;

	
}
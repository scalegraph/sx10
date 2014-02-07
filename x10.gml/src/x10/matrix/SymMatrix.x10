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

package x10.matrix;

import x10.util.StringBuilder;

import x10.matrix.blas.DenseMatrixBLAS;

public type SymMatrix(m:Long, n:Long)=SymMatrix{m==n, self.M==m, self.N==n};
public type SymMatrix(m:Long)=SymMatrix{self.M==m,self.N==m};
public type SymMatrix(C:SymMatrix)=SymMatrix{self==C};
public type SymMatrix(C:Matrix)=SymMatrix{self==C};


/**
 * The symmetric dense matrix is derived from dense matrix. It inherits the memory layout of the
 * dense matrix instance.  Therefore, it complies with
 * BLAS symmetric matrix. Only the lower part of matrix data is referenced.
 * <p>
 * Result of cell-wise operations on symmetric matrix is stored in dense instances.
 * Most cellwise operations resort to the super dense class's operations without
 * optimization.
 * 
 * @deprecated use {@link SymDense} instead
 */
public class SymMatrix extends DenseMatrix{self.M==self.N} {
	

	// Base data structure

	

	// Constructor, maker, and clone method

	public def this(n:Long, x:Rail[Double]) : SymMatrix(n){
		super(n, n, x);
	}
	

	public static def make(n:Long):SymMatrix(n) {
		val x = new Rail[Double](n*n);
		return new SymMatrix(n, x);
	}

	public static def make(src:SymMatrix):SymMatrix(src.M) {
		val n = src.M;
		val newd = new Rail[Double](src.d);
		return new SymMatrix(n, newd);
	}

	
	public def clone():SymMatrix(M){
		val nd = new Rail[Double](this.d);
		val nm = new SymMatrix(M, nd);
		return nm as SymMatrix(M);
	}
	
	public  def alloc(m:Long, n:Long):SymMatrix(m,n) {
		Debug.assure(m==n);
		val x = new Rail[Double](m*m);
		val nm = new SymMatrix(m, x);
		return nm as SymMatrix(m,n);
	}
	
	public def alloc() = alloc(this.M, this.M);
	

	// Data copy and reset 

	/**
	 * Copy the lower part to target and mirror the lower part to upper part.
	 */
	public def copyTo(dm:DenseMatrix(M,N)):void {
		var colstt:Long=0;
		for (var len:Long=N; len > 0; len--, colstt+=M+1) {
			Rail.copy(this.d, colstt, dm.d, colstt, len);		
		}
		mirrorLowerToUpper(dm);
	}
	
	public def copyTo(smat:SymMatrix(N)): void {
		super.copyTo(smat as DenseMatrix(M,N));
	}

	public def copyTo(mat:Matrix(M,N)) : void {
		if (mat instanceof DenseMatrix) {
			copyTo(mat as DenseMatrix(M,N));
		} else if (likeMe(mat)) {
			copyTo(mat as SymMatrix(N));
		} else {
			Debug.exit("CopyTo: Target matrix type is not compatible");
		}
	}

	/**
	 *  Copy lower triangular part to upper part.
	 */
	public static def mirrorLowerToUpper(dm:DenseMatrix) :void {
		var colstt:Long=0;
		var len:Long = (dm.M>dm.N)?dm.N:dm.M;
		for (; len > 0; len--, colstt+=dm.M+1) {
			var j:Long = colstt+dm.M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=dm.M)
				dm.d(j) = dm.d(i);		
		}
	}

	/**
	 * Copy upper part to lower
	 */
	public static def mirrorUpperToLower(dm:DenseMatrix) :void {
		var colstt:Long=0;
		var len:Long = (dm.M>dm.N)?dm.N:dm.M;
		for (; len > 0; len--, colstt+=dm.M+1) {
			var j:Long = colstt+dm.M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=dm.M)
				dm.d(i) = dm.d(j);		
		}
	}




	// Data initialization

	/**
	 * Initialize all elements of the symmetric matrix with a constant value.
	 * Only the lower part is initialized.
	 * 
	 * @param  iv 	the constant value
	 */	
	public def init(iv:Double): SymMatrix(this) {
		for (var i:Long=0; i<this.d.size; i++)
			this.d(i) = iv;
		return this;
	}
	
	
	/**
	 * Init with function. Only the lower triangular part and mirror the lower to
	 * upper part.
	 * 
	 * @param f    The function to use to initialize the matrix
	 * @return this object
	 */
	public def init(f:(Long,Long)=>Double): SymMatrix(this) {
		var i:Long=0;
		for (var c:Long=0; c<N; c++, i+=c)
			for (var r:Long=c; r<M; r++, i++)
				this.d(i) = f(r, c);
		mirrorLowerToUpper(this as DenseMatrix);
		return this;
	}
	
	/**
	 * Initialize lower part of matrix with random values, and then mirror to upper part. 
	 */	
	public def initRandom(): SymMatrix(this) {
		val rgen = RandTool.getRandGen();
		var colstt:Long=0;
		for (var len:Long=N; len>0; len--, colstt+=M+1)
			for (var i:Long=colstt; i<colstt+len; i++)
				this.d(i) = rgen.nextDouble();
		mirrorLowerToUpper(this);
		return this;
	}
	
	/**
	 * Initialize lower part with random values within the given bounds, and then
	 * mirror to upper part.
	 *  
	 * @param lb	lower bound of random values
	 * @param up	upper bound of random values
	 */	
	public def initRandom(lb:Long, ub:Long): SymMatrix(this) {
		val rgen = RandTool.getRandGen();
		val l = Math.abs(ub-lb)+1;
		var colstt:Long=0;
		for (var len:Long=N; len>0; len--, colstt+=M+1)
			for (var i:Long=colstt; i<colstt+len; i++)
				this.d(i) = rgen.nextLong(l)+lb;
		mirrorLowerToUpper(this);
		return this;
	}
	

	// Data access and set

	
	public  operator this(x:Long, y:Long):Double {
		if (y < x)
			return this.d(y*M+x);
		else
			return this.d(x*M+y);
	}
	
	public  operator this(x:Long,y:Long) = (v:Double):Double {
		if (y < x) 
			this.d(y*M +x) = v;
		else
			this.d(x*M +y) = v;
		return v;
	}	

	// Transpose

	public def T(sym:SymMatrix(M)) {
		copyTo(sym);
	}
	
	public def T():SymMatrix(N) = clone();
	

	// Cellwise operations. Only lower triangular part is modified.

	public  def scale(a:Double):SymMatrix(this)  {
		for (var i:Long =0; i<M*N; i++)
			this.d(i) *= a;
		return this;
	}
	
	public def sum():Double {
		var tt:Double = 0.0;
		var colstt:Long=1;
		for (var len:Long=M-1; len>0; len--, colstt+=M+1) 
			for (var i:Long=colstt; i<colstt+len; i++)
				tt += this.d(i);
		tt *= 2;
		for (var i:Long=0; i<N*M; i+=M+1)
			tt += this.d(i);
		return tt;
	}
	

	// Lower half cell-add operation

	public def cellAdd(v:Double) : SymMatrix(this) = 
		super.cellAdd(v) as SymMatrix(this);
		
	public def cellAdd(x:SymMatrix(M,N)):SymMatrix(this) =
		super.cellAdd(x as DenseMatrix(M,N)) as SymMatrix(this);
	
	public def cellAddTo(x:DenseMatrix(M,N)):DenseMatrix(x) {
		var colstt:Long=0;
		for (var len:Long=M; len>0; len--, colstt+=M+1) {
			x.d(colstt) += this.d(colstt);
			var j:Long = colstt+M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=M) {		
				x.d(i) += this.d(i);
				x.d(j) += this.d(i);
			}
		}
		return x;
	}
	

	// Cell-wise matrix multiplication

	
	// public def lowerCellSub(v:Double):SymMatrix(this) {
	// 	var colstt:Long=0;
	// 	for (var len:Long=M; len>0; len--, colstt+=M+1)
	// 		for (var i:Long=colstt; i<colstt+len; i++)		
	// 			this.d(i) -= v;
	// 	return this;
	// }
	public def cellSub(v:Double):SymMatrix(this) = 
		super.cellSub(v) as SymMatrix(this);

	public def cellSubFrom(v:Double):SymMatrix(this) =
		super.cellSubFrom(v) as SymMatrix(this);
	
	public def cellSub(x:SymMatrix(M,N)):SymMatrix(this) =
		super.cellSub(x as DenseMatrix(M,N)) as SymMatrix(this);
	
	/**
	 * x = x - this;
	 */
	public def cellSubFrom(x:DenseMatrix(M,N)):DenseMatrix(x) {
		var colstt:Long=0;
		for (var len:Long=M; len>0; len--, colstt+=M+1) {
			x.d(colstt) -= this.d(colstt);
			var j:Long = colstt+M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=M) {		
				x.d(i) -= this.d(i);
				x.d(j) -= this.d(i);
			}
		}
		return x;
	}
	

	// Lower part cell-wise matrix multiplication

	// public def lowerCellMult(v:Double):SymMatrix(this) {
	// 	var colstt:Long=0;
	// 	for (var len:Long=M; len>0; len--, colstt+=M+1)
	// 		for (var i:Long=colstt; i<colstt+len; i++)		
	// 			this.d(i) *= v;
	// 	return this;
	// }
	public def cellMult(v:Double):SymMatrix(this) =
		super.cellMult(v) as SymMatrix(this);
	
	public def cellMult(x:SymMatrix(M,N)):SymMatrix(this) =
		super.cellMult(x as DenseMatrix(M,N)) as SymMatrix(this);
	
	public def cellMultTo(x:DenseMatrix(M,N)):DenseMatrix(x) {
		var colstt:Long=0;
		for (var len:Long=N; len>0; len--, colstt+=M+1) {
			x.d(colstt) *= this.d(colstt);
			var j:Long = colstt+M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=M) {	
				x.d(i) *= this.d(i);
				x.d(j) *= this.d(i);
			}
		}
		return x;
	}

	// Cellwise division

	public def cellDiv(v:Double):SymMatrix(this) =
		super.cellDiv(v) as SymMatrix(this);
	
	public def cellDivBy(v:Double):SymMatrix(this) =
		super.cellDivBy(v) as SymMatrix(this);
	
	public def cellDiv(x:SymMatrix(M,N)):SymMatrix(this) =
		super.cellDiv(x as DenseMatrix(M,N)) as SymMatrix(this);
	
	public def cellDivBy(x:DenseMatrix(M,N)):DenseMatrix(x) {
		var colstt:Long=0;
		for (var len:Long=M; len>0; len--, colstt+=M+1) {
			x.d(colstt) /= this.d(colstt);
			var j:Long = colstt+M;
			for (var i:Long=colstt+1; i<colstt+len; i++, j+=M) {	
				x.d(i) /= this.d(i);
				x.d(j) /= this.d(i);
			}
		}
		return x;
	}
	

	// Matrix multiply operations: self this<- op(A)*op(B) + (plus?1:0) * C
	// Default is using BLAS driver
	// Use DenseMatrixBLAS method calls

	// public def mult(A:Matrix(this.M), B:Matrix(A.N,this.N),	plus:Boolean):SymMatrix(this) {
	// 	throw new UnsupportedOperationException("Matrix multiply does not support using SymMatrix as output matrix");
	// }

	// public def transMult(A:Matrix{self.N==this.M}, B:Matrix(A.M,this.N), plus:Boolean):SymMatrix(this) {
	// 	throw new UnsupportedOperationException("Matrix transposed multiply does not support using SymMatrix as output matrix");
	// }
	// 		
	// public def multTrans(A:Matrix(this.M), B:Matrix(this.N, A.N), plus:Boolean):SymMatrix(this) {
	// 	throw new UnsupportedOperationException("Matrix multiply transposed does not support using SymMatrix as output matrix");
	// }



	// Operator

	public operator - this            = this.clone().scale(-1.0)      as SymMatrix(M,N);
	public operator this + (v:Double) = this.clone().cellAdd(v)       as SymMatrix(M,N);
	public operator (v:Double) + this = (this + v) as SymMatrix(M,N);

	public operator this - (v:Double) = this.clone().cellSub(v)       as SymMatrix(M,N);
	public operator (v:Double) - this = this.clone().cellSubFrom(v)   as SymMatrix(M,N);
	public operator this / (v:Double) = this.clone().cellDiv(v)       as SymMatrix(M,N);
	public operator (v:Double) / this = this.clone().cellDivBy(v)     as SymMatrix(M,N);
	public operator this * (alpha:Double) = this.clone().scale(alpha) as SymMatrix(M,N);
	public operator (alpha:Double) * this = this * alpha;
	
	
	public operator this + (that:SymMatrix(M)) = this.clone().cellAdd(that)  as SymMatrix(M,N);
	public operator this - (that:SymMatrix(M)) = this.clone().cellSub(that)  as SymMatrix(M,N);
	public operator this * (that:SymMatrix(M)) = this.clone().cellMult(that) as SymMatrix(M,N);
	public operator this / (that:SymMatrix(M)) = this.clone().cellDiv(that)  as SymMatrix(M,N);

// 	/**
// 	 * Operation with dense matrix and store result in dense format
// 	 */
// 	public operator this + (that:DenseMatrix(M,N)):DenseMatrix(M,N) = this.cellAddTo(that.clone());
// 	public operator this - (that:DenseMatrix(M,N)):DenseMatrix(M,N) = this.toDense().cellSub(that) as DenseMatrix(M,N);
// 	public operator this * (that:DenseMatrix(M,N)):DenseMatrix(M,N) = this.cellMultTo(that.clone()) as DenseMatrix(M,N);
// 	public operator this / (that:DenseMatrix(M,N)):DenseMatrix(M,N) = this.toDense().cellDiv(that) as DenseMatrix(M,N);
// 	
// 	
// 	public operator (that:DenseMatrix(M,N)) + this = this + that;
// 	public operator (that:DenseMatrix(M,N)) - this = this.cellSubFrom(that.clone());
// 	public operator (that:DenseMatrix(M,N)) * this = this * that;
// 	public operator (that:DenseMatrix(M,N)) / this = this.cellDivBy(that.clone()) as DenseMatrix(M,N);
	
	
	/**
	 * Operation multiply, result stores in dense 
	 */
	public operator this % (that:DenseMatrix(N)):DenseMatrix(M,that.N) {
		val ret = DenseMatrix.make(this.M, that.N);
		DenseMatrixBLAS.comp(this, that, ret, false);
		return ret;
	}
	
	public operator (that:DenseMatrix{self.N==this.M}) % this :DenseMatrix(that.M,N) {
		val ret = DenseMatrix.make(that.M, this.N);
		DenseMatrixBLAS.comp(that, this, ret, false);
		return ret;
	}
		

	// Utils

	public def likeMe(m:Matrix):Boolean {
		if ((m instanceof SymMatrix) && m.M==M && m.N==N) return true;
		return false;
	}
	
	public static def test(dm:DenseMatrix):Boolean {
		if (dm.M != dm.N) return false;
		
		for (var c:Long=0; c<dm.M; c++)
			for (var r:Long=c+1; r<dm.M; r++)
				if (dm(r,c) != dm(c,r)) return false;
		return true;
	}
	
	public def toString():String {
		var idx:Long=0;
		val outstr = new StringBuilder();
		outstr.add("--------- Symmetric Matrix "+M+" x "+N+" lower part data ---------\n");
		for (var r:Long=0; r<M; r++) {
			outstr.add(r+"\t[");
			for (var c:Long=0; c<=r; c++)
				outstr.add(this(r,c).toString()+" ");
			outstr.add("]\n");
		}
		outstr.add("---------------------------------------\n");
		return outstr.toString();	
	}
}

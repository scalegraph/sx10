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

package x10.matrix.dist;

import x10.io.Console;
import x10.util.Timer;
//
import x10.matrix.Debug;
import x10.matrix.MathTool;
import x10.matrix.VerifyTools;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.sparse.SparseCSC;
//
import x10.matrix.comm.CommHandle;


public  type DupSparseMatrix(M:Int)=DupSparseMatrix{self.M==M};
public  type DupSparseMatrix(M:Int, N:Int)=DupSparseMatrix{self.M==M, self.N==N};
public  type DupSparseMatrix(C:Matrix)=DupSparseMatrix{self==C};

/**
 * Implementation of duplicated sparse matrix. All duplicated 
 * sparse matrices are stored in DistArray.  Distribution of dense matrices is unique, one
 * duplicated copy is mapped to one place.
 * 
 */
public class DupSparseMatrix extends Matrix {

    //===================================================
	
	/**
	 * Data duplication
	 */
	public val dist:Dist(1);
    public val dupMs:DistArray[SparseCSC](1);
	public var comm:CommHandle;

	//----------- Profiling -----------
    public var calcTime:Long=0;
    public var commTime:Long=0;

	//==================================================================
	//==================================================================
	/**
	 * Construct duplicated sparse matrix. 
	 *
	 * @param dms     distribute array of sparse matrices
	 */
	public def this(dms:DistArray[SparseCSC](1)) {
		super(dms(here.id()).M, dms(here.id()).N);
		//
		dist = dms.dist;
		//count = dms.region.size();
		dupMs = dms;
		comm = new CommHandle();
	}

	//-----------------------------------------------------------------
	// No bcast is performed
	/**
	 * Create duplicated sparse matrix instance using a sparse matrix
	 *
	 * @param mat     sparse matrix (in CSC format)
	 */
	public static def make(mat:SparseCSC) : DupSparseMatrix(mat.M,mat.N) {
		val dist = Dist.makeUnique();
		val m = mat.M;
		val n = mat.N;
		val dms  = DistArray.make[SparseCSC](dist);
		val root = here.id();
		val nzc  = mat.getStorageSize();

		finish ateach (val [p]:Point in dms.dist) {
			val mypid = here.id();
			if (mypid != root) 
				dms(mypid) = SparseCSC.make(m, n, nzc);
		}
		dms(root) = mat;
		val dm  = new DupSparseMatrix(dms) as DupSparseMatrix(mat.M, mat.N);
		dm.sync(); // sync mat 
		return dm;
	}

	//===
	// Only memory space is allocated on all places
	/**
	 * Create duplicated dense matrix for specified number of nonzero elements.
	 *
	 * @param m     number of rows
	 * @param n     number of columns
	 * @param nzcnt     number of nonzero elements.
	 */
	public static def make(m:Int, n:Int, nzcnt:Int): DupSparseMatrix(m,n) {
		val dist = Dist.makeUnique();
		val dms  = DistArray.make[SparseCSC](dist);
		finish ateach (val [p]:Point in dms.dist) {
			val mypid = here.id();
			dms(mypid) = SparseCSC.make(m, n, nzcnt);
		}
		val dm  = new DupSparseMatrix(dms) as DupSparseMatrix(m,n);
		return dm;
	}

	/**
	 * Create duplicated dense matrix for specified sparsity.
	 *
	 * @param m     number of rows
	 * @param n     number of columns
	 * @param nzd     nonzero density or sparsity.
	 */
	public static def make(m:Int, n:Int, nzd:Double): DupSparseMatrix(m,n) {
		return make(m, n, (nzd * m * n) as Int);
	}

	/**
	 * For testing purpose.
	 *
	 * <p> Create duplicated sparse matrix instance and initialized matrix data with
	 * random values
	 *
	 * @param m     number of rows
	 * @param n     number of columns
	 * @param nzcnt     number of nonzero elements
	 */
	public static def makeRand(m:Int, n:Int, nzcnt:Int): DupSparseMatrix(m,n) {
		val ddm = make(m, n, nzcnt);
		ddm.initRandom();
		return ddm;
	}

	/**
	 * For testing purpose.
	 *
	 * <p> Create duplicated sparse matrix instance and initialized matrix data with
	 * random values
	 *
	 * @param m     number of rows
	 * @param n     number of columns
	 * @param nzd     sparsity
	 */
	public static def makeRand(m:Int, n:Int, nzd:Double): DupSparseMatrix(m,n) {
		val nzcnt:Int = (nzd * m * n) as Int;
		return makeRand(m, n, nzcnt);
	}

	/**
	 * For testing purpose.
	 *
	 * <p> Initial duplicated sparse matrix and assign each nonzero element
	 * by a constant.						   
	 * 
	 * @param ival     initial constant value.
	 */
	public def init(ival:Double) : void {
		local().init(ival);
		sync();
	}

	/**
	 * For testing purpose.
	 *
	 * <p> Initialize duplicated sparse matrix with random values.
	 *
	 * @param nzd     the sparsity used int initialzation.
	 */
	public def initRandom(nzd:Double) : void {
		local().initRandom(nzd);
		sync();
	}

	/**
	 * For testing purpose.
	 *
	 * <p> Initialize duplicated sparse matrix with random values.
	 */
	public def initRandom() : void {
		local().initRandom();
		sync();
	}
	//================================================================
	// Data copy and reset
	//================================================================	

	/**
	 * Allocate memory space with same storage for duplicated sparse matrix(m,n)
	 */
	public def alloc(m:Int, n:Int)  = make(m, n, local().getStorageSize());

	/**
	 * Make a copy of all duplicated sparse matrix in all places.
	 * If the source duplicated matrix is not synchronized in all places,
	 * the clone copy is also not synchronized.
	 */	
	public def clone() : DupSparseMatrix(this.M, this.N) {
		val ds  = DistArray.make[SparseCSC](dupMs.dist);
		finish ateach (val [p]:Point in ds) {
			val mypid = here.id();
			ds(mypid) = this.dupMs(mypid).clone();
		}
		val dsm = new DupSparseMatrix(ds) as DupSparseMatrix(M,N);
		return dsm;
	}


	/**
	 * Copy element values to the target matrix in same dimension.
	 *
	 * @param dst      the target dense matrix.		
	 */
	public def copyTo(dm:DenseMatrix(M,N)) {
		local().copyTo(dm);
	}


	/**
	 * Copy element values to the target matrix in all places.
	 *
	 * @param dst      the target dense matrix.		
	 */
	public def copyTo(dm:DupDenseMatrix(M,N)) {
		finish ateach (val [p]:Point in dupMs) {
			local().copyTo(dm.local());
		}
	}

	//================================================================
	// Data access
	//================================================================
	//public def apply(x:Int, y:Int) = this.dupMs(here.id()).apply(x, y);
	/**
	 * Access data at (x, y)
	 */
    public operator this(x:Int, y:Int):Double=local()(x, y);

	/**
	 * Assign v to (x, y) in the copy at here. Other copies are not
	 * modified.
	 */
	public operator this(x:Int,y:Int) = (v:Double):void {
		//this.dupMs(here.id()).d(y*this.M+x) = v;
		local()(x, y) = v;
	}


	/**
	 * Return the matrix copy at here.
	 */
	public def getMatrix():SparseCSC = this.dupMs(here.id()); 
	
	/**
	 * Return the local copy of sparse matrix at here with dimension check.
	 */
	public def local():SparseCSC(M,N) = this.dupMs(here.id()) as SparseCSC(M,N);

	/**
	 * Return the copy of sparse matrix at place p. Must be executed at
	 * place p.
	 */
	//public def getMatrix(p:Int):SparseCSC(M,N) = this.dupMs(p) as SparseCSC(M,N) ;

	/**
	 * Reset matrix and all copies.
	 */
	public def reset():void {
		finish ateach (val [p]:Point in this.dupMs.dist) {
			local().reset();
		}
		calcTime=0;
		commTime=0;
	}
	/**
	 * Check Matrix A is duplicated and has the same dist or not
	 */
	public def likeMe(A:Matrix):Boolean =
	    (A instanceof DupSparseMatrix &&
		 (A as DupSparseMatrix).dupMs.dist.equals(this.dupMs.dist));
	
	//================================================================
	//================================================================

	/**
	 * Broadcast the copy of sparse matrix at here to all other copies.
	 */
	public def sync() : void {
		/* Timing */ val st:Long = Timer.milliTime();
		comm.bcast(dupMs);
		/* Timing */ commTime += Timer.milliTime() - st;
	}

	/**
	 * Currently not supported.
	 */
	public def T() :DupDenseMatrix(N,M) {
	    throw new UnsupportedOperationException();
	}

	//====================================================================
	// Cellwise operation
	//====================================================================

	/**
	 * Scaling method. All copies are updated concurrently
	 */
 	public def scale(alpha:Double) {
		/* Timing */ val st= Timer.milliTime();
		finish ateach(val [p] :Point in this.dupMs) {
			local().scale(alpha);
		}
		/* Timing */ calcTime += Timer.milliTime() - st;
		return this;
    }

	//-------------------------------
	// Cellwise addition
	//-------------------------------

	/**
	 * Not support. Cellwise subtraction.
	 */
	public def cellAdd(A:Matrix(M,N)) {
		Debug.exit("Not supported for using sparse matrix to store result");
		return this;
	}

	/**
	 * dst += this
	 */
    public def cellAddTo(dst:DenseMatrix(M,N)) = local().cellAddTo(dst);

	/**
	 * dst += this for all copies
	 */
    public def cellAddTo(dst:DupDenseMatrix(M,N)) {
		finish ateach(val [p] :Point in this.dupMs) {
			local().cellAddTo(dst.local());
		}
		return dst;
	}

	//-----------------------------
	// Cellwise subtraction
	//-----------------------------

	/**
	 * Not support. Cellwise subtraction.
	 */
	public def cellSub(A:Matrix(M,N)) {
		Debug.exit("Not supported for using sparse matrix to store result");
		return this;
	}

	/**
	 * dst = dst - this
	 */
	public def cellSubFrom(dst:DenseMatrix(M,N)) = local().cellSubFrom(dst);

	/**
	 * dst = dst - this for all copies
	 */
	public def cellSubFrom(dst:DupDenseMatrix(M,N)) {
		finish ateach(val [p] :Point in this.dupMs) {
			local().cellSubFrom(dst.local());
		}
		return dst;		
	}

	//-------------------------------
	// Cellwise multiplication
	//-------------------------------
	/**
	 * Not support. Concurrently perform cellwise addition on all copies.
	 */
	public def cellMult(A:Matrix(M,N))  {
		Debug.exit("Not supported for using sparse matrix to store result");
		return this;
	}

	/**
	 * Compute dst = dst &#42 this;
	 */
	public def cellMultTo(dst:DenseMatrix(M,N)) = local().cellMultTo(dst);


	/**
	 * Compute dst = dist &#42 this for all copies
	 */
	public def cellMultTo(dst:DupDenseMatrix(M,N)) {
		finish ateach(val [p] :Point in this.dupMs) {
			local().cellMultTo(dst.local());
		}
		return dst;	
	} 

	//---------------------------------
	// Cellwise division
	//---------------------------------
	/**
	 * Not support. Concurrently perform cellwise subtraction on all copies
	 */	
	public def cellDiv(A:Matrix(M,N)) {
		Debug.exit("Not supported for using sparse matrix to store result");
		return this;
	}

	/**
	 * dst = this / dst
	 */
	public def cellDivBy(dst:DenseMatrix(M,N)) = local().cellDivBy(dst);


	/**
	 * dst = this / dst for all copies
	 */
	public def cellDivBy(dst:DupDenseMatrix(M,N)) {
		finish ateach(val [p] :Point in this.dupMs) {
			local().cellDivBy(dst.local());
		}
		return dst;		
	}

	//====================================================================
	// Operator overload
	//====================================================================
	/**
	 * Perform cell-wise addition, return this + that in a new dup dense matrix. 
	 */
	public operator this + (that:DupSparseMatrix(M,N)):DupDenseMatrix(M,N) {
		val ddm = DupDenseMatrix.make(M,N);
		this.copyTo(ddm);
	    that.cellAddTo(ddm);
	    return ddm;
	}
	
	/**
	 * Perform cell-wise subtraction, return this - that in a new dup dense format
	 */
	public operator this - (that:DupSparseMatrix(M,N)):DupDenseMatrix(M,N) {
		val ddm = DupDenseMatrix.make(M,N);
	    that.copyTo(ddm);
		this.cellSubFrom(ddm);
	    return ddm;
	}
    /**
     * Perform cell-wise multiplication, return this * that in dup dense format
     */
	public operator this * (that:DupSparseMatrix(M,N)):DupDenseMatrix(M,N) {
		val ddm = DupDenseMatrix.make(M,N);
		this.copyTo(ddm);
	    that.cellMultTo(ddm);
	    return ddm;
	}

	/**
	 * Perform cell-wise division, return this / that in a new dup dense matrix
	 */
	public operator this / (that:DupSparseMatrix(M,N)):DupDenseMatrix(M,N) {
		val ddm = DupDenseMatrix.make(M,N);
		that.copyTo(ddm);
	    this.cellDivBy(ddm);
	    return ddm;
	}

	//====================================================================
	// Multiplication operations 
	//====================================================================

	/**
	 * Multiplication method by using X10 driver. All copies are updated.
	 * this = A * B if plus is false, else this += A * B
	 */
	public def mult(
			A:Matrix(this.M), 
			B:Matrix(A.N,this.N), 
			plus:Boolean):DupSparseMatrix(this) {

		Debug.exit("Not support using sparse matrix to store result");
		return this;
	}


	//---------------------------------------------------
	public def transMult(
			A:Matrix{self.N==this.M}, 
			B:Matrix(A.M,this.N), 
			plus:Boolean):DupSparseMatrix(this) {
		
		Debug.exit("Not support using sparse matrix to store result");
		return this;
	}


	//-----------------------------------------------------------------

	/**
	 * this = A * B^T
	 */
	public def multTrans(
			A:Matrix(this.M), 
			B:Matrix(this.N, A.N), 
			plus:Boolean):DupSparseMatrix(this)  {

		Debug.exit("Not support using sparse matrix to store result");
		return this;
	}

	//====================================================================
	// Util
	//====================================================================

 	//------------
	public def getCommTime():Long = this.commTime;
	public def getCalcTime():Long = this.calcTime;
	//------------

	// Check integrity 
	public def syncCheck():Boolean {
		val m = local();
		for (var p:Int=0; p<Place.MAX_PLACES; p++) {

			val pid = p;
			val dm = at (dupMs.dist(pid)) local();
			if (!m.equals(dm)) {
				Console.OUT.println("Integrity check found differences between the copy at here and copy at "+pid);
				Console.OUT.flush();
				return false;
			}
		}
		return true;
	}

	//==================================================================
	public def toString() :String {
		var output:String = "---Duplicated Dense Matrix size:["+M+"x"+N+"]---\n";
		output += dupMs(here.id()).toString();
		output += "--------------------------------------------------\n";
		return output;
	}
	//
	public def print() {
		print("");
	}

	public def print(msg:String) :void {
		Console.OUT.print(msg);
		Console.OUT.print(this.toString());
		Console.OUT.flush();
	}
	//
	public def debugPrint() {
		debugPrint("");
	}

	public def debugPrint(msg:String) : void {
		if (Debug.disable) return;
		val dbstr:String = msg+ this.toString();
		Debug.println(dbstr);
		Debug.flush();
	}
	
	//
	public def allToString() : String {
		var output:String = "Duplicated Dense Matrix size:["+M+"x"+N+"]\n";
		for (var p:Int=0; p<Place.MAX_PLACES; p++) { 
			val pid = p;
			val mstr = at (dupMs.dist(pid)) dupMs(pid).toString();
			output += "Duplication at place " + pid + "\n"+mstr;
		}
		return output;
	}

	public def printAll(msg:String) :void {
		Console.OUT.print(msg+allToString());
		Console.OUT.flush();
	}
	public def printAll() {
		printAll("");
	}
		
}

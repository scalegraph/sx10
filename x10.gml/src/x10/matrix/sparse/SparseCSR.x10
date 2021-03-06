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

package x10.matrix.sparse;

import x10.io.Console;
import x10.util.Pair;
import x10.util.StringBuilder;

import x10.matrix.Debug;
import x10.matrix.Matrix;
import x10.matrix.MathTool;
import x10.matrix.DenseMatrix;

public type SparseCSR(M:Int)=SparseCSR{self.M==M};
public type SparseCSR(M:Int,N:Int)=SparseCSR{self.M==M, self.N==N};
public type SparseCSR(C:SparseCSR)=SparseCSR{self==C};

/**
 * User be adviced. SparseCSR is outdated, and is not fully supported as SparseCSC.
 * <p>
 * This class defines sparse matrix compressed in row order, or CSR format.
 * The class is used as temporary storage when SparseCSC needs to be
 * transposed in matrix multiply. 
 * 
 */
public class SparseCSR extends Matrix {
 
	//======================================================================
	public val crdata:Compress2D;
	//
	
	// This temporary memory space is used for type conversion
	private var tmprow:Array[Double](1);
	private var tmpcol:Array[Double](1);
	//======================================================================
	// Used for serialization index value, reset or build 
	private var copyRowOff:Int;
	private var copyRowCnt:Int;
	private var copyDataCnt:Int;

	//=================================================================
	/**
	 * Construct a SparseCSR matrix instance using specified Compress2D.
	 *.
	 * @param m     Number of rows or compressed lines
	 * @param n     Number of columns
	 * @param cd     Compressed data
	 */
	public def this(m:Int, n:Int, cd:Compress2D):SparseCSR(m,n) {
		super(m, n);
		crdata = cd;
		Debug.assure(cd.size() == m);
		tmpcol = new Array[Double](0);
		tmprow = new Array[Double](0);
	}

	/**
	 * Construct a CSR instance using specified compressed array data storage.
	 *
	 * @param m     Number of rows in the CSR sparse matrix
	 * @param n     Number of columns in the CSR sparse matrix
	 * @param ca     The data storage of compressed array.
	 */
	public def this(m:Int, n:Int, ca:CompressArray):SparseCSR(m,n) {
		super(m, n);
		crdata = Compress2D.make(m, ca);
		//sparsity = 1.0*countNonZero() /m/n;

		tmprow = new Array[Double](0);
		tmpcol = new Array[Double](0);
	}

	//================================================================
	/**
	 * Create a SparseCSR matrix instance with specified dimension and 
	 * storage for specified number of nonzero elements ,
	 */
	public static def make(m:Int, n:Int, nzcnt:Int) {
		val ca = new CompressArray(nzcnt);
		val sp = new SparseCSR(m, n, ca); 
		return sp;
	}

	/**
	 * Create a SparseCSR instance with specified dimension and storage
	 * for specified sparsity.
	 */	
	public static def make(m:Int, n:Int, nzd:Double) {
		val cnt = SparseCSC.compAllocSize(m, n, nzd);
	    return SparseCSR.make(m, n, cnt);
	}

	/**
	 * Create a SparseCSR matrix instance with memory storage for all
	 * elements in the matrix.
	 */
	public static def make(m:Int, n:Int) = SparseCSR.make(m, n, m*n); 

	   
	//================================================================
	//----------------------------------------------------------------

	/**
	 * Create a m x n SparseCSR matrix based on CSR data format input
	 * ia, ja, and av.
	 * @param ia     integer array, the sizes of nonzero of each row
	 * @param ja     integer array, the surface index array of nonzero in rows
	 * @param av     double array, the actual matrix element data corresponding
	 *              to the surface index at the same position in ja.
	 */
	public static def make(m:Int, n:Int,	
						   ia:Array[Int](1),
						   ja:Array[Int](1),
						   av:Array[Double](1){av.size==ja.size}
						   ):SparseCSR(m,n){
		val crd = Compress2D.make(ia, ja, av);
		return new SparseCSR(m, n, crd);
	}
	//---------------------------------------------------------------

	/**
	 * For testing purpose,
	 *
	 * @param v      Initial value for all elements
	 * @param sp     Nonzero sparsity
	 * @see SparseCSC.init(v:Double, sp:Double)
	 */
	public def init(v:Double, sp:Double):SparseCSR(this) {
		val cnt = crdata.initConst(N, v, sp);
		//sparsity = 1.0 * cnt/M/N;
		return this;
	} 
	
	public def init(v:Double):SparseCSR(this) {
		val nzd = 1.0 * getStorageSize()/M/N;
		init(v, nzd);
		return this;
	}

	/**
	 * For testing purpose,
	 * 
	 * @param sp     Nonzero sparsity
	 * @see SparseCSC.initRandom(sp:Double)
	 */
	public def initRandom(sp:Double) : SparseCSR(this) {
		val cnt = crdata.initRandomFast(N, sp);
		//sparsity = 1.0 * cnt/M/N;
		return this;
	}

	public def initRandom(): SparseCSR(this) {
		val nzd = 1.0 * getStorageSize()/M/N;
		initRandom(nzd);
		return this;
	}
	
	public def initRandom(lo:Int, up:Int, nzp:Double): SparseCSR(this) {
		crdata.initRandomFast(M, nzp, lo, up);
		return this;
	}
	
	public def initRandom(lo:Int, up:Int): SparseCSR(this) {
		val nzd = 1.0 * getStorageSize()/M/N;
		crdata.initRandomFast(M, nzd, lo, up);
		return this;
	}	
	/**
	 * Initialize with given function with range [0..M, 0..N]
	 */
	public def init(f:(Int, Int)=>Double): SparseCSR(this) {
		
		var offset:Int=0;
		val ca = getStorage();
		for (var r:Int=0; r<M; r++) {
			val crow = crdata.cLine(r);
			crow.offset = offset;
			for (var c:Int=0; c<N; c++) {
				val nzval:Double = f(r, c);
				if (! MathTool.isZero(nzval)) {
					ca.index(offset)=c;
					ca.value(offset)=nzval;
					offset++;
				}
			}
			crow.length = offset - crow.offset;
		}
		ca.count = offset;
		return this;
	}
	
	/**
	 * Initial sparse matrix using function and row and column offsets.
	 */
	public def init(rowoff:Int, coloff:Int, f:(Int, Int)=>Double): SparseCSR(this) {
		
		var offset:Int=0;
		val ca = getStorage();
		for (var r:Int=0; r<M; r++) {
			val crow = crdata.cLine(r);
			crow.offset = offset;
			for (var c:Int=0; c<N&&offset<ca.index.size; c++) {
				val nzval:Double = f(r+rowoff, c+coloff);
				if (! MathTool.isZero(nzval)) {
					ca.index(offset)=c;
					ca.value(offset)=nzval;
					offset++;
				}
			}
			crow.length = offset - crow.offset;
		}
		ca.count = offset;
		return this;
	}	
	
	/**
	 * For testing purpose.
	 *
	 */
	public static def makeRand(m:Int, n:Int, nzd:Double): SparseCSR(m, n) {
		val csr = SparseCSR.make(m, n, nzd);
		csr.initRandom(nzd);
		return csr;
	}

	//--------------------------------------------------------------
	// Compress 2D stored in column-wise way in column-compressed 2D
	//--------------------------------------------------------------

	/**
	 * Allocate the same compressed array storage for a new SparseCSR.
	 * 
	 * @param  m      number of rows
	 * @param  n      number of columns
	 */
	public def alloc(m:Int, n:Int):SparseCSR(m,n) {
		// Maximum memory allocation
		val nz = countNonZero() as Int;
		val az = getStorageSize();
		val sz = Math.max(nz, az);
		val ca = new CompressArray(sz);
		return new SparseCSR(m, n, ca);
	}
	
	/**
	 * Make a copy of myself.
	 */
	public def clone():SparseCSR(M,N){
		val cd = crdata.clone();
		//Debug.flushln("Clone matrix "+M+" "+N+" "+crdata.size()+" "+cd.size());
		return new SparseCSR(this.M, this.N, cd);
	}

	/**
	 * Reset all data to 0 and nonzero count to 0
	 */
	public def reset() {
		crdata.reset();
	}

    //========================================================================
	// Data access
    //========================================================================
	/**
	 * Return the compressed value array
	 */
	public def getValue() = crdata.getValue();

	/**
	 * Return the compressed surface index array
	 */
	public def getIndex() = crdata.getIndex();

	/**
	 * Return the compressed data array
	 */	
	public def getStorage() = crdata.getStorage(); // return compress array

	/**
	 * Return the matrix element value at the r-th row and c-th column.
	 */
	public operator this(r:Int, c:Int) = crdata(r, c);
	public operator this(a:Int) = crdata(a%M, a/M);
	
    //========================================================================
	/**
	 * Set v at r-th row and c-th column. 
	 * The data entry (r,c) must exist in the compressed array, otherwise
	 * the operation fails. 
	 *
	 * Modifying sparse matrix after creation is not advised
	 */
	public operator this(r:Int, c:Int) = (v:Double):Double {
	    crdata(c)=Pair[Int,Double](r,v);
	    return v;
	}

	public def setRow(r:Int, cl:Compress1D) {
			crdata.setLine(r, cl);
	}

	public def compressAt(r:Int, off:Int, d:Array[Double](1)) =
		crdata.cLine(r).compressAt(off, d);

    //========================================================================

	public def getRow(r:Int) = crdata.getLine(r);
	public def getCol(c:Int) : Compress1D {
		val tc = getTempCol();
		extractCol(c, tc);
		return Compress1D.compress(tc);
	}

	//=====================================================================
	// Remote copy
	//=====================================================================

	/**
	 * Setup before the remote copy of rows at the source sparse matrix.
	 *
	 * @param rowoff     the starting row in the source sparse matrix
	 * @param rowcnt     number of rows to copy in the source sparse matrix
	 *
	 */
	public def setupRemoteCopyAtSource(rowoff:Int, rowcnt:Int):void {
		copyRowOff = rowoff;
		copyRowCnt = rowcnt;
		copyDataCnt = crdata.serializeIndex(this.N, rowoff, rowcnt);
	}

	/**
	 * Setup the remote copy of rows at the destination place
	 *
	 * @param rowoff      the starting row in the target matrix
	 * @param rowcnt      number of rows to receive
	 * @param datcnt      number of elements (index-value pairs) 
	 */
	public def setupRemoteCopyAtDest(rowoff:Int, rowcnt:Int, datcnt:Int) : void {
		copyRowOff = rowoff;
		copyRowCnt = rowcnt;
		copyDataCnt= datcnt;
	}

	/**
	 *  Finish the remote copy of sparse matrix at source place 
	 */
	public def finishRemoteCopyAtSource(): void {
		crdata.resetIndex(this.N, copyRowOff);
	}

	//--------------
	/**
	 * Serilaize index values in its storage by marking the row start index, so that
	 * after index array is copied to destination place, the destination place
	 * can rebuild the compressed array's offset and length.
	 *
	 * @param rowoff     Offset of row
	 * @param rowcnt     Number of rows
	 * @return number of element indexes changed
	 *
	 */
	public def serializeIndex(rowoff:Int, rowcnt:Int) :void {
		copyDataCnt = crdata.serializeIndex(this.N, rowoff, rowcnt);
	}

	/**
	 * Reverse serialization.
	 */
	public def resetIndex(rowoff:Int) : void {
		crdata.resetIndex(this.N, rowoff);
	}

	/**
	 * Set the offset and length in index array for each compress line, after the compress data
	 * in storage is copied from remote place.
	 *
	 *
	 * @param rowoff     Offset of row
	 * @param rowcnt     Number of rows
	 * @param datcnt     Number of elements in storage copied from remote place
	 * @return     Number of elements unclaimed
	 */
	public def buildIndex(rowoff:Int, rowcnt:Int, datcnt:Int):Int =
		crdata.buildIndex(this.N, rowoff, rowcnt, datcnt); 

   
	//=====================================================================
	// Access temporary space
	//=====================================================================
	public def getTempCol() : Array[Double](1) {
		if (tmpcol.size == 0)
			tmpcol = new Array[Double](this.M, 0.0);
		else {
			for (var i:Int=0; i<this.M; i++) tmpcol(i)=0.0;
		}
		return tmpcol;
	}
	//
	public def getTempRow() : Array[Double](1) {
		if (tmprow.size == 0) 
			tmprow = new Array[Double](this.N, 0.0);
		else {
			// reset the temp array
			for (var i:Int=0; i<this.N; i++) tmprow(i) = 0.0;
		}
		return tmprow;
	}

	//=====================================================================
	// Copy to a continuous memory space
	//=====================================================================
	/**
	 * Copy specified range of rows from source to target sparse in CSR
	 *
	 * @param src           The source sparse matrix
	 * @param srcRowOffset     The starting columns for copy at source
	 * @param dst           The target sparse matrix
	 * @param dstRowOffset     The starting columns in the target matrix
	 * @param colcnt        The number of columns for copy
	 * @return     Number of nonzero elements copied
	 */
	public static def copyRows(src:SparseCSR, srcRowOffset:Int,
							   dst:SparseCSR, dstRowOffset:Int, rowcnt:Int) :Int =
		Compress2D.copy(src.crdata, srcRowOffset, dst.crdata, dstRowOffset, rowcnt);

	/**
	 * Copy all rows from source to target. If target has more rows,
	 * they are reset to 0 length of nonzero elements.
	 *
	 */
	public static def copy(src:SparseCSR, dst:SparseCSR) : Int =
		Compress2D.copy(src.crdata, dst.crdata);
						

	/**
	 * Copy rows from source to target sparse matrix in CSR. The target row offset
	 * is always 0 in sparse matrix.
	 *
	 * @param src              The source sparse matrix in CSR
	 * @param srcColOffset     The starting column in source matrix
	 * @param dst              The target matrix in sparse CSR
	 * @param dstColOffset     The starting column in target matrix, must be 0
	 * @param colcnt           The number of columns to copy
	 * @return     Number of nonzero elements copied.
	 */
	public static def copyCols(src:SparseCSR, srcColOffset:Int, 
							   dst:SparseCSR, dstColOffset:Int{self==0}, colcnt:Int) : Int =
		Compress2D.copySection(src.crdata, srcColOffset, dst.crdata, colcnt);

	//=====================================================================
	// Extract data from columns and put the result in array or dense matrix
	//=====================================================================

	// Extract data in the row to array. The target array needs to reset first!!!
	public def extractRow(r:Int, ln:Array[Double](1)) {
		crdata.getLine(r).extract(ln);
	}

	public def extractCol(c:Int, cl:Array[Double](1)) {
		for (var r:Int=0; r<this.M; r++) cl(r) = this(r, c);
	}

	//----------------------------
	// Using tmp storage space to hold data
	/**
	 * Extracting row to temp array. 
	 */
	public def extractRow(r:Int):Array[Double](1) {
		val tr = getTempRow();
		extractRow(r, tr);
		return tr;
	}
	public def extractCol(c:Int):Array[Double](1) {
		val tc = getTempCol();
		extractCol(c, tc);
		return tc;
	}
	
    //========================================================================
	// Extract data to dense matrix
    //========================================================================

	/**
	 * Expand multiple compressed rows into the dense matrix.
	 * The target dense matrix is reset.
	 * @param start_row     the starting row
	 * @param num_row       number of rows to extract
	 * @param dm            the target dense matrix to hold the expanded data
	 */
	public def extractRows(start_row:Int, 
						   num_row:Int, 
						   dm:DenseMatrix{self.M==num_row,self.N==this.N}
						   ) : void {
		Debug.assure(num_row<=dm.M&&this.N<=dm.N);
		//
		for (var i:Int=0; i<dm.d.size; i++) dm.d(i) = 0.0;
		for (var r:Int=start_row; r<start_row+num_row; r++) {
			val rowln = getRow(r);
			for (var cidx:Int=0; cidx<rowln.length; cidx++) {
			    dm(r, rowln.getIndex(cidx))=rowln.getValue(cidx);
			}
		}
	}

	/**
	 * Expand multiple compressed columns into the dense matrix
	 * The target dense matrix is reset.
	 * @param start_col     the starting column
	 * @param num_col       number of columns to extract
	 * @param dm            the target dense matrix to hold the expanded data
	 */
	public def extractCols(start_col:Int, 
						   num_col:Int, 
						   dm:DenseMatrix{self.M==this.M,self.N==num_col}
						   ) : void {
		Debug.assure(this.M<=dm.M&&num_col<=dm.N);
		var colst:Int = 0;//offset
		for (var x:Int=0; x<this.N; x++, colst+=dm.M) {
			val cl = crdata.getLine(x);
			cl.extract(start_col, num_col, colst, dm.d);
		}
	}

	public def extractCols(start_col:Int, 
						   num_col:Int
						   ): DenseMatrix(this.M,num_col) {
		val dm = new DenseMatrix(this.M, num_col);
		extractCols(start_col, num_col, dm);
		return dm;
	}

    //========================================================================
	/**
	 * Compute nonzero sparsity in storage
	 */
	public def compSparsity():Double {
		/* !!!!!!!!!!!!!!! */
		/* M * N could be larger than INT_MAX (2147483648, or 2*10^10 */
		/* in currnt X10c++, the maximum size for array */
		val nz:Double = crdata.countNonZero() as Double;
		return nz/(this.M*this.N as Double);
	}

	/**
	 * Get number of nonzero elements in specified row
	 */
	public def getColNonZeroCount(row:Int) = crdata.cLine(row).length;

	//---
	/**
	 * Get the offset in CompressArray for the specified row's starting offset
	 */
	public def getNonZeroOffset(row:Int) = crdata.cLine(row).offset;

	/**
	 * Get the number of nonzero in the specified range of rows
	 *
	 * @param rowoff     Offset row
	 * @param rowcnt     Number of rows
	 * @return     the number of elements in compressed array
	 */
	public def countNonZero(rowoff:Int, rowcnt:Int):Int =
		crdata.countNonZero(rowoff, rowcnt);

	public def countNonZero():Int = countNonZero(0, M);

	/**
	 * Get storage size.
	 */
	public def getStorageSize() = getStorage().storageSize();

    //========================================================================
	// Format conversion: to SCR and dense matrix
	//========================================================================
	/**
	 * Convert to a new SparseCSC. This operation is expensive.
	 */
	public def toCSC():SparseCSC(M,N) {
		val sm = SparseCSC.make(this.M, this.N, countNonZero() as Int);
		toCSC(sm);
		return sm;
	}

	/**
	 * Convert to SparseCSC using provided memory space
	 * This operation is expensive
	 */	
	public def toCSC(sm:SparseCSC(M,N)):void {
		var off:Int = 0;
		val tc = getTempCol();
		for (var c:Int=0; c<this.N; c++) {
			extractCol(c, tc);
			off += sm.compressAt(c, off, tc);
		}
	}

	/**
	 * Copy data to dense matrix
	 */
	public def copyTo(dm:DenseMatrix(M,N)): void {
		extractRows(0, this.M, dm);
	}
	
	public static def copyTo(sp:SparseCSR, dm:DenseMatrix, roff:Int, coff:Int): void {
		Debug.exit("Not implemented yet");
	}
	
	public def copyTo(that:SparseCSR(M,N)) = copy(this, that);
	
	public def copyTo(that:Matrix(M,N)):void {
		if (that instanceof DenseMatrix)
			copyTo(that as DenseMatrix);
		else if (that instanceof SparseCSR)
			copyTo(that as SparseCSR);
		else
			Debug.exit("CopyTo: target matrix type not supported");	
	}

	/**
	 * Convert to Dense format, allocating new space for the data.
	 */
	public def toDense():DenseMatrix(M,N) {
		val dm = DenseMatrix.make(M,N);
		toDense(dm);
		return dm;
	}

	/**
	 * Convert to the provided dense matrix
	 */
	public def toDense(dm:DenseMatrix(M,N)) {
		extractRows(0, this.M, dm);
	}

	//===================================================================
	// Transpose methods
	//===================================================================

	/**
	 * Transpose matrix into CSC format. No additional memory allocation used.
	 */
	public def TtoCSC():SparseCSC{self.M==this.N,self.N==this.M} {
		return new SparseCSC(N, M, crdata);
	}

	/**
	 * Transpose matrix. Expensive.
	 * This sparse matrix is converted to CSC using
	 * the provided storage of CSR. 
	 */	
	public def T(tm:SparseCSR(N,M)):void {
		Debug.assure(this.getStorageSize() <= tm.getStorageSize());
		val csc = new SparseCSC(M, N, tm.crdata);
		toCSC(csc);
	}

	//===================================================================
	// Cell-wise methods
	//===================================================================
    /**
     * Multiply in place each element of this matrix by alpha.
     */
	public def scale(alpha:Double) {
		val ca = getStorage();
		for (var row:Int=0; row<M; row++) {
			val rln = getRow(row);
			for (var e:Int=0; e<rln.length; e++)
				ca.value(rln.offset+e) *= alpha;
		}
		return this;
	}

    /**
     * Multiply in place each element of this matrix by alpha.
     */
	public def scale(alpha:Int) = scale(alpha as Double);


	//----------------------------

    /**
     * Return this += x; not supported
     */
    public def cellAdd(x:Matrix(M,N)):SparseCSR(this)  {
    	throw new UnsupportedOperationException("Cell-wise addition does not support using SparseCSR to store result");
    }
    
    public def cellAdd(d:Double):SparseCSR(this) {
    	throw new UnsupportedOperationException("Cell-wise addition does not support using SparseCSC as output matrix");
    }   
    
	/**
	 * dst += this
	 */
	protected def cellAddTo(dst:DenseMatrix(M,N)) {
		SparseAddToDense.comp(this, dst);
		return dst;
	}
	//-----------------------------
    /**
     * Return this = this - x, not supported
     */
    public def cellSub(x:Matrix(M,N)) {
		Debug.exit("Cell-wise subtraction does not support using SparseCSR to store result");
		return this;
    }
	/**
	 * x = x - this
	 */
	protected def cellSubFrom(x:DenseMatrix(M,N)) {
		SparseSubToDense.comp(x, this);
		return x;
	}
	
	public def cellSubFrom(dv:Double):SparseCSR(this) {
		throw new UnsupportedOperationException("Cell-wise multiplication does not support using SparseCSR to store result");
	}
	
	//-----------------------
    /**
     * Return this *= x, not supported
     */
    public def cellMult(x:Matrix(M,N)):SparseCSR(this) {
    	throw new UnsupportedOperationException("Cell-wise multiplication does not support using SparseCSR to store result");
    }
	/**
	 * x = this * x
	 */
	protected def cellMultTo(dst:DenseMatrix(M,N)) {
		SparseAddToDense.comp(this, dst);
		return dst;
	}
	//----------------
    /**
     * Return this = this / x, not supported
     */
    public def cellDiv(x:Matrix(M,N)):SparseCSR(this)  {
    	throw new UnsupportedOperationException("Cell-wise division does not support using SparseCSR to store result");
    }

	/**
	 * x = this / x
	 */
	protected def cellDivBy(dst:DenseMatrix(M,N)) {
		SparseDivToDense.comp(this, dst);
		return dst;
	}


	//--------------------------------
//     public def cellAddUpdate(A:Matrix(M,N), B:Matrix(M,N), addIn:Boolean):void {
//     	Debug.exit("Cell-wise addition is not supported by sparse matrix");
//     }
	//-------------------------------
    /**
     * Scaling operation return this &#42 double in dense format
     */
    public operator this * (dblv:Double):SparseCSR(M,N) {
        val x = clone();
        x.scale(dblv);
        return x;
    }
    /**
     * Scaling operation return this &#42 integer in dense format
     */
    public operator this * (intv:Int):SparseCSR(M,N) {
        val x = clone();
        x.scale(intv as Double);
        return x;
    }
	public operator (dblv:Double) * this = this * dblv;
	public operator (intv:Int) * this = this * intv;

    //========================================================================
	// Add methods, return dense format result
    //========================================================================
	/**
	 *  Return this + that in a new dense 
	 */
	def add(that:SparseCSR(M,N)) : DenseMatrix(M,N) {
		val dm = that.toDense();
		SparseAddToDense.comp(this, dm);
		return dm;
	}

	/**
	 *  Return this + that in a new dense 
	 */
	def add(that:SparseCSC(M,N)) : DenseMatrix(M,N) {
		val dm = that.toDense();
		SparseAddToDense.comp(this, dm);
		return dm;
	}

	/**
	 *  Return this + that in a new dense 
	 */
	def add(that:DenseMatrix(M,N)): DenseMatrix(M,N) {
		val dm = that.clone();
		SparseAddToDense.comp(this, dm);
		return dm;
	}

	//------------------------------
	// Add operator 
	//------------------------------
	public operator this + (that:SparseCSC(M,N))   = this.add(that);
	public operator this + (that:SparseCSR(M,N))   = this.add(that);
	public operator this + (that:DenseMatrix(M,N)) = this.add(that);
	public operator (that:DenseMatrix(M,N)) + this = this.add(that);

    //========================================================================
	// Substract method
    //========================================================================
 
	/**
	 *  Return this - that in a new dense 
	 */
	def sub(that:SparseCSC(M,N)) : DenseMatrix {
		val dm:DenseMatrix(M,N) = this.toDense();
		SparseSubToDense.comp(dm, that);
		return dm;
	}

	/**
	 *  Return this - that in a new dense 
	 */
	def sub(that:SparseCSR(M,N))  : DenseMatrix {
		val dm:DenseMatrix(M,N) = this.toDense();
		SparseSubToDense.comp(dm, that);
		return dm;
	}


	/**
	 *  Return this - that in a new dense 
	 */
	def sub(that:DenseMatrix(M,N))  : DenseMatrix {
		val dm:DenseMatrix(M,N) = that.clone();
		SparseSubToDense.comp(this, dm);
		return dm;
	}
	
	//------------------------------
	// Sub operator overloading
	//------------------------------
	/**
		Sub this with another matrix. 
	*/
	public operator this - (that:SparseCSC(M,N))   = this.sub(that);
	public operator this - (that:SparseCSR(M,N))   = this.sub(that);
	public operator this - (that:DenseMatrix(M,N)) = this.sub(that);

	public operator (that:DenseMatrix(M,N)) - this :DenseMatrix(M,N) {
		val dm:DenseMatrix(M,N) = that.clone();
		SparseSubToDense.comp(dm, this);
		return dm;
	}

    //========================================================================
	// Multiply method
    //========================================================================
    /**
     * Not support. Cannot use sparse matrix to store multiplication result.
     */
	public def mult(A:Matrix(M), B:Matrix(A.N,N), plus:Boolean): SparseCSR(this) {
		Debug.exit("Not supported. Use SparseMultSparseToDense,"+
				   "or SparseMultDenseToDense or DenseMultSparseToDense " +
				   "corresponding multiplication method");	
		return this;
	}
	/** 
	 * Not support
	 */
	public def transMult(
			A:Matrix{self.N==this.M}, 
			B:Matrix(A.M,this.N), 
			plus:Boolean):SparseCSR(this) {
	   Debug.exit("Not support");			 
	   return this;
	}
    
	/** 
	 * Not support
	 */
	public def multTrans(A:Matrix(M), 
			B:Matrix{A.N==self.N,self.M==this.N}, 
			plus:Boolean):SparseCSR(this) {
	   Debug.exit("Not support");			 
	   return this;
    }
	/**
	 * Compute this sparse matrix &#42 sparseCSC matrix. Result stores in dense
	 */
	public operator this % (that:SparseCSC{self.M==this.N}) 
		= SparseMultSparseToDense.comp(this, that);
	
	/**
	 * Compute this sparse matrix &#42 sparseCSR matrix. Result stores in dense
	 */
	public operator this % (that:SparseCSR{self.M==this.N}) 
		= SparseMultSparseToDense.comp(this, that);
	/**
	 * Compute Sparse matrix &#42 dense matrix. Result stores in dense
	 */	
	public operator this % (that:DenseMatrix{self.M==this.N}) 
		= SparseMultDenseToDense.comp(this, that);
	/**
	 * Compute dense matrix &#42 this sparse matrix. Result stores in dense
	 */
	public operator (that:DenseMatrix{self.N==this.M}) % this 
		= DenseMultSparseToDense.comp(that, this);


    //========================================================================
	// Util
    //========================================================================
	public def likeMe(m:Matrix):Boolean {
		return m instanceof SparseCSR && m.M==M && m.N==N;
	}

	public def toString() = crdata.toString();
	/**
	   Print the sparse matrix in CSR format
	*/
	public def print(msg:String) {
		val outstr:String = msg + 
			"------- Sparse Matrix in CSR "+M+"x"+N+"-------\n"+
			this.toString() +
			"-----------------------------------------------\n";
		Console.OUT.print(outstr);
		Console.OUT.flush();
	}

	public def print() { print("");}
	public def debugPrint(msg:String) {
		if (Debug.disable) return;
		Debug.println(msg+this.toString());	
	}
	//
	public def debugPrint() { debugPrint(""); }

	//---------------------------
	// X10 Int MAX_VALUE is 2*10^10, change M*N to Double, in case
	// exceeding MAX_VALUE
	public static def compAllocSize(m:Int, n:Int, nz:Double):Int {
		var nzd:Double = nz;
		if (nzd > 1.0) {
			Console.OUT.println("Nonzero density > 1.0, reset to 1.0");
			Console.OUT.flush();
			nzd = 1.0;
		}
		var tc:Double = (n * m as Double) * nzd + 1.0;
		if (tc > Int.MAX_VALUE) {
			Console.OUT.printf("Warning: size %f exceeds maximum value %d\n", 
							   tc, Int.MAX_VALUE);
			Console.OUT.flush();
			tc = Int.MAX_VALUE;
		}
		return tc as Int;
	}
	
}

/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2012.
 */
package x10.matrix.block;

import x10.compiler.Inline;
import x10.util.StringBuilder;

import x10.matrix.Debug;
import x10.matrix.MathTool;

/**
 * This class represents meta-information for block matrices, 
 * with the rows divided into equal-sized blocks, 
 * and the columns divided into equal-sized blocks. 
 * If the number of rows (or columns) in the matrix cannot be equally divided
 * the first row blocks (or column blocks) are assigned with one more row (or column).
 * <p>
 * In grid partitioning, blocks in the same row of partition grid (row blocks) share the same number of rows.
 * Blocks in the same column of partition grid (column blocks) share the same number of columns.
 * 
 * <p> Public field of "rowBs" is used to specify the list of numbers of rows for blocks, 
 * and field "colBs" specifies the list of numbers of columns for blocks. 
 *
 * <p> The constructor in this class divides rows (columns) of matrix evenly
 * among partitioning row blocks (column blocks). If it cannot evenly divide, 
 * the first row blocks (column blocks) are assigned with one extra row (column) 
 * than the rest of row blocks (column blocks).
 *
 * <p> Users can specified their own partitioning by constructing instance
 * based on specified "rowBs" and "colBs", as far as, it complies with
 * grid-partitioning.
 */
public type Grid(bM:Int,bN:Int)=Grid{self.numRowBlocks==bM, self.numColBlocks==bN};
public type Grid(m:Int,n:Int,bM:Int,bN:Int)=Grid{self.M==m,self.N==n,self.numRowBlocks==bM,self.numColBlocks==bN};

public class Grid(M:Int, N:Int,numRowBlocks:Int, numColBlocks:Int) {
	/**
	 * Number of blocks in partitioning
	 */
	public val size:Int;

	/**
	 * List of numbers of rows in blocks. Blocks in the same partitioning row blocks 
	 * have the same number of rows.
	 */
	public val rowBs:Array[Int](1){rail}; //list of block row sizes  

	/**
	 * List of numbers of columns in blocks. Blocks in the same partitioning column
	 * blocks have the same number of columns.
	 */
	public val colBs:Array[Int](1){rail}; //list of block column sizes

	/**
	 * Construct a partition for an m x n matrix with the rows divided by the
	 * specified number of row blocks (rb) and columns divided by the 
	 * specified number of column blocks (cb). 
	 * In the partitioning, blocks in the same partition row have the same number of
	 * rows, and blocks in the same partition column have the same number of columns,
	 * making a grid partitioning.
	 *
	 * <p> In partitioned blocks, number of rows and columns of the matrix 
	 * are evenly divided by the number of row blocks and the number of column blocks.
	 * If the numbers cannot be evenly divided, 
	 * the first m%rb partition row blocks are assigned with one extra
	 * row, and n%cb partition column blocks are assigned with one extra
	 * column.
	 *
	 * @param  m         number of rows in matrix
	 * @param  n         number of columns in matrix
	 * @param numRowBs   number of partition row blocks
	 * @param numColBs   number of partition column blocks
	 */
	public def this(m:Int, n:Int, numRowBs:Int, numColBs:Int) {

		property(m,n, numRowBs, numColBs);
		size = numRowBs*numColBs;
		//Guard for divided by 0 condition!
		Debug.assure(numRowBs!=0&&numColBs!=0);
		rowBs = partition(M, numRowBs);
		colBs = partition(N, numColBs);
	}

	/**
	 * Create a partition for an m x n matrix in specified sizes for rows and columns
	 * of blocks
	 *
	 * @param  m      number of rows in matrix
	 * @param  n      number of columns in matrix
	 * @param rBs     list of rows for blocks
	 * @param cBs     list of columns for blocks
	 */
	public def this(m:Int, n:Int, rBs:Array[Int](1){rail}, cBs:Array[Int](1){rail}) {

		property(m,n, rBs.size, cBs.size);
		size = rBs.size * cBs.size;
		rowBs = rBs;
		colBs = cBs;
	}

    /**
     * Construct a grid partitioning with specified numbers of row and 
	 * specified numbers of column for partitioning blocks. 
	 * Blocks in the same partition row share the same number of rows, and
	 * blocks in the same partition column share the same number of columns.
	 *
	 * @param  rbs    rows of blocks rowwise
	 * @param  cbs    columns of blocks columnwise
     */
    public def this(rbs:Array[Int](1){rail}, cbs:Array[Int](1){rail}){
        property(
			 ( ()=> {
				 var t:Int=0; 
				 val rs = rbs.size;
				 for (var i:Int=0; i < rs; i++)
					 t += rbs(i);
				 t } )(), 
			 ( () => {
				 var t:Int=0; 
				 val cs = cbs.size;
				 for (var i:Int=0; i < cs; i++)
					 t += cbs(i); 
				 t } )(), 
			 rbs.size, 
			 cbs.size 
			);
		rowBs = rbs;
		colBs = cbs;
		size  = rbs.size * cbs.size;
    }

	/**
	 * Make a grid partitioning based on specified matrix dimension and
	 * maximize the number of row blocks allowed.
	 *
	 * @param  m          number of rows in matrix
	 * @param  n          number of columns in matrix
	 * @param maxRowBs    the largest number of blocks in partition row
	 * @param totalBs     total number of blocks
	 * 
	 */
	public static def makeMaxRow(m:Int, n:Int, var maxRowBs:Int, totalBs:Int) {
		if (maxRowBs > m ) maxRowBs = m;
		while (totalBs % maxRowBs != 0) { maxRowBs--; }
		if (maxRowBs == 0) maxRowBs = 1;
		val cb = totalBs/maxRowBs;
		return new Grid(m, n, maxRowBs, cb);
	}
	
	public static def makeMaxCol(m:Int, n:Int, var maxColBs:Int, totalBs:Int) {
		if (maxColBs > n ) maxColBs = n;
		while (totalBs % maxColBs != 0) { maxColBs--; }
		if (maxColBs == 0) maxColBs = 1;
		val rb = totalBs/maxColBs;
		return new Grid(m, n, rb, maxColBs);
	}	
	/**
	 * Make a grid partitioning based on specified matrix dimension and total
	 * number of blocks, where the difference between numbers of row blocks
	 * and column blocks is minimized. This method makes a squared partitioning 
	 * or close-squared partitioning.
	 *
	 * @param  m         number of rows in matrix
	 * @param  n         number of columns in matrix
	 * @param totalBs    total number of partition blocks
	 */
	public static def make(m:Int, n:Int, totalBs:Int) =
		makeMaxRow(m, n, Math.sqrt(totalBs as Double) as Int, totalBs); 
	/*{
		var rb:Int = Math.sqRoot(s) as Int;
		if (rb == 0) rb = 1;
		while (s % rb != 0) { rb--; }
		val cb = s / rb;
		return new Grid(m, n, rb, cb); 		
	}*/


	/**
	 * Make a grid partitioning based on specified matrix dimension targeting
	 * the total number of places in current parallel execution.
	 * This method creates a squared or close to squared partitioning.
	 */
	public static def make(m:Int, n:Int) =
		makeMaxRow(m, n, Math.sqrt(Place.MAX_PLACES) as Int, Place.MAX_PLACES); 

	
	/**
	 * 
	 */
	public static def make(rbs:Int, cbs:Int, 
			rowPartFunc:(Int)=>Int, colPartFunc:(Int)=>Int) {
		val rBzList = new Array[Int](rbs, (i:Int)=>rowPartFunc(i));
		val cBzList = new Array[Int](cbs, (i:Int)=>colPartFunc(i));
		var m:Int=0, n:Int=0;
		for (var r:Int=0; r<rbs; r++) m+=rBzList(r);
		for (var c:Int=0; c<cbs; c++) n+=cBzList(c);
		
		return new Grid(m, n, rBzList, cBzList);
	}
	
	//================================================================
	/** 
	 * Compute the size of segment in partitioning.
  	 *
	 * @param  n     the number of rows or columns to be partitioned
	 * @param  b     the number of blocks in partitioning
	 */
	public static def partition(n:Int, b:Int):Array[Int](1){rail} {

		val bdim = new Array[Int](b);
		val bs:Int = n / b;
		var k:Int  = n % b;

		Debug.assure(bs>0, "Partition has 0 size");
		for (var i:Int=0; i< b; i++) {
			//bdim(i) = compBlockSize(n, b, i);
		    bdim(i) = bs;
			if (k>0) {
				bdim(i)++; 
				k--;
			} 
		}
		return bdim;
	}

	@Inline
	public static def compBlockSize(nTotal:Int, blkNum:Int, blkId:Int):Int {
		var sz:Int = (blkId < nTotal % blkNum)?1:0;
		sz += nTotal / blkNum;
		return sz;
	}
	//--------------------------------------
	// Mapping 1D->2D
	//--------------------------------------
	/**
	 * For a given place/block id, return its row block id 
	 */
	public def getRowBlockId(id:Int):Int = (id%numRowBlocks);

	/**
	 * For a given place/block id, return the column block id
	 */		
	public def getColBlockId(id:Int):Int = (id/numRowBlocks);
	//-----------
	/**
	 * Given a block's row and column id, return the place id
	 */	
	public def getBlockId(r:Int, c:Int):Int = (c*numRowBlocks+r);
	
 	//-----------
	/**
	 * Given a place id, return the number of rows in the corresponding block
	 */	
	public def getRowSize(id:Int):Int = rowBs(getRowBlockId(id));
	/**
	 * Given a place id, return the number of columns in the corresponding block
	 */	
	public def getColSize(id:Int):Int = colBs(getColBlockId(id));

	/**
	 * Given place id, return the number of elements in the block
	 */	
	public def getBlockSize(id:Int):Int = getRowSize(id) * getColSize(id);
	/**
	 * Given the block's row and column id, return the block's size.
	 */	
	public def getBlockSize(rid:Int, cid:Int):Int = rowBs(rid) * colBs(cid);
	
	// The last row block has the smallest size.
	public def getMinRowSize() = rowBs(numRowBlocks-1); 
	// The last col block has the smallest size.
	public def getMinColSize() = colBs(numColBlocks-1); 
	//----------
	
	//================================================
	/**
	 * Given a place id, return the place id next to it in
	 * the same row. The method is used is SUMMA on DistDense/SparseMatrix
	 */	
	public def nextRow(pid:Int):Int = ((pid % numRowBlocks + 1)==numRowBlocks) ? (pid - numRowBlocks + 1):(pid + 1);
	/**
	 * Given a place id, return the place id next to it in
	 * the same column
	 */	
	
	public def nextCol(pid:Int):Int  = ((pid / numRowBlocks + 1)==numColBlocks) ? (pid % numRowBlocks):(pid + numRowBlocks);
	/**
	 * Given a place id, return its previous place id in
	 * the same row.
	 */	
	public def prevRow(pid:Int):Int = (pid % numRowBlocks==0) ? (pid + numRowBlocks - 1):(pid - 1);
	/**
	 * Given a place id, return its previous place id in
	 * the same cyclic column
	 */
	public def prevCol(pid:Int):Int = (pid / numRowBlocks==0) ? (pid + numRowBlocks*(numColBlocks - 1)):(pid - numRowBlocks); 
	//=========================================================
	/**
	 * Non-cyclic neighboring block.
	 */
	public def getNorthId(rid:Int, cid:Int):Int = rid>0?               getBlockId(rid-1,cid):-1;
	public def getSouthId(rid:Int, cid:Int):Int = rid<numRowBlocks-1 ? getBlockId(rid+1,cid):-1;
	public def getWestId(rid:Int, cid:Int):Int  = cid>0?               getBlockId(rid,cid-1):-1;
	public def getEastId(rid:Int, cid:Int):Int  = cid<numColBlocks-1 ? getBlockId(rid,cid+1):-1;
	
	         
	//----------------------------
	// Locating block matrix
	//----------------------------
	//

	/**
	 * Given row and column position of element find its partition blocks and
	 * row and column index within the block
	 *
	 * @param x -- row position in matrix
	 * @param y -- column position in matrix
	 * @param [row block index, column block index, row index, column index]
	 */
   	public def find(var x:Int, var y:Int): Array[Int](1){rail}{
		
		var br:Int;
		var bc:Int;
		Debug.assure( (x >=0) && (x<this.M) && (y>=0) && (y<this.N));
	 
		for (br=0; x >= rowBs(br) && br<numRowBlocks; br++) {
				x -= rowBs(br); 
		}
		for (bc=0; y >= colBs(bc) && bc<numColBlocks; bc++) {
				y -= colBs(bc); 
		}
		
		return [br, bc, x, y];
	}
			
   	/**
   	 * Given row and column index, return block ID in the coloumn-wise
   	 * indexing.
   	 */
   	public def findBlock(var x:Int, var y:Int): Int {
   	
   		var br:Int;
   		var bc:Int;
   		var bid:Int=0;
   		Debug.assure( (x >=0) && (x<this.M) && (y>=0) && (y<this.N));

   		for (bc=0; y >= colBs(bc) && bc<numColBlocks; bc++) {
   			y -= colBs(bc);
   			bid += numRowBlocks;
   		}
   		
   		for (br=0; x >= rowBs(br) && br<numRowBlocks; br++) {
   			x -= rowBs(br);
   			bid++;
   		}
   		return bid;   		
   	}
   	
   	/**
   	 * Compute the starting row for a given row block id
   	 */
   	public def startRow(rid:Int):Int {
   		var sttrow:Int=0;
   		for (var i:Int=0; i<rid; i++)
   			sttrow += rowBs(i);
   		return sttrow;
   	}
   	
   	/**
   	 * Compute the starting column for a given column block id;
   	 */
   	public def startCol(cid:Int):Int {
   		var sttcol:Int=0;
   		for (var i:Int=0; i<cid; i++)
   			sttcol += colBs(i);
   		return sttcol;
   	}  	
   	
   	public def startColumn(cid:Int) = startCol(cid);

	//-----------------------------------------------------
	/**
	 * Return a grid partition for the transposed matrix.
	 */
	public def newT():Grid = new Grid(this.colBs, this.rowBs);
	//{
		//val g = new Grid(this.N, this.M, this.numColBlocks, this.numRowBlocks);
		//g.transpose = !this.transpose;
		//return g;
	//}

	//=========================================================
	/**
	 * Test two partitions are same or not. This method only
	 * check matrix dimension and numbers row blocks and column blocks.
	 *
	 * @param that   the target partitioning
	 */
	public def likeMe(that:Grid):Boolean {

		return M == that.M && N == that.N 
			&& numRowBlocks == that.numRowBlocks
			&& numColBlocks == that.numColBlocks;
	}

	/**
	 * Check two partitions are exactly same or not. This method only
	 * check all row block sizes and column block sizes.
	 *
	 * @param that   the target partitioning
	 */
	public def equals(that:Grid):Boolean {
		
		if (this == that) return true;
		if (!likeMe(that)) return false;

		return (match(this.rowBs, that.rowBs) && match(this.colBs, that.colBs));
	}
	
	public static def match(alist:Array[Int](1), blist:Array[Int](1)):Boolean {
		var ret:Boolean = true;
		for (var i:Int=0; i<alist.size&&i<blist.size&&ret; i++)
			ret &= (alist(i) == blist(i));
		return ret;
	}
	
	//=========================================================
	// Assuming one-to-one unique distribution	
	/**
	 * Return a distributed array, each entry contains an array of place ids
	 * which locate in the same row as the entry's index
	 */
	public def getRowBsPsMap():DistArray[Array[Int](1)](1) {
		val map:DistArray[Array[Int](1)](1);
		val d:Dist(1) = Dist.makeUnique();
		map = DistArray.make[Array[Int](1)](d);			
		finish ateach(val [p]:Point in map.dist){
			val pid = here.id();
			map(here.id()) = new Array[Int](numColBlocks,
					(c:Int)=>(getBlockId(getRowBlockId(pid), c)));
		}
		return map;
	}
	
	public def getColBsPsMap():DistArray[Array[Int](1)](1) {
		val map : DistArray[Array[Int](1)](1);
		val d   = Dist.makeUnique();
		map = DistArray.make[Array[Int](1)](d);
		finish ateach(val [p]:Point in map.dist) {
			val pid = here.id();
			map(here.id())= new Array[Int](numRowBlocks, 
					(r:Int)=>getBlockId(r, getColBlockId(pid))); 
		}
		return map;
	}	
	
	//=========================================================
	//
	public def toString() : String {
		val strbld = new StringBuilder();
		strbld.add("Partition "+M+" rows into "+numRowBlocks+" [");
		for (var i:Int=0; i<numColBlocks; i++, strbld.add(","))
			strbld.add(rowBs(i));
		strbld.add("]\nPartition "+N+" cols into "+numColBlocks+" [");
		for (var i:Int=0; i<numRowBlocks; i++, strbld.add(","))
			strbld.add(colBs(i));
		strbld.add("]\n");
		return strbld.toString();
	}
	//
	public def print() {
		Console.OUT.print("Matrix:"+M+"x"+N+" partitioned in ("+numRowBlocks+","+numColBlocks+") blocks\n");
		Console.OUT.print("Column block: ");
		Console.OUT.print(this.toString());
		Console.OUT.flush();;
	}
	//
	public def debugPrint() {
		if (Debug.disable) return;
		val dbstr:String = "Matrix:"+M+"x"+N+" partitioned in ("+numRowBlocks+","+numColBlocks+") blocks\n";
		Debug.print(dbstr+this.toString());
 	}
	//

}


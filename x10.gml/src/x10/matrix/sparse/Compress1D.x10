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

import x10.compiler.Inline;
import x10.io.Console;
import x10.util.Pair;
import x10.util.StringBuilder;

import x10.matrix.Debug;
import x10.matrix.MathTool;

/**
 * This class provides abstraction of compressed 1-dimension array or
 * compressed vector class based on compressed array.
 */
public class Compress1D {

	public val cArray:CompressArray;
	public var offset:Int;
	public var length:Int;

	/**
	 * Create a Compress1D object with the given data.
	 * @param off     offset in storage
	 * @param count      number of entries
	 * @param ca     The storage for the compressed data
	 */
	public def this(offset:Int, count:Int, ca:CompressArray) {
		Debug.assure(offset+count <= ca.count);
		this.cArray = ca;
		this.offset = offset;
		this.length = count;
	}


	/** 
	 * Initialize the Compress1D instance based on given storage and assign each
	 * nonzero element by a constant value.  Used for debug purpose.
	 *
	 * @param ldm     Maximum index, or leading dimension
	 * @param v     The value used to initialize each entry
	 * @param nzp     Percentage of nonzero entries
	 * @param off     Offset of storage data
	 * @param ca     The storage for the compressed data
	 */
	@Inline
	public def initConst(ldm:Int, v:Double, nzp:Double, off:Int, ca:CompressArray):void {
		initConst(0, ldm, v, nzp, off, ca);
	}
	public def initConst(sttIndex:Int, ldm:Int, v:Double, nzp:Double, off:Int, ca:CompressArray):void {
		val cnt = ca.initConstValue(sttIndex, off, ldm, v, nzp);
		this.offset = off;
		this.length = cnt;
	}
	
	/**
	 * Initialize the Compress1D instance with random values
	 *
	 * @param ldm     Maximum index
	 * @param nzp     Percentage of nonzero entries, or sparsity
	 * @param off     Offset for the storage
	 * @param ca     The storage of compressed array
	 */
	@Inline	public def initRandom(maxIndex:Int, nzp:Double, offset:Int, ca:CompressArray):void {
		initRandom(0, maxIndex, nzp, offset, ca);
	}
	
	public def initRandom(sttIndex:Int, maxIndex:Int, nzp:Double, offset:Int, ca:CompressArray):void {
		val cnt=ca.initRandom(offset, sttIndex, maxIndex, nzp);
		this.offset = offset;
		this.length = cnt;		
	}
	//-------------------------------------------------------------
	//-------------------------------------------------------------
	/**
	 * Short version of make and initialize with random values
	 * @see constructor()
	 * @see initRandom()
	 */
	public static def makeRand(maxIndex:Int, 
							   nzp:Double, 
							   offset:Int,
							   ca:CompressArray):Compress1D {
		val cnt=ca.initRandom(offset, maxIndex, nzp);
		return new Compress1D(offset, cnt, ca);
	}

	/**
	 * Initialize the Compress1D instance with random values with
	 * uniform distributed distance between two nonzero indexes. 
	 *
	 * @param ldm     Maximum index
	 * @param nzp     Percentage of nonzero entries, or sparsity
	 * @param off     Offset for the storage
	 * @param ca      The shared storage
	 * @param lb      lower bound
	 * @param up      upper bound
	 */
	public def initRandomFast(ldm:Int,      // Maximum index
							  nzp:Double,   // Nonzero percentage
							  off:Int,      // Offset for the storage
							  ca:CompressArray, // The shared storage 
							  lb:Int, ub:Int):void {
		val cnt = ca.initRandomFast(off, 0, ldm, nzp, lb, ub); 
		this.offset = off;
		this.length = cnt;
	}
	
	public def initRandomFast(sttIndex:Int, //Starting index value,
			ldm:Int,      // Maximum index
			nzp:Double,   // Nonzero percentage
			off:Int,      // Offset for the storage
			ca:CompressArray, // The shared storage 
			lb:Int, ub:Int):void {
		val cnt = ca.initRandomFast(off, sttIndex, ldm, nzp, lb, ub); 
		this.offset = off;
		this.length = cnt;
	}
	
	/**
	 * Initialize the Compress1D instance with random values with
	 * uniform distributed distance between two nonzero indexes. 
	 * 
	 * @param ldm     Maximum index
	 * @param nzp     Percentage of nonzero entries, or sparsity
	 * @param off     Offset for the storage
	 * @param ca      The shared storage
	 */
	public def initRandomFast(ldm:Int, nzp:Double, off:Int, ca:CompressArray):void {
		initRandomFast(0, ldm, nzp, off, ca, 0, 0);
	}
	
	public def initRandomFast(sttIndex:Int, ldm:Int, nzp:Double, off:Int, ca:CompressArray):void {
		initRandomFast(sttIndex, ldm, nzp, off, ca, 0, 0);
	}
	/**
	 * Short version of make and initialize with random values fast function
	 * @see this()
	 * @see initRandomFast()
	 */
	public static def makeRandomFast(maxIndex:Int, 
									 nzp:Double, 
									 offset:Int,
									 ca:CompressArray):Compress1D {
		val cnt=ca.initRandomFast(offset, maxIndex, nzp);
		return new Compress1D(offset, cnt, ca);
	}

	/**
	 * Clone this object. The resulting object is allocated in
	 * a new memory space, which will be not be shared with other
	 * 1D instance, resulting disjointed memory space for 2D and
	 * CSC and CSR object. To clone sparse matrix or Compress2D
	 * using shared storage among all compressed row/columns, do not
	 * use this method.
	 */
	public def clone():Compress1D {
		val ca = new CompressArray(length);
		for (var i:Int=0; i<length; i++)
		    ca(i)=Pair(cArray.getIndex(offset+1), cArray.getValue(offset+i));
		ca.count = length;
		return new Compress1D(0, length, ca);
	}

	//================================================================
	// 
	// Memory disjointed compress
	public static def compress(src:Array[Double](1) //The data array to be compressed
							   ): Compress1D {
		val ca = CompressArray.compress(src);
		return new Compress1D(0, ca.count, ca);
	}
	
	// Memory-jointed compress to CompressArray
	/**
	 * Compress src array into this compressed 1D line.
	 */
	public def compressAt(off:Int,   //The offset in storage to hold compress data
						  d:Array[Double](1) //The source data to be compressed
						  ):Int {  // Return number of data compressed
		offset = off;
		length = cArray.compressAt(offset, d);
		return length;
	}

	//=========================================================
	// Data Access
	//=========================================================
	/**
	 * Return the number of entries in the compressed array.
	 */
	public def size():Int = this.length;

	/**
	 * Return the data value array storing the compressed data
	 */	
	public def getData() = cArray.value;

	/**
	 * Return the index value array storing the positions of corresponding values
	 * in the uncompressed data arry
	 */	
	public def getIndex() = cArray.index;
	
	// Given index and find its value in compressed line
	/**
	 * Return element value paired with idx in the compressed array
	 * within the range of this.offset and this.offset+this.length
	 * If idx is not found in the surface index array, 0.0D is returned.
	 * This method allows random access of data in column/row given
	 * the surface index, however expensive.
	 */
	public operator this(idx:Int):Double {
		val pos = find(idx);
		if (pos >= 0 ) 
			return cArray.getValue(offset+pos);
		return 0.0D;
	}

	//----------
	// Using relative position
	// The count of CompressArray is not changed.

	/**
	 * Set the surface index and value pair (the left-hand parameters)
	 * to position of the underlying index at this.offset+pos 
	 * (pos is the right-hand parameter) in the compressed array
	 * This method does not involve searching in the compressed array.
	 * Using this method should be careful. The compressed array must preserve
	 * the surface indices in creasing order.
	 *
	 * To set (surface index, value) pair in the compressed 1D, the 
	 * underlying index of surface index needs be found first, and then
	 * use this method. We do not advice to modify the compressed
	 * data or modify sparse matrix after it is created.
	 */
	public operator this(pos:Int)=(w:Pair[Int,Double]) :void {
		cArray(offset+pos)=w;
	}
		
    /**
	 * Return the surface index at the this.offset+pos of 
	 * compressed array of surface index 
	 */
	public def getIndex(pos:Int) = cArray.getIndex(offset+pos);

	/**
	 * Return the value of compressed array at the this.offset+pos 
	 */
	public def getValue(pos:Int) = cArray.getValue(offset+pos);

	/**
	 * Initialize or reset data in the compressed line.
	 */
	public def reset():void {
		length = 0;
		offset = 0;
	}


	/**
	 * Find the underlying index in compressed line for the given 
	 * surface index. If not found, -1 is returned. 
	 * If found, the underlying index is returned (relative value
	 * to the compressed line's offset.
	 *
	 * This method is expensive. The binary search is used.
	 */
	public def find(index:Int):Int {
		if (this.length == 0) return -1;
		val fpos = findIndex(index);
		if (cArray.getIndex(fpos) == index )
			return fpos-offset;// change to relative index
		return -1;  //a zero entry
	}
	
	/**
	 * If not found, return the nonzero index in the next position
	 * If found, return absolute position
	 */
	def findIndex(idx:Int):Int {
	    val end = offset+length-1;
	    val pos = cArray.find(idx, offset, end);
	    if (pos > end) return end;
	    return pos;   
	}


	/**
	 * Search a range of surface indices in the compressed data array.
	 * This method locates the underlying indices range in 
	 * (starting, count) pair of the specified surface index range.
	 * @param start     The surface index value from which to start
	 * @param end     The surface index value at which to end.
	 */
	def findIndexRange(start:Int, end:Int) : Pair[Int,Int] { 
		// Special case: empty line case
		if (this.length == 0) return Pair[Int,Int](offset, 0);

		val startPos = findIndex(start);
		val endPos   = findIndex(end);
		val foundStart = cArray.getIndex(startPos);
		val foundEnd   = cArray.getIndex(endPos);
		
		// Get the actual nonzero index length
		var count:Int=0;
		if (foundStart >= start)  {
		    count = endPos - startPos + (foundEnd <= end?1:0);
		}
		return Pair[Int,Int](startPos, count);
	}
	
	public def countIndexRangeBefore(mid:Int) : Int { 
		// Special case: empty line case
		if (this.length == 0) return 0;
		
		val startPos = offset;
		val endPos   = findIndex(mid);
		//val foundStart = cArray.getIndex(startPos);
		val foundEnd   = cArray.getIndex(endPos);
		
		// Get the actual nonzero index length
		val count = endPos - startPos + (foundEnd <= mid?1:0);
		
		return count;
	}
	
	public def countIndexRangeAfter(mid:Int, end:Int) : Int { 
		// Special case: empty line case
		if (this.length == 0) return 0;
		
		val startPos = findIndex(mid);
		val endPos   = offset+length-1;
		val foundStart = cArray.getIndex(startPos);
		//val foundEnd   = cArray.getIndex(endPos);
		// Get the actual nonzero index length
		val count = endPos - startPos + (mid <= foundStart?1:0);
		
		return count;
	}
	
	//-----------------------------------------------------
	/** obsolete
	 * Copy length entries from startIndex.
	 * @param startIndex
	 * @param length
	 * @param destOffset
	 * @param ca     The destination compressed array.
	 * @return     number of data entries copied.
	 */
	public def copyPart(startIndex:Int, 
						length:Int, 
						destOffset:Int,
						ca:CompressArray
						):Int {          
		val r = findIndexRange(startIndex, startIndex+length-1);
		val startPos = r.first; 
		val count   = r.second;
		//Console.OUT.printf("Copy range from %d, len %d\n", stpos, cnt);
		if (count > 0) 
			cArray.copyRange(startPos, count, destOffset, startIndex, ca);
		return count;
	}
	
	/** obsolete
	 * @param destOffset     The offset in the target
	 * @param ca     The target compressed array
	 * @return     The number of items copied
	 */
	public def copyAll(dstoff:Int, ca:CompressArray ):Int {    
		cArray.copyRange(offset, length, dstoff, 0, ca);
		return length;
	}



	//--------------------------------------------------
	// Copy data 
	//--------------------------------------------------
	/**
	 * Copy all elements (index-value pairs) from source compress line to target.
	 * The target's offset field is used as the starting position, and the length
	 * field is reset to the source's.
	 *
	 * @param src     The source compress line
	 * @param dst     The target compress line
	 */
	public static def copy(src:Compress1D, dst:Compress1D):void {
		CompressArray.copy(src.cArray, src.offset, 
						   dst.cArray, dst.offset, src.length);
		dst.length = src.length;
	}

	// Copy a region to target
	/**
	 * Copy a specified range elements from its source to the target.
	 *
	 * <p> The range is specified by the starting index of the original data array (uncompressed) and
	 * number of indexes in the original array. 
	 * The traget's compress line's offset is used as the starting point to store the elements,
	 * in the target compress array, and the compress line length field will be reset to the
	 * number of elements (index-value pairs) copied.
	 * The index values in the target starts from 0, and the source index array is adjusted, 
	 * using the starting index in the compress line.
	 *
	 * @param src          The source compress line
	 * @param idxStart     The starting index in the uncompressed data array
	 * @param dst          The target compress line
	 * @param idxCount     The number of data in the uncompressed data array to be copied
	 */
	public static def copySection(src:Compress1D, idxStart:Int, 
								  dst:Compress1D, idxCount:Int): void {

		val rng = src.findIndexRange(idxStart, idxStart+idxCount-1);
		val off = rng.first; 
		val cnt = rng.second;
		//Debug.flushln("At source copy range from offset:"+off+" len:" + cnt+
		//				" to dst off:"+dst.offset);
		if (cnt > 0) 
			CompressArray.copy(src.cArray, off, 
							   dst.cArray, dst.offset, cnt, idxStart);
		dst.length = cnt;
	}

	// attach to the target
	/**
	 * Copy all elements, and attach at the end of the target. The target indexing values
	 * are updated accordingly.
	 *
	 *
	 * @param src          source compress line
	 * @param dst          target compress line
	 * @param sttidx       starting index at target 
	 */
	public def appendTo(dst:Compress1D, sttidx:Int): void {

		val dstoff = dst.offset+dst.length;
		CompressArray.copy(cArray, offset, 
						   dst.cArray, dstoff, length, -sttidx);
		dst.length += length;
	}


	//=========================================================
	// Decompress data and store it in an array
	//=========================================================
	/**
	 * Extract a specified range of data from compress line. The range
	 * of starting offset and length is specified in uncompress condition.
	 * 
	 * @param startIndex     starting index of the uncompress array
	 * @param length         length of the index range of the uncompress array
	 * @param destOffset     offset in the destination
	 * @param dest           target array
	 */
	public def extract(startIndex:Int, 
					   length:Int, 
					   destOffset:Int, 
					   dest:Array[Double](1)):void {
		val r = findIndexRange(startIndex, startIndex+length-1);
		val startPos = r.first;
		val count   = r.second;
		if (count == 0) return;
		cArray.extract(startPos, count, destOffset, dest);
	}
	//
	public def extract(dst:Array[Double](1)) :void {
		//Debug.flushln("Extract "+offset+" "+length+" ");
		if (length > 0)
			cArray.extract(offset, length, 0, dst);
	}

	/**
	 * Extract all data in compress line
	 *
	 * @param dstoff      Destination array offset
	 * @param dst         Destination arra
	 */
   	public def extract(dstoff:Int, dst:Array[Double](1)) {
		cArray.extract(offset, length, dstoff, dst);
	}

   	/**
   	 * Add a compressed line to the uncompressed array.
	 * This method is used for SUMMA transposed multiplication
   	 */
	public def addToArray(dstoff:Int, dst:Array[Double](1)):void {
		//Set the source 1 (dest) compress line cline
		for (var i:Int=0; i<this.length; i++) {
			val dstpos = dstoff+getIndex(i);
			dst(dstpos) = getValue(i);
		}
	}
	//=========================================================
	public def countNonZeroTo(idxval:Int):Int {
		
		var n:Int =0;
		for (var i:Int=offset; i< offset+length; i++) {
			if (cArray.index(i) <= idxval) 
				n++;	
			else
				break;
		}
		return n;
	}
	
	//=========================================================
	// Util methods
	//=========================================================
	public def toString():String {
		val outstr = new StringBuilder();
		outstr.add("Compress 1D off:"+offset+" len:"+this.length+" [ ");
		for (var i:Int=0; i<this.length; i++) {
			outstr.add(" "+getIndex(i)+":"+getValue(i)+" ");
		}
		outstr.add(" ]");
		return outstr.toString();
	}

	public def print(msg:String) {
		val ostr:String = msg +toString();
		Console.OUT.println(ostr);
		Console.OUT.flush();
	}
	public def print() { print("");}

	public def debugPrint(msg:String) {
		if (Debug.disable) return;
		val output:String= msg+toString();
		Debug.println(output);
	}
	public def debugPrint() { debugPrint(""); }

	public def equals(cl:Compress1D):Boolean {
		if (this.length != cl.length) 
			return false;
		//
		for (var i:Int=0; i<this.length; i++) {
			if ((this.getIndex(i) != cl.getIndex(i)) ||
				(this.getValue(i) != cl.getValue(i)))
				return false;
		}
		return true;
	}
	//
	public def testIn(al:Array[Double](1)):Boolean {
		for (var i:Int=0; i<this.length; i++) {
			if (MathTool.equals(al(getIndex(i)), getValue(i)))
				continue;
			else
				return false;
		}
		return true;
	}

	//----------------------------------------------
	// Randomness info
	//----------------------------------------------
	public def compAvgIndexDst():Double {
		val lpos = this.length-1;
		if (lpos <= 0) return 0.0;
		return 1.0*(this.getIndex(lpos) - this.getIndex(0))/(this.length-1);
	}
	//
	public def compIndexDstSumDvn(avg:Double):Double {
		var df:Int=0;
		var dv:Double=0;
		for (var i:Int=0; i<this.length-1; i++) {
			df = this.getIndex(i+1) - this.getIndex(i);
			Debug.assure(df > 0);
			dv += (df-avg)*(df-avg);
		} 
		return dv;
	}
	//
	public def compIndexDstStdDvn() : Double {
		if (this.length <= 1) return 0.0;
		val d =compIndexDstSumDvn(compAvgIndexDst());
		return x10.lang.Math.sqrt(d / (this.length-1));
	}
}
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

package x10.matrix.comm;

import x10.matrix.ElemType;

/**
 * This class provides implementation for reduce-sum  operation for data arrays which
 * can be accessed via DistArray structure in all places.
 */
public class DistArrayReduce extends DistArrayRemoteCopy {
	/**
	 * Reduce all data in DistArray from all places to here
	 *
	 * @param ddmat   Input/Output. Distributed storage of data array in all places
	 * @param ddtmp   Temp distributed storage of data array used for receiving data.
	 * @param datcnt  count of double-precision data 
	 */
	public static def reduceSum(
			ddmat:DistDataArray,
			ddtmp:DistDataArray, 
			datCnt:Long):void {
		
		x10ReduceSum(ddmat, ddtmp, datCnt);
	}

	/**
	 * Sum of all data of arrays from all places. The input data will be replaced by
	 * the result.
	 *
	 * @param ddmat    distributed storage for data arrays in all places
	 * @param ddtmp    temp distributed storage used to receive data
	 */
	public static def x10ReduceSum(
			ddmat:DistDataArray, 
			ddtmp:DistDataArray, 
			datCnt:Long): void{
		
		val root = here.id();
		val mat  = ddmat(root);
		val pcnt = ddmat.region.size();

		reduceSumToHere(ddmat, ddtmp, datCnt, pcnt);
	}

	/**
	 * Binary recursive reduce sum.
	 * Notice dmat is input and output data array.
	 */
	protected static def reduceSumToHere(
			ddmat:DistDataArray, 
			ddtmp:DistDataArray,
			datCnt:Long,
			var pcnt:Long): void {
		
		val root = here.id();
		val ttpcnt = ddmat.region.size();
		val dstbuf = ddmat(root);
		
		if (root + pcnt > ttpcnt) pcnt = ttpcnt-root;

		if (pcnt <= 1) return;

		val lfcnt  = (pcnt+1) / 2; // make sure left part is larger, if cnt is odd 
		val rtcnt  = pcnt - lfcnt;
		val rtroot = root + lfcnt;
		if (pcnt > 2) {
			finish {
				if (lfcnt > 1) async {
					reduceSumToHere(ddmat, ddtmp, datCnt, lfcnt); 
				}
				if (rtcnt > 1 ) {
					at(ddmat.dist(rtroot)) async {
						reduceSumToHere(ddmat, ddtmp, datCnt, rtcnt);
					}
				}
			}
		}
		
		val rcvbuf = ddtmp(root);
		x10Copy(ddmat, rtroot, 0, rcvbuf, 0, datCnt);
		for (var i:Long=0; i<datCnt; i++) dstbuf(i) += rcvbuf(i);
	}

	/**
	 * Perform all reduce sum operation. 
	 * @see reduceSum()
	 * Result is synchronized for all copies 
	 *
	 * @param ddmat    input and output. Distributed storage for data arrays in all places. 
	 * @param ddtmp -- temp distributed storage used to receive data.
	 */
	public static def allReduceSum(
			ddmat:DistDataArray,
			ddtmp:DistDataArray, 
			datCnt:Long):void {
		
		x10AllReduceSum(ddmat, ddtmp, datCnt); 
	}

	protected static def x10AllReduceSum(
			ddmat:DistDataArray,
			ddtmp:DistDataArray, 
			datCnt:Long): void {
		
		val root = here.id();
		x10ReduceSum(ddmat, ddtmp, datCnt);
		DistArrayBcast.x10Bcast(ddmat, datCnt);
	}

	// Reduce data from specified places
	
	/**
	 * Perform reduce sum of all array data accessed via PlaceLocalHandle
	 * from specified list of places. This method is not optimized.
	 * Result is stored in the array at root place.
	 * 
	 * @param ddmat    distributed storage for input and output data arrays. 
	 * @param tmp      temp data array storing the inter-place communication data at root.
	 * @param datCnt   column count
	 * @param plist    list of place IDs
	 */
	public static def reduceSum(
			ddmat:DistDataArray,
			tmp:Rail[ElemType], 
			datCnt:Long,
			plist:Rail[Long]):void{
		
		assert (tmp.size >= datCnt) : "Temp data buffer overflow";
		val root = here.id();
		val dstbuf = ddmat(here.id());
		val srcbuf = tmp;
		for (p in plist.range()) {
			if (plist(p) != here.id()) {
				copy(ddmat, plist(p), 0, srcbuf, 0, datCnt);
				for (var i:Long=0; i<datCnt; i++) dstbuf(i) += srcbuf(i);
			}
		}
	}
}


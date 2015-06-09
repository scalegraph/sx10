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
 * This struct is used to pack information of sparse matrix from the source place.
 * The data fields of the struct are captured and copied to here, which are needed
 * by remote array copy to transfer data from the remote place to here.
 */
protected struct SparseRemoteSourceInfo {
	public val idxbuf:GlobalRail[Long];
	public val valbuf:GlobalRail[ElemType];
	public val offset:Long;
	public val length:Long;
	
	public def this(idx:GlobalRail[Long], vlu:GlobalRail[ElemType], off:Long, len:Long) {
		idxbuf = idx; 
		valbuf = vlu;	
		offset = off; 
		length = len;
	}

	public def this(idx:Rail[Long], vlu:Rail[ElemType], off:Long, len:Long) {
		idxbuf = new GlobalRail[Long](idx as Rail[Long]{self!=null}); 
		valbuf = new GlobalRail[ElemType](vlu as Rail[ElemType]{self!=null});	
		offset = off; 
		length = len;
	}
}

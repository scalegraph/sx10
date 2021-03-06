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

package x10.matrix.comm;

import x10.io.Console;
import x10.util.Timer;
import x10.util.ArrayList;

import x10.compiler.Ifdef;
import x10.compiler.Ifndef;
import x10.compiler.Uninitialized;


import x10.matrix.Debug;
//import x10.matrix.comm.mpi.UtilMPI;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;

import x10.matrix.block.Grid;
import x10.matrix.block.BlockMatrix;
import x10.matrix.block.MatrixBlock;

import x10.matrix.distblock.DistBlockMatrix;


/**
 * This class provide scatter communication for distributed matrix. 
 * There is no MPI collective used. All are based on p2p communication
 */
public class BlockScatter extends BlockRemoteCopy {

	//==============================================
	// Constructor
	//==============================================
	public def this() {
		super();
	}
	//==============================================

	//==============================================
	// Dense matrix block scatter
	//==============================================
	/**
	 * Scatter data from matrix blocks from here to distributed blocks in all places.
	 * A local block in the array is sent to a distributed block, which is corresponding
	 * to its distributed array
	 *
	 * @param src     source matrix block array.
	 * @param dst     target distributed matrix blocks 
	 */
	public static def scatter(src:ArrayList[MatrixBlock], dst:BlocksPLH) : void {
		
		val dstgrid = dst().getGrid();
		val nb = dstgrid.size;
		Debug.assure(src.size()<=nb, 
			"Number blocks in dist and local array mismatch");
		
		finish for (var bid:Int=0; bid<nb; bid++) {
			//Debug.flushln("Scatter: copy to block:"+bid);
			copy(src(bid).getMatrix(), dst, bid);
		}
	}


	//======================================================
	/**
	 * Scatter dense matrix at here to distributed dense blocks, partitioned 
	 * in single row blocks.
	 * 
	 * @param gp         single row block partitioning
	 * @param srcden     source dense matrix.
	 * @param dst        target distributed dense block matrix 
	 */
	public static def scatterRowBs(src:Matrix, dst:BlocksPLH): void {

		var coloff:Int=0;
		val gp = dst().getGrid();
		Debug.assure(gp.numRowBlocks==1, "Cannot perform non-single row blocks scatter");
		for (var cb:Int=0; cb<gp.numColBlocks; cb++) {

			val colcnt = gp.colBs(cb);	
			copy(src, coloff, dst, dst().findPlace(cb), cb, 0, colcnt); 
			coloff += colcnt;
		}
	}

	/**
	 * Scatter 1-column matrix (vector) to distributed dense blocks.
	 */
	public static def scatterVector(src:DenseMatrix{self.N==1}, dst:BlocksPLH): void {

		var rowoff:Int=0;
		val gp = dst().getGrid();
		for (var rb:Int=0; rb<gp.numRowBlocks; rb++) {

			val rowcnt = gp.rowBs(rb);
			
			copyOffset(src, rowoff, dst, dst().findPlace(rb), rb, 0, rowcnt); 
			rowoff += rowcnt;
		}
	}
}

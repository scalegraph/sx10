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
 * This class provide gather communication for distributed block matrix.
 * There is no MPI collective used. All are based on p2p communication.
 */
public class BlockGather extends BlockRemoteCopy {

	//==============================================
	// Constructor
	//==============================================
	public def this() {
		super();
	}
	//==============================================


	/**
	 * Gather blocks from distributed BlockSet in all places to block matrix
	 * at here.
	 * 
	 * @param src     source distributed matrix blocks
	 * @param dst     target matrix block array 
	 */
	public static def gather(src:BlocksPLH, dst:ArrayList[MatrixBlock]) : void {
		
		val grid   = src().getGrid();
		Debug.assure(dst.size() >= grid.size,
				"Not enough blocks at receiving side"); 
		
		finish for (var bid:Int=0; bid<grid.size; bid++) {
			val dstmat = dst(bid).getMatrix();
			BlockRemoteCopy.copy(src, src().findPlace(bid), bid, 0, 
					dstmat, 0, dstmat.N);
		}
	}
	
	/**
	 * Copy distributed dense matrix blocks from all places to the dense matrix
	 * at here.
	 * 
	 * @param gp     single row block partitioning
	 * @param src     source matrix, distributed in all places.
	 * @param dstden     target dense matrix at here
	 */
	public static def gatherRowBs(src:BlocksPLH, dst:Matrix): void {

		val gp = src().getGrid();
		var coloff:Int=0;
		Debug.assure(gp.numRowBlocks==1, "Cannot perform non-single row blocks gather");
		
		for (var cb:Int=0; cb<gp.numColBlocks; cb++) {

			val colcnt = gp.colBs(cb);
			
			BlockRemoteCopy.copy(src, src().findPlace(cb), cb, 0, dst, coloff, colcnt); 
			coloff += colcnt;
		}
	}
	
	/**
	 * Gather distrubuted vector (single-column) matrix to here
	 * in a vector. Only dense format is allowed
	 */
	public static def gatherVector(src:BlocksPLH, dst:DenseMatrix{self.N==1}): void {

		val gp = src().getGrid();
		var rowoff:Int=0;
		for (var rb:Int=0; rb<gp.numRowBlocks; rb++) {

			val rowcnt = gp.rowBs(rb);
			
			BlockRemoteCopy.copyOffset(src, src().findPlace(rb), rb, 0, dst, rowoff, rowcnt); 

			rowoff += rowcnt;
		}
	}


}

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

package x10.matrix.distblock;

import x10.util.Timer;

import x10.matrix.Matrix;
import x10.matrix.DenseMatrix;
import x10.matrix.Debug;

import x10.matrix.block.Grid;
import x10.matrix.block.MatrixBlock;
import x10.matrix.block.BlockBlockMult;

import x10.matrix.distblock.DistMap;
import x10.matrix.distblock.BlockSet;
import x10.matrix.distblock.DistBlockMatrix;
import x10.matrix.distblock.DupBlockMatrix;

/**
 * Block matrix distributed in (1, n) places multiply block matrix distributed in (n, 1)
 * or (n, 1) trans-multiply (n, 1), or (1, n) multiply-trans (1, n) 
 * Result is stored in Duplicated block matrix at here.
 */
public class DistDistMult {
	
	/**
	 * 
	 */
	public static def mult(
			A:DistBlockMatrix, 
			B:DistBlockMatrix(A.N), 
			C:DupBlockMatrix(A.M,B.N), plus:Boolean) : DupBlockMatrix(C) {

		val gA = A.getGrid();
		val gB = B.getGrid();
		val gC = C.getGrid();
		
		//Global.assure(A.flagTranspose == true);
		Debug.assure(Grid.match(gA.rowBs, gC.rowBs),
				"Row partition of first and result matrix mismatch");
		Debug.assure(Grid.match(gB.colBs, gC.colBs),
				"Column partition of second and result matrix mismatch");
		
		finish ateach (Dist.makeUnique()) {
			//
			val bsA = A.handleBS();
			val bsB = B.handleBS();
			val bsC = C.local();
			bsA.buildBlockMap(); //bsA.printBlockMap();
			bsB.buildBlockMap();
			bsC.buildBlockMap(); //bsC.printBlockMap();
			BlockBlockMult.mult(bsA.blockMap, bsB.blockMap, bsC.blockMap, plus);
			//BlockBlockMult.mult(bsA.blocklist, bsB.blocklist, bsC, plus);
		}
		C.allReduceSum();
		return C;
	}
	
	public static def transMult(
			A:DistBlockMatrix, 
			B:DistBlockMatrix(A.M), 
			C:DupBlockMatrix(A.N,B.N), plus:Boolean) : DupBlockMatrix(C) {

		val gA = A.getGrid();
		val gB = B.getGrid();
		val gC = C.getGrid();
	
		Debug.assure(Grid.match(gA.colBs, gC.rowBs),
		"Column partition of first and result matrix mismatch");
		Debug.assure(Grid.match(gB.colBs, gC.colBs),
		"Column partition of second and result matrix mismatch");
		
		finish ateach (Dist.makeUnique()) {
			//
			val bsA = A.handleBS();
			val bsB = B.handleBS();
			val bsC = C.local();
			bsA.buildBlockMap();
			bsB.buildBlockMap();
			bsC.buildBlockMap();
			BlockBlockMult.transMult(bsA.blockMap, bsB.blockMap, bsC.blockMap, plus);
			//BlockBlockMult.transMult(bsA.blocklist, bsB.blocklist, bsC, plus);
		}
		
		C.allReduceSum(); 
		return C;

	}
	
	public static def multTrans(
			A:DistBlockMatrix, 
			B:DistBlockMatrix{self.N==A.N},
			C:DupBlockMatrix(A.M,B.M), plus:Boolean) : DupBlockMatrix(C) {

		val gA = A.getGrid();
		val gB = B.getGrid();
		val gC = C.getGrid();
	
		Debug.assure(Grid.match(gA.rowBs, gC.rowBs),
		"Row partition of first and result matrix mismatch");
		Debug.assure(Grid.match(gB.rowBs, gC.colBs),
		"Row partition of second and result matrix mismatch");
		
		finish ateach (Dist.makeUnique()) {
			//
			val bsA = A.handleBS();
			val bsB = B.handleBS();
			val bsC = C.local();
			bsA.buildBlockMap();
			bsB.buildBlockMap();
			bsC.buildBlockMap();
			BlockBlockMult.multTrans(bsA.blockMap, bsB.blockMap, bsC.blockMap, plus);
			//BlockBlockMult.multTrans(bsA.blocklist, bsB.blocklist, bsC, plus);
		}
		
		C.allReduceSum(); 
		return C;	

	}
	//==================================================
	
}

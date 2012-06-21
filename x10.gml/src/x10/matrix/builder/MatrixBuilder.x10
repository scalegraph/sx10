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

package x10.matrix.builder;

import x10.matrix.Matrix;

public type MatrixBuilder(blder:MatrixBuilder)=MatrixBuilder{self==blder};

public interface MatrixBuilder {
	
	//-----------------------------------------------------------------
	public def init(initFun:(Int,Int)=>Double):MatrixBuilder(this);

	public def initRandom(nonZeroDensity:Double, initFun:(Int,Int)=>Double):MatrixBuilder(this);

	//public def initRandomTri(up:Boolean):MatrixBuilder;
	//public def initRandomTri(halfNonZeroDensity:Double, up:Boolean):MatrixBuilder(this);
	//public def initRandomSym(halfNonZeroDensity:Double):MatrixBuilder(this);
	//-----------------------------------------------------------------
	public def set(r:Int, c:Int, value:Double): void;
	public def reset(r:Int, c:Int):Boolean;
	
	//-----------------------------------------------------------------
	//public def makeTranspose(mat:Matrix):void;
	
	public def toMatrix():Matrix;

	//-----------------------------------------------------------------

}
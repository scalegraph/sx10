/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2012.
 */

import x10.io.Console;

import x10.matrix.Debug;
import x10.matrix.MathTool;
import x10.matrix.DenseMatrix;

import x10.matrix.block.Grid;
import x10.matrix.block.BlockMatrix;
import x10.matrix.block.DenseBlockMatrix;

import x10.matrix.distblock.DistMap;
import x10.matrix.distblock.DistGrid;
import x10.matrix.distblock.DistBlockMatrix;
import x10.matrix.distblock.DupBlockMatrix;
import x10.matrix.distblock.DistDupMult;
import x10.matrix.distblock.DistDistMult;
import x10.matrix.distblock.summa.SummaMult;

/**
   <p>
 * Examples of distributed block matrix
   <p>
 */
public class MatMatMult {
	
    public static def main(args:Array[String](1)) {
		val testcase = new MatMatMult(args);
		testcase.run();
	}
	public val nzp:Double;
	public val M:Int;
	public val N:Int;
	public val K:Int;
	public val bM:Int;
	public val bK:Int;
	public val bN:Int;
	public val vrf:Boolean;

    public def this(args:Array[String](1)) {
		M = args.size > 0 ?Int.parse(args(0)):30;
		N = args.size > 1 ?Int.parse(args(1)):M+1;
		K = args.size > 2 ?Int.parse(args(2)):M+2;
		bM= args.size > 3 ?Int.parse(args(3)):Place.MAX_PLACES+1;
		bK= args.size > 4 ?Int.parse(args(4)):bM+1;
		bN= args.size > 5 ?Int.parse(args(5)):Place.MAX_PLACES+15;
		nzp = args.size > 6 ?Double.parse(args(6)):0.9;
		vrf = args.size > 7 ?false:true;
		
		Console.OUT.printf("Matrix dimensions M:%d K:%d N:%d, blocking:(%d, %d) \n", M, N, K, bM, bN);
		
	}

    public def run ():void {
    	
		var ret:Boolean = true;
 		// Set the matrix function
		ret &= (demoDistDupMult());
		ret &= (demoDistDistMultToDup());
		ret &= (demoDistDistMultToDup2());
		ret &= (demoDistDistSUMMA());

	}


    public def demoDistDupMult():Boolean{
    	Console.OUT.println("Starting Dist-Dup block matrix multiply. Dist matrix must have vertical distribution");
    	val pM = Place.MAX_PLACES	;
    	val pN = 1;
    	val A = DistBlockMatrix.makeDense(M, K, bM, bK, pM, pN).initRandom();
     	val B = DupBlockMatrix.makeDense(K, N, bK, bN).initRandom();
    	val C = DistBlockMatrix.makeDense(M, N, bM, bN, pM, pN);
    	
    	DistDupMult.comp(A, B, C, false);

    	if (vrf) {
    		val c = A.toDense() % B.toDense();
    		if (c.equals(C as Matrix(C.M, C.N))) 
    			Console.OUT.println("Dist-Dup Block matrix multiply verified");
    		else {
    			Console.OUT.println("--------ERROR: Dist-Dup Block matrix multiply!--------");
    			return false;
    		}
    	}
    	return true;
    }
    
    public def demoDistDistMultToDup():Boolean{
    	Console.OUT.println("Starting DistMatrix-DistMatrix multiply.");
    	Console.OUT.println("First DistMatrix has horizontal and second has vertical distribution");
    	
    	val A = DistBlockMatrix.makeDense(M, K, bM, bK, 1, Place.MAX_PLACES).initRandom();
    	val B = DistBlockMatrix.makeDense(K, N, bK, bN, Place.MAX_PLACES,1).initRandom();
    	val C = DupBlockMatrix.makeDense(M, N, bM, bN);
    	
    	DistDistMult.mult(A, B, C, false);

    	if (vrf) {
    		val c = A.toDense() % B.toDense();
    		if (c.equals(C))
    			Console.OUT.println("Dist-Dist Block matrix multiply verified");
    		else {
    			Console.OUT.println("--------ERROR Dist Block matrix multiply!--------");
    			return false;
    		}
    	}
    	return true;
    }

    public def demoDistDistMultToDup2():Boolean{
    	Console.OUT.println("Starting DistMatrix-DistMatrix multiply demo 2.");
    	Console.OUT.println("First DistMatrix has horizontal and second has vertical distribution");
    	
    	//val gPartA = new Grid(M, K, bM, bK); may not be balanced in row-wise partitioning
    	val gPartA = DistGrid.makeGrid(M, K, bM, bK, 1, Place.MAX_PLACES);
    	val gDistA = DistGrid.makeHorizontal(gPartA);
    	val A = DistBlockMatrix.makeDense(gPartA, gDistA.dmap).initRandom() as DistBlockMatrix(M,K);

    	//val gPartB = new Grid(K, N, bK, bN); may not be balanced in column-wise partitioning
    	val gPartB = DistGrid.makeGrid(K, N, bK, bN, Place.MAX_PLACES, 1);
    	val gDistB = DistGrid.makeVertical(gPartB);
    	val B = DistBlockMatrix.makeDense(gPartB, gDistB).initRandom() as DistBlockMatrix(K,N);

    	val C = DupBlockMatrix.makeDense(M, N, bM, bN);
    	
    	DistDistMult.mult(A, B, C, false);

    	val A1 = DistBlockMatrix.makeDense(gPartA, gDistA);
    	
    	if (vrf) {
    		val c = A.toDense() % B.toDense();
    		if (c.equals(C))
    			Console.OUT.println("Dist-Dist Block matrix multiply demo 2 verified");
    		else {
    			Console.OUT.println("--------ERROR Dist Block matrix multiply demo 2!--------");
    			return false;
    		}
    	}
    	return true;
    }

    public def demoDistDistSUMMA():Boolean {
    	Console.OUT.println("Demo of using SUMMA for DistMatrix-DistMatrix multiplication");
    	val pM = MathTool.sqrt(Place.MAX_PLACES);
    	val pN = Place.MAX_PLACES/pM;
    	   	
    	Console.OUT.printf("matrix (%dx%d) x (%dx%d) partitioned in (%dx%d) blocks ",
    			M, K, K, N, bM, bN);
    	
    	var ret:Boolean = true;
    	val A = DistBlockMatrix.makeDense(M, K, bM, bK, pM, pN).initRandom();
    	val B = DistBlockMatrix.makeSparse(K, N, bK, bN, pM, pN, nzp).initRandom();
    	val C = DistBlockMatrix.makeDense(M, N, bM, bN, pM, pN);

    	Console.OUT.printf("Starting SUMMA DistBlockMatrix * DistBlockMatrix\n");
    	SummaMult.mult(A, B, C, false);

    	if (vrf) {
    		Console.OUT.printf("Start verification\n");
    	
    		val c= A.toDense() % B.toDense();
    		if (c.equals(C))
    			Console.OUT.println("Distributed block matrix SUMMA multiplication verified");
    		else {
    			Console.OUT.println("--------ERROR: Distributed sparse block matrix SUMMA mult test failed!--------");
    			return false;
    		}
    	}
    		
    	return true;
    }

} 

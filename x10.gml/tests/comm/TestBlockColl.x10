/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011.
 */

import x10.io.Console;
import x10.util.Timer;
import x10.array.DistArray;


import x10.matrix.Matrix;
import x10.matrix.Debug;
import x10.matrix.DenseMatrix;
import x10.matrix.sparse.SparseCSC;
import x10.matrix.block.MatrixBlock;
import x10.matrix.block.BlockMatrix;
import x10.matrix.block.Grid;
import x10.matrix.distblock.DistBlockMatrix;		

import x10.matrix.comm.BlockBcast;
import x10.matrix.comm.BlockScatter;
import x10.matrix.comm.BlockGather;
import x10.matrix.comm.BlockRingCast;


/**
   This class contains test cases P2P communication for matrix over different places.
   <p>

   <p>
 */

public class TestBlockColl{
    public static def main(args:Array[String](1)) {
		val m = args.size > 0 ?Int.parse(args(0)):2;
		val n = args.size > 1 ?Int.parse(args(1)):2;
		val bm= args.size > 2 ?Int.parse(args(2)):2;
		val bn= args.size > 3 ?Int.parse(args(3)):3;
		val d = args.size > 4 ? Double.parse(args(4)):0.80;
		val testcase = new BlockCollTest(m, n, bm, bn, d);
		testcase.run();
	}
}


class BlockCollTest {

	public val M:Int;
	public val N:Int;
	public val nzdensity:Double;
	public val bM:Int;
	public val bN:Int;
	
	public val numplace:Int;

	public val dbmat:DistBlockMatrix;
	public val sbmat:DistBlockMatrix;
	
	public val dblks:BlockMatrix;
	public val sblks:BlockMatrix;
	
	public val rootbid:Int = 0;
	
    public def this(m:Int, n:Int, bm:Int, bn:Int, d:Double) {

		M=m; N=n;
		nzdensity = d;
		bM = bm; bN = bn;
		
		dbmat = DistBlockMatrix.makeDense(m*bm, n*bn, bm, bn);
		sbmat = DistBlockMatrix.makeSparse(m*bm, n*bn, bm, bn, nzdensity);
		
		dbmat.initBlock(rootbid, (x:Int, y:Int)=>(1.0+x+y));
		sbmat.initBlock(rootbid, (x:Int, y:Int)=>1.0*(x+y)*((x+y)%2));

		dblks = BlockMatrix.makeDense(dbmat.getGrid());
		sblks = BlockMatrix.makeSparse(sbmat.getGrid(), nzdensity);
		
		numplace = Place.numPlaces();
	}
	
	public def run(): void {
 		// Set the matrix function
		var retval:Boolean = true;
		Console.OUT.println("Test dense blocks collective commu in distributed block matrix");

		retval &= testBcast(dbmat);
		retval &= testGather(dbmat, dblks);
		retval &= testScatter(dblks, dbmat);
		retval &= testRingCastRow(dbmat);
		retval &= testRingCastCol(dbmat);

		Console.OUT.println("");
		Console.OUT.println("Test sparse blocks collective commu in distributed block matrix");	
		retval &= testBcast(sbmat);
		retval &= testGather(sbmat, sblks);
		retval &= testScatter(sblks, sbmat);
		retval &= testRingCastRow(sbmat);
		retval &= testRingCastCol(sbmat);		
		if (retval) 
			Console.OUT.println("Block communication test collective commu passed!");
		else
			Console.OUT.println("------------Block communication test collective commu failed!-----------");
	}
	//------------------------------------------------
	//------------------------------------------------
	public def testBcast(bmat:DistBlockMatrix):Boolean {
		var ret:Boolean = true;
		var ds:Int = 0;
		var avgt:Double=0;
		Console.OUT.println("\nTest Bcast on dist block matrix, each block ("+M+"x"+N+") "+
				"("+bM+","+bN+") blocks over "+ numplace+" places");
		
		//bmat.fetchBlock(rootbid).print("BCast root");
		for (var rtbid:Int=0; rtbid < bmat.getGrid().size; rtbid++) {
			Console.OUT.println("Bcast from root block "+rtbid);
			bmat.reset();
			bmat.initBlock(rtbid, (r:Int, c:Int)=>(1.0+r+c)*((r+c)%2));
			val st:Long =  Timer.milliTime();
			BlockBcast.bcast(bmat.handleBS, rtbid);
			avgt += (Timer.milliTime() - st);
			//bmat.printMatrix();
			ret &= dbmat.syncCheck();
		}
	
		Console.OUT.printf("Bcast %d bytes average time: %.3f ms\n", 
						   ds*8, avgt/bmat.getGrid().size);
		
		//ret = dbmat.syncCheck();
		if (ret)
			Console.OUT.println("Bcast dist block matrix passed!");
		else
			Console.OUT.println("--------Bcast block matrix test failed!--------");
		
		return ret;

	} 	
	
	public def testGather(distmat:DistBlockMatrix, blksmat:BlockMatrix):Boolean {
		var ret:Boolean = true;
		
		Console.OUT.printf("\nTest gather of dist block matrix over %d places\n", numplace);
		blksmat.reset();
		
		Debug.flushln("Start gathering "+numplace+" places");
		var st:Long =  Timer.milliTime();
		BlockGather.gather(distmat.handleBS, blksmat.listBs);
		//Debug.flushln("Done");
		
		ret = distmat.equals(blksmat as Matrix(distmat.M, distmat.N));
		Debug.flushln("Done verification");

		if (ret)
			Console.OUT.println("Test gather for dist block matrix test passed!");
		else
			Console.OUT.println("-----Test gather for dist block matrix failed!-----");
		return ret;
	}
	
	public def testScatter(blksmat:BlockMatrix, distmat:DistBlockMatrix):Boolean {
		var ret:Boolean = true;
		
		Console.OUT.printf("\nTest scatter of dist block matrix over %d places\n", numplace);
		distmat.reset();
		
		Debug.flushln("Start scatter among "+numplace+" places");
		var st:Long =  Timer.milliTime();
		BlockScatter.scatter(blksmat.listBs, distmat.handleBS);
		//Debug.flushln("Done");
		
		ret = distmat.equals(blksmat as Matrix(distmat.M,distmat.N));
		Debug.flushln("Done verification");

		if (ret)
			Console.OUT.println("Test scatter for dist block matrix test passed!");
		else
			Console.OUT.println("-----Test scatter for dist block matrix failed!-----");
		return ret;
	}
	
	public def testRingCastRow(distmat:DistBlockMatrix):Boolean {
		var ret:Boolean = true;
		val grid = distmat.getGrid();

		Console.OUT.printf("\nTest ring cast row-wise of dist block matrix over %d places\n", numplace);
		distmat.reset();
		var cid:Int =0;
		
		for (var rid:Int=0; rid<grid.numRowBlocks; rid++, cid=(cid+1)%grid.numColBlocks) {
			val rtbid  = grid.getBlockId(rid, cid);
			val colcnt = grid.getColSize(rtbid);
			
			distmat.initBlock(rtbid, (r:Int,c:Int)=>1.0*((r+c)%2)*(r+c+1.0));
			//distmat.printMatrix("Init");
			BlockRingCast.rowCast(distmat.handleBS, rtbid, colcnt);
			//distmat.printMatrix("row cast result");
			
		}
		
		ret = distmat.syncCheck();
		Debug.flushln("Done verification");

		if (ret)
			Console.OUT.println("Test ringcast row-wise for dist block matrix test passed!");
		else
			Console.OUT.println("-----Test ringcast row-wise for dist block matrix failed!-----");
		return ret;
	}
	
	public def testRingCastCol(distmat:DistBlockMatrix):Boolean {
		var ret:Boolean = true;
		val grid = distmat.getGrid();

		Console.OUT.printf("\nTest ring cast column-wise of dist block matrix over %d places\n", numplace);
		distmat.reset();
		var rid:Int =0;
		
		for (var cid:Int=0; cid<grid.numColBlocks; cid++, rid=(rid+1)%grid.numRowBlocks) {
			val rtbid  = grid.getBlockId(rid, cid);
			val colcnt = grid.getColSize(rtbid);
			//Debug.flushln("Set root block:"+rtbid+"("+rid+","+cid+")");

			distmat.initBlock(rtbid, (r:Int,c:Int)=>1.0*((r+c)%2)*(r+c+1.0));
			//distmat.printMatrix("Init root block");
			BlockRingCast.colCast(distmat.handleBS, rtbid, colcnt);
			//distmat.printMatrix("");
		}
		
		ret = distmat.syncCheck();
		Debug.flushln("Done verification");

		if (ret)
			Console.OUT.println("Test ringcast column-wise for dist block matrix test passed!");
		else
			Console.OUT.println("-----Test ringcast column-wise for dist block matrix failed!-----");
		return ret;
	}
	
}
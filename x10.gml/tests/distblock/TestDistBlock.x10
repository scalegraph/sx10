/*
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011.
 */

import x10.compiler.Ifndef;

import x10.matrix.util.Debug;
import x10.matrix.DenseMatrix;
import x10.matrix.block.Grid;
import x10.matrix.block.BlockMatrix;
import x10.matrix.distblock.DistMap;
import x10.matrix.distblock.DistGrid;
import x10.matrix.distblock.DistBlockMatrix;

public class TestDistBlock {
    public static def main(args:Rail[String]) {
		val testcase = new TestDB(args);
		testcase.run();
	}
}

class TestDB {
	public val nzp:Double;
	public val M:Long;
	public val N:Long;
	public val K:Long;
	public val bM:Long;
	public val bN:Long;

	public val grid:Grid;
	public val dmap:DistMap;
	
    public def this(args:Rail[String]) {
		M = args.size > 0 ? Long.parse(args(0)):30;
		nzp = args.size > 1 ?Double.parse(args(1)):0.9;
		N = args.size > 2 ? Long.parse(args(2)):(M as Int)+1;
		K = args.size > 3 ? Long.parse(args(3)):(M as Int)+2;
		bM= args.size > 4 ? Long.parse(args(4)):4;
		bN= args.size > 5 ? Long.parse(args(5)):5;
		
		grid = new Grid(M, N, bM, bN);
		dmap = DistGrid.make(grid).dmap; 
		Console.OUT.printf("Matrix M:%d K:%d N:%d, blocks(%d, %d) on %d places\n", M, N, K, bM, bN, Place.MAX_PLACES);
	}

    public def run (): void {
		Console.OUT.println("DistBlockMatrix clone/add/sub/scaling tests");

		var ret:Boolean = true;
	@Ifndef("MPI_COMMU") { // TODO Deadlocks!
		ret &= (testClone());
		ret &= (testCopyTo());
		ret &= (testScale());
		ret &= (testAdd());
		ret &= (testAddSub());
		ret &= (testAddAssociative());
		ret &= (testScaleAdd());
		ret &= (testCellMult());
		ret &= (testCellDiv());
    }
		if (!ret)
			Console.OUT.println("----------------Test failed!----------------");
	}
	public def testClone():Boolean{
		var ret:Boolean = true;
		Console.OUT.println("DistBlockMatrix clone test on dense blocks");
		val ddm = DistBlockMatrix.makeDense(grid, dmap).init((r:Long, c:Long)=>(1.0+r+c));
		
		val ddm1 = ddm.clone();
		ret = ddm.equals(ddm1);
		
		val den = DenseMatrix.make(grid.M, grid.N).init((r:Long,c:Long)=>(1.0+r+c));
		ret &= den.equals(ddm);
		
		if (!ret)
		 	Console.OUT.println("--------DistBlockMatrix Clone test failed!--------");
		return ret;
	}

	public def testCopyTo():Boolean {
		var ret:Boolean = true;
		Console.OUT.println("DistBlockMatrix copyTo test");
		val dstblk = DistBlockMatrix.makeDense(grid, dmap);
		val blkden = BlockMatrix.makeDense(grid);
		val den    = DenseMatrix.make(M,N);

		dstblk.initRandom();
		dstblk.copyTo(blkden);
		ret &= dstblk.equals(blkden as Matrix(dstblk.M, dstblk.N));
		if (! ret)  return ret;
		
		dstblk.reset();
		dstblk.copyFrom(blkden);
		ret &= blkden.equals(dstblk as Matrix(blkden.M,blkden.N));
		if (! ret) return ret;

		val dmat = DistBlockMatrix.make(M, 1, bM, 1, Place.MAX_PLACES, 1).allocDenseBlocks().initRandom();
		val denm = DenseMatrix.make(M, 1);
		
		dmat.copyTo(denm);
		ret &= dmat.equals(denm as Matrix(dmat.M, dmat.N));
		if (! ret) return ret;
		
		
		if (!ret)
			Console.OUT.println("--------Dist dense matrix copyTo test failed!--------");	
		return ret;
	}

 	public def testScale():Boolean{
 		Console.OUT.println("DistBlockMatrix scaling test");
 		val dm = DistBlockMatrix.make(M, N, bM, bN).allocDenseBlocks().initRandom();

 		val dm1  = dm * 2.5;
 		val m = dm.toDense();
 		val m1 = m * 2.5;
 		val ret = dm1.equals(m1);
 		if (!ret)
 			Console.OUT.println("--------Dist block matrix Scaling test failed!--------");	
 		return ret;
 	}
 
	public def testAdd():Boolean {
		Console.OUT.println("DistBlockMatrix add test");
		val dm = DistBlockMatrix.make(M, N, bM, bN).allocDenseBlocks().initRandom();

		val dm1 = dm  * -1.0;
		val dm0 = dm + dm1;
		val ret = dm0.equals(0.0);
		if (!ret)
			Console.OUT.println("--------DistBlockMatrix Add: dm + dm*-1 test failed--------");
		return ret;
	}

	public def testAddSub():Boolean {
		Console.OUT.println("DistBlockMatrix add-sub test");
		val dm = DistBlockMatrix.makeDense(grid, dmap).initRandom();
		val dm1= DistBlockMatrix.makeDense(grid, dmap).initRandom();

		val dm2   = dm  + dm1;
		val dm_c  = dm2 - dm1;
		val ret   = dm.equals(dm_c as Matrix(dm.M, dm.N));
		if (!ret)
			Console.OUT.println("--------DistBlockMatrix Add-sub test failed!--------");
		return ret;
	}


	public def testAddAssociative():Boolean {
		Console.OUT.println("DistBlockMatrix associative test");

		val a = DistBlockMatrix.makeDense(grid, dmap).initRandom();
		val b = DistBlockMatrix.makeDense(grid, dmap).initRandom();;
		val c = DistBlockMatrix.makeSparse(grid, dmap, nzp).initRandom();

		val c1 = a + b + c;
		val c2 = a + (b + c);
		val ret = c1.equals(c2);
		if (!ret)
			Console.OUT.println("--------DistBlockMatrix Add associative test failed!--------");
		return ret;
	}

	public def testScaleAdd():Boolean {
		Console.OUT.println("DistBlockMatrix scaling-add test");

		val a = DistBlockMatrix.makeDense(grid, dmap).initRandom();

		val m = a.toDense();
		val a1= a * 0.2;
		val a2= 0.8 * a;
		var ret:Boolean = a.equals(a1+a2);
		ret &= a.equals(m);

		if (!ret)
			Console.OUT.println("--------DistBlockeMatrix scaling-add test failed!--------");
		return ret;
	}

	public def testCellMult():Boolean {
		Console.OUT.println("DistBlockMatrix cellwise mult test");

		val a = DistBlockMatrix.makeDense(grid, dmap).initRandom();
		val b = DistBlockMatrix.makeDense(grid, dmap).initRandom();

		val c = (a + b) * a;
		val d = a * a + b * a;
		var ret:Boolean = c.equals(d);

		val da= a.toDense();
		val db= b.toDense();
		val dc= (da + db) * da;
		ret &= dc.equals(c);

		if (!ret)
			Console.OUT.println("--------Dist block matrix cellwise mult test failed!--------");
		return ret;
	}

	public def testCellDiv():Boolean {
		Console.OUT.println("DistBlockMatrix cellwise mult-div test");

		val a = DistBlockMatrix.makeDense(grid, dmap).initRandom();
		val b = DistBlockMatrix.makeDense(grid, dmap).initRandom();

		val c = (a + b) * a;
		val d =  c / (a + b);
		var ret:Boolean = d.equals(a);

		if (!ret)
			Console.OUT.println("--------Dist block matrix cellwise mult-div test failed!--------");
		return ret;
	}
} 

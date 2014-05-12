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

import x10.matrix.util.Debug;
import x10.matrix.DenseMatrix;

import x10.matrix.dist.DistDenseMatrix;
import x10.matrix.dist.DistMatrix;

public class TestDistMatrix {
    public static def main(args:Rail[String]) {
		val testcase = new TestDM(args);
		testcase.run();
	}
}

class TestDM {
	public val nzp:Double;
	public val M:Long;
	public val N:Long;
	public val K:Long;	

    public def this(args:Rail[String]) {
		M = args.size > 0 ? Long.parse(args(0)):10;
		nzp = args.size > 1 ?Double.parse(args(1)):0.5;
		N = args.size > 2 ? Long.parse(args(2)):(M as Int)+1;
		K = args.size > 3 ? Long.parse(args(3)):(M as Int)+2;	
	}

    public def run (): void {
		Console.OUT.println("Starting DistMatrix clone/add/sub/scaling tests");
		Console.OUT.printf("Matrix M:%d K:%d N:%d\n", M, N, K);

		var ret:Boolean = true;
 		// Set the matrix function
		ret &= (testClone());
		ret &= (testCopyTo());
		ret &= (testScale());
		ret &= (testAdd());
		ret &= (testAddSub());
		ret &= (testAddAssociative());
		ret &= (testScaleAdd());
		ret &= (testCellMult());
		ret &= (testCellDiv());

		if (ret)
			Console.OUT.println("Test passed!");
		else
			Console.OUT.println("----------------Test failed!----------------");
	}
	public def testClone():Boolean{
		var ret:Boolean = true;
		Console.OUT.println("Starting dist matrix clone test");
		val ddm = DistMatrix.makeDense(M, N);
		Debug.flushln("Starting initialization");
		ddm.initRandom();
		Debug.flushln("DistMatrix initialization done");

		val ddm1 = ddm.clone();
		//Debug.flushln("Clone done");
		ret = ddm.equals(ddm1);
		//Debug.flushln("Equal test done");
		
		if (ret)
			Console.OUT.println("DistMatrix Clone test passed!");
		else
			Console.OUT.println("--------DistMatrix Clone test failed!--------");

                ddm(1, 1) = ddm(2,2) = 10.0;

                if ((ddm(1,1)==ddm(2,2)) && (ddm(1,1)==10.0)) {
                        ret &= true;
                        Console.OUT.println("Dist Matrix chain assignment test passed!");
                } else {
                        ret &= false;
                        Console.OUT.println("---------- Dist Matrix chain assignment test failed!-------");
                }

		return ret;
	}

	public def testCopyTo():Boolean {
		var ret:Boolean = true;
		Console.OUT.println("Starting DistMatrix copyTo DistDenseMatrix test");
		val distmat = DistMatrix.makeDense(M, N);
		val ddm = DistDenseMatrix.make(distmat.grid);
		val dm  = DenseMatrix.make(M,N);

		Debug.flushln("Starting initialization");
		ddm.initRandom();
		Debug.flushln("Initialization done");

		distmat.copyTo(ddm);
		Debug.flushln("Convert to dist dense block matrix done");
		ret &= distmat.equals(ddm);
		Debug.flushln("Verify copyTo dense block matrix  done");
		
	
		if (ret)
			Console.OUT.println("Dist dense Matrix copyTo test passed!");
		else
			Console.OUT.println("--------Dist dense matrix copyTo test failed!--------");	
		return ret;
	}

	public def testScale():Boolean{
		Console.OUT.println("Starting DistMatrix scaling test");
		val dm = DistMatrix.makeDense(M, N);
		dm.initRandom();
		val dm1  = dm * 2.5;
		val m = dm.toDense();
		val m1 = m * 2.5;
		
		val ret = dm1.equals(m1);
		if (ret)
			Console.OUT.println("DistMatrix scaling test passed!");
		else
			Console.OUT.println("--------DistMatrix Scaling test failed!--------");	
		return ret;
	}

	public def testAdd():Boolean {
		Console.OUT.println("Starting DistMatrix add test");
		val dm = DistMatrix.makeDense(M, N);
		dm.initRandom();
		val dm1 = dm * -1.0;
		val dm0 = dm + dm1;
		val ret = dm0.equals(0.0);
		if (ret)
			Console.OUT.println("DistMatrix Add: dm + dm*-1 test passed");
		else
			Console.OUT.println("--------DistMatrix Add: dm + dm*-1 test failed--------");
		return ret;
	}

	public def testAddSub():Boolean {
		Console.OUT.println("Starting DistMatrix add-sub test");
		val dm = DistMatrix.makeDense(M, N);
		dm.initRandom();
		val dm1= DistMatrix.makeDense(dm.grid);
		dm.initRandom();
		val dm2= dm  + dm1;
		val dm_c  = dm2 - dm1;
		val ret   = dm.equals(dm_c);
		if (ret)
			Console.OUT.println("DistMatrix Add-sub test passed!");
		else
			Console.OUT.println("--------DistMatrix Add-sub test failed!--------");
		return ret;
	}


	public def testAddAssociative():Boolean {
		Console.OUT.println("Starting DistMatrix associative test");

		val a = DistMatrix.makeDense(M, N);
		val b = DistMatrix.makeDense(a.grid);
		val c = DistMatrix.makeDense(a.grid);
		a.initRandom();
		b.initRandom();
		c.initRandom();
		val c1 = a + b + c;
		val c2 = a + (b + c);
		val ret = c1.equals(c2);
		if (ret)
			Console.OUT.println("DistMatrix Add associative test passed!");
		else
			Console.OUT.println("--------DistMatrix Add associative test failed!--------");
		return ret;
	}

	public def testScaleAdd():Boolean {
		Console.OUT.println("Starting DistMatrix scaling-add test");

		val a = DistMatrix.makeDense(M, N);
		a.initRandom();

		val m = a.toDense();
		val a1= a * 0.2;
		val a2= 0.8 * a;
		var ret:Boolean = a.equals(a1+a2);
		ret &= a.equals(m);

		if (ret)
			Console.OUT.println("DistMatrix scaling-add test passed!");
		else
			Console.OUT.println("--------DistMatrix scaling-add test failed!--------");
		return ret;
	}

	public def testCellMult():Boolean {
		Console.OUT.println("Starting DistMatrix cellwise mult test");

		val a = DistMatrix.makeDense(M, N);
		val b = DistMatrix.makeDense(a.grid);
		a.initRandom();
		b.initRandom();

		val c = (a + b) * a;
		val d = a * a + b * a;
		var ret:Boolean = c.equals(d);

		val da= a.toDense();
		val db= b.toDense();
		val dc= (da + db) * da;
		ret &= dc.equals(c);

		if (ret)
			Console.OUT.println("DistMatrix cellwise mult passed!");
		else
			Console.OUT.println("--------DistMatrix cellwise mult test failed!--------");
		return ret;
	}

	public def testCellDiv():Boolean {
		Console.OUT.println("Starting DistMatrix cellwise mult-div test");

		val a = DistMatrix.makeDense(M, N);
		val b = DistMatrix.makeDense(a.grid);
		a.initRandom();
		b.initRandom();

		val c = (a + b) * a;
		val d =  c / (a + b);
		var ret:Boolean = d.equals(a);

		if (ret)
			Console.OUT.println("DistMatrix cellwise mult-div passed!");
		else
			Console.OUT.println("--------DistMatrix cellwise mult-div test failed!--------");
		return ret;
	}



} 

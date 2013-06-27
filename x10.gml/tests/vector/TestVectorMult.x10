/**
 *  This file is part of the X10 Applications project.
 *
 *  (C) Copyright IBM Corporation 2011.
 *  (C) Copyright Australian National University 2011.
 */

import x10.matrix.Vector;
import x10.matrix.Debug;

import x10.matrix.DenseMatrix;
import x10.matrix.SymDense;
import x10.matrix.TriDense;

/**
 * This class contains test cases for dense matrix addition, scaling, and negation operations.
 */
public class TestVectorMult{
    public static def main(args:Rail[String]) {
		val n = (args.size > 0) ? Int.parse(args(0)):4;
		val testcase = new VectorMult(n);
		testcase.run();
	}
}

class VectorMult {
	public val N:Long;
	public val M:Long;

	public def this(n:Long) {
		N = n;
		M = n;
	}

    public def run (): void {
		Console.OUT.println("Starting vector-symmetric/triangular/matrix multiply tests" +
							" on vector size of "+ N);
		var ret:Boolean = true;
 		// Set the matrix function
		ret = testVecMat();
		ret = testSymMultVec();
		ret = testTriMultVec();
		ret = testSolver();

		if (ret)
			Console.OUT.println("Vector multiply test passed!");
		else
			Console.OUT.println("----------------Vector multiply test failed!----------------");
	}

	public def testVecMat():Boolean{
		Console.OUT.println("Starting vector-matrix multiply test");
		val v = Vector.make(N).initRandom();
		val m = DenseMatrix.make(M, N).initRandom(); 
		val mv = m % v;
		
		val m1  = new DenseMatrix(N, 1, v.d);
		val mm1 = m % m1;

		var ret:Boolean = mv.equals(mm1);

		val v2  = Vector.make(M).initRandom();
		val v2m = v2 % m;
	
		val m2  = new DenseMatrix(1, M, v2.d);
		val m2m = m2 % m;
		ret &= v2m.equals(m2m);
		
		if (ret)
			Console.OUT.println("Vector-matrix multiply test passed!");
		else
			Console.OUT.println("--------Vector-matrix multiply test failed!--------");
		
		return ret;
	}

	public def testSymMultVec():Boolean{
		var ret:Boolean= true;
		Console.OUT.println("Starting vector-symmetric matrix multiply test");
		val v = Vector.make(N).initRandom();
		val s = SymDense.make(N).initRandom(); 
		val m1 = new DenseMatrix(N, 1, v.d);
		val sm = s.toDense();

		val sv = s % v;		
		val smm1 = sm % m1;
		ret &= sv.equals(smm1);
		
		val v2 = Vector.make(N).initRandom();
		val m2 = new DenseMatrix(1, N, v2.d);
		val v2s = v2 % s;
		val m2s = m2 % sm;
		ret &= v2s.equals(m2s); 

		if (ret)
			Console.OUT.println("Vector-Symmetric matrix multiply test passed!");
		else
			Console.OUT.println("--------Vector-Symmetric matrix multiply test failed!--------");
		
		return ret;
	}
	
	public def testTriMultVec():Boolean{
		var ret:Boolean= true;
		Console.OUT.println("Starting vector-triangular matrix multiply test");
		val v = Vector.make(N).init(1);
		val t = TriDense.make(N).init(1); 
		val m1 = new DenseMatrix(N, 1, t.d);
		val tm = t.toDense();

		val tv = t % v;
		val tmm1 = tm % m1;
		ret &= tv.equals(tmm1);
		
		val v2 = Vector.make(N).initRandom();
		val m2 = new DenseMatrix(1, N, v2.d);
		val v2t = v2 % t;
		val m2tm = m2 % tm;
		ret &= v2t.equals(m2tm); 

		if (ret)
			Console.OUT.println("Vector-Triangular matrix multiply test passed!");
		else
			Console.OUT.println("--------Vector-Triangular matrix multiply test failed!--------");
		
		return ret;
	}
	
	public def testSolver():Boolean {
		var ret:Boolean = true;
		Console.OUT.println("Starting vector-triangular matrix multiply test");
		
		val t = TriDense.make(N).init(1);
		val v = Vector.make(N).init(1);
		val b = t % v;
		
		b.solveTriMultSelf(t);
		
		ret = b.equals(v);
		if (ret)
			Console.OUT.println("Triangular-Vector multiply solver test passed!");
		else
			Console.OUT.println("--------Triangular-Vector multiply solver test failed!--------");
		
		return ret;
	}
 }

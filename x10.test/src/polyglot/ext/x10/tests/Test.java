package polyglot.ext.x10.tests;

import junit.framework.TestSuite;

/*
 * Created by vj on Jan 11, 2005
 *
 * 
 */

/**
 * @author vj Jan 11, 2005
 * 
 */
public class Test extends TestX10 {

	/**
	 * @param name
	 */
	public Test(String name) {
		super(name);
	}

	public void test_Array1() {
        run("./Array1.x10","Array1");
    }

    public void test_Array2() {
        run("./Array2.x10","Array2");
    }
    public void test_Array3() {
        run("./Array3.x10","Array3");
    }
    public void test_Array4() {
        run("./Array4.x10","Array4");
    }
    public void test_Array5() {
        run("./Array5.x10","Array5");
    }

    public void test_ArrayCopy1() {
        run("./ArrayCopy1.x10","ArrayCopy1");
    }

    public void test_ArrayCopy2() {
        run("./ArrayCopy2.x10","ArrayCopy2");
    }

    public void test_ArrayCopy3() {
        run("./ArrayCopy3.x10","ArrayCopy3");
    }

    public void test_AsyncTest() {
        run("./AsyncTest.x10","AsyncTest");
    }

    public void test_AsyncTest1() {
        run("./AsyncTest1.x10","AsyncTest1");
    }

    public void test_Ateach() {
        run("./Ateach.x10","Ateach");
    }
    public void test_Ateach2() {
        run("./Ateach2.x10","Ateach2");
    }

    public void test_Atomic1() {
        run("./Atomic1.x10","Atomic1");
    }

    public void test_AtomicTest() {
        run("./AtomicTest.x10","AtomicTest");
    }

    public void test_AwaitTest() {
        run("./AwaitTest.x10","AwaitTest");
    }

    public void test_AwaitTest1() {
        run("./AwaitTest1.x10","AwaitTest1");
    }

    public void test_AwaitTest2() {
        run("./AwaitTest2.x10","AwaitTest2");
    }

    public void test_Boxing0() {
        run("./Boxing0.x10","Boxing0");
    }

    public void test_Boxing1() {
        run("./Boxing1.x10","Boxing1");
    }

    public void test_ClockTest() {
        run("./ClockTest.x10","ClockTest");
    }

    public void test_ClockTest1() {
        run("./ClockTest1.x10","ClockTest1");
    }

    public void test_ClockTest2() {
        run("./ClockTest2.x10","ClockTest2");
    }

    public void test_ClockedFinalTest() {
        run("./ClockedFinalTest.x10","ClockedFinalTest");
    }

    public void test_ConditionalAtomicQueue() {
        run("./ConditionalAtomicQueue.x10","ConditionalAtomicQueue");
    }

    public void test_ConditionalAtomicTest() {
        run("./ConditionalAtomicTest.x10","ConditionalAtomicTest");
    }

    public void test_DistributionTest() {
        run("./DistributionTest.x10","DistributionTest");
    }

    public void test_FinishTest1() {
        run("./FinishTest1.x10","FinishTest1");
    }

    public void test_Foreach() {
        run("./Foreach.x10","Foreach");
    }
    public void test_Foreach2() {
        run("./Foreach2.x10","Foreach2");
    }

    public void test_Future0() {
        run("./Future0.x10","Future0");
    }

    public void test_Future1() {
        run("./Future1.x10","Future1");
    }

    public void test_Future1Boxed() {
        run("./Future1Boxed.x10","Future1Boxed");
    }

    public void test_Future2Boxed() {
        run("./Future2Boxed.x10","Future2Boxed");
    }

    public void test_Future3() {
        run("./Future3.x10","Future3");
    }

    public void test_Future3Boxed() {
        run("./Future3Boxed.x10","Future3Boxed");
    }

    public void test_Future4() {
        run("./Future4.x10","Future4");
    }

    public void test_Future4Boxed() {
        run("./Future4Boxed.x10","Future4Boxed");
    }

    public void test_FutureNullable0() {
        run("./FutureNullable0.x10","FutureNullable0");
    }

    public void test_FutureNullable1Boxed() {
        run("./FutureNullable1Boxed.x10","FutureNullable1Boxed");
    }

    public void test_FutureTest2() {
        run("./FutureTest2.x10","FutureTest2");
    }

    public void test_ImportTest() {
        run("./ImportTest.x10","ImportTest");
    }

    public void test_ImportTestPackage1_SubPackage_T3() {
        run("./ImportTestPackage1/SubPackage/T3.x10","ImportTestPackage1.SubPackage.T3");
    }

    public void test_Jacobi() {
        run("./Jacobi.x10","Jacobi");
    }

    public void test_MiscTest1() {
        run("./MiscTest1.x10","MiscTest1");
    }

    public void test_NopTest() {
        run("./NopTest.x10","NopTest");
    }

    public void test_Nullable0Ref() {
        run("./Nullable0Ref.x10","Nullable0Ref");
    }

    public void test_Nullable1() {
        run("./Nullable1.x10","Nullable1");
    }

    public void test_Nullable2() {
        run("./Nullable2.x10","Nullable2");
    }

    public void test_Nullable5() {
        run("./Nullable5.x10","Nullable5");
    }

    public void test_NullableFuture0() {
        run("./NullableFuture0.x10","NullableFuture0");
    }

    public void test_NullableFuture1() {
        run("./NullableFuture1.x10","NullableFuture1");
    }

    public void test_NullableFuture2() {
        run("./NullableFuture2.x10","NullableFuture2");
    }

    public void test_NullableObject() {
        run("./NullableObject.x10","NullableObject");
    }

    public void test_NullableObject2() {
        run("./NullableObject2.x10","NullableObject2");
    }

    public void test_RandomAccess() {
        run("./RandomAccess.x10","RandomAccess");
    }

    public void test_RegionTest() {
        run("./RegionTest.x10","RegionTest");
    }
    public void test_RegionTest01() {
        run("./RegionTest01.x10","RegionTest01");
    }

    public void test_RegionTest1() {
        run("./RegionTest1.x10","RegionTest1");
    }

    public void test_RegionTest2() {
        run("./RegionTest2.x10","RegionTest2");
    }

    public void test_RegionTestIterator() {
        run("./RegionTestIterator.x10","RegionTestIterator");
    }

    public void test_ValueClass() {
        run("./ValueClass.x10","ValueClass");
    }

    public void test_queensList() {
        run("./queensList.x10","queensList");
    }
  

    public static void main(String[] args) {
        TestX10.Main(Test.class);
    }

    public static TestSuite suite() {
    	return TestX10.suite(Test.class);
    }
	
}

/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

/*
 * A method implements an interface, and the method contains concurrent
 */
public class InterfaceMethod4 {

	def getITest():ITest{
		return new AAA();
	}
	
	public def run() {
		var passed:boolean = true;
		
		val a:ITest = getITest();
		val r = a.set(2);
		passed &= (r == 2);
		Console.OUT.println("r  = " + r);
		
		return passed;
	}

	public static def main(Array[String](1)) {
	    val r = new InterfaceMethod4().run();
	    if (r) {
	        x10.io.Console.OUT.println("++++++Test succeeded.");
	    }

	    val x = new B().set(1);
	}
}

interface ITest{
	def set(v:int):int;
}

class A implements ITest{

    public def set(v:int):int{
    	return -1;
    }
}

class B implements ITest{

    public def set(v:int):int{
    	return -1;
    }
}

class AA extends A{

    public def set(v:int):int{
        val value:int;
        finish {
            async value = v;
        }
        return value;
    }
}

class AAA extends AA{
	public def foo2(){
		val aaaa = new AAAA();
		aaaa.foo();
	}
}

class AAAA extends AAA{

	public def foo(){
		set(-1); //dead code, need transform, too
	}
	
    public def set(v:int):int{
    	return -1;
    }
}

class AB extends A{

    public def set(v:int):int{
    	return -1;
    }
}


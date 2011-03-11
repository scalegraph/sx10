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

import x10.compiler.tests.*; // err markers
import harness.x10Test;

/**
 * @author yoav (yzibin) 1/2011
 */ 

public class XTENLANG_2330 extends x10Test
{ 
    public def run() {
        new Helper2330(50).run(0);
        return true;
    }

    public static def main(Array[String](1)) {
        new XTENLANG_2330().execute();
    }
}


class Helper2330(p:Int) {
	// test inner classes (both instance & nested), inheritance, overriding, generics (for generics I just checked codegen below, not runtime behaviour)
	// new & call (with and without target/qualifier), operators

	def fail():void { throw new RuntimeException("test failed!"); }
	def run(z:Int) { // z is 0 at runtime (I use it to make sure the guard cannot be statically resolved)

		/////////////////////////////////////////////////////////////
		// testing method calls (that return void or non-void)
		/////////////////////////////////////////////////////////////
		val r = new Helper2330(z+51);
		// without qualifier
		m1(z+1); // ERR
		try { m1(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// with this qualifier
		m2(z+2); // ERR
		try { m2(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// with type qualifier
		Helper2330.m1(z+1); // ERR
		try { Helper2330.m1(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// with expr/receiver qualifier
		r.m1(z+1); // ERR
		try { r.m1(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		r.m2(z+2); // ERR
		try { r.m2(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		
		m3(z+50); // ERR
		try { m3(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		r.m3(z+51); // ERR
		try { r.m3(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// test non-void method		
		m33(z+50); // ERR
		try { m33(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		r.m33(z+51); // ERR
		try { r.m33(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		// testing static and instance methods that use a guard
		val a = new A(z+52);
		A.m4(z+4); // ERR
		try { A.m4(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		a.m4(z+4); // ERR
		try { a.m4(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		a.m5(z+52); // ERR
		try { a.m5(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		// testing accessing the outer instance properties in a guard
		val b1 = new B(z+53);
		val b2 = r.new B(z+53);
		b1.m6(z+50); // ERR
		try { b1.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		b2.m6(z+51); // ERR
		try { b2.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// testing overriding
		val cc1 = new C(z+53);
		val cc2 = r.new C(z+53);
		cc1.m6(z+50); // ERR
		try { cc1.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		cc2.m6(z+51); // ERR
		try { cc2.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// testing dynamic dispatching
		val bc1:B = cc1;
		val bc2:B = cc2;
		bc1.m6(z+50); // ERR
		try { bc1.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		bc2.m6(z+51); // ERR
		try { bc2.m6(z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR


		/////////////////////////////////////////////////////////////
		// testing new expressions
		/////////////////////////////////////////////////////////////
		new Helper2330(z+1,z+2); // ERR
		try { new Helper2330(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		val q1 = new Helper2330(z+1,z+2); // ERR
		try { val q2 = new Helper2330(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		(()=>{return new Helper2330(z+1,z+2);})(); // ERR
		try { (()=>{return new Helper2330(z+1,z+1);})(); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		// testing creating a static nested class (without any qualifier, with type qualifier, with expr qualifier)
		new A(z+1,z+2); // ERR
		try { new A(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		new Helper2330.A(z+1,z+2); // ERR
		try { new Helper2330.A(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		//r.new A(z+1,z+2); // Semantic Error: Cannot provide a containing instance for non-inner class Helper2330.A.

		// testing creating an instance (non-static) nested class (without any qualifier, with type qualifier, with expr qualifier)
		new B(z+1,z+2); // ERR
		try { new B(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		new Helper2330.B(z+1,z+2); // ERR
		try { new Helper2330.B(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		r.new B(z+1,z+2); // ERR
		try { r.new B(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		this.new B(z+1,z+2); // ERR
		try { this.new B(z+1,z+1); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		// C's guard: (i1:Int, i2:Int) {i1!=i2, i1==p}
		val c1 = new C(z+50,z+42); // ERR
		val c2 = r.new C(z+51,z+42); // ERR
		try { new C(z+1,z+2); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { new C(z+50,z+50); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { r.new C(z+1,z+2); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { r.new C(z+51,z+51); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		// D's guard: (i1:Int, i2:Int) {i1!=i2, i1==p, i2==c}
		val d1 = c1. new D(z+50,z+42); // ERR
		val d2 = c2. new D(z+51,z+42); // ERR
		try { c1. new D(z+1,z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { c1. new D(z+50,z+50); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { c2.new D(z+1,z+42); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { c2.new D(z+51,z+51); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		/////////////////////////////////////////////////////////////
		// test binary operators	
		/////////////////////////////////////////////////////////////	
		use(this+(z+50)); // ERR
		try { use(this+(z+42)); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		use(r+(z+51)); // ERR
		try { use(r+(z+42)); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		use(this*(z+50)); // ERR
		try { use(this*(z+42)); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		use(r*(z+51)); // ERR
		try { use(r*(z+42)); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	

		// test unary operators	
		use(+this); // ERR
		use(-this); // ERR
		try { use(+r); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { use(-r); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	

		// implicit and explicit as (casting)
		val dd1:Helper2330 = z+5.5; // ERR
		val ss1:Helper2330 = ((z+'a') as Char) as Helper2330; // ERR
		try { val dd2:Helper2330 = z+5.6; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { val ss2:Helper2330 = ((z+'b') as Char) as Helper2330; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR

		// apply & set (and SettableAssign)
		this(z+50); // ERR
		r(z+51); // ERR
		this(z+50) = z+50; // ERR
		r(z+51) = z+51; // ERR
		
		this(z+50) += z+0; // ERR

		try { this(z+51); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { r(z+52); fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { this(z+50) = z+51; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { this(z+51) = z+50; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { r(z+51) = z+52; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { r(z+52) = z+51; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	
		try { this(z+50) += z+1; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR
		try { this(z+51) += z+0; fail(); } catch (e:UnsatisfiedGuardException) {} // ERR	

		// We already handle: Call, New, Binary, Unary, SettableAssign, Cast
		// We still need to handle in the desugarer: ConstructorCall (XTENLANG_2376) , ClosureCall (XTENLANG_2329)
		// far to do: after XTENLANG_2376 is resolved, we should check runtime checks for guards for ctor calls (super&this)	
		// todo: After XTENLANG_2329 is resolved, we could check guards for closure calls

		Console.OUT.println("Test succeeded!");
		return true;
	}
	def use(x:Any) {}

	def this(i:Int) {
		property(i);
	}
	def this(i1:Int, i2:Int) {i1!=i2} {
		property(42);
	}

	static def m1(x:Int) {x==1} {}
	def m2(x:Int) {x==2} {}
	def m3(x:Int) {x==p} {}
	def m33(x:Int) {x==p} = this;

	// binary ops
	static operator (x:Helper2330)+(i:int) {x.p==i} : Int = i;
	operator this * (i:Int) {p==i} : Int = i;
	// unary op
	static operator +(x:Helper2330) {x.p==50} : Int = 42;
	operator - this  {p==50} : Int = 43;

	// implicit_as
	static operator (x:Double) {x==5.5} : Helper2330= null;
	// explicit as
	static operator (x:Char) as ? {x=='a'} : Helper2330 = null;
	
	// apply
	operator this(i:Int) {i==p} :Int = i;
	// settable assign
	operator this(i:Int)=(v: Int) {p==i, i==v} : void {}


	static class A(a:Int) {		
		def this(i:Int) {
			property(i);
		}
		def this(i1:Int, i2:Int) {i1!=i2} {
			property(42);
		}

		static def m4(x:Int) {x==4} {}
		def m5(x:Int) {x==A.this.a} {}
	}
	class B(b:Int) {					
		def this(i:Int) {
			property(i);
		}
		def this(i1:Int, i2:Int) {i1!=i2} {
			property(42);
		}

		def m6(x:Int) {x==Helper2330.this.p} {} // testing access to outer instance properties
	}
	class C(c:Int) extends B {
		def m6(x:Int) {x==Helper2330.this.p} {} // testing overriding
	
		def this(i:Int) {
			super(i);
			property(i);
		}
		def this(i1:Int, i2:Int) {i1!=i2, i1==p}  { // testing 2 binary expressions
			super(i1);
			property(i2);
		}

		class D(d:Int) extends A {
			def this(i1:Int, i2:Int) {i1!=i2, i1==p, i2==c}  { // testing 3 binary expressions
				super(i1);
				property(i2);
			}
		}
	}

}








// Here I just test that post-compilation works on more complicated examples (but I don't check runtime behaviour)

class GenericInstantiateTest[T] {
	def test() {
		new GenericInstantiateTest[Double](null); // ERR
	}
	def this(b:GenericInstantiateTest[T]) {b==null} {}
}
class NullTestInGuard(x:String) {
	def m(b:NullTestInGuard) {b.x!=null}  = 1;
	def test() {
		val z = m(null);  // ERR:  Warning: Generated a dynamic check for the method guard.
	}
	def test2():void {
		val z = m(null);  // ERR:  Warning: Generated a dynamic check for the method guard.
	}
}

class TestWithoutGenerics {
	static def m2(a:Int, b:Outer.A) {a==44, b.x==null} = 1;

	class Outer(zz:Int) {
		class A(x:String,y:Int) {
			def m0(a:Int, b:A) = 1;
			def m(a:Int, b:A) {a==y, b.x==null, zz==33}  = 1;
			def this(a:A,b:A,c:Int) {a==null && b.x!=null && c==42, zz==c} : A {
				property(null,5);
			}

			def test(i:A,j:A,k:A, w:Int, h:TestWithoutGenerics, outer:Outer) {
				val z:A = new A(k,null,3*4);  // ERR:  Warning: Generated a dynamic check for the method guard.
				val z1:A = outer.new A(k,null,3*4);  // ERR:  Warning: Generated a dynamic check for the method guard.

				// this is what the compiler generate
				val z2 = ((x0:A, x1:Int, x2:A)=> {
						if (!(x1==x0.y && x2.x==null)) throw new ClassCastException();
						return x0.m0(x1,x2);
					}) (null, w*2, i);
				val z22 = j.m(w*2,i); // ERR: Warning: Generated a dynamic check for the method guard.
				val z3 = h.m2(w*2,i);  // ERR:  Warning: Generated a dynamic check for the method guard.
				val z4 = TestWithoutGenerics.m2(w*2,i);  // ERR:  Warning: Generated a dynamic check for the method guard.
				m(w*2,this); // ERR:  Warning: Generated a dynamic check for the method guard.

				return 43;
			}
		}
	}
}
class TestWithGenerics {
	static def m2(a:Int, b:Outer.A[Double]) {a==44} = 1;

	class Outer(zz:Int) {
		class A[T](x:String,y:Int) {
			def m0(a:Int, b:A[T]) = 1;
			def m(a:Int, b:A[T]) {a==y, b.x==null, zz==33}  = 1;
			def this(a:A[A[Int]],b:A[T],c:Int) {a==null && b.x!=null && c==42, zz==c} : A[T] {
				property(null,5);
			}

			def test(i:A[Double],j:A[Double],k:A[A[Int]], w:Int, h:TestWithGenerics, outer:Outer) {
				val z:A[Double] = new A[Double](k,null,3*4);  // ERR:  Warning: Generated a dynamic check for the method guard.
			    // gives an error (see XTENLANG_2377) :val z1 = outer.new A[Double](k,null,3*4);

				// this is what the compiler generate
				val z2 = ((x0:A[Double], x1:Int, x2:A[Double])=> {
						if (!(x1==x0.y && x2.x==null)) throw new ClassCastException();
						return x0.m0(x1,x2);
					}) (null, w*2, i);
				val z22 = j.m(w*2,i); // ERR: Warning: Generated a dynamic check for the method guard.
				val z3 = h.m2(w*2,i);  // ERR:  Warning: Generated a dynamic check for the method guard.
				val z4 = TestWithGenerics.m2(w*2,i);  // ERR:  Warning: Generated a dynamic check for the method guard.
				m(w*2,this); // ERR:  Warning: Generated a dynamic check for the method guard.

				return 43;
			}
		}
	}
}

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

package x10.constraint.tests;



import junit.framework.TestCase;
import junit.framework.TestSuite;
import x10.constraint.*;
public class MyTest extends TestCase {
	public MyTest() {
		super("MyTest");
	}
	public void test6() throws Throwable {
		XConstraint c = new XConstraint_c();
		XVar v1 = XTerms.makeLocal(XTerms.makeName("v1"));
		XVar v2 = XTerms.makeLocal(XTerms.makeName("v2"));

		XConstraint d = new XConstraint_c();
		XVar x1 = d.genEQV(XTerms.makeName("x1"), true);
		d.addBinding(v1,x1);
		d.addBinding(v2,x1);

		//XVar x1 = c0.genEQV(XTerms.makeName("x1"), true);
		boolean result = c.entails(d);
		System.out.println("test6:");
		System.out.println("c: " + c);
		System.out.println("d: " + d);
		System.out.println("d.extConstraints(): " + d.extConstraints());
		
		assertFalse(result);
		
	}

}

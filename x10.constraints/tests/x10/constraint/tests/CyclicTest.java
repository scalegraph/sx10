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
import x10.constraint.XConstraint;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.constraint.XVar;

public class CyclicTest extends TestCase {
	public CyclicTest() {
		super("CyclicTest");
	}
	XTerm zero = XTerms.makeLit(new Integer(0));
	XTerm one = XTerms.makeLit(new Integer(1));
	XTerm two = XTerms.makeLit(new Integer(2));
	XVar v0 = XTerms.makeUQV("v0");
	XVar v1 = XTerms.makeUQV("v1");
	XVar v2 = XTerms.makeUQV("v2");
	XVar v0a = XTerms.makeField(v0, "a");
	XVar v1b = XTerms.makeField(v1, "b");
	
	/**
	 * v0=v1,v1=v2 |- v0=v2
	 * @throws Throwable
	 */
	public void test1() throws Throwable {
		XConstraint c = new XConstraint();
		c.addBinding(v0a, v1);
		c.addBinding(v1b, v0);
		System.out.println("c is|" + c+ "|");
		assertTrue(c.consistent());
	}
}
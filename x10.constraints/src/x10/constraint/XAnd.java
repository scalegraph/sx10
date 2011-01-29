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

package x10.constraint;

/**
 * Represents the conjunction of two formulas.
 * (Not treated as an atomic formula.)
 * @author njnystrom
 * @author vj
 *
 */

public class XAnd extends XFormula {
	
	public XAnd(XTerm left, XTerm right) {
		super(XTerms.andName, XTerms.asExprAndName, false, left, right);
	}
	
	public XPromise internIntoConstraint(XConstraint c, XPromise last)  {
		return super.internIntoConstraint(c, last);
	}

	@Override
	public String toString() {
		return left() + ", " + right();
	}

}

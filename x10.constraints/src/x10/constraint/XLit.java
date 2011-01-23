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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A representation of a literal. A literal is both an XVar and an XPromise.
 * 
 * This class and its subclasses should not have mutable state.
 * @author vijay
 *
 */
public class XLit extends XVar implements XPromise {
	final protected Object val;

	public XLit(Object l) {
		val = l;
	}

	public Object val() { 
		return val;
	}

	public XTerm var() {
		return this;
	}
	
	public boolean hasDisBindings() { return false; }

	public XTermKind kind() { return XTermKind.LITERAL;}
	public List<XEQV> eqvs() {
		return Collections.emptyList();
	}

	public String toString() {
		if (val == null)
			return "null";
		if (val instanceof String)
			return "\"" + val.toString() + "\"";
		if (val instanceof Character)
			return "'" + val.toString() + "'";
		if (val instanceof Float)
			return val.toString() + "F";
		if (val instanceof Long)
			return val.toString() + "L";
		return val.toString();
	}

	public int hashCode() {
		return ((val == null) ? 0 : val.hashCode());
	}

	public boolean hasVar(XVar v) {
		return false;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof XLit))
			return false;
		XLit other = (XLit) o;
		return val == null ? o == null : val.equals(other.val);
	}

	@Override
	public XTerm subst(XTerm y, XVar x, boolean propagate) {
		return super.subst(y, x, propagate);
	}

	// methods from Promise
	public XPromise intern(XVar[] vars, int index) throws XFailure {
		return intern(vars, index, null);
	}

	public XPromise intern(XVar[] vars, int index, XPromise last) throws XFailure {
		if (index != vars.length) {
			throw new XFailure("Cannot extend path " + vars + "index=" + index + " beyond the literal " + this + ".");
		}
		return this;
	}

	public XPromise lookup(XVar[] vars, int index) {
		if (index != vars.length) {
			return null;
		}
		return this;
	}

	public XPromise lookup(XName s) {
		return null;
	}

	public XPromise lookup() {
		return this;
	}

	public boolean forwarded() {
		return false;
	}

	public boolean hasChildren() {
		return false;
	}

	public boolean bind(XPromise target) throws XFailure {
		if (target.term().equals(this))
			return true;
		if (target.term() instanceof XLit) {
			throw new XFailure("Cannot bind literal " + this + " to " + target);
		}
		if (target.term() instanceof XVar) {
			return target.bind(this);
		}
		if (!equals(target))
			throw new XFailure("Cannot bind literal " + this + " to " + target);
		return false;
	}
	
	public boolean disBind(XPromise target) throws XFailure {
		XTerm t = target.term();
		if (t.equals(this))
			return false;
		if (t  instanceof XLit) 
			return true; // these two literals are not equal.
		if (t instanceof XVar) 
			return target.disBind(this);
		if (equals(target))
			throw new XFailure("Cannot bind literal " + this + " to " + target);
		return true;
	}

	public boolean canReach(XPromise other) {
		return equals(other);
	}

	public XVar term() {
		return this;
	}


	public void dump(XVar path, List<XTerm> result,  boolean dumpEQV, boolean hideFake) {
		// nothing to dump.
	}

	public void addIn(XName s, XPromise orphan) throws XFailure {
		throw new XFailure("Cannot add an " + s + " child " + orphan + " to a literal, " + this + ".");
	}

	public void setTerm(XTerm term) { /* ignore */ }
	public void setTerm(XTerm term, Set<XPromise> visited) { /* ignore */ }

	public String instance() {
		return toString();
	}

	/** In case this is a field selection x.f1...fn, return x, x.f1, x.f1.f2, ... x.f1.f2...fn */
	public XVar[] vars() {
		return new XVar[0];
	}

	/** In case this is a field selection x.f1...fn, return x, else this. */
	public XVar rootVar() {
		return this;
	}

	public void replaceDescendant(XPromise y, XPromise x, XConstraint c) {
		// nothing to do.
	}

	public XPromise value() {
		return null;
	}

	public HashMap<XName, XPromise> fields() {
		return null;
	}

	/*public XPromise cloneRecursively(Map<XPromise, XPromise> env) {
		return this;
	}*/
	public void transfer(Map<XPromise, XPromise> env) {
	    // nothing to do.
	}
	public XLit cloneShallow() {
	    return this; // new XLit(this.val)
	}

	public void variables(List<XVar> result) {}

	public XPromise internIntoConstraint(XConstraint constraint, XPromise last) throws XFailure {
		throw new XFailure("Internal error -- should not be called.");
	}
	public void addDisEquals(XPromise p) throws XFailure {
		if (p instanceof XLit) {
			if (equals(p))
				throw new XFailure("Literals " + this + " and " + p 
						+ " are equal, hence cannot be disequated.");
			// otherwise there is nothing to do.
			return;
		}
		// otherwise must be an XPromise_c .. make it record that it must disequal this.
		p.addDisEquals(this);
	}
	public boolean isDisBoundTo(XPromise o) { 
		if (o instanceof XLit) {
			return ! equals(o);
		}
		return o.isDisBoundTo(this);
	}
}

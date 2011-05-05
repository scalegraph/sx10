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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Constraints constrain XTerms. Thus XTerms are the basic building blocks 
 * of constraints.This class is the root class of constraint terms.
 * Class should not have any state.
 * 
 * @author njnystrom
 * @author vj
 *
 */
public abstract class XTerm implements  Serializable, Cloneable {

	public XTerm() {
		super();
	}

	// The default is OBJECT. May be overridden by subclasses.
	public XTermKind kind() { return XTermKind.OBJECT;}
	
	/**
	 * Return the result of substituting the term y for x in this.
	 * Should be overridden in subtypes.
	 * @param y
	 * @param x
	 * @return
	 */
	public final XTerm subst(XTerm y, XVar x) {
	    return subst(y, x, true);
	}
	
	/**
	 * Returns true only if this term is allowed to occur inside a constraint.
	 * Terms a&&b, a||b, a==b etc must return false.
	 * @return
	 */
	public abstract boolean okAsNestedTerm();

	// int nextId = 0;
	
	/**
	 * Return the result of substituting y for x in this.
	 * 
	 * @param y --
	 *            the value to be substituted
	 * @param x --
	 *            the variable which is being substituted for
	 * @return the term with the substitution applied
	 */
	public XTerm subst(XTerm y, final XVar x, boolean propagate) {
	    XTerm t = this;
	    return t;
	}

	@Override
	public XTerm clone() {
		try {
			XTerm n = (XTerm) super.clone();
			return n;
		}
		catch (CloneNotSupportedException e) {
			return this;
		}
	}

	/**
	 * Does this contain an existentially quantified variable?
	 * Default no; should be overridden by subclasses representing eqvs.
	 * @return true if it is, false if it isn't.
	 */
	public boolean hasEQV() {
		return false;
	}
	
	/**
	 * Is this itself an EQV?
	 * Default no; should be overridden by subclasses representing eqvs.
	 * @return
	 */
	public boolean isEQV() {
		return false;
	}
	
	public List<XEQV> eqvs() {
	    return Collections.emptyList();
	}

	/**
	 * Is <code>this</code> a prefix of <code>term</code>, i.e. is 
	 * <code>term</code> of the form <code>this.f1...fn</code>?
	 * Default no; should be overridden by subclasses.
	 * @return
	 */
	/*public boolean prefixes(XTerm term) {
		return false;
	}*/

	/**
	 * If true, bind this variable when processing this=t, for
	 * any term t. In case t also prefers being bound, choose any
	 * one.
	 * 
	 * @return true if this  prefers being bound in a constraint this==t.
	 */
	public boolean prefersBeingBound() {
		return false;
	}

	/**
	 * Returns true if this term is an atomic formula.
	 *  == constraints are represented specially, and not considered atomic formulas.
	 * 
	 * @return true -- if this term represents an atomic formula
	 */
	public boolean isAtomicFormula() {
	    return false;
	}


	/** 
	 * Returns true if the variable v occurs in this term.
	 * @param v -- the variable being checked.
	 * @return true if v occurs in this
	 */
	public abstract boolean hasVar(XVar v);

	/**
       Intern this term into constraint and return the promise
       representing the term. 
       
       <p> Throw an XFailure if the resulting constraint is inconsistent.
	 */
	abstract XPromise internIntoConstraint(XConstraint constraint, XPromise last);

	public abstract int hashCode();
	public abstract boolean equals(Object o);

    // Wrote my own visitor, cause the XGraphVisitor is too cumbersome
    public interface TermVisitor {
        /**
         * Visit the term tree.
         * @param term
         * @return  A term if normal traversal is to stop, <code>null</code> if it
         * is to continue.
         */
        XTerm visit(XTerm term);
    }
    /**
     * Given a visitor, we traverse the entire term (which is like a tree).
     * @param visitor
     * @return If the visitor didn't return any new child, then we return "this" (otherwise we create a clone with the new children)
     */
    public XTerm accept(TermVisitor visitor) {
        // The default implementation for "leave" terms (that do not have any children)
        XTerm res = visitor.visit(this);
        if (res!=null) return res;
        return this;
    }
    
    /**
     * Return the normal form for this term in this given constraint.
     * The normal form of a term t in a constraint c, t.nf(c), is a term 
     * s with the property that 
     * for all u: s=u.nf(c) iff c |- s=u
     * From this it follows that s=s.nf(c).
     * The normal form is computed as nfp(c).term().
     * @param c
     * @return
     */
    public final XTerm nf(XConstraint c) {
    	assert c != null;
    	return nfp(c).term();
    }
    
    /**
     * Return the promise corresponding to the normal form of the term, 
     * interning the term if it is not interned already. 
     * If p is the return value, then guaranteed p!= null and p=p.lookup().
     * @param c
     * @return
     */
    public abstract XPromise nfp(XConstraint c);
}

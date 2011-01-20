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

import x10.util.CollectionFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


/**
 * 
 *  A constraint solver for the following constraint system. Note terms in this constraint system are untyped.
 * <verbatim>
 * t ::= x                  -- variable
 *       | t.f              -- field access
 *       | g(t1,..., tn)    -- uninterpreted function symbol
 *       
 * c ::= t == t             -- equality
 *       | t != t           -- dis-equality
 *       | c,c              -- conjunction
 *       | p(t1,..., tn)    -- atomic formula
 * </verbatim>  
 * 
 * The constraint system implements the usual congruence rules for equality. That is, if <code>s1,...,sn</code> and 
 * <code>t1,...,tn</code> are terms, and <code> s1 == t1,..., sn == tn</code>, then 
 * <code>g(s1,..., sn) == g(s1,...,sn)</code>, and 
 * <code>p(t1,..., tn) == p(t1,...,tn)</code>. Further, 
 * <uline>
 *   <li> <code>s equals t</code> implies <code>t equals s</code>
 *   <li> <code>s equals t</code> and <code>t equals u</code> implies <code> s equals u</code>
 *   <li> it is always the case that <code>s equals s</code>
 * </uline>
 * <p>Terms are created using the static API in XTerms. The <code>==</code> relation on terms at the level of the 
 * constraint system is translated into the <code>equals</code> relation on the Java representation of the terms.
 * 
 * <p>A constraint is implemented as a graph whose nodes are XPromises. Two different constraints will 
 * not share @link{XPromise}. See the description of @link{XPromise} for more information about the 
 * structure of a promise.
 * 
 * <p>This representation is a bit different from the Nelson-Oppen and Shostak congruence closure 
 * algorithms described, e.g. in Cyrluk, Lincoln and Shankar "On Shostak's Decision Procedure
 * for Combination of Theories", CADE 96.
 * 
 * <p>
 * <bf>TODO:</bf>
 * Use Shostak's congruence procedure. Treat <tt>t.f</tt> as the term <tt>f(t)</tt>, 
 * i.e. each field is regarded as a unary function symbol. This will be helpful in implementing
 * Nelson-Oppen integration of decision procedures.
 * 
 * <p> Additional Notes.
 * This constraint system and its implementation knows nothing about X10 or internal compiler structures. Specifically
 * it knows nothing about X10 types. The package x10.types.constraints contains an extension of this type system that is aware 
 * of a this variable, a self variable and other compiler-related data-structures.
 * 
 * @author vj
 *
 */
public class XConstraint implements Cloneable {
    protected HashMap<XTerm, XPromise> roots;
    protected boolean consistent = true;
    protected boolean valid = true;

    public XConstraint() {}
    
    public HashMap<XTerm, XPromise> roots() {
        return roots;
    }
    
    /**
	 * Return the list of existentially quantified variables in this constraint.
	 * @return
	 */
    public List<XVar> eqvs() {
    	List<XVar> xvars = new LinkedList<XVar>();
    	if (roots==null) return xvars;
    	for (XTerm xt : roots.keySet()) {
    		if (xt.isEQV())
    			xvars.add((XVar) xt);
    	}
    	return xvars;
    }

    /**
     * Return the set of terms occuring in this constraint.
     * @return
     */
    public Set<XTerm> rootTerms() {
    	return roots == null ? Collections.<XTerm> emptySet() : roots.keySet();
    }
    private void addTerm(XTerm term, Set<XVar> result) {
        if (term==null)
            return;
        if (term instanceof XFormula) {
            XFormula form = (XFormula) term;
            for (XTerm arg : form.arguments())
                addTerm(arg, result);
            return;
        } 
        if (term instanceof XVar)
            addVar((XVar) term, result);
    }
    private void addVar(XVar var, Set<XVar> result) {
        if (var == null)
            return;
        result.add(var);
        if (var instanceof XField) {
            addVar(((XField)var).receiver(), result);
        }
    }
    public Set<XVar> vars() {
        List<XTerm> terms = constraints();
        Set <XVar> result = CollectionFactory.newHashSet();
        for (XTerm term : terms) {
           addTerm(term, result);
        }
        return result;   
    }
    /**
     * Copy this constraint logically; that is, create a new constraint
     * that contains the same equalities (if any) as the current one.
     * Copy also the consistency, and validity status. 
     */
    public XConstraint copy() {
        XConstraint c = new XConstraint();
        c.init(this);
        return c;
    }
    
    /**
     * Return the result of copying this into c.  
     * @param c
     * @return
     */
    protected XConstraint copyInto(XConstraint c) throws XFailure {
        c.addIn(this);
        return c;
    }

    XConstraint addIn(XConstraint c)  throws XFailure {
    	if (c != null) {
    		List<XTerm> result = c.constraints();
    		if (result == null)
    			return this;
    		for (XTerm t : result) {
    			addTerm(t);
    		}
    	}
    	return this;
    }

    
	/**
	 * Return the term v is bound to in this constraint, and null
	 * if there is no such term. This term will be distinct from v.
	 * */
    public XVar bindingForVar(XVar v) {
    	XPromise p = lookup(v);
    	if (p != null && p.term() instanceof XVar && ! p.term().equals(v)) {
    		return (XVar) p.term();
    	}
    	return null;
    }
    
    public XTerm bindingForRootField(XVar root, XName name) {
    	 if (!consistent || roots == null)
             return null;
         XPromise self = (XPromise) roots.get(root);
         if (self == null)
             return null;
         XPromise result = self.lookup(name);
         return result == null ? null : result.term();
    }

	/**
	 * Return the list of atoms (atomic formulas) in this constraint.
	 * @return
	 */
    public List<XFormula> atoms() {
    	List<XFormula> r = new LinkedList<XFormula>();
    	if (roots == null)
    		return r;
    		
    	for (XTerm t : roots.keySet()) {
    		if (t instanceof XFormula) {
    			r.add((XFormula) t);
    		}
    	}
    	return r;
    }
    /**
	 * Is the consistent consistent? That is, does it have a solution?
	 * 
	 * @return true iff the constraint is consistent.
     */
    public boolean consistent() {
        return consistent;
    }

    /** Is the constraint valid? i.e. vacuous.
     * 
     */
    public boolean valid() { 	
        if (valid) {
            if (! consistent)
                return false;
            List<XTerm> atoms = constraints();
            valid = atoms.size() == 0;
        }		
        return valid;
    }

    /**
     * Add t1=t2 to the constraint, unless it is inconsistent. 
     * Note: constraint is modified in place.
     * @param var -- t1
     * @param val -- t2
     */
    public void addBinding(XTerm left, XTerm right) throws XFailure {
    	assert left != null;
        assert right != null;

        if (!consistent)
            return;
        
        if (roots == null)
            roots = new LinkedHashMap<XTerm, XPromise>();

        XPromise p1 = intern(left);
        XPromise p2 = intern(right);
        
        boolean modified = p1.bind(p2);
        valid &= !modified;
    }

    /**
	 * Add t1 != t2 to the constraint.
	 * Note: Constraint is updated in place.
	 * @param var
	 * @param t
	 */
    public void addDisBinding(XTerm left, XTerm right) throws XFailure {
    	assert left != null;
    	assert right !=null;
    	if (! consistent)
    		return;
    	if (roots == null)
    		roots = new LinkedHashMap<XTerm, XPromise>();
    	XPromise p1 = intern(left);
    	XPromise p2 = intern(right);
    	if (p1.equals(p2)) {
    		throw new XFailure(this + " already entails " + left + "==" + right);
    	}
    	boolean modified = p1.disBind(p2);
        valid &= !modified;
    }
    

	/**
	 * Add an atomic formula to the constraint.
	 * Note: Constraint is modified in place.
	 * @param term
	 * @return
	 * @throws XFailure
	 */
    public void addAtom(XTerm t) throws XFailure {
        if (!consistent)
            return;
        
        if (roots == null)
            roots = new LinkedHashMap<XTerm, XPromise>();

        XPromise p = lookup(t);
        
        if (p != null)
            // nothing to do
            return;
        
        p = intern(t);
    }
    /**
	 * Does this entail constraint other?
	 * 
	 * @param t
	 * @return
	 */
    public boolean entails(XConstraint other)  {
        if (!consistent())
            return true;
        if (other == null || other.valid())
            return true;
        List<XTerm> otherConstraints = other.extConstraints();
        for (XTerm t : otherConstraints) {
        	boolean result = entails(t);
        	if (! result)
        		return false;
        }
        return true;
    }
    
    public void setInconsistent() {
        this.consistent = false;
    }  
   
    /**
	 * Return the least upper bound of this and other. That is, the resulting constraint has precisely
	 * the constraints entailed by both this and other.
	 * @param other
	 * @return
	 */
    public XConstraint leastUpperBound(XConstraint other) {
     
       	XConstraint result = new XConstraint();
       	for (XTerm term : other.constraints()) {
       		try {
       			if (entails(term)) {
       				result.addTerm(term);
       			}
       		} catch (XFailure z) {

       		}
       	}
       	return result;
       }
       
  
    
    /**
	 * Return a list of bindings t1-> t2 equivalent to the current
	 * constraint. Equivalent to constraints(new ArrayList()).
	 * 
	 * @return
	 */
    public List<XTerm> constraints() {
        return constraints(new ArrayList<XTerm>());
    }  
    
    /**
	 * Return a list of bindings t1-> t2 equivalent to the current
	 * constraint, added to result.
	 * 
	 * @return
	 */
    public List<XTerm> constraints(List<XTerm> result) {
        if (roots == null)
            return result;
        for (XPromise p : roots.values()) {
        	// vj: To check if c entails exists X1...Xn.d
        	// where exists X1...Xn. d is satisfiable, 
        	// simply check that all the constraints in d
        	// that do not involve X1,..., Xn are entailed by c.
        	//if (p.term() ==null ||  p.term().isEQV())
        	//	continue;
        	p.dump(null, result,  true, false);
        }
        return result;
    }
    

	/**
	 * Return a list of bindings t1-> t2 equivalent to the current
	 * constraint except that equalities involving EQV variables are ignored.
	 * 
	 * @return
	 */

    public List<XTerm> extConstraints() {
        return extConstraints(new ArrayList<XTerm>());
    }
    public List<XTerm> extConstraintsHideFake() {
        return extConstraintsHideFake(new ArrayList<XTerm>());
    }

    /**
	 * Return (appended to result) a list of bindings t1-> t2 equivalent to the current
	 * constraint except that equalities involving EQV variables are ignored.
	 * 
	 * @return
	 */

    public List<XTerm> extConstraints(List<XTerm> result) {
        if (roots == null)
            return result;
        for (XPromise p : roots.values()) {
            p.dump(null, result, false, false);
        }
        return result;
    }
    public List<XTerm> extConstraintsHideFake(List<XTerm> result) {
        if (roots == null)
            return result;
        for (XPromise p : roots.values()) {
            p.dump(null, result, false, true);
        }
        return result;
    }
	/**
	 * Does this entail a != b?
	 * @param a
	 * @param b
	 * @return this |- a != b
	 */
    public boolean disEntails(XTerm t1, XTerm t2)  {
    	if (! consistent) return true;
    	XPromise p1 = lookup(t1);
    	if (p1 == null) // this constraint knows nothing about t1.
    		return false;
    	XPromise p2 = lookup(t2);
    	if (p2 == null)
    		return false;
    	return p1.isDisBoundTo(p2);
    	
    }
  
	/**
	 * Does this entail a==b? 
	 * @param a
	 * @param b
	 * @return true iff this |- a==b
	 */
    public boolean entails(XTerm t1, XTerm t2)  {
        if (!consistent)
            return true;
        if (t1.isEQV() || t2.isEQV())
        	return true;
        XPromise p1 = lookupPartialOk(t1);
        if (p1 == null) // No match, the term t1 is not equated to anything by this.
            return false;

        int r1Count = 0;
        XVar[] vars1 = null;
        if (p1 instanceof XPromise_c) {
        	if (t1 instanceof XVar) {
        		r1Count = ((XPromise_c) p1).lookupReturnValue();
        		vars1 = ((XVar) t1).vars();
        	}
        }

        XPromise p2 = lookupPartialOk(t2);
        if (p2 == null) // No match, the term t2 is not equated to anything by this.
        	return false;

        int r2Count = 0;
        XVar[] vars2 = null;
        if (p2 instanceof XPromise_c) {
        	if (t2 instanceof XVar) {
        		r2Count = ((XPromise_c) p2).lookupReturnValue();
        		/* if (! (t2 instanceof XVar)) {
            	assert false: "Internal Error:" + t2 + "expected to be an XVar.";
            }*/
        		vars2 = ((XVar) t2).vars();
        	}
        }

        if ((!(t1 instanceof XVar) || (r1Count == 0 || r1Count == vars1.length))
        		&& (! (t1 instanceof XVar) || (r2Count == 0 || r2Count == vars2.length))) {
        		
            // exact lookups
            return p1.equals(p2);
        }

        // at least one of them had a suffix left over
        // Now the returned promises must match, and they must have the same
        // suffix.
        if (!p1.equals(p2))
            return false;

        // Now ensure that they have the same suffix left over.
        int residual1 = vars1.length - r1Count, residual2 = vars2.length - r2Count;
        if (residual1 != residual2)
            return false;

        for (int i = 0; i < residual1; i++) {
            XVar v1 = vars1[r1Count + i];
            XVar v2 = vars2[r2Count + i];
            if (v1 instanceof XField && v2 instanceof XField) {
                XField f1 = (XField) v1;
                XField f2 = (XField) v2;
                if (! f1.field().equals(f2.field())) {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        return true;
    }
    
	/**
	 * Does this entail c, and c entail this? 
	 * 
	 * @param t
	 * @return
	 */
    public boolean equiv(XConstraint other) throws XFailure {
        boolean result = entails(other);
        if (result) {
            if (other == null)
                result = valid;
            else
                result = other.entails(this);
        }
        return result;
    }

    /** Return true if this constraint entails t. */
    public boolean entails(XTerm t) {
        if (t instanceof XEquals) {
            XEquals f = (XEquals) t;
            XTerm left = f.left();
            XTerm right = f.right();
            
            if (entails(left, right)) {
                return true;
            }
            if (right instanceof XEquals) {
            	XEquals r = (XEquals) right;
            	if (entails(r.left(), r.right())) {
            		return entails(left, XTerms.TRUE);
            	}
            	if (disEntails(r.left(), r.right())) {
            		return entails(left, XTerms.FALSE);
            	}
            }
            if (right instanceof XDisEquals) {
            	XDisEquals r = (XDisEquals) right;
            	if (entails(r.left(), r.right())) {
            		return entails(left, XTerms.FALSE);
            	}
            	if (disEntails(r.left(), r.right())) {
            		return entails(left, XTerms.TRUE);
            	}
            }
            
        } else if (t instanceof XDisEquals) {
            XDisEquals f = (XDisEquals) t;
            XTerm left = f.left();
            XTerm right = f.right();
            
            if (disEntails(left, right)) {
                return true;
            }
        }
        else if (t instanceof XFormula) {
        	XFormula f = (XFormula) t;
        	XName op = f.operator();
        	List<XTerm> args = f.arguments();
        	int n = args.size();
        	for (XFormula x : atoms()) {
        		if (x.operator().equals(op)) {
        			List<XTerm> xargs = x.arguments();
        			if (n!= xargs.size())
        				continue;
        			int i=0;
        			while(i < n && entails(args.get(i), xargs.get(i))) i++;
        			if (i==n) return true;
        		}
        	}
        	return false;
        }

        return false;
    }
 
    
   // private static boolean printEQV = true;

    public String toString() {
        XConstraint c = this;
        
        if (! c.consistent()) {
            return "{inconsistent}";
        }
        
     /*  try {
           // c = c.substitute(c.genEQV(XTerms.makeName("self"), false), c.self());
        }
        catch (XFailure z) {
            return "{inconsistent}";
        }*/

        String str ="";

        final boolean exists_toString = false;
        if (exists_toString) {
            List<XVar> eqvs = eqvs();
            if (!eqvs.isEmpty()) {
                String temp = eqvs.toString();
                str = "exists " + temp.substring(1, temp.length() - 1) + ".";
            }
            String constr = c.constraints().toString();
            str += constr.substring(1, constr.length() - 1);
        }
        else {
            String constr = c.extConstraintsHideFake().toString();
            str += constr.substring(1, constr.length() - 1);
        }
        
        return "{" + str + "}";
    }

    
    /**
	 * Perform substitute y for x for every binding x -> y in bindings.
	 * 
	 */
    public XConstraint substitute(HashMap<XVar, XTerm> subs) throws XFailure {
        XConstraint c = this;
        for (Map.Entry<XVar,XTerm> e : subs.entrySet()) {
            XVar x = e.getKey();
            XTerm y = e.getValue();
            c = c.substitute(y, x);            
        }
        return c;
    }
    
    /**
	 * If y equals x, or x does not occur in this, return this, else copy
	 * the constraint and return it after performing applySubstitution(y,x).
	 * 
	 * 
	 */
    public XConstraint substitute(XTerm y, XVar x) throws XFailure {
        return substitute(new XTerm[] { y }, new XVar[] { x });
    }
     public XConstraint substitute(XTerm[] ys, XVar[] xs, boolean propagate) throws XFailure {
    	return substitute(ys, xs);
    }
    
	/**
	 * xs and ys must be of the same length. Perform substitute(ys[i],
	 * xs[i]) for each i < xs.length.
	 */

    public XConstraint substitute(XTerm[] ys, XVar[] xs) throws XFailure {
    	assert (ys != null && xs != null);
    	assert xs.length == ys.length;
    	
    	boolean eq = true;
		for (int i = 0; i < ys.length; i++) {
			XTerm y = ys[i];
			XVar x = xs[i];

			if (! y.equals(x))
				eq = false;
		}
		if (eq)
			return this;
    	
    	if (! consistent)
    		return this;
    	
    	// Don't do the quick occurrence check; x might occur in a self constraint.
    	//		XPromise last = lookupPartialOk(x);
    	//		if (last == null) return this; 	// x does not occur in this
    	
    	XConstraint result = new XConstraint();
    	
    	for (XTerm term : constraints()) {
    		XTerm t = term;
    		
    		// if term is y==x.f, the subst will produce y==y.f, which is a cycle--bad!
    		//		    if (term instanceof XEquals_c) {
    		//		        XEquals_c eq = (XEquals_c) term;
    		//		        XTerm l = eq.left();
    		//		        XTerm r = eq.right();
    		//		        if (y.equals(l) || y.equals(r))
    		//		            continue;
    		//		    }
    		for (int i = 0; i < ys.length; i++) {
    			XTerm y = ys[i];
    			XVar x = xs[i];
    			t = t.subst(y, x, true);
    		}
    		
    		// t = t.subst(result.self(), self(), true);

    		try {
    			result.addTerm(t);
    		}
    		catch (XFailure z) {
    			throw z;
    		}
    	}
    	//		XConstraint_c result = clone();
    	//		result.valid = true;
    	//		result.applySubstitution(y,x);
    	return result;
    }

    /**
	 * Does this constraint contain occurrences of the variable v?
	 * 
	 * @param v
	 * @return true iff v is a root variable of this.
	 */
    public boolean hasVar(XVar v) {
        return roots != null && roots.keySet().contains(v);
    }

  
	/**
	 * Add the binding term=true to the constraint.
	 * 
	 * @param term -- must be of type Boolean.
	 * @return new constraint with term=true added.
	 * @throws SemanticException
	 */
    // FIXME: need to convert f(g(x)) into \exists y. f(y) && g(x) = y when f and g both atoms
    // This is needed for Nelson-Oppen to work correctly.
    // Each atom should be a root.
    public void addTerm(XTerm term) throws XFailure {
        if (term.isAtomicFormula()) {
            addAtom(term);
        }
        else if (term instanceof XVar) {
            addBinding(term, XTerms.TRUE);
        }
        /*else if (term instanceof XNot) {
            XNot t = (XNot) term;
            if (t.unaryArg() instanceof XVar)
                addBinding(t.unaryArg(), XTerms.FALSE);
            if (t.unaryArg() instanceof XNot)
                addTerm(((XNot) t.unaryArg()).unaryArg());
        }*/
        else if (term instanceof XAnd) {
            XAnd t = (XAnd) term;
            addTerm(t.left());
            addTerm(t.right());
        }
        else if (term instanceof XEquals) {
            XEquals eq = (XEquals) term;
            XTerm left = eq.left();
            XTerm right = eq.right();
            addBinding(left, right);
        } else if (term instanceof XDisEquals) {
        	XDisEquals dq = (XDisEquals) term;
        	   XTerm left = dq.left();
               XTerm right = dq.right();
               addDisBinding(left, right);
        }
        else {
            throw new XFailure("Unexpected term |" + term + "|");
        }
    }
    
    // *****************************************************************INTERNAL ROUTINES

	/**
	 * Return the promise obtained by interning this term in the constraint.
	 * This may result in new promises being added to the graph maintained
	 * by the constraint. 
	 * <p>term: Literal -- return the literal. 
	 * <p> term:LocalVariable, Special, Here Check if term is already in the roots
	 * maintained by the constraint. If so, return the root, if not add a
	 * promise to the roots and return it. 
	 * <p> term: XField. Start with the rootVar x and follow the path f1...fk, 
	 * if term=x.f1...fk. If the graph contains no nodes after fi, 
	 * for some i < k, add promises into the graph from fi+1...fk. 
	 * Return the last promise.
	 * 
	 * <p> Package protected -- should only be used in the implementation of the constraint
	 * system.
	 * @param term
	 * @return
	 * @throws XFailure
	 */

    XPromise intern(XTerm term) throws XFailure {
        return intern(term, null);
    }
    
    /**
     * Used to implement substitution:  if last != null, term, is substituted for 
     * the term that was interned previously to produce the promise last. This is accomplished by
     * returning last as the promise obtained by interning term, unless term is a literal, in which
     * case last is forwarded to term, and term is returned. This way incoming and outgoing edges 
     * (from fields) from last are preserved, but term now "becomes" last.
     * Required: on entry, last.value == null.
     * The code will work even if we have literals that are at types where properties are permitted.
     * @param term
     * @param last
     * @return
     */
    XPromise intern(XTerm term, XPromise last) throws XFailure {
    	assert term != null;
        if (term instanceof XPromise) {
            XPromise q = (XPromise) term;
            
            // this is the case for literals, for here
            if (last != null) {
                try {
                    last.bind(q);
                }
                catch (XFailure f) {
                    throw new XFailure("A term ( " + term + ") cannot be interned to a promise (" + last + ") whose value is not null.");
                }
            }
            return q;
        }

        // let the term figure out what to do for itself.
        return term.internIntoConstraint(this, last);
    }

    XPromise internBaseVar(XVar baseVar, boolean replaceP, XPromise last) throws XFailure {
        if (roots == null)
            roots = new LinkedHashMap<XTerm, XPromise>();
        XPromise p = (XPromise) roots.get(baseVar);
        if (p == null) {
            p = (replaceP && last != null) ? last : new XPromise_c(baseVar);
            roots.put(baseVar, p);
        }
        return p;
    }
    
    void addPromise(XTerm p, XPromise node) {
        if (roots == null)
            roots = new LinkedHashMap<XTerm, XPromise>();
        roots.put(p, node);
    }

     void internRecursively(XVar v) throws XFailure {
        intern(v);
    }
    
     /**
 	 * Look this term up in the constraint graph. Return null if the term
 	 * does not exist. Does not create new nodes in the constraint graph.
 	 * Does not return a forwarded promise (looks it up recursively, instead).
 	 * 
 	 * @param term
 	 * @return the terminal promise this term is associated with (if any), null otherwise
 	 */
    XPromise lookup(XTerm term) {
        XPromise result = lookupPartialOk(term);
        if (!(result instanceof XPromise_c))
            return result;
        // it must be the case that term is a XVar.
        if (term instanceof XVar) {
            XVar var = (XVar) term;
            XVar[] vars = var.vars();
            XPromise_c resultC = (XPromise_c) result;
            int index = resultC.lookupReturnValue();
            return (index == vars.length) ? result : null;
        }
        if (term instanceof XFormula)
        	return result;
        return null;
    }
    
	/**
	 * Look this term up in the constraint graph. If the term is of the form
	 * x.f1...fk and the longest prefix that exists in the graph is
	 * x.f1..fi, return the promise corresponding to x.f1...fi. If the
	 * promise is a Promise_c, the caller must invoke lookupReturnValue() to
	 * determine if the match was partial (value returned is not equal to
	 * the length of term.vars()). If not even a partial match is found, or
	 * the partial match terminates in a literal (which, by definition,
	 * cannot have fields), then return null.
	 * 
	 * @seeAlso lookup(C_term term)
	 * @param term
	 * @return
	 * @throws XFailure
	 */
    XPromise lookupPartialOk(XTerm term) {
        if (term == null)
            return null;
        
        if (term instanceof XPromise)
            // this is the case for literals, for here
            return (XPromise) term;
        // otherwise it must be a XVar.
        if (roots == null)
            return null;
        if (term instanceof XVar) {
            XVar var = (XVar) term;
            XVar[] vars = var.vars();
            XVar baseVar = vars[0];
            XPromise p = (XPromise) roots.get(baseVar);
            if (p == null)
                return null;
            return p.lookup(vars, 1);
        }
        
        {
        	XPromise p = roots.get(term);
        	if (p != null)
        		return p;
        }
        
        return null;
    }

    
  /*
    public XConstraint addBindingPromise(XTerm t1, XPromise p)  {
        try {
            assert t1 != null;
            if (!consistent)
                return this;
            if (roots == null)
                roots = new LinkedHashMap<XTerm, XPromise>();
            XPromise p1 = intern(t1);
            boolean modified = p1.bind(p);
        }
        catch (XFailure z) {
            consistent = false;
        }
        return this;
    }

    public void addTerms(List<XTerm> terms) throws XFailure {
        for (XTerm t : terms) {
            addTerm(t);
        }
    }
*/
    
    
    /**
	 * Preconditions: x occurs in this. It must be the case that the real
	 * clause of the type of y, S, entails the real clause of the type of x,
	 * T. Assume that this and S are fully explicit, that is the
	 * consequences of the types U of variables v occurring in them have
	 * been added to them.
	 * 
	 * Replace all occurrences of x in this by y. For every binding y.p = t
	 * in the result, for every binding self.p.q = t1 in S add
	 * y.p.q=t1[y/self] to the result. Return this now fully explicit
	 * constraint.
	 * 
	 * @param y
	 * @param x
	 */
    protected void applySubstitution(HashMap<XVar, XTerm> subs) throws XFailure {
        for (Map.Entry<XVar, XTerm> e : subs.entrySet()) {
            XVar x = e.getKey();
            XTerm y = e.getValue();
            applySubstitution(y,x);
        }
    }
    
	protected void applySubstitution(XTerm y, XVar x) throws XFailure {
        if (roots == null) {
            // nothing to substitute
            return;
        }
        
        // Get the node for p.
        XPromise p = (XPromise) roots.get(x);

        if (p == null) {
            // nothing to substitute
            return;	
        }

        // Remove x to avoid variable capture issues (y may be contain or be equal to x).
        roots.remove(x);

        // Get the node for y.  Since q may contain references to p or nodes reachable from p, interning y
        // may add back x to the root set.  For example, we might be replacing self with self.home.
        XPromise q = intern(y);

        // Replace references to p with references to q.
        replace(q, p);

        {
            Map<XName, XPromise> pfields = p.fields();
            Map<XName, XPromise> qfields = q.fields();

            if (pfields != null && qfields != null)
                for (XName field : pfields.keySet()) {
                    XPromise pf = pfields.get(field);
                    XPromise qf = qfields.get(field);
                    if (qf != null)
                        replace(qf, pf);
                }
        }

        // Substitute y for x in the promise terms.
        {
            // Collection<XPromise> rootPs = roots.values();
            for (Map.Entry<XTerm, XPromise> e : cloneRoots().entrySet()) {
                if (!e.getKey().equals(p.term())) {
                    XPromise px = e.getValue();
                    XTerm t = px.term();
                    t = t.subst(q.term(), x);
                    XPromise tp = intern(t);
                    if (tp != px)
                        px.setTerm(tp.term());
                }
            }
        }

        // Now, add back x as a root, if we can.
        if (q.term().equals(x)) {
            // Cannot replace x with x.  Instead,
            // introduce an EQV and substitute that for x.

            XEQV v = XTerms.makeEQV();

            // Clone the root map, with the renaming map primed
            // with x -> v
            HashMap<XPromise, XPromise> renaming = new LinkedHashMap<XPromise,XPromise>();
            renaming.put(q, new XPromise_c(v));

            for (Map.Entry<XTerm, XPromise> m : roots.entrySet()) {
               //  XTerm var = m.getKey();
                XPromise p2 = m.getValue();
                XPromise q2 = p2.cloneRecursively(renaming);
                m.setValue(q2);
            }

            return;
        }

        if (p instanceof XLit) {
            q.bind(p);
            return;
        }

        XPromise xf = p.value();

        if (xf != null) {
            q.bind(xf);
        }
        else {
            // p is no longer a root, but fields reachable from p may still mention x rather than y (or more precisely, q.term()).
            // Replace the term in p with q's term; this will fix up fields of x to be fields of y.
            Map<XName,XPromise> fields = p.fields(); 
            if (fields != null) {
                for (Map.Entry<XName, XPromise> entry : fields.entrySet()) {
                    XPromise p1 = entry.getValue();
                    if (p1.term() instanceof XField) {
                        XName field = ((XField) p1.term()).field();
                        XTerm t = XConstraint.makeField(q.term(), field);
                        XPromise q1 = intern(t);
                        if (q1 == p1) {
                            p1.setTerm(t);
                        }
                        else {
                            // The old field node was replaced and so unifies with a different node.
                            p1.setTerm(q1.term());
                        }
                        if (p1.value() == p1) {
                            ((XPromise_c) p1).value = null;
                        }
                    }
                }
            }

            //		    if (fields != null) {
            //			for (Map.Entry<XName, XPromise> entry : fields.entrySet()) {
            //			    XName s = entry.getKey();
            //			    XPromise orphan = entry.getValue();
            //			    orphan.replaceDescendant(q, p);
            //			    XField oldTerm = (XField) orphan.term();
            //			    XName oldField = oldTerm.field();
            //			    XTerm t = makeField(q.term(), oldField);
            //			    XPromise tp = intern(t);
            //			    orphan.setTerm(tp.term());
            //			    q.addIn(s, orphan);
            //			}
            //		    }
        }
    }

    @SuppressWarnings("unchecked") // Casting to a generic type
    private Map<XTerm, XPromise> cloneRoots() {
        return ((Map<XTerm,XPromise>) roots.clone());
    }

    static XTerm makeField(XTerm target, XName field) {
        XTerm t;
        if (target instanceof XVar) {
            t = XTerms.makeField((XVar) target, field);
        }
        else {
            t = XTerms.makeAtom(field, target);
        }
        return t;
    }

    /** Replace all pointers entering x in this constraint with pointers entering y.
     * 
     * @param y
     * @param x
     * @param c TODO
     */
    private void replace(XPromise y, XPromise x) throws XFailure {
        //		HashMap<XPromise, XPromise> renaming = new LinkedHashMap<XPromise,XPromise>();
        //		renaming.put(x, y);
        //
        //		for (Map.Entry<XTerm, XPromise> m : roots.entrySet()) {
        //			XTerm var = m.getKey();
        //			XPromise p2 = m.getValue();
        //			XPromise q2 = p2.cloneRecursively(renaming);
        //			m.setValue(q2);
        //		}

      //  Collection<XPromise> rootPs = roots.values();
        for (Map.Entry<XTerm, XPromise> e : roots.entrySet()) {
            if (!e.getKey().equals(x.term())) {
                XPromise p = e.getValue();
                p.replaceDescendant(y, x, this);
            }
        }
    }
    /**
     * Initialize this constraint from the given constraint
     * @param from -- the constraint whose state is used to initialize this.
     */
    protected void init(XConstraint from) {
   	 try {
           consistent = from.consistent;
           valid = from.valid;
           from.copyInto(this);
        }
        catch (XFailure f) {
            setInconsistent();
        }
   }
	

  
}

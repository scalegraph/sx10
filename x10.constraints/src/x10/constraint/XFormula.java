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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A representation of an atomic formula op(t1,..., tn).
 * 
 * @author vijay
 *
 */
public class XFormula extends XTerm {
	    public final XName op;
	    public final XName asExprOp;
	    public List<XTerm> arguments;

	    /**
	     * Create a formula with the given op and given list of arguments.
	     * @param op
	     * @param args
	     */
	    public XFormula(XName op, XName opAsExpr, List<XTerm> args) {

	    	  this.op = op;
	    	  this.asExprOp = opAsExpr;
	        this.arguments = args;
	    }
	    public XFormula(XName op, XName opAsExpr, XTerm... args) {
	        this.op = op;
	        this.asExprOp = opAsExpr;
	        this.arguments = new ArrayList<XTerm>(args.length);
	        for (XTerm arg : args) {
	            this.arguments.add(arg);
	        }
	    }
	    public XTermKind kind() { return XTermKind.FN_APPLICATION;}
	    public List<XEQV> eqvs() {
	        List<XEQV> eqvs = new ArrayList<XEQV>();
	        for (XTerm arg : arguments) {
	            eqvs.addAll(arg.eqvs());
	        }
	        return eqvs;
	    }

	    @Override
	    public XTerm subst(XTerm y, XVar x, boolean propagate) {
	        List<XTerm> newArgs = new ArrayList<XTerm>(this.arguments().size());
	        boolean changed = false;
	        for (XTerm arg: this.arguments()) {
	        	if (arg == null)
	        		assert arg != null : "Argument to a formula cannot be null.";
	            XTerm a = arg.subst(y, x, propagate);
	            if (a != arg)
	                changed = true;
	            newArgs.add(a);
	        }
	        XFormula n = (XFormula) super.subst(y, x, propagate);
	        if (! changed)
	            return n;
	        if (n == this) n = (XFormula) clone();
	        n.arguments = newArgs;
	        return n;
	    }

	    public XName operator() {
	        return op;
	    }
	    
	    public XName asExprOperator() {
	        return asExprOp;
	    }

	    public boolean isUnary() {
	        return arguments.size() == 1;
	    }
	    public boolean isBinary() {
	        return arguments.size() == 2;
	    }
	    public XTerm unaryArg() {
	        if (isUnary())
	            return arguments.get(0);
	        return null;
	    }
	    public XTerm left() {
	        if (isBinary())
	            return arguments.get(0);
	        return null;
	    }
	    public XTerm right() {
	        if (isBinary())
	            return arguments.get(1);
	        return null;
	    }

	    public List<XTerm> arguments() { return arguments; }

	    public boolean hasVar(XVar v) {
	        for (XTerm arg : arguments) {
	            if (arg.hasVar(v))
	                return true;
	        }
	        return false;
	    }

	    public XPromise internIntoConstraint(XConstraint c, XPromise last) throws XFailure {
	        assert last == null;
	        // Evaluate left == right, if both are literals.
	        XPromise result = c.lookup(this);
	        if (result != null) // this term has already been interned.
	            return result;
	        HashMap<XName, XPromise> fields = CollectionFactory.newHashMap();
	        for (int i = 0; i < arguments.size(); i++) {
	            XTerm arg = arguments.get(i);
	            if (arg == null) {
	            	// System.err.println("XFormula_c: null arg in " + this);
	            	continue;
	            }
	            	
	            XPromise child = c.intern(arg);
	            fields.put(new XNameWrapper<Integer>(i), child);
	        }
	        // C_Local_c v = new C_Local_c(op);
	        XTerm v = this;
	        // fields.put(new C_NameWrapper<Integer>(-1), c.intern(v));
	        // create a new promise and return it.
	        XPromise p = new XPromise_c(fields, v);
	        c.addPromise(v, p);
	        result = p;
	        return result;
	    }

	    public String toString() {
	        StringBuilder sb = new StringBuilder();
	        sb.append(op);
	        sb.append("(");
	        String sep = "";
	        for (XTerm arg : arguments) {
	            sb.append(sep);
	            sep = ", ";
	            sb.append(arg);
	        }
	        sb.append(")");
	        return sb.toString();
	    }

	    public int hashCode() {
	        int hash = 29;
	        for (XTerm arg: arguments) {
	        	if (arg != null)
	            hash += arg.hashCode();
	        }
	        return hash;
	    }
	    
	    public boolean hasEQV() {
	        for (XTerm arg: arguments) {
	            if (arg.hasEQV())
	                return true;
	        }
	        return false;
	    }

	    /**
	     * An XFormula is equal t another object if it is == to it, or the other object
	     * is an XFormula with equal ops and equal args.
	     */
	    public boolean equals(Object o) {
	        if (this == o)
	            return true;
	        if (o instanceof XFormula) {
	            XFormula c = (XFormula) o;
	            if (! c.op.equals(op))
	            	return false;
	            if (c.arguments().size() == arguments().size()) {
	                for (int i = 0; i < arguments().size(); i++) {
	                    XTerm ti = arguments().get(i);
	                    XTerm ci = c.arguments().get(i);
	                    if (! ti.equals(ci))
	                        return false;
	                }
	                return true;
	            }
	        }
	        return false;
	    }

	    public XPromise toPromise() {
	        throw new Error("Not implemented yet.");
	    }
}

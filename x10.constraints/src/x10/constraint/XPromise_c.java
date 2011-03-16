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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of a Promise. The nodes in a graph maintained by a
 * constraint are either Promise_c's or other implementations of Promise, such
 * as C_Lit and C_Here.
 * 
 * <p>Invariant: If fields is not empty, then var is an XVar.
 * 
 * <p>Note: This class is not public. It is internal to the implementation of the constraint system. No 
 * client of the constraint system should use this class.
 * 
 * @author vj
 * 
 */
class XPromise_c implements XPromise, Serializable {
    /**
     * The externally visible XTerm that this node represents in the constraint
     * graph. May be null, if this promise corresponds to an internal node.
     */
    protected XTerm var;

    /**
     * This node may have been equated to another node, n. If so, value contains
     * the reference to n. Can be null.
     */
    protected XPromise value;
    
    /**
     * Lazily created collection of XPromises to which this one has been disequated.
     */
    protected Collection<XPromise> disEquals;

    /**
     * fields captures constraints on fields of this variable, if any.
     * Invariant: if value is not null, then fields must be null, the
     * constraints are translated to the target of the value.
     * 
     * We represent composite terms (e.g., binary operators, unary operators,
     * calls) as promises with fields representing the operator and the
     * operands.
     */
    protected Map<Object, XPromise> fields;

    /**
     * Create a new promise labeled with the external term c.
     * @param c -- the term labeling the promise.
     */
    public XPromise_c(XTerm term) {
        super();
        value = null;
        var = term;
    }

    /**
     * Create a new promise with the given fields. 
     * @param fields -- the fields of the promise
     */
    public XPromise_c(Map<Object, XPromise> fields) {
        this(fields, null);
    }

    /**
     * Create a new promise with the given fields, and labeled with the given term.
     * @param fields -- the fields of the promise
     * @param term -- the term labeling the promise
     */
    public XPromise_c(Map<Object, XPromise> fields, XTerm term) {
        this(term, null, fields);
    }

    /**
     * Create a new promise with the given fields, the given label (var) and
     * pointing to value. Either value==null || fields==null. var may also be null.
     * @param var -- the term labeling the promise
     * @param value -- the XTerm that this promise points to
     * @param fields -- the fields of the promise.
     */
    public XPromise_c(XTerm var, XPromise value, Map<Object, XPromise> fields) {
    	assert value==null || fields == null;
        this.var = var;
        this.value = value;
        if (fields != null)
            this.fields = new LinkedHashMap<Object, XPromise>(fields);
    }

    public XPromise_c cloneShallow() {
        return new XPromise_c(this.var);
    }
    /**
     * Transfer data from this to redirects(this).
     */
    public void transfer(Map<XPromise, XPromise> redirects) {
        XPromise_c newP = (XPromise_c) redirects.get(this);
        assert newP != null;
        if (fields != null) {
            newP.fields = CollectionFactory.<Object, XPromise> newHashMap(fields.size());
            for (Map.Entry<Object, XPromise> entry : fields.entrySet()) {
                newP.fields.put(entry.getKey(), getOrClone(entry.getValue(), redirects));
            }
        }
        if (value != null) {
            newP.value = getOrClone(value, redirects);
        }
        if (disEquals != null) {
            newP.disEquals = CollectionFactory.<XPromise> newHashSet(disEquals.size());
            for (XPromise t : disEquals) {
                newP.disEquals.add(getOrClone(t, redirects));
            }
        }
        
    }
    private XPromise getOrClone(XPromise t, Map<XPromise, XPromise> redirects) {
        XPromise newT = redirects.get(t);
        if (newT == null) {
            newT = t.cloneShallow();
            redirects.put(t, newT);
            t.transfer(redirects);
        }
        return newT;
    }
    /*
     * (non-Javadoc)
     * 
     * @see polyglot.ext.x10.types.constr.Promise#term()
     */
    public XTerm term() {
        return var;
    }

    public void setTerm(XTerm term) {
        Set<XPromise> visited = CollectionFactory.newHashSet();
        visited.add(this);
        setTerm(term, visited);
    }

    public void setTerm(XTerm term, Set<XPromise> visited) {
        var = term;
        if (var != null && fields != null) {
            for (Map.Entry<Object, XPromise> entry : fields.entrySet()) {
                Object key = entry.getKey();
                XPromise p = entry.getValue();
                if (visited.contains(p))
                    continue;
                visited.add(p);
                assert p.term() instanceof XField : term + "." + key + " = " + p + " (not a field)";
                XField<?> f = (XField<?>) p.term();
                assert f.field().equals(key) : term + "." + key + " = " + p + " (different field)";
                p.setTerm(f.copy((XVar) term), visited);
            }
        }
    }

    public boolean forwarded() {
        return value != null;
    }

    public boolean hasChildren() {
        return fields != null;
    }

    public void setVar(XVar v) {
        var = v;
    }

    protected XPromise_c clone() {
        XPromise_c result = null;
        try {
            result = (XPromise_c) super.clone();
        }
        catch (CloneNotSupportedException z) {
            // But it is!
        }
        return result;
    }

    int lookupReturnValue;

    public int lookupReturnValue() {
        return lookupReturnValue;
    }

    public XPromise lookup(XVar[] vars, int index)  {
        assert index >= 1;

        // follow the eq link if there is one.
        if (value != null)
            return value.lookup(vars, index);
        if (index == vars.length) {
            lookupReturnValue = index;
            return this;
        }
        if (fields == null) {
            lookupReturnValue = index;
            return this;
        }
        assert vars[index] instanceof XField;
        XField<?> f = (XField<?>) vars[index];
        Object field = f.field();
        // check this edge already exists.
        XPromise p = fields.get(field);
        if (p == null) {
            lookupReturnValue = index;
            return this;
        }
        return p.lookup(vars, index + 1);
    }

    public XPromise lookup() {
        if (value != null)
            return value.lookup();
        return this;
    }

    public XPromise lookup(Object s) {
        // follow the eq link if there is one.
        if (value != null)
            return value.lookup(s);
        if (fields == null)
            return null;
        // check this edge already exists.
        XPromise p = fields.get(s);
        return p == null ? null : p.lookup();
    }

    public XPromise intern(XVar[] vars, int index) throws XFailure {
        return intern(vars, index, null);
    }

    public XPromise intern(XVar[] vars, int index, XPromise last)  {
        assert index >= 1;

        // follow the eq link if there is one.
        if (value != null )
            return value.intern(vars, index, last);
        if (index == vars.length)
            return this;
        // if not, we need to add this path here. Ensure fields is initialized.
        if (fields == null)
            fields = CollectionFactory.<Object, XPromise>newHashMap();
        assert vars[index] instanceof XField;
        XField<?> f = (XField<?>) vars[index];
        Object s = f.field();
        // check this edge already exists.
        XPromise p = (XPromise) fields.get(s);
        if (p == null) {
            // no edge. Create a new promise and add this edge.
            p = (index == vars.length - 1 && last != null) 
            ? last : new XPromise_c(var instanceof XVar ? f.copy((XVar) var) : f);
            fields.put(s, p);
        }
        // recursively, intern the rest of the path on the child.
        return p.intern(vars, index + 1, last);
    }

    public void addIn(Object s, XPromise orphan) throws XFailure {
        if (value != null) {
            // Alternative is to fwd it blindly, that would be correct, but i
            // want to know
            // if this is happening. It should not happen.
            throw new XFailure("The node " + this + " is forwarded to " 
                               + value + "; the " + s + " child, " + orphan 
                               + ", cannot be added to it.");
        }

        if (fields == null)
            fields = new LinkedHashMap<Object, XPromise>();
        XPromise child = (XPromise) fields.get(s);

        if (child != null) {
            while (child.forwarded())
                child = child.value();
            while (orphan.forwarded())
                orphan = orphan.value();

            orphan.bind(child);
            return;
        }
        fields.put(s, orphan);
    }
    
   

    public boolean bind(/* @nonnull */XPromise target) throws XFailure {
        assert target.value() == null;

        if (disEquals != null) 
      	  for (XPromise i : disEquals) 
      		  // Note: i's value may be set.. so need to get to the end of the chain.
      		  if (i.lookup().equals(target))
      			  throw new XFailure("The promise " + this 
      					  + " cannot be bound to the already disequated " 
      					  + target + ".");
          
        if (!(target.equals(value)) && !target.equals(var)) {
            if (forwarded())
                throw new XFailure("The promise " + this + 
                                   " is already bound to " + value 
                		+ "; cannot bind it to " + target + ".");
            if (this == target) // nothing to do!
                return false;

            // Check for cycles!
            if (canReach(target) || target.canReach(this))
                throw new XFailure("Binding " + this + " to " + target + " creates a cycle.");
            if ((!term().prefersBeingBound() && target.term().prefersBeingBound())
            		){
                return target.bind(this);
            }
            value = target;
        }
        if (fields != null) { // transfer fields
            for (Map.Entry<Object, XPromise> i : fields.entrySet()) {
                target.addIn(i.getKey(), i.getValue());
            }
            fields = null;
        }
        
        if (disEquals != null) { // transfer disequals links
        	  for (XPromise i : disEquals) 
                  target.addDisEquals(i.lookup());
              disEquals = null;
        }
        return true;
    }
    
    public boolean disBind(/* @nonnull */XPromise target) throws XFailure {
        assert target.value() == null;
        assert forwarded() == false;
        if (disEquals == null) disEquals = CollectionFactory.newHashSet();
        boolean result = disEquals.add(target);
        return result;
        //if (!target.equals(value) && !target.equals(var)) {
    }

    /**
     * Can this promise reach p in the directed graph representing the
     * constraints?
     */
    public boolean canReach(XPromise p) {
        if (p == this)
            return true;
        if (value != null)
            return value.canReach(p);
        if (fields != null)
        	for (XPromise q : fields.values())
                if (q.canReach(p))
                    return true;
        return false;
    }
    
    public boolean canReachThroughValue(XPromise p) {
    	XPromise temp = this;
    	while (temp != null) {
    		if (temp == p)
    			return true;
    		temp = temp.value();
    	}
        return false;
    }

    public boolean visit(XVar path, boolean dumpEQV, boolean hideFake, XGraphVisitor xg) {
        XTerm t1 = path == null? term() : path;
        if (t1 == null)
            return true;
        if (t1.isAtomicFormula()) {
            return xg.visitAtomicFormula(t1);
        }

        if (value != null) {
                if (dumpEQV || ! t1.hasEQV()) {
                    XTerm t2 = lookup().var();
                    if (hideFake && t1 instanceof XField && ((XField) t1).isHidden())
                        return true;
                    if (hideFake && t2 instanceof XField && ((XField) t2).isHidden())
                        return true;
                    if (dumpEQV || ! t2.hasEQV())
                        return xg.visitEquals(t1, t2);
                }
            return true;
        }
        
        if (fields != null) 
            if (dumpEQV || ! t1.hasEQV())  {
                XVar v = t1 instanceof XVar ? (XVar) t1 : null;
                // If t1 is not an XVar, it is an atomic formula, and the fields are its subterms.
                // hence v shd be null.
                for (Map.Entry<Object,XPromise> m : fields.entrySet()) {
                    Object name = m.getKey();
                    XPromise p = m.getValue();
                    XTerm t = p.term();
                    XVar path2 = null;
//                  if (v != null && !(t instanceof XField && ((XField) t).receiver().equals(v))) {
//                      assert false;
////                        path2 = XTerms.makeField(v, name);
//                  }
//                  path2 = v == null ? null : (XVar) t;
                  
                    if (v == null) {
                        path2 = null;
                    } else {
                        assert t instanceof XField<?>;
                        XField<?> ft = (XField<?>) t;
                        path2 = ft.copy(v);
                    }
                    boolean result = p.visit(path2,dumpEQV, hideFake, xg);
                    if (!result)
                        return result;
                }
            }
        if (disEquals != null) {
            if (dumpEQV || ! t1.hasEQV())
                for (XPromise i : disEquals) {
                    boolean result = xg.visitDisEquals(t1, i.lookup().var());
                    if (! result)
                        return result;
                }
               
        }
        return true;
    }
    
    private boolean toStringMark = false;
    public String toString() {
        if (toStringMark)
            return "...";
        toStringMark = true;
        String res = var + ((value != null)
                ? "->" + value 
                        : ((fields != null) ? fields.toString() : "")
                        + (disEquals != null ? " != " + disEquals.toString() : ""));
        toStringMark = false;
        return res;
    }

    public void replaceDescendant(XPromise y, XPromise x, XConstraint c) {
        if (value != null ) {
            if (value.equals(x)) {
                if (this.equals(y)) {
                    // don't create a self-cycle; it's redundant!
                    value = null;
                }
                else {
                    value = y;
                }
            }
            else {
                value.replaceDescendant(y, x, c);
            }
        }

        if (fields != null) {
            for (Map.Entry<Object, XPromise> p : fields.entrySet()) {
                Object key = p.getKey();
                XPromise val = p.getValue();
                // doing this / self, and val == self.home -> this.home
                // add this.home / self.home to the replacements to perform
                if (val.equals(x)) {
                    p.setValue(y);
                }
                else {
                    val.replaceDescendant(y, x, c);
                }
            }
        }
    }

    public XPromise value() {
        return value;
    }

    public XTerm var() {
        return var;
    }

    public Map<Object, XPromise> fields() {
        return fields;
    }
    public void addDisEquals(XPromise other) {
    	if (disEquals == null) 
    		disEquals = CollectionFactory.newHashSet();
    	disEquals.add(other);
    }
    public boolean hasDisBindings() { 
    	return disEquals != null || ! disEquals.isEmpty();
    }
    public boolean isDisBoundTo(XPromise other) {
    	if (disEquals == null)
    		return false;
    	for (XPromise p : disEquals) {
    		if (p.canReach(other))
    			return true;
    	}
    	return false;
    }
}

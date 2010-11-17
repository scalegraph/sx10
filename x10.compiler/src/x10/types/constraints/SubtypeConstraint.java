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

package x10.types.constraints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Copy;
import polyglot.util.InternalCompilerError;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.types.matcher.Subst;

public class SubtypeConstraint implements Copy, Serializable {
	private static final long serialVersionUID = 4026637095619421750L;

    public enum Kind {
        SUBTYPE, // <:
        EQUAL, // ==
        HASZERO}; // haszero
	
    
	Kind KIND;
    Type subtype;
    Type supertype;
   

    public SubtypeConstraint(Type subtype, Type supertype, Kind kind) {
    	 this.subtype = subtype;
         this.supertype = supertype;
         this.KIND = kind;
        if (isHaszero())
            assert subtype!=null && supertype==null;
        else
            assert subtype!=null && supertype!=null;
    }
 
    public SubtypeConstraint(Type subtype, Type supertype, boolean equals) {
    	this(subtype, supertype, 
    			equals ? Kind.EQUAL : Kind.SUBTYPE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see x10.types.SubtypeConstraint#isEqualityConstraint()
     */
    public boolean isEqualityConstraint() {
        return KIND==Kind.EQUAL;
    }
    public boolean isSubtypeConstraint() {
        return KIND==Kind.SUBTYPE;
    }
    public boolean isHaszero() {
        return KIND==Kind.HASZERO;
    }
   
    public boolean isKind(Kind k) { return k==KIND;}
    public Kind kind() { return KIND;}
    
    /*
     * (non-Javadoc)
     * 
     * @see x10.types.SubtypeConstraint#subtype()
     */
    public Type subtype() {
        return subtype;
    }

    /*
     * (non-Javadoc)
     * 
     * @see x10.types.SubtypeConstraint#supertype()
     */
    public Type supertype() {
        return supertype;
    }

    public SubtypeConstraint copy() {
        try {
            return (SubtypeConstraint) super.clone();
        }
        catch (CloneNotSupportedException e) {
            assert false;
            return this;
        }
    }

    public static Type subst(Type t, XTerm y, XVar x) {
        try {
            return Subst.subst(t, y, x);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see x10.types.TypeConstraint#subst(x10.constraint.XTerm,
     * x10.constraint.XVar, boolean)
     */
    public SubtypeConstraint subst(XTerm y, XVar x) {
        Type left = subtype();
        Type l = subst(left, y, x);
        Type right = supertype();
        Type r = subst(right, y, x);

        if (l == left && r == right)
            return this;

        SubtypeConstraint n = copy();
        n.subtype = l;
        n.supertype = r;
        return n;
    }

    @Override
    public String toString() {
        return subtype() +
                (isHaszero() ? " haszero" :
                (isEqualityConstraint() ? " == " : " <: ") + supertype());
    }


}

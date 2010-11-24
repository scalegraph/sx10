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
import java.util.Collection;

import polyglot.util.Copy;
import x10.constraint.XFailure;
import x10.constraint.XTerm;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.types.ConstrainedType;
import x10.types.MacroType;
import x10.types.ParameterType;
import x10.types.ParameterType_c;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import polyglot.types.Context;
import x10.types.X10ProcedureDef;
import x10.types.X10ProcedureInstance;
import x10.types.X10TypeMixin;
import x10.types.X10Context_c;
import x10.types.TypeParamSubst;
import polyglot.types.TypeSystem;
import x10.types.ParameterType.Variance;
import polyglot.types.Name;
import polyglot.types.PrimitiveType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;

/**
 * Todo: This needs to be fixed. The constraints in this have to be used to figure
 * out whether c is entailed. This needs a proper constraint representation, e.g.
 * X <: Y, Y <: Z |- X <: Z
 * 
 * @author njnystrom
 * @author vj
 *
 */
public class TypeConstraint implements Copy, Serializable {
    private static final long serialVersionUID = -6305620393028945867L;

    List<SubtypeConstraint> terms;

    public TypeConstraint() {
        terms = new ArrayList<SubtypeConstraint>();
    }

    private void addTypeParameterBindings(X10ClassDef xcd, X10ClassType xct, Type ytype) throws XFailure {
        if (ytype == null)
            return;

        if (ytype instanceof ConstrainedType) {
            ConstrainedType ct = (ConstrainedType) ytype;
            addTypeParameterBindings(xcd, xct, ct.baseType().get());
        }

        if (ytype instanceof MacroType) {
            MacroType mt = (MacroType) ytype;
            addTypeParameterBindings(xcd, xct, mt.definedType());
        }

        if (ytype instanceof X10ClassType) {
            X10ClassType yct = (X10ClassType) ytype;
            X10ClassDef ycd = yct.x10Def();
            if (ycd == xcd) {
                if (xct.typeArguments() != null && yct.typeArguments() != null) {
                for (int i = 0; i < yct.typeArguments().size(); i++) {
                    Type xt = xct.typeArguments().get(i);
                    Type yt = yct.typeArguments().get(i);
                    ParameterType.Variance v = xcd.variances().get(i);
                    TypeSystem xts = (TypeSystem) xcd.typeSystem();
                    switch (v) {
                    case INVARIANT: {
                        addTerm(new SubtypeConstraint(xt, yt, true));
                        break;
                    }
                    case CONTRAVARIANT: {
                        addTerm(new SubtypeConstraint(xt, yt, false));
                        break;
                    }
                    case COVARIANT: {
                        addTerm(new SubtypeConstraint(yt, xt, false));
                        break;
                    }
                    }
                }
                }
            }
            else {
                addTypeParameterBindings(xcd, xct, yct.superClass());
                for (Type t: yct.interfaces()) {
                    addTypeParameterBindings(xcd, xct, t);
                }
            }
        }
    }

    /**
     * Add the binding xtype = ytype to this.
     * @param xtype
     * @param ytype
     * @throws XFailure
     */
    public void addTypeParameterBindings(Type xtype, Type ytype) throws XFailure {
    	if (xtype instanceof ParameterType) {
    		TypeSystem xts = (TypeSystem) xtype.typeSystem();
    		//	    XVar Xi = xts.xtypeTranslator().transTypeParam((ParameterType) xtype);
    		//	    XTerm Yi = xts.xtypeTranslator().trans(ytype);
    		//	    env.addBinding(Xi, Yi);
    		addTerm(new SubtypeConstraint(ytype, xtype, false));
    	}
    	if (xtype instanceof X10ClassType) {
    		X10ClassType xct = (X10ClassType) xtype;
    		X10ClassDef xcd = xct.x10Def();
    		addTypeParameterBindings(xcd, xct, ytype);
    	}
    	if (xtype instanceof ConstrainedType) {
    		ConstrainedType ct = (ConstrainedType) xtype;
    		addTypeParameterBindings(ct.baseType().get(), ytype);
    	}
    	if (xtype instanceof MacroType) {
    		MacroType mt = (MacroType) xtype;
    		addTypeParameterBindings(mt.definedType(), ytype);
    	}
    	if (xtype instanceof PrimitiveType) {
    		// Nothing to do
    	}
    }

    /**
     * Modifies "this" to match the unification of the two types.
     * Returns true if the two types are unifiable.
     * @param t1
     * @param t2
     * @return
     */
    public boolean unify(Type t1, Type t2, TypeSystem xts) {
    	final Context emptyContext = (Context) t1.typeSystem().emptyContext();
    	t1 = X10TypeMixin.stripConstraints(t1);
    	t2 = X10TypeMixin.stripConstraints(t2);   	
    	if (xts.typeEquals(t1, t2, emptyContext /*dummy*/))
            return true;
    	if ((t1 instanceof ParameterType) || (t2 instanceof ParameterType)) {
    		addTerm(new SubtypeConstraint(t1, t2, SubtypeConstraint.Kind.EQUAL));
    		if (! (consistent(emptyContext)))
    			return false;
    	}
    	if ((t1 instanceof X10ClassType) && (t2 instanceof X10ClassType)) {
    		X10ClassType xt1 = (X10ClassType) t1;
    		X10ClassType xt2 = (X10ClassType) t2;
    		Type bt1 = xt1.x10Def().asType();
    		Type bt2 = xt2.x10Def().asType();
    		if (!xts.typeEquals(bt1,bt2, emptyContext)) {
    			return false;
    		}
    		List<Type> args1 = xt1.typeArguments();
    		List<Type> args2 = xt2.typeArguments();
    		if (args1 == null && args2 == null) {
    		    return true;
    		}
    		if (args1 == null || args2 == null) {
    		    return false;
    		}
    		if (args1.size() != args2.size()) {
    			return false;
    		}
    
    		for (int i=0; i < args1.size(); ++i) {
    			Type p1 = args1.get(i);
    			Type p2 = args2.get(i);
    			boolean res = unify(p1,p2,xts);
    			if (!res) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    public List<SubtypeConstraint> terms() {
        return terms;
    }

    public TypeConstraint copy() {
        try {
            return (TypeConstraint) super.clone();
        }
        catch (CloneNotSupportedException e) {
            assert false;
            return this;
        }
    }
    
    public TypeConstraint addIn(TypeConstraint c) {
        terms.addAll(c.terms());
        return this;
    }
    
    public void addTerm(SubtypeConstraint c) {
        terms.add(c);
    }

    public void addTerms(Collection<SubtypeConstraint> terms) {
        this.terms.addAll(terms);
    }

    public boolean entails(TypeConstraint c, Context context) {
        Context xc = ((X10Context_c)context).pushTypeConstraintWithContextTerms(this);  
        return c.consistent(xc);
    }

    public boolean consistent(Context context) {
        TypeSystem ts = context.typeSystem();
        for (SubtypeConstraint t : terms()) {
            if (t.isEqualityConstraint()) {
                if (! ts.typeEquals(t.subtype(), t.supertype(), context)) {
                    return false;
                }
            }
            else if (t.isSubtypeConstraint()) {
                if (! ts.isSubtype(t.subtype(), t.supertype(), context)) {
                    return false;
                }
            } else if (t.isHaszero()) {
                if (!X10TypeMixin.isHaszero(t.subtype(),context))
                    return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see x10.types.TypeConstraint#subst(x10.constraint.XTerm,
     * x10.constraint.XVar, boolean)
     */
    public TypeConstraint subst(XTerm y, XVar x) {
        TypeConstraint c = new TypeConstraint();
        List<SubtypeConstraint> l = c.terms;
        for (SubtypeConstraint s : terms) {
            l.add(s.subst(y, x));
        }
        return c;
    }

    @Override
    public String toString() {
        return terms.toString();
    }

	public void checkTypeQuery( TypeConstraint query, XVar ythis, XVar xthis, XVar[] y, XVar[] x, 
			 Context context) throws SemanticException {
		 if (! consistent(context)) {
	         throw new SemanticException("Call invalid; type environment is inconsistent.");
	     }
	    if (query != null) {
	    	 if ( ! ((TypeSystem) context.typeSystem()).consistent(query, context)) {
	             throw new SemanticException("Type guard " + query + " cannot be established; inconsistent in calling context.");
	         }
	        TypeConstraint query2 = xthis==null ? query : query.subst(ythis, xthis);
	        for (int i = 0; i < y.length; i++)
	            query2 = query2.subst(y[i], x[i]);
	        if (! entails(query2, context)) {
	            throw new SemanticException("Call invalid; calling environment does not entail the method guard.");
	        }
	    }
		
	}

	public static <PI extends X10ProcedureInstance<?>> Type[] inferTypeArguments(PI me, Type thisType, List<Type> actuals, List<Type> formals, 
			List<Type> typeFormals, Context context) throws SemanticException {
	    TypeSystem xts = (TypeSystem) thisType.typeSystem();
	
	    TypeConstraint tenv = new TypeConstraint();
	    CConstraint env = new CConstraint();
	
	    XVar ythis = X10TypeMixin.selfVar(thisType);
	
	    if (ythis == null) {
	        CConstraint c = X10TypeMixin.xclause(thisType);
	        c = (c == null) ? new CConstraint() : c.copy();
	
	        try {
	            ythis = XTerms.makeUQV(); // xts.xtypeTranslator().genEQV(thisType, false);
	            c.addSelfBinding(ythis);
	            c.setThisVar(ythis);
	        }
	        catch (XFailure e) {
	            throw new SemanticException(e.getMessage(), me.position());
	        }
	
	        thisType = X10TypeMixin.xclause(X10TypeMixin.baseType(thisType), c);
	    }
	
	    assert actuals.size() == formals.size();
	
	    ParameterType[] X = new ParameterType[typeFormals.size()];
	    Type[] Y = new Type[typeFormals.size()];
	    Type[] Z = new Type[typeFormals.size()];
	    XVar[] x = new XVar[formals.size()];
	    XVar[] y = new XVar[formals.size()];
	
	    for (int i = 0; i < typeFormals.size(); i++) {
	        Type xtype = typeFormals.get(i);
	        xtype = xts.expandMacros(xtype);
	        Type ytype = new ParameterType_c(xts, me.position(), Name.makeFresh(), Types.ref((X10ProcedureDef) me.def()));
	
	        // TODO: should enforce this statically
	        if (! (xtype instanceof ParameterType))
	        assert xtype instanceof ParameterType : xtype + " is not a ParameterType, is a " + (xtype != null ? xtype.getClass().getName() : "null");
	
	        tenv.addTerm(new SubtypeConstraint(xtype, ytype, true));
	
	        X[i] = (ParameterType) xtype;
	        Y[i] = ytype;
	        Z[i] = ytype;
	    }
	
	    for (int i = 0; i < formals.size(); i++) {
	        Type xtype = formals.get(i);
	        Type ytype = actuals.get(i);
	
	        xtype = xts.expandMacros(xtype);
	        ytype = xts.expandMacros(ytype);
	
	        // Be sure to copy the constraints since we use the self vars
	        // in other constraints and don't want to conflate them if
	        // realX returns the same constraint twice.
	        final CConstraint yc = X10TypeMixin.realX(ytype).copy();
	
	        XVar xi;
	        XVar yi;
	
	        yi = X10TypeMixin.selfVar(yc);
	
	        if (yi == null) {
	            // This must mean that yi was not final, hence it cannot occur in 
	            // the dependent clauses of downstream yi's.
	            yi = XTerms.makeUQV(); // xts.xtypeTranslator().genEQV(ytype, false);
	        }
	
	        try {
	            tenv.addTypeParameterBindings(xtype, ytype);
	        }
	        catch (XFailure f) {
	        }
	
	        CConstraint xc = X10TypeMixin.realX(xtype).copy();
	        xi = xts.xtypeTranslator().trans(me.formalNames().get(i), xtype);
	
	        x[i] = xi;
	        y[i] = yi;
	    }
	
	    // We'll subst selfVar for THIS.
	    XVar xthis = null; // xts.xtypeTranslator().transThis(thisType);
	
	    if (me.def() instanceof X10ProcedureDef)
	        xthis = (XVar) ((X10ProcedureDef) me.def()).thisVar();
	
	    if (xthis == null)
	        xthis = XTerms.makeLocal(XTerms.makeFreshName("this"));
	
	    try {
	        expandTypeConstraints(tenv, context);
	    }
	    catch (XFailure f) {
	    }

	    // Create a big query for inferring type parameters.
	    // LIMITATION: can only infer types when actuals are subtypes of formals.
	    // This updates Y with new actual type arguments.
	    inferTypeArguments(context, me, tenv, X, Y, Z, x, y, ythis, xthis);
	
	    for (int i = 0; i < Z.length; i++) {
	        if (Y[i] == Z[i])
	            throw new SemanticException("Cannot infer type for type parameter " + X[i] + ".", me.position());
	    }
	
	    return Y;
	}

	private static void expandTypeConstraints(TypeConstraint tenv, Context context) throws XFailure {
	    List<SubtypeConstraint> originalTerms = new ArrayList<SubtypeConstraint>(tenv.terms());
	    for (SubtypeConstraint term : originalTerms) {
	        expandTypeConstraints(tenv, term, context);
	    }
	}

	/**
	 * Expand generic constraints in the type environment.
	 * If we have a constraint on two generic types, <code>A[X]</code> and <code>B[Y]</code>,
	 * also add the appropriate constraint on the parameter types <code>X</code> and <code>Y</code>.
	 * Here are the possibilities:
	 * <table border="1"><tr><td valign="top">
	 * 1. A[X] == A[Y] </td><td colspan="2"> X==Y                                                                      </td></tr><tr><td valign="top">
	 * 2. A[X] == B[Y] </td><td colspan="2"> not consistent                                                            </td></tr><tr><td rowspan="3" valign="top">
	 * 3. A[X] <: A[Y] </td><td>             A[ T] (invariant)?     </td><td> X==Y                                     </td></tr><tr><td>
	 *                                       A[+T] (covariant)?     </td><td> X<:Y                                     </td></tr><tr><td>
	 *                                       A[-T] (contravariant)? </td><td> X:>Y                                     </td></tr><tr><td rowspan="10" valign="top">
	 * 4. A[X] <: B[Y] </td><td>             A[ T] <: B[ T]         </td><td> X==Y                                     </td></tr><tr><td>
	 *                                       A[+T] <: B[ T]         </td><td> X<:Y                                     </td></tr><tr><td>
	 *                                       A[-T] <: B[ T]         </td><td> X:>Y                                     </td></tr><tr><td>
	 *                                       A[ T] <: B[+T]         </td><td> X<:Y                                     </td></tr><tr><td>
	 *                                       A[+T] <: B[+T]         </td><td> X<:Y                                     </td></tr><tr><td>
	 *                                       A[-T] <: B[+T]         </td><td> no constraint on X and Y                 </td></tr><tr><td>
	 *                                       A[ T] <: B[-T]         </td><td> X:>Y                                     </td></tr><tr><td>
	 *                                       A[+T] <: B[-T]         </td><td> no constraint on X and Y                 </td></tr><tr><td>
	 *                                       A[-T] <: B[-T]         </td><td> X:>Y                                     </td></tr><tr><td>
	 *                                       A[T] <: B[S] && T??S   </td><td> X??Y (instantiate constraint on T and S) </td></tr><tr><td>
	 * 5. exists Q s.t. A <: Q[X] <: B[Y] </td><td colspan="2"> ??? </td>
	 * </tr></table>
	 * FIXME: Only the equality case (1) and the same type case (3) are handled for now.  Also "haszero" is not expanded.
	 */
	private static void expandTypeConstraints(TypeConstraint tenv, SubtypeConstraint term, Context context) throws XFailure {
        if (term.isHaszero()) return;

	    TypeSystem xts = (TypeSystem) context.typeSystem();
	    Type b = xts.expandMacros(term.subtype());
	    Type p = xts.expandMacros(term.supertype());
	    if (!b.isClass() || !p.isClass()) return;
	    X10ClassType sub = (X10ClassType) b.toClass();
	    X10ClassType sup = (X10ClassType) p.toClass();
	    List<Type> subTypeArgs = sub.typeArguments();
	    List<Type> supTypeArgs = sup.typeArguments();
	    if (term.isEqualityConstraint()) {
	        X10ClassDef def = sub.x10Def();
	        if (def != sup.x10Def()) return; // skip case 2
	        if (subTypeArgs == null || supTypeArgs == null) return;
	        if (subTypeArgs.isEmpty() || subTypeArgs.size() != supTypeArgs.size()) return;
	        for (int i = 0; i < subTypeArgs.size(); i++) {
	            Type ba = subTypeArgs.get(i);
	            Type pa = supTypeArgs.get(i);
	            if (xts.typeEquals(ba, pa, context)) continue;
	            SubtypeConstraint eq = new SubtypeConstraint(ba, pa, true);
                tenv.addTerm(eq);
                expandTypeConstraints(tenv, eq, context);
	        }
	    }
	    else {
            assert term.isSubtypeConstraint();
	        X10ClassDef def = sub.x10Def();
	        if (def != sup.x10Def()) return; // FIXME: skip cases 4 and 5
	        if (subTypeArgs == null || supTypeArgs == null) return;
	        if (subTypeArgs.isEmpty() || subTypeArgs.size() != supTypeArgs.size()) return;
	        List<Variance> variances = def.variances();
	        for (int i = 0; i < subTypeArgs.size(); i++) {
	            Type ba = subTypeArgs.get(i);
	            Type pa = supTypeArgs.get(i);
	            if (xts.typeEquals(ba, pa, context)) continue;
	            SubtypeConstraint eq = null;
	            switch (variances.get(i)) {
	            case INVARIANT:
	                eq = new SubtypeConstraint(ba, pa, true);
	                break;
	            case COVARIANT:
	                eq = new SubtypeConstraint(ba, pa, false);
	                break;
	            case CONTRAVARIANT:
	                eq = new SubtypeConstraint(pa, ba, false);
	                break;
	            }
	            tenv.addTerm(eq);
                expandTypeConstraints(tenv, eq, context);
	        }
	    }
	}

	private static <PI extends X10ProcedureInstance<?>> void inferTypeArguments(Context context, PI me, TypeConstraint tenv,
	        ParameterType[] X, Type[] Y, Type[] Z, XVar[] x, XVar[] y, XVar ythis, XVar xthis) throws SemanticException
	{
	    TypeSystem xts = (TypeSystem) me.typeSystem();

	    for (int i = 0; i < Y.length; i++) {
	        Type Yi = Y[i];

	        List<Type> upper = new ArrayList<Type>();
	        List<Type> lower = new ArrayList<Type>();

	        List<Type> worklist = new ArrayList<Type>();
	        worklist.add(Yi);

	        for (int j = 0; j < worklist.size(); j++) {
	            Type m = worklist.get(j);
	            for (SubtypeConstraint term : tenv.terms()) {
	                SubtypeConstraint eq = term;
                    if (term.isHaszero()) continue;
	                Type sub = eq.subtype();
	                Type sup = eq.supertype();
	                if (term.isEqualityConstraint()) {
	                    if (m.typeEquals(sub, context)) {
	                        if (!upper.contains(sup))
	                            upper.add(sup);
	                        if (!lower.contains(sup))
	                            lower.add(sup);
	                        if (!worklist.contains(sup))
	                            worklist.add(sup);
	                    }
	                    if (m.typeEquals(sup, context)) {
	                        if (!upper.contains(sub))
	                            upper.add(sub);
	                        if (!lower.contains(sub))
	                            lower.add(sub);
	                        if (!worklist.contains(sub))
	                            worklist.add(sub);
	                    }
	                }
	                else {
                        assert term.isSubtypeConstraint();
	                    if (m.typeEquals(sub, context)) {
	                        if (!upper.contains(sup))
	                            upper.add(sup);
	                        if (!worklist.contains(sup))
	                            worklist.add(sup);
	                    }
	                    if (m.typeEquals(sup, context)) {
	                        if (!lower.contains(sub))
	                            lower.add(sub);
	                        if (!worklist.contains(sub))
	                            worklist.add(sub);
	                    }
	                }
	            }
	        }

	        for (Type Xi : X) {
	            upper.remove(Xi);
	            lower.remove(Xi);
	        }
	        for (Type Zi : Z) {
	            upper.remove(Zi);
	            lower.remove(Zi);
	        }

	        Type upperBound = null;
	        Type lowerBound = null;

	        for (Type t : upper) {
	            if (t != null) {
	                if (upperBound == null)
	                    upperBound = t;
	                else
	                    upperBound = X10TypeMixin.meetTypes(xts, upperBound, t, context);
	            }
	        }

	        for (Type t : lower) {
	            if (t != null) {
	                if (lowerBound == null)
	                    lowerBound = t;
	                else
	                    lowerBound = xts.leastCommonAncestor(lowerBound, t, context);
	            }
	        }

	        if (upperBound != null)
	            Y[i] = upperBound;
	        else if (lowerBound != null)
	            Y[i] = lowerBound;
	        else {
	        	/*System.err.println("(Diagnostic) No constraint on type parameters. " 
	        			 +
	        			"Returning Any instead of throwing an exception."
	        			 + (X[i] != null ? "\n\t: Position: " +  X[i].position().toString() : ""));
	        			 */
	        	Y[i] = xts.Any();
	           // throw new SemanticException("Could not infer type for type parameter " + X[i] + ".", me.position());
	        }
	    }
	}
}

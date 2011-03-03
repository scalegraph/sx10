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

package x10.types.matcher;

import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem_c;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import x10.constraint.XFailure;
import x10.constraint.XVar;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.errors.Errors;
import x10.types.ParameterType;
import polyglot.types.Context;
import x10.types.X10FieldInstance;
import polyglot.types.TypeSystem;
import x10.types.constraints.CConstraint;

public class X10FieldMatcher {
    public static Type instantiateAccess(Type container, Type t, XVar oldThis, boolean contextKnowsReceiver) throws SemanticException {
        assert container!=null && t!=null;
        CConstraint c = Types.xclause(container);
        
        // Let v be the symbolic name for the target. If there is none, we make one up.
        // Let t = T{tc}, and ct = U{c}.
        // If c does not have a selfVarBinding, then we want to set t to
        // t = T{exists vv. (tc,this==vv),ct[vv/self]}
        // If c does have a selfVarBinding, v, then we want to set t to
        // t = T{exists v. (tc, this=v, ct)}
        XVar v = Types.selfVarBinding(container);
        XVar vv = null;
        if (v == null) {
        	v = vv =XTerms.makeUQV();
        }
        if (oldThis != null && v == null && vv==null)
        	assert false;
        /*if (c != null)
        	c = c.copy().instantiateSelf(v);*/

        { // Update t
            CConstraint tc = Types.realX(t).copy();

            if (! contextKnowsReceiver)
                tc.addIn(v, c);

            t = Types.constrainedType(Types.baseType(t), tc);
            t = Subst.subst(t,
                            new XVar[] {v},
                            new XVar[] {oldThis},
                            new Type[] {}, new ParameterType[] {});
            if (vv != null) { // Hide vv, i.e. substitute in an anonymous EQV
                t = Subst.subst(t,
                                new XVar[] {XTerms.makeEQV()},
                                new XVar[] {vv},
                                new Type[] {}, new ParameterType[] {});
            }
            final CConstraint tmpTc = Types.realX(t).copy();
            tmpTc.addIn(v,c);
            if (! tmpTc.consistent()) {
                throw new Errors.InconsistentType(t, t.position());
            }
        }
        return t;
    }
    public static X10FieldInstance instantiateAccess(X10FieldInstance fi, Name name, Type container, boolean contextKnowsReceiver, Context context) throws SemanticException {
	    if (! fi.name().equals(name)) {
		return null;
	    }
        TypeSystem ts = (TypeSystem) fi.typeSystem();
        Type t = fi.type();
        Type rt = fi.rightType();
        
        // Now need to figure out the type of the field, from the declaration of the field
        // in the container, and the type of the container.
        // The task is to transfer constraints from the target to the field.
        Type ct = container != null ? container : fi.container();
        CConstraint c = Types.xclause(ct);

        // Let v be the symbolic name for the target. If there is none, we make one up.
        // Let t = T{tc}, and ct = U{c}.
        // If c does not have a selfVarBinding, then we want to set t to
        // t = T{exists vv. (tc,this==vv),ct[vv/self]}
        // If c does have a selfVarBinding, v, then we want to set t to
        // t = T{exists v. (tc, this=v, ct)}
        XVar v = Types.selfVarBinding(ct);
        XVar vv = null;
        if (v == null) {
        	v = vv =XTerms.makeUQV();
        }
        XVar oldThis = fi.x10Def().thisVar();
        if (oldThis != null && v == null && vv==null)
        	assert false;
        /*if (c != null)
        	c = c.copy().instantiateSelf(v);*/

        t = instantiateAccess(ct,t,oldThis, contextKnowsReceiver);
        rt = instantiateAccess(ct,rt,oldThis, contextKnowsReceiver);
        
        //rt = Subst.subst(rt, (new XVar[] { w }), (new XVar[] { oldThis }), new Type[] {}, new ParameterType[] {});
        //if (v != null)
        //	rt = X10TypeMixin.setThisVar(rt, v);
        // }

        if (!ts.consistent(t, (Context) context)) {
            throw new Errors.InconsistentType(t, Position.COMPILER_GENERATED);
        }
        if (!ts.consistent(rt, (Context) context)) {
            throw new Errors.InconsistentType(rt, Position.COMPILER_GENERATED);
        }

        return fi.type(t, rt);
    }
}
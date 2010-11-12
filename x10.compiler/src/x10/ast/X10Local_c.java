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

package x10.ast;

/**
 * Immutable representation of a local variable access. 
 * Introduced to add X10 specific type checks. A local variable accessed
 * in a deptype must be final.
 * 
 * @author vj
 */

import polyglot.ast.Id;
import polyglot.ast.Local_c;
import polyglot.ast.Node;
import polyglot.types.CodeDef;
import polyglot.types.LocalInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.types.VarDef;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

import x10.constraint.XFailure;
import x10.constraint.XTerm;
import x10.constraint.XConstraint;
import x10.errors.Errors;
import polyglot.types.Context;
import x10.types.X10Flags;
import x10.types.X10LocalInstance;
import x10.types.X10ProcedureDef;
import x10.types.X10TypeMixin;
import polyglot.types.TypeSystem;
import x10.types.X10TypeSystem_c;
import x10.types.X10Context_c;
import x10.types.X10LocalDef_c;
import x10.types.constraints.CConstraint;
import x10.types.constraints.XConstrainedTerm;

public class X10Local_c extends Local_c {

	public X10Local_c(Position pos, Id name) {
		super(pos, name);
		
	}
	public Node typeCheck(ContextVisitor tc) {
	    X10Context_c context = (X10Context_c) tc.context();
        final Name liName = name.id();
	    LocalInstance li = localInstance();
	    if (!((X10LocalInstance) li).isValid()) {
	        li = findAppropriateLocal(tc, liName);
	    }

	    context.recordCapturedVariable(li);

        if (!li.flags().isFinal()) {
            // if the local is defined in an outer class, then it must be final
            // shared was removed from the language: you cannot access var in a closure
            // Note that an async is similar to a closure (we create a dummy closure)
            boolean isInClosure = false;
	        if (context.isLocal(liName)) {
                // ok
            } else if (!context.isLocalIncludingAsyncAt(liName)) {
	            // this local is defined in an outer class
                isInClosure = true;
	            Errors.issue(tc.job(), new SemanticException("Local variable \"" + liName +"\" is accessed from an inner class or a closure, and must be declared final.",this.position()));
	        } else {
                // if the access is in an async and the local-var is not local, then we must ensure that the scoping looks like this: var ... (no async) ... finish ... async
                // algorithm: we go up the context (going outwards) looking for a finish
                // (setting flag foundFinish to true when we find a finish, and to false when we find an async)
                // when we get to the var definition, then foundFinish must be true.
               if (!context.isSequentialAccess(true,liName))
                   Errors.issue(tc.job(), new SemanticException("Local variable \"" + liName +"\" cannot be captured in an async if there is no enclosing finish in the same scoping-level as \"" + liName +"\"; consider changing \"" + liName +"\" from var to val.",this.position()));
            }

            if (!isInClosure) {
            // we check that usages inside an "at" are at the origin place if it is a "var" (for "val" we're fine)
            final X10LocalDef_c localDef_c = (X10LocalDef_c) li.def();
            XTerm origin = localDef_c.placeTerm();
            // origin maybe null when typechecking a method to get the return type (see XTENLANG-1902)
            // but we will type check that method again later (with correct placeTerm)
            if (origin!=null) { // origin = PlaceChecker.here();
                final XConstrainedTerm placeTerm = context.currentPlaceTerm();
                final XTerm currentPlace = placeTerm.term();
                XConstraint constraint = new XConstraint();
                boolean isOk = false;
                try {
                    constraint.addBinding(origin,currentPlace);
                    if (placeTerm.constraint().entails(constraint)) {
                        //ok  origin == currentPlace
                        isOk = true;
                    }
                } catch (XFailure xFailure) {
                    // how can it happen? in any case, we should report an error so isOk=false
                }
                if (!isOk)
                    Errors.issue(tc.job(), new SemanticException("Local variable \"" + liName +"\" is accessed at a different place, and must be declared final.",this.position()));
            }
            }
        }


        X10Local_c result = (X10Local_c) localInstance(li).type(li.type());

		try {
			VarDef dli = context.varWhoseTypeIsBeingElaborated();
			if (context.inDepType()) {
				li = result.localInstance();
				if (! (li.def().equals(dli)) && ! li.flags().isFinal()) {
					throw new SemanticError("A var local variable " + liName
							+ " is not allowed in a constraint.", 
							position());
				}
			}
			
			// Add in self==x to local variable x.

			Type type = ((X10LocalInstance) li).rightType();
			result = (X10Local_c) result.type(type);
			
			// Fold in the method's guard.
			// %%% FIXME: move method guard into context.currentConstraint
			CodeDef ci = context.currentCode();
			if (ci instanceof X10ProcedureDef) {
			    X10ProcedureDef pi = (X10ProcedureDef) ci;
				CConstraint c = Types.get(pi.guard());
				if (c != null) {
					TypeSystem xts = (TypeSystem) tc.typeSystem();

					// Substitute self for x (this local) in the guard.
//					C_Var var = new TypeTranslator(xts).trans(localInstance());
//        			Promise p = c.intern(var);
//        			System.out.println("before = " + c);
//        			c = c.substitute(p.term(), C_Special.Self);
//        			System.out.println("after = " + c);

      			    // Add the guard into the constraint for this type. 
        			Type t = result.type();

        			CConstraint dep = X10TypeMixin.xclause(t);
        			if (dep == null) dep = new CConstraint();
        			else dep = dep.copy();
//        			XTerm resultTerm = xts.xtypeTranslator().trans(result);
//        			dep.addSelfBinding((XVar) resultTerm);
        			try {
        			    dep.addIn(c);
        			} catch (XFailure e) {
        			    throw new SemanticException(e.getMessage(), position());
        			}
        			
        			t = X10TypeMixin.xclause(X10TypeMixin.baseType(t), dep);
        			
					return result.type(t);
				}
			}
			
		} catch (SemanticException z) {
			Errors.issue(tc.job(), z, this);
		}
		return result;
	}

    public static X10LocalInstance findAppropriateLocal(ContextVisitor tc, Name name) {
        Context context = (Context) tc.context();
        SemanticException error = null;
        try {
            return (X10LocalInstance) context.findLocal(name);
        } catch (SemanticException e) {
            error = e;
        }
        // If not returned yet, fake the local instance.
        X10TypeSystem_c xts = (X10TypeSystem_c) tc.typeSystem();
        X10LocalInstance li = xts.createFakeLocal(name, error);
        return li;
    }
}

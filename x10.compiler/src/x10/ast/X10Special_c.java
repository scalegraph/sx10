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

import polyglot.ast.Node;
import polyglot.ast.Special_c;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import x10.constraint.XFailure;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.errors.Errors;
import x10.types.ConstrainedType;
import x10.types.X10ConstructorDef;
import polyglot.types.Context;
import x10.types.X10Flags;
import x10.types.X10MethodDef;
import x10.types.X10ParsedClassType;
import x10.types.X10ProcedureDef;

import x10.types.X10TypeMixin;
import polyglot.types.TypeSystem;
import x10.types.XTypeTranslator;
import x10.types.checker.PlaceChecker;
import x10.types.constraints.CConstraint;
import x10.types.constraints.CConstraint;
import x10.types.constraints.XConstrainedTerm;

public class X10Special_c extends Special_c implements X10Special {

    public X10Special_c(Position pos, Kind kind, TypeNode qualifier) {
        super(pos, kind, qualifier);
    }


    public static X10Special self(Position pos) {
        X10Special_c self = new X10Special_c(pos, SELF, null);
        return self;
    }

    public boolean isSelf() { return kind == SELF; }

    /** Type check the expression. */
    public Node typeCheck(ContextVisitor tc) {
        TypeSystem ts = (TypeSystem) tc.typeSystem();
        Context c = (Context) tc.context();

        if (isSelf()) {
            Type tt = c.currentDepType();

            if (tt == null) {
                Errors.issue(tc.job(),
                        new SemanticException("self may only be used within a dependent type", position()));
                tt = ts.unknownType(position());
            }

            // The type of self should not include a dep clause; otherwise
            // self in C{c} could have type C{c}, causing an infinite regress
            // later in type checking.
            tt = X10TypeMixin.baseType(tt);

            assert tt != null;
            return type(tt);
        }
        
        Type t = null;

        if (qualifier == null) {
            // an unqualified "this" 
            t = c.currentClass();

            // If in the class header declaration, make this refer to the current class, not the enclosing class (or null).
            if (c.inSuperTypeDeclaration()) {
                if (kind == SUPER) {
                    Errors.issue(tc.job(),
                            new SemanticException("Cannot refer to \"super\" from within a class or interface declaration header."),
                            this);
                }
                t = c.supertypeDeclarationType().asType();
            }

            // Use the constructor return type, not the base type.
            if (c.currentDepType() == null)
                if (c.currentCode() instanceof X10ConstructorDef) {
                    X10ConstructorDef cd = (X10ConstructorDef) c.currentCode();
                    Type returnType =  cd.returnType().get();
                    returnType =  ts.expandMacros(returnType);
                    t = returnType;
                }
        }
        else {
            if (qualifier.type().isClass()) {
                ClassType ct =  qualifier.type().toClass();
                t=ct;
                CodeDef cd = c.currentCode();
           
                if (!c.currentClass().hasEnclosingInstance(ct)) {
                    Errors.issue(tc.job(),
                            new SemanticException("The nested class \"" +c.currentClass() + "\" does not have an enclosing instance of type \"" +ct + "\".", qualifier.position()),
                            this);
                }
                
            }
            else {
                Errors.issue(tc.job(),
                        new SemanticException("Invalid qualifier for \"this\" or \"super\".", qualifier.position()),
                        this);
            }
        }
        
        if (t == null || (c.inStaticContext() && ts.typeEquals(t, c.currentClass(), c))) {
            // trying to access "this" or "super" from a static context.
            Errors.issue(tc.job(),
                    new SemanticException("Cannot access a non-static field or method, or refer to \"this\" or \"super\" from a static context.", position()));
        }

        X10Special result = this;
        TypeSystem xts = (TypeSystem) ts;
        assert (t.isClass());
        // Instantiate with the class's type arguments
        t = X10TypeMixin.instantiateTypeParametersExplicitly(t);

        if (kind == THIS) {
            Type tt = X10TypeMixin.baseType(t);
            CConstraint cc = X10TypeMixin.xclause(t);
            cc = cc == null ? new CConstraint() : cc.copy();
            try {
                XVar var = (XVar) xts.xtypeTranslator().trans(cc, this, c);
                cc.addSelfBinding(var);
                cc.setThisVar(var);
                //PlaceChecker.AddThisHomeEqualsPlaceTerm(cc, var, c);
            }
            catch (XFailure e) {
                Errors.issue(tc.job(),
                        new SemanticException("Constraint on this is inconsistent; " + e.getMessage(), position()));
            }
            tt = X10TypeMixin.xclause(X10TypeMixin.baseType(tt), cc);
            
            result = (X10Special) type(tt);
        }
        else if (kind == SUPER) {
            Type superClass =  X10TypeMixin.superClass(t);
            Type tt = X10TypeMixin.baseType(superClass);
            CConstraint cc = X10TypeMixin.xclause(superClass);
            cc = cc == null ? new CConstraint() : cc.copy();
            try {
                XVar var = (XVar) xts.xtypeTranslator().trans(cc, this, c);
                cc.addSelfBinding(var);
                //PlaceChecker.AddThisHomeEqualsPlaceTerm(cc, var, c);
            }
            catch (XFailure e) {
                Errors.issue(tc.job(),
                        new SemanticException("Constraint on super is inconsistent; " + e.getMessage(), position()));
            }
            tt = X10TypeMixin.xclause(X10TypeMixin.baseType(tt), cc);
            result = (X10Special) type(tt);
        }
       
        assert result.type() != null;

        // Fold in the method's guard, if any.
        CodeDef ci = c.currentCode();
        if (ci instanceof X10ProcedureDef) {
            X10ProcedureDef pi = (X10ProcedureDef) ci;
            CConstraint guard = Types.get(pi.guard());
            if (guard != null) {
                Type newType = result.type();
                CConstraint dep = X10TypeMixin.xclause(newType).copy();
                try {
                    dep.addIn(guard);
                }
                catch (XFailure e) {
                    Errors.issue(tc.job(), new SemanticException(e.getMessage(), position()));
                }
                newType = X10TypeMixin.xclause(X10TypeMixin.baseType(newType), dep);
                return result.type(newType);
            }
        }

        return result;
    }

    public String toString() {
        String typeString = null;
        if (qualifier != null)
            typeString = qualifier.toString();
        else {
            Type type =  type();
            if (type != null) {
                ClassType k = type.toClass();
                if (k != null)
                    typeString = k.toString();
            }
        }

        return (typeString == null ? "" : typeString + ".") + kind;
    }
}

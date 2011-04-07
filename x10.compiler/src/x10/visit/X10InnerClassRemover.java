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

package x10.visit;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.FieldAssign;
import polyglot.ast.Formal;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.ContainerType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.InnerClassRemover;
import polyglot.visit.NodeVisitor;
import x10.ast.DepParameterExpr;
import x10.ast.TypeParamNode;
import x10.ast.X10ClassDecl;
import x10.ast.X10ConstructorDecl;
import x10.ast.X10FieldDecl;
import x10.ast.X10MethodDecl;
import x10.ast.X10New;
import x10.types.ConstrainedType;
import x10.types.ParameterType;
import x10.types.X10ClassDef;
import x10.types.X10ClassType;
import x10.types.X10ConstructorDef;
import x10.types.X10ConstructorInstance;
import x10.types.X10FieldDef;
import x10.types.X10FieldInstance;

import x10.types.X10MethodDef;
import x10.types.MethodInstance;
import x10.types.X10ParsedClassType;
import x10.types.X10ProcedureDef;
import x10.types.ParameterType.Variance;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;

/**
 * This class rewrites inner classes to static nested classes.
 * As part of the rewrite, it first transforms local classes
 * (including anonymous classes) to inner/nested classes using
 * an instance of {@link X10LocalClassRemover}.
 * 
 * The transformation of an inner class I is as follows:
 * <pre>
 * class X[A,B]{g} {
 *     class Y[C,D]{h} {
 *         class I[E,F]{c} extends S[A,B,C,D,E,F] {
 *             def this(a:Z) {
 *                 cons
 *             }
 *             body
 *         }
 *     }
 *     ...
 *     val v = qual.new I[P,Q](e); // qual is of type X[K,L].Y[M,N]
 *     val w: X[K,L].Y[M.N].I[P.Q] = v;
 * }
 * </pre>
 * to
 * <pre>
 * class X[A,B]{g} {
 *     static class Y[C,D,A',B']{h[A'/A,B'/B]&&g[A'/A,B'/B]} {
 *         ...val out$: X[A',B'];...
 *         static class I[E,F,C',D',A',B']{c[A'/A,B'/B,C'/C,D'/D]&&h[A'/A,B'/B,C'/C,D'/D]&&g[A'/A,B'/B]} extends S[A',B',C',D',E,F] {
 *             val out$: Y[C',D',A',B'];
 *             def this(a:Z, o: Y[C',D',A',B']) {
 *                 out$ = o;
 *                 cons[A'/A,B'/B,C'/C,D'/D,out$/Y.this,out$.out$/X.this]
 *             }
 *             body[A'/A,B'/B,C'/C,D'/D]
 *         }
 *     }
 *     ...
 *     val v = new X.Y.I[K,L,M,N,P,Q](e, qual);
 *     val w: X.Y.I[K,L,M,N,P,Q] = v;
 * }
 * </pre>
 */
public class X10InnerClassRemover extends InnerClassRemover {
    public X10InnerClassRemover(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    protected ContextVisitor localClassRemover() {
        return new X10LocalClassRemover(this);
    }

    @Override
    protected Node leaveCall(Node old, Node n, NodeVisitor v) {
        Node res = super.leaveCall(old, n, v);
        return xform.transform(res, old, (ContextVisitor) v);
    }

    private InnerClassTypeExpander xform = new InnerClassTypeExpander();

    // FIXME: factor out common functionality with TypeParamSubst!
    private final class InnerClassTypeExpander extends TypeTransformer {

        @Override
        protected X10MethodDecl transform(X10MethodDecl d, X10MethodDecl old) {
            return fixMethodDecl(d);
        }
        @Override
        protected X10ConstructorDecl transform(X10ConstructorDecl d, X10ConstructorDecl old) {
            return fixConstructorDecl(d);
        }
        @Override
        protected X10FieldDecl transform(X10FieldDecl d, X10FieldDecl old) {
            return fixFieldDecl(d);
        }

        protected CConstraint transformConstraint(CConstraint c) {
            // TODO Auto-generated method stub
            return c;
        }

        protected TypeConstraint transformTypeConstraint(TypeConstraint c) {
            // TODO Auto-generated method stub
            return c;
        }

        @Override
        protected X10ConstructorInstance transformConstructorInstance(X10ConstructorInstance ci) {
            Type returnType = ci.returnType();
            Type newReturnType = transformType(returnType);
            if (newReturnType != returnType) {
                ci = (X10ConstructorInstance) ci.returnType(newReturnType);
            }
            List<Type> formalTypes = ci.formalTypes();
            List<Type> newFormalTypes = transformTypeList(formalTypes);
            if (newFormalTypes != formalTypes) {
                ci = (X10ConstructorInstance) ci.formalTypes(newFormalTypes);
            }
            //List<Type> throwTypes = ci.throwTypes();
            //List<Type> newThrowTypes = transformTypeList(throwTypes);
            //if (newThrowTypes != throwTypes) {
            //    ci = (X10ConstructorInstance) ci.throwTypes(newThrowTypes);
            //}
            ContainerType container = ci.container();
            ContainerType newContainer = (ContainerType) transformType(container);
            if (newContainer != container) {
                ci = (X10ConstructorInstance) ci.container(newContainer);
            }
            CConstraint guard = ci.guard();
            CConstraint newGuard = transformConstraint(guard);
            if (newGuard != guard) {
                ci = (X10ConstructorInstance) ci.guard(newGuard);
            }
            TypeConstraint typeGuard = ci.typeGuard();
            TypeConstraint newTypeGuard = transformTypeConstraint(typeGuard);
            if (newTypeGuard != typeGuard) {
                ci = (X10ConstructorInstance) ci.typeGuard(newTypeGuard);
            }
            return ci;
        }

        @Override
        protected MethodInstance transformMethodInstance(MethodInstance mi) {
            Type returnType = mi.returnType();
            Type newReturnType = transformType(returnType);
            if (newReturnType != returnType) {
                mi = (MethodInstance) mi.returnType(newReturnType);
            }
            List<Type> formalTypes = mi.formalTypes();
            List<Type> newFormalTypes = transformTypeList(formalTypes);
            if (newFormalTypes != formalTypes) {
                mi = (MethodInstance) mi.formalTypes(newFormalTypes);
            }
            //List<Type> throwTypes = mi.throwTypes();
            //List<Type> newThrowTypes = transformTypeList(throwTypes);
            //if (newThrowTypes != throwTypes) {
            //    mi = (X10MethodInstance) mi.throwTypes(newThrowTypes);
            //}
            ContainerType container = mi.container();
            ContainerType newContainer = (ContainerType) transformType(container);
            if (newContainer != container) {
                mi = (MethodInstance) mi.container(newContainer);
            }
            CConstraint guard = mi.guard();
            CConstraint newGuard = transformConstraint(guard);
            if (newGuard != guard) {
                mi = (MethodInstance) mi.guard(newGuard);
            }
            TypeConstraint typeGuard = mi.typeGuard();
            TypeConstraint newTypeGuard = transformTypeConstraint(typeGuard);
            if (newTypeGuard != typeGuard) {
                mi = (MethodInstance) mi.typeGuard(newTypeGuard);
            }
            return mi;
        }

        @Override
        protected X10FieldInstance transformFieldInstance(X10FieldInstance fi) {
            Type type = fi.type();
            Type newType = transformType(type);
            if (newType != type) {
                fi = (X10FieldInstance) fi.type(newType);
            }
            ContainerType container = fi.container();
            ContainerType newContainer = (ContainerType) transformType(container);
            if (newContainer != container) {
                fi = (X10FieldInstance) fi.container(newContainer);
            }
            return fi;
        }

        @Override
        protected Type transformType(Type t) {
            return fixType(t);
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> void updateRefUnsafe(Ref<? extends T> ref, T value) {
        ((Ref<T>)ref).update(value);
    }

    private X10MethodDecl fixMethodDecl(X10MethodDecl md) {
        X10MethodDef def = md.methodDef();
        // Adjust various types in the def.
        Ref<? extends Type> dtr = def.returnType();
        Ref<? extends Type> tr = md.returnType().typeRef();
        if (dtr != tr && !ts.typeEquals(Types.get(tr), Types.get(dtr), context)) {
            updateRefUnsafe(dtr, Types.get(tr));
        }
        List<Formal> formals = md.formals();
        List<Ref<? extends Type>> formalTypes = def.formalTypes();
        List<LocalDef> formalNames = def.formalNames();
        for (int i = 0; i < formalTypes.size(); i++) {
            Ref<? extends Type> dftr = formalTypes.get(i);
            LocalDef fn = formalNames.get(i);
            assert (ts.typeEquals(Types.get(fn.type()), Types.get(dftr), context));
            Ref<? extends Type> ftr = formals.get(i).type().typeRef();
            if (!ts.typeEquals(Types.get(ftr), Types.get(dftr), context)) {
                if (dftr != ftr)
                    updateRefUnsafe(dftr, Types.get(ftr));
                if (fn.type() != ftr) {
                    updateRefUnsafe(fn.type(), Types.get(ftr));
                }
            }
        }
        if (md.guard() != null) {
            DepParameterExpr g = md.guard();
            if (def.guard() != g.valueConstraint()) {
                def.guard().update(Types.get(g.valueConstraint()));
            }
            if (def.typeGuard() != g.typeConstraint()) {
                def.typeGuard().update(Types.get(g.typeConstraint()));
            }
        }
        if (md.offerType() != null) {
            Ref<? extends Type> dotr = def.offerType();
            Ref<? extends Type> otr = md.offerType().typeRef();
            if (dotr != otr && !ts.typeEquals(Types.get(otr), Types.get(dotr), context)) {
                updateRefUnsafe(dotr, Types.get(otr));
            }
        }
        return md;
    }

    private X10ConstructorDecl fixConstructorDecl(X10ConstructorDecl cd) {
        X10ConstructorDef def = cd.constructorDef();
        // Adjust various types in the def.
        if (cd.returnType() != null) {
            Ref<? extends Type> dtr = def.returnType();
            Ref<? extends Type> tr = cd.returnType().typeRef();
            if (dtr != tr && !ts.typeEquals(Types.get(tr), Types.get(dtr), context)) {
                updateRefUnsafe(dtr, Types.get(tr));
            }
        }
        List<Formal> formals = cd.formals();
        List<Ref<? extends Type>> formalTypes = def.formalTypes();
        List<LocalDef> formalNames = def.formalNames();
        assert formalNames.size() == formals.size();
        assert formalNames.size() == formalTypes.size();
        for (int i = 0; i < formalTypes.size(); i++) {
            Ref<? extends Type> dftr = formalTypes.get(i);
            LocalDef fn = formalNames.get(i);
            assert (ts.typeEquals(Types.get(fn.type()), Types.get(dftr), context));
            Ref<? extends Type> ftr = formals.get(i).type().typeRef();
            if (!ts.typeEquals(Types.get(ftr), Types.get(dftr), context)) {
                if (dftr != ftr)
                    updateRefUnsafe(dftr, Types.get(ftr));
                if (fn.type() != ftr) {
                    updateRefUnsafe(fn.type(), Types.get(ftr));
                }
            }
        }
        if (cd.guard() != null) {
            DepParameterExpr g = cd.guard();
            if (def.guard() != g.valueConstraint()) {
                def.guard().update(Types.get(g.valueConstraint()));
            }
            if (def.typeGuard() != g.typeConstraint()) {
                def.typeGuard().update(Types.get(g.typeConstraint()));
            }
        }
        if (cd.offerType() != null) {
            Ref<? extends Type> dotr = def.offerType();
            Ref<? extends Type> otr = cd.offerType().typeRef();
            if (dotr != otr && !ts.typeEquals(Types.get(otr), Types.get(dotr), context)) {
                updateRefUnsafe(dotr, Types.get(otr));
            }
        }
        return cd;
    }

    private X10FieldDecl fixFieldDecl(X10FieldDecl fd) {
        X10FieldDef def = fd.fieldDef();
        // Adjust field type.
        Ref<? extends Type> dtr = def.type();
        Ref<? extends Type> tr = fd.type().typeRef();
        if (dtr != tr && !ts.typeEquals(Types.get(tr), Types.get(dtr), context)) {
            updateRefUnsafe(dtr, Types.get(tr));
        }
        return fd;
    }

    protected ClassDecl fixClassDecl(ClassDecl n) {
        // FIX:XTENLANG-215
        X10ClassDecl cd = (X10ClassDecl) n;
        Flags flags = cd.flags().flags();
        X10ClassDef def = cd.classDef();
        if ((def.isMember() && !flags.isStatic())) {
            def.flags(def.flags().clearStatic()); // temporarily turn off the "static" flag
            List<ParameterType> typeParameters = new ArrayList<ParameterType>();
            List<ParameterType.Variance> variances = new ArrayList<ParameterType.Variance>();
            gatherOuterTypeParameters(def, typeParameters, variances);
            assert (typeParameters.size() == variances.size());
            List<TypeParamNode> typeParamNodes = new ArrayList<TypeParamNode>(cd.typeParameters());
            for (int p = 0; p < typeParameters.size(); p++) {
                ParameterType tp = typeParameters.get(p);
                // FIXME: [IP] this is a hack.  We should really rename type parameters.
                NodeFactory xnf = (NodeFactory) nf;
                Position genPos = tp.position().markCompilerGenerated();
                typeParamNodes.add(xnf.TypeParamNode(genPos, xnf.Id(genPos, tp.name()), variances.get(p)).type(tp));
            }
            if (!typeParameters.isEmpty()) {
                cd = cd.typeParameters(typeParamNodes);
            }
            List<ClassMember> newMember = new ArrayList<ClassMember>();
            List<X10FieldDecl> fieldDs = new ArrayList<X10FieldDecl>();
            List<ClassMember> members = cd.body().members();
            // remove initializers of final variables;
            // final int bar = foo(define outer class val) -> final int bar;  
            for (ClassMember classMember : members) {
                if (classMember instanceof X10FieldDecl) {
                    X10FieldDecl fieldD = (X10FieldDecl) classMember;
                    if (fieldD.fieldDef().flags().isFinal() && !fieldD.fieldDef().flags().isStatic() && fieldD.init() != null) {
                        fieldDs.add(fieldD);
                        newMember.add(fieldD.init(null));
                        continue;
                    }
                }
                newMember.add(classMember);
            }
            // add code to initialize final variables after super()
            for (int i = 0; i < members.size(); i++) {
                ClassMember classMember = members.get(i);
                if (classMember instanceof ConstructorDecl) {
                    ArrayList<Stmt> statements = new ArrayList<Stmt>();
                    for (X10FieldDecl fieldD : fieldDs) {
                        Position pos = fieldD.position().markCompilerGenerated();
                        FieldDef fi = fieldD.fieldDef();
                        FieldAssign a = nf.FieldAssign(pos, nf.This(pos).type(fi.asInstance().container()), nf.Id(pos, fi.name()), Assign.ASSIGN, fieldD.init());
                        a = (FieldAssign) a.type(fi.asInstance().type());
                        a = a.fieldInstance(fi.asInstance());
                        a = a.targetImplicit(false);

                        Eval e = nf.Eval(pos, a);
                        statements.add(e);
                    }
                    ConstructorDecl decl = (ConstructorDecl) classMember;
                    Block block = decl.body();
                    if (block.statements().size() > 0) {
                        Stmt stmt = block.statements().get(0);
                        if (stmt instanceof ConstructorCall) {
                            statements.add(0, stmt);
                        }
                        statements.addAll(block.statements().subList(1, block.statements().size()));
                    }
                    newMember.set(i,(ClassMember) decl.body(block.statements(statements)));
                }
            }
            cd = cd.body(cd.body().members(newMember));
            def.flags(def.flags().Static()); // set the "static" flag back on
        }
        cd = (X10ClassDecl) super.fixClassDecl(cd);
        if ((def.isMember() && !flags.isStatic())) {
            cd = cd.flags(cd.flags().flags(flags.Static()));
        }
        return cd;
    }

    protected void adjustConstructorFormals(ConstructorDef ci, List<Formal> newFormals) {
        super.adjustConstructorFormals(ci, newFormals);
        assert (ci instanceof X10ConstructorDef);
        List<LocalDef> newFormalNames = new ArrayList<LocalDef>();
        for (Formal f : newFormals) {
            newFormalNames.add(f.localDef());
        }
        ((X10ConstructorDef) ci).setFormalNames(newFormalNames);
    }

    protected CConstraint fixConstraint(CConstraint constraint) {
        return constraint; // TODO
    }

    protected Type fixType(Type t) {
        if (t == null)
            return null;
        if (!t.isClass())
            return t;
        X10ParsedClassType qt = (X10ParsedClassType) t.toClass();
        X10ClassDef def = qt.x10Def();
        if (def.isMember() && !def.flags().isStatic()) {
            List<ParameterType> typeParameters = new ArrayList<ParameterType>();
            List<ParameterType.Variance> variances = new ArrayList<ParameterType.Variance>();
            gatherOuterTypeParameters(def, typeParameters, variances);
            assert (typeParameters.size() == variances.size());
            for (int p = 0; p < typeParameters.size(); p++) {
                ParameterType tp = typeParameters.get(p);
                // FIXME: [IP] this is a hack.  We should really rename type parameters.
                def.addTypeParameter(tp, variances.get(p));
            }
            def.flags(def.flags().Static());
        }
        t = Types.instantiateTypeParametersExplicitly(t);
        CConstraint constraint = t instanceof ConstrainedType ? ((ConstrainedType) t).getRealXClause() : null;
        t = propagateTypeArgumentsToInnermostType(t);
        if (constraint != null) {
            CConstraint newConstraint = fixConstraint(constraint);
            if (newConstraint != constraint)
                t = Types.xclause(t, newConstraint);
        }
        return t;
    }

    @Override
    public TypeNode fixTypeNode(TypeNode tn) {
        tn = super.fixTypeNode(tn);
        Type t = tn.type();
        Type xqt = fixType(t);
        if (xqt != t) {
            tn = tn.typeRef(Types.ref(xqt));
        }
        return tn;
    }

    @Override
    public New fixNew(New neu) {
        Expr q = neu.qualifier();
        X10New xneu = (X10New) super.fixNew(neu);
        if (q == null)
            return xneu;
        assert (q.type().isClass());
        X10ParsedClassType qt = (X10ParsedClassType) q.type().toClass();
        List<TypeNode> typeArguments = new ArrayList<TypeNode>(xneu.typeArguments());
        List<Type> tArgs = qt.typeArguments();
        if (tArgs != null && !tArgs.isEmpty()) {
            for (Type ta : tArgs) {
                typeArguments.add(nf.CanonicalTypeNode(q.position().markCompilerGenerated(), ta));
            }
            xneu = xneu.typeArguments(typeArguments);
            // Object type has already been transformed by the visitor.
        }
        return xneu;
    }

    private static Type propagateTypeArgumentsToInnermostType(Type t) {
        if (t instanceof X10ParsedClassType) {
            X10ParsedClassType ct = (X10ParsedClassType) t;
            if (ct.isMember()) {
                ct = ct.container((X10ClassType) propagateTypeArgumentsToInnermostType((X10ParsedClassType) ct.container()));
                if (!ct.flags().isStatic() || ct.typeArguments() == null || ct.typeArguments().size() != ct.x10Def().typeParameters().size()) {
                    List<Type> containerArgs = ct.container().typeArguments();
                    if (containerArgs != null && !containerArgs.isEmpty()) {
                        List<Type> newTypeArgs = new ArrayList<Type>();
                        if (ct.typeArguments() != null)
                            newTypeArgs.addAll(ct.typeArguments());
                        newTypeArgs.addAll(containerArgs);
                        ct = ct.typeArguments(newTypeArgs);
                    }
                }
                ct = ct.container(resetTypeArguments((X10ParsedClassType) ct.container()));
            }
            List<Type> typeArguments = ct.typeArguments();
            List<Type> newTypeArguments = typeArguments;
            if (typeArguments != null) {
                List<Type> res = new ArrayList<Type>();
                for (Type a : typeArguments) {
                    Type ia = propagateTypeArgumentsToInnermostType(a);
                    if (ia != a)
                        newTypeArguments = res;
                    res.add(ia);
                }
            }
            ct = ct.typeArguments(newTypeArguments);
            return ct;
        } else if (t instanceof ConstrainedType) {
            ConstrainedType ct = (ConstrainedType) t;
            Type baseType = Types.get(ct.baseType());
            Type newBaseType = propagateTypeArgumentsToInnermostType(baseType);
            if (newBaseType != baseType)
                t = ct.baseType(Types.ref(newBaseType));
        }/* else if (t instanceof AnnotatedType) {
            AnnotatedType at = (AnnotatedType) t;
            Type baseType = at.baseType();
            Type newBaseType = propagateTypeArgumentsToInnermostType(baseType);
            if (newBaseType != baseType)
                t = at.baseType(newBaseType);
        }*/
        return t;
    }

    private static X10ParsedClassType resetTypeArguments(X10ParsedClassType t) {
        if (t.isMember()) {
            t = t.container(resetTypeArguments((X10ParsedClassType) t.container()));
        }
        return t.typeArguments(null);
    }

    private void gatherOuterTypeParameters(X10ClassDef def, List<ParameterType> typeParameters, List<Variance> variances) {
        if (!def.isMember() || def.flags().isStatic()) return;
        def = (X10ClassDef) Types.get(def.outer());
        List<ParameterType> tp = def.typeParameters();
        List<ParameterType.Variance> v = def.variances();
        typeParameters.addAll(tp);
        variances.addAll(v);
        gatherOuterTypeParameters(def, typeParameters, variances);
    }

    @Override
    protected FieldDef boxThis(ClassType currClass, ClassType outerClass) {
        outerClass = (ClassType) fixType(outerClass);
        FieldDef fi = super.boxThis(currClass, outerClass);
        //if (!(fi.flags() instanceof X10Flags))
        //    fi.setFlags(X10Flags.toX10Flags(fi.flags()));
        return fi;
    }
}

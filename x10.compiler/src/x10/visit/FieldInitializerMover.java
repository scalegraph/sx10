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
import java.util.Collections;
import java.util.List;

import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.FieldAssign;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.TypeNode;
import polyglot.ast.MethodDecl;
import polyglot.ast.Call;
import polyglot.ast.Id;
import polyglot.frontend.Job;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Flags;
import polyglot.types.Types;
import polyglot.types.Name;
import polyglot.types.MethodDef;
import polyglot.types.Ref;
import polyglot.types.FieldInstance;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.types.X10ClassType;
import x10.types.X10ConstructorDef;
import x10.types.X10Def;
import polyglot.types.TypeSystem;
import x10.ast.X10ConstructorDecl;
import x10.ast.X10MethodDecl_c;
import x10.ast.AssignPropertyCall;

/**
 * Visitor that moves field initializers to the constructor
 * and insert explicit super constructor invocations
 * unless constructor is annotated @NoSuperCall.
 *
Yoav added:
 * Field initializers are placed after the AssignProperty call, because we have a 3-phase init: super, properties, ctor-code
 * Note that the type of a field may refer to a property, so even field assignment is prohibited until after the AssignProperty call.
 *
 * Finally, we do not move constant fields because it is possible to switch over a constant field (final fields with compile-time known value):
 * E.g., in Java (and similarly in X10):
class Test {
    final int i=2+1;
    void test() {
        switch(3*4) {
            case i:
            case 4:
        }
    }
}
 */
public class FieldInitializerMover extends ContextVisitor {

    public FieldInitializerMover(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    protected ConstructorCall superCall(Type superType, Context context) throws SemanticException {
        Position CG = Position.COMPILER_GENERATED;
        assert (superType.isClass());
        Expr qualifier = null;
        if (superType.toClass().def().isMember() && !superType.toClass().flags().isStatic())
            qualifier = nf.This(CG, nf.CanonicalTypeNode(CG, superType.toClass().outer()));
        ConstructorCall cc = nf.SuperCall(CG, qualifier, Collections.<Expr>emptyList());
        ConstructorInstance ci = ts.findConstructor(superType, ts.ConstructorMatcher(superType, Collections.<Type>emptyList(), context));
        return cc.constructorInstance(ci);
    }
    
    protected static boolean mustCallSuper(X10ConstructorDecl cdecl) throws SemanticException {
        X10ConstructorDef def = cdecl.constructorDef();
        Type t = (Type) def.typeSystem().systemResolver().find(QName.make("x10.compiler.NoSuperCall"));
        return def.annotationsMatching(t).isEmpty();
    }
    class FindProperty extends NodeVisitor {
        private final Stmt evalCall;
        private boolean didFindProperty = false;
        private FindProperty(Stmt evalCall) {
            this.evalCall = evalCall;
        }
        @Override public Node leave(Node old, Node n, NodeVisitor v) {
            if (n instanceof AssignPropertyCall) {
                AssignPropertyCall propCall = (AssignPropertyCall) n;
                didFindProperty = true;
                return nf.Block(n.position().markCompilerGenerated(),propCall,evalCall);
            }
            return n;
        }
    }
    private ClassDecl changeClass(ClassDecl cdecl) throws SemanticException {
        final ClassBody cb = cdecl.body();
        final List<ClassMember> members = new ArrayList<ClassMember>();
        final NodeFactory nf = this.nodeFactory();
        final TypeSystem ts = this.typeSystem();
        final Context context = cdecl.enterChildScope(cb, this.context());

        final Position p = cdecl.position().markCompilerGenerated();
        final Special this_ = (Special) nf.This(p).type(cdecl.classDef().asType());

        List<Stmt> assignments = new ArrayList<Stmt>();
        for (ClassMember cm : cb.members()) {
            if (cm instanceof FieldDecl) {
                FieldDecl fd = (FieldDecl) cm;
                FieldDef def = fd.fieldDef();

                if (fd.init() != null && !def.flags().isStatic() && !def.isConstant()) {
                    final FieldInstance fieldInstance = def.asInstance();

                    FieldAssign a = nf.FieldAssign(p, this_, nf.Id(p, def.name()), Assign.ASSIGN, fd.init());
                    a = a.fieldInstance(fieldInstance);
                    a = (FieldAssign) a.type(fieldInstance.type());

                    assert this_.type() != null;
                    assert a.type() != null;

                    Eval eval = nf.Eval(p, a);
                    assignments.add(eval);

                    fd = fd.init(null);
                }

                members.add(fd);
            }
            else {
                members.add(cm);
            }
        }
        Stmt evalCall = nf.Block(p); // an empty statement
        if (assignments.size()>0) {
            // create a private method that includes all the field initializers
            TypeNode returnType = nf.CanonicalTypeNode(p,ts.Void());
            final Name name = Name.makeFresh("__fieldInitializers");
            final Id nameId = nf.Id(p, name);
            final Flags flags = Flags.PRIVATE.Final();
            MethodDecl method = nf.MethodDecl(p,nf.FlagsNode(p, flags),returnType, nameId,
                    Collections.<Formal>emptyList(), nf.Block(p,assignments));
            MethodDef md = ts.methodDef(p, Types.ref(cdecl.classDef().asType()), flags, returnType.typeRef(), name, Collections.<Ref<? extends Type>>emptyList());
            method = method.methodDef(md);
            members.add(method);

            // create the call to __fieldInitializers
            Call call = nf.Call(p,this_,nameId,Collections.<Expr>emptyList()).methodInstance(md.asInstance());
            call = (Call) call.type(ts.Void());
            evalCall = nf.Eval(p, call);
        }

        final List<ClassMember> members2 = new ArrayList<ClassMember>();

        for (ClassMember cm : members) {
            if (cm instanceof X10ConstructorDecl) {
                X10ConstructorDecl cd = (X10ConstructorDecl) cm;

                Block body = cd.body();
                if (body == null) {
                    body = nf.Block(p);
                }

                // if there is a property(...) call, then we replace it with: { property(...); __fieldInitializers(); }
                FindProperty findProperty = new FindProperty(evalCall);
                body = (Block) body.visit(findProperty);
                boolean didFindProperty = findProperty.didFindProperty;

                List<Stmt> stmts = body.statements();
                if (stmts.size() > 0 && stmts.get(0) instanceof ConstructorCall) {
                    ConstructorCall cc = (ConstructorCall) stmts.get(0);
                    if (cc.kind() == ConstructorCall.SUPER) {
                        List<Stmt> ss = new ArrayList<Stmt>();
                        ss.add(cc);
                        if (!didFindProperty) ss.add(evalCall);
                        ss.addAll(stmts.subList(1, stmts.size()));
                        body = body.statements(ss);
                    }
                }
                else {
                    // implicit super call
                    List<Stmt> ss = new ArrayList<Stmt>();
                    if (cdecl.superClass() != null && mustCallSuper(cd))
                        ss.add(superCall(cdecl.superClass().type(), cb.enterChildScope(cd, context)));
                    if (!didFindProperty) ss.add(evalCall);
                    ss.addAll(stmts);
                    body = body.statements(ss);
                }

                if (body != cd.body()) {
                    cd = (X10ConstructorDecl) cd.body(body);
                }

                members2.add(cd);
            }
            else {
                members2.add(cm);
            }
        }


        return cdecl.body(cb.members(members2));
    }
    @Override
    public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        FieldInitializerMover fim = (FieldInitializerMover) v;
        if (n instanceof ClassDecl) {
            final ClassDecl cdecl = (ClassDecl) n;
            if (!cdecl.flags().flags().isInterface())
                return fim.changeClass(cdecl);            
        }
        
        return super.leaveCall(old, n, v);
    }
}

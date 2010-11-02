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

package x10.util;

import java.util.*;

import polyglot.ast.*;
import polyglot.types.Flags;
import polyglot.types.LazyRef;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.TypeBuilder;
import x10.ast.*;
import x10.constraint.XName;
import x10.constraint.XNameWrapper;
import x10.constraint.XVar;
import x10.constraint.XTerms;
import x10.types.*;
import x10.extension.X10Ext;
import x10cpp.visit.SharedVarsMethods;

public class Struct {
    private final static java.util.Set<String> ignoreTypes = new HashSet<String>();

    static {
        ignoreTypes.add("Boolean");
        ignoreTypes.add("Byte");
        ignoreTypes.add("UByte");
        ignoreTypes.add("Char");
        ignoreTypes.add("Short");
        ignoreTypes.add("UShort");
        ignoreTypes.add("Int");
        ignoreTypes.add("UInt");
        ignoreTypes.add("Long");
        ignoreTypes.add("ULong");
        ignoreTypes.add("Float");
        ignoreTypes.add("Double");
        ignoreTypes.add("Place");
    }

    public static X10ClassDecl_c addStructMethods(TypeBuilder tb, X10ClassDecl_c n) {
        final X10TypeSystem_c xts = (X10TypeSystem_c) tb.typeSystem();
        final X10ClassDef cd = (X10ClassDef) n.classDef();
        X10ParsedClassType ct = (X10ParsedClassType) cd.asType();

        QName fullName = cd.fullName();

        String strName = fullName.name().toString();
        final QName qualifier = fullName.qualifier();

        final ArrayList<Ref<? extends Type>> interfacesList = new ArrayList<Ref<? extends Type>>(cd.interfaces());
        interfacesList.add(xts.lazyAny());
        cd.setInterfaces(interfacesList);

       final Position pos = Position.compilerGenerated(n.body().position());

       String fullNameWithThis = fullName + "#this";
       //String fullNameWithThis = "this";
       XName thisName = new XNameWrapper<Object>(new Object(), fullNameWithThis);
       XVar thisVar = XTerms.makeLocal(thisName);




       final LazyRef<X10ParsedClassType> PLACE = Types.lazyRef(null);
       PLACE.setResolver(new Runnable() {
           public void run() {
               PLACE.update((X10ParsedClassType) xts.Place());
           }
       });
       final LazyRef<X10ParsedClassType> STRING = Types.lazyRef(null);
       STRING.setResolver(new Runnable() {
           public void run() {
               STRING.update((X10ParsedClassType) xts.String());
           }
       });
       final LazyRef<X10ParsedClassType> BOOLEAN = Types.lazyRef(null);
       BOOLEAN.setResolver(new Runnable() {
           public void run() {
               BOOLEAN.update((X10ParsedClassType) xts.Boolean());
           }
       });
       final LazyRef<X10ParsedClassType> OBJECT = Types.lazyRef(null);
       OBJECT.setResolver(new Runnable() {
           public void run() {
               OBJECT.update((X10ParsedClassType) xts.Object());
           }
       });







        // Now I add the auto-generated methods (equals(Any), equals(SomeStruct), hashCode(), toString())
        // if the programmer didn't already defined them
        // primitive classes do not define hashCode, and we should not auto-create hashCode for them,
        // or the java backend will translate:
        //      var x:Int; x.hashCode
        // to
        //      x.hashCode()
        // and it should translate it to:
        //      ((Object)x).hashCode()

        boolean isPrim = (qualifier!=null && qualifier.toString().equals("x10.lang") && ignoreTypes.contains(strName));
        boolean seenToString = isPrim;
        boolean seenHashCode = isPrim;
        boolean seenEquals = isPrim;




       for (ClassMember member : n.body().members())
           if (member instanceof MethodDecl_c) {
                MethodDecl_c mdecl = (MethodDecl_c) member;
               final Flags methodFlags = mdecl.flags().flags();
               if (methodFlags.isStatic() || methodFlags.isAbstract()) continue;

                // The compiler provides implementations of equals, hashCode, and toString if
                // there are no user-defined implementations.  So, we need to search the struct's members
                // and determine which methods to auto-generate and which ones are user-provided.
                if (mdecl.name().id().toString().equals("toString") &&
                    mdecl.formals().isEmpty()) {
                    seenToString = true;
                }
                if (mdecl.name().id().toString().equals("hashCode") &&
                    mdecl.formals().isEmpty()) {
                    seenHashCode = true;
                }
                if (mdecl.name().id().toString().equals("equals") &&
                    mdecl.formals().size() == 1) {
                    seenEquals = true;
                }
            }


       // I use the AST instead of the type, because the type hasn't been built yet (so ct.fields() is empty!)
       // I modify the AST before type-checking, because I want to connect calls to "equals"
       // with the correct  "equals(SomeStruct)"  or  "equals(Any)"
       // This is important for C++ efficiency (to prevent auto-boxing of structs in equality checking)
       ArrayList<FieldDecl> fields = new ArrayList<FieldDecl>();
       fields.addAll(n.properties());
       for (ClassMember member : n.body().members())
           if (member instanceof FieldDecl_c) {
                FieldDecl_c field = (FieldDecl_c) member;
                if (field.flags().flags().isStatic()) continue;
                fields.add(field);
           }

        final Flags flags = X10Flags.PUBLIC.Final();
        final NodeFactory nf = (NodeFactory)tb.nodeFactory();
        final TypeNode intTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","Int"));
        final TypeNode boolTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","Boolean"));
        final TypeNode placeTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","Place"));
        final TypeNode objectTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","Object"));
        final TypeNode stringTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","String"));
        final TypeNode anyTypeNode = nf.TypeNodeFromQualifiedName(pos,QName.make("x10.lang","Any"));
        final List<TypeParamNode> typeParamNodeList = n.typeParameters();
        List<TypeNode> params = new ArrayList<TypeNode>();
        for (TypeParamNode p : typeParamNodeList)
            params.add(nf.TypeNodeFromQualifiedName(pos,QName.make(null,p.name().id())));
        final TypeNode structTypeNode = typeParamNodeList.isEmpty() ? nf.TypeNodeFromQualifiedName(pos,fullName) :
                nf.AmbDepTypeNode(pos, null,
                        nf.Id(pos,fullName.name()), params, Collections.<Expr>emptyList(), null);
        ArrayList<Stmt> bodyStmts;
        Expr expr;
        Block block;
        String methodName;
        X10MethodDecl md;


        /*
        // final public global safe def typeName():String { return "FULL_NAME"; }
        bodyStmts = new ArrayList<Stmt>();
        expr = nf.StringLit(pos, fullName.toString());
        bodyStmts.add(nf.Return(pos, expr));
        block = nf.Block(pos).statements(bodyStmts);
        methodName = "typeName";
        md = nf.MethodDecl(pos,nf.FlagsNode(pos,flags),stringTypeNode,nf.Id(pos,Name.make(methodName)),Collections.EMPTY_LIST,Collections.EMPTY_LIST,block);
        n = (X10ClassDecl_c) n.body(n.body().addMember(md));
        */

        {
            X10Flags nativeFlags = X10Flags.toX10Flags(Flags.PUBLIC.Native().Final());
            ArrayList<AnnotationNode> natives;
            Formal formal;
            // In the Java backend, some structs (like Int) are mapped to primitives (like int)
            // So I must add a native annotation on this method.

            //@Native("java", "x10.rtt.Types.typeName(#0)")
            //@Native("c++", "x10aux::type_name(#0)")
            //global safe def typeName():String;
            natives = createNative(nf, pos, "x10.rtt.Types.typeName(#0)", "x10aux::type_name(#0)");
            AnnotationNode nonEscaping = nf.AnnotationNode(pos, nf.AmbMacroTypeNode(pos, nf.PrefixFromQualifiedName(pos,QName.make("x10.compiler")), nf.Id(pos, "NonEscaping"), Collections.<TypeNode>emptyList(), Collections.<Expr>singletonList(nf.StringLit(pos,""))));
            natives.add(nonEscaping);
            methodName = "typeName";
            md = nf.MethodDecl(pos,nf.FlagsNode(pos,nativeFlags),stringTypeNode,nf.Id(pos,Name.make(methodName)),Collections.<Formal>emptyList(),null);
            md = (X10MethodDecl) ((X10Ext) md.ext()).annotations(natives);
            n = (X10ClassDecl_c) n.body(n.body().addMember(md));
        }

        if (!seenToString) {
            // final public global safe def toString():String {
            //  return "struct NAME:"+" FIELD1="+FIELD1+...;
            //          or
            //  return "struct NAME"; // if there are no fields
            // }
            bodyStmts = new ArrayList<Stmt>();
            expr = nf.StringLit(pos, "struct " + fullName + (fields.size()==0?"":":"));
            for (FieldDecl fi : fields) {
                 String name = fi.name().toString();
                 expr = nf.Binary(pos,expr, Binary.ADD,nf.StringLit(pos," "+name+"="));
                 expr = nf.Binary(pos,expr, Binary.ADD,nf.Field(pos,nf.This(pos),nf.Id(pos,name)));
            }
            bodyStmts.add(nf.Return(pos, expr));
            block = nf.Block(pos).statements(bodyStmts);
            methodName = "toString";
            md = nf.MethodDecl(pos,nf.FlagsNode(pos,flags),stringTypeNode,nf.Id(pos,Name.make(methodName)),Collections.<Formal>emptyList(),block);
            n = (X10ClassDecl_c) n.body(n.body().addMember(md));
        }
        if (!seenHashCode) {
            // final public global safe def hashCode():Int {
            //  var result:Int = 0;
            //  result = 31*result + FIELD1.hashCode();
            //  ...
            //  return result;
            // }
            bodyStmts = new ArrayList<Stmt>();
            bodyStmts.add(nf.LocalDecl(pos, nf.FlagsNode(pos,Flags.NONE), intTypeNode,nf.Id(pos,"result"),nf.IntLit(pos, IntLit.INT,0)));
            final Local target = nf.Local(pos, nf.Id(pos, "result"));
            for (FieldDecl fi : fields) {
                String name = fi.name().toString();
                bodyStmts.add(nf.Eval(pos,nf.Assign(pos, target, Assign.ASSIGN,
                    nf.Binary(pos,
                        nf.Binary(pos,nf.IntLit(pos,IntLit.INT,31),Binary.MUL,target),
                        Binary.ADD,
                        nf.Call(pos,nf.Field(pos,nf.This(pos),nf.Id(pos,name)),nf.Id(pos,"hashCode"))))));
            }
            bodyStmts.add(nf.Return(pos, target));
            block = nf.Block(pos).statements(bodyStmts);
            methodName = "hashCode";
            md = nf.MethodDecl(pos,nf.FlagsNode(pos,flags),intTypeNode,nf.Id(pos,Name.make(methodName)),Collections.<Formal>emptyList(),block);
            n = (X10ClassDecl_c) n.body(n.body().addMember(md));
        }
        // _struct_equals is used for == even when the user defined equals
        // (both backends need to convert == to _struct_equals)
        for (boolean isStructEquals : new boolean[]{false,true}) {
            methodName = isStructEquals ? SharedVarsMethods.STRUCT_EQUALS_METHOD : "equals";
            if (!isStructEquals && seenEquals) continue;

            // final public global safe def equals(other:Any):Boolean {
            //  if (!(other instanceof NAME)) return false;
            //  return equals(other as NAME);
            // }
            bodyStmts = new ArrayList<Stmt>();
            Expr other =nf.Local(pos,nf.Id(pos,"other"));
            bodyStmts.add(nf.If(pos, nf.Unary(pos, Unary.NOT,
                    nf.Instanceof(pos,other,structTypeNode)),
                    nf.Return(pos,nf.BooleanLit(pos,false))));
            bodyStmts.add(nf.Return(pos,nf.Call(pos,nf.Id(pos,methodName),nf.Cast(pos,structTypeNode,other))));
            block = nf.Block(pos).statements(bodyStmts);
            Formal formal = nf.Formal(pos,nf.FlagsNode(pos,Flags.NONE),anyTypeNode,nf.Id(pos,"other"));
            md = nf.MethodDecl(pos,nf.FlagsNode(pos,flags),boolTypeNode,nf.Id(pos,Name.make(methodName)), Collections.singletonList(formal),block);
            n = (X10ClassDecl_c) n.body(n.body().addMember(md));

            // final public global safe def equals(other:NAME):Boolean {
            //  return true && FIELD1==other.FIELD1 && ...;
            // }
            bodyStmts = new ArrayList<Stmt>();
            Expr res = fields.isEmpty() ? nf.BooleanLit(pos, true) : null;
            for (FieldDecl fi : fields) {
                String name = fi.name().toString();
                final Id id = nf.Id(pos, name);
                Expr right = nf.Binary(pos,nf.Field(pos,nf.This(pos),id),Binary.EQ,nf.Field(pos,other,id));
                if (res==null)
                    res = right;
                else
                    res = nf.Binary(pos,res,Binary.COND_AND,right);
            }
            bodyStmts.add(nf.Return(pos, res));
            block = nf.Block(pos).statements(bodyStmts);
            formal = nf.Formal(pos,nf.FlagsNode(pos,Flags.NONE),structTypeNode,nf.Id(pos,"other"));
            md = nf.MethodDecl(pos,nf.FlagsNode(pos,flags),boolTypeNode,nf.Id(pos,Name.make(methodName)),Collections.singletonList(formal),block);
            n = (X10ClassDecl_c) n.body(n.body().addMember(md));
        }

       return n;
    }
    private static ArrayList<AnnotationNode> createNative(NodeFactory nf,Position pos, String java, String cpp) {
        ArrayList<AnnotationNode> res = new ArrayList<AnnotationNode>(2);
        for (int i=0; i<2; i++) {
            List<Expr> list = new ArrayList<Expr>(2);
            list.add(nf.StringLit(pos, i==0 ? "java" : "c++"));
            list.add(nf.StringLit(pos, i==0 ? java : cpp));
            res.add( nf.AnnotationNode(pos, nf.AmbMacroTypeNode(pos, nf.PrefixFromQualifiedName(pos,QName.make("x10.compiler")), nf.Id(pos, "Native"), Collections.<TypeNode>emptyList(), list)) );
        }
        return res;
    }
}

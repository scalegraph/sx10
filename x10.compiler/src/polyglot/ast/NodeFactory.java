/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.frontend.ExtensionInfo;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.Position;

/**
 * A <code>NodeFactory</code> constructs AST nodes.  All node construction
 * should go through this factory or be done with the <code>copy()</code>
 * method of <code>Node</code>.
 */
public interface NodeFactory
{

	/**
	 * Returns the ExtensionInfo object associated with this factory.
	 */
	public ExtensionInfo extensionInfo();
	
    /**
     * Returns a disambiguator for nodes from this factory.
     */
    Disamb disamb();
    
    //////////////////////////////////////////////////////////////////
    // Factory Methods
    //////////////////////////////////////////////////////////////////

    FlagsNode FlagsNode(Position pos, Flags flags);

    Id Id(Position pos, Name id);
    Id Id(Position pos, String id); // for backward compat
    
    AmbExpr AmbExpr(Position pos, Id name);
    Expr ExprFromQualifiedName(Position pos, QName qualifiedName);
    
    // type or expr
    AmbReceiver AmbReceiver(Position pos, Id name);
    AmbReceiver AmbReceiver(Position pos, Prefix prefix, Id name);
    Receiver ReceiverFromQualifiedName(Position pos, QName qualifiedName);
    
    // package or type
    AmbQualifierNode AmbQualifierNode(Position pos, Id name);
    AmbQualifierNode AmbQualifierNode(Position pos, Prefix qual, Id name);
    QualifierNode QualifierNodeFromQualifiedName(Position pos, QName qualifiedName);
    
    // package or type or expr
    AmbPrefix AmbPrefix(Position pos, Id name);
    AmbPrefix AmbPrefix(Position pos, Prefix prefix, Id name);
    Prefix PrefixFromQualifiedName(Position pos, QName qualifiedName);
    
    AmbTypeNode AmbTypeNode(Position pos, Id name);
    AmbTypeNode AmbTypeNode(Position pos, Prefix qualifier, Id name);
    TypeNode TypeNodeFromQualifiedName(Position pos, QName qualifiedName);
    
    ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base);
    CanonicalTypeNode CanonicalTypeNode(Position pos, Type type);

    ArrayAccess ArrayAccess(Position pos, Expr base, Expr index);

    ArrayInit ArrayInit(Position pos);
    ArrayInit ArrayInit(Position pos, List<Expr> elements);

    Assert Assert(Position pos, Expr cond);
    Assert Assert(Position pos, Expr cond, Expr errorMessage);

    Assign Assign(Position pos, Expr target, Assign.Operator op, Expr source);
    LocalAssign LocalAssign(Position pos, Local target, Assign.Operator op, Expr source);
    FieldAssign FieldAssign(Position pos, Receiver target, Id name, Assign.Operator op, Expr source);
    ArrayAccessAssign ArrayAccessAssign(Position pos, Expr array, Expr index, Assign.Operator op, Expr source);
    AmbAssign AmbAssign(Position pos, Expr target, Assign.Operator op, Expr source);

    Binary Binary(Position pos, Expr left, Binary.Operator op, Expr right);

    Block Block(Position pos);
    Block Block(Position pos, Stmt s1);
    Block Block(Position pos, Stmt s1, Stmt s2);
    Block Block(Position pos, Stmt s1, Stmt s2, Stmt s3);
    Block Block(Position pos, Stmt s1, Stmt s2, Stmt s3, Stmt s4);
    Block Block(Position pos, List<Stmt> statements);

    SwitchBlock SwitchBlock(Position pos, List<Stmt> statements);

    BooleanLit BooleanLit(Position pos, boolean value);

    Branch Break(Position pos);
    Branch Break(Position pos, Id label);

    Branch Continue(Position pos);
    Branch Continue(Position pos, Id label);

    Branch Branch(Position pos, Branch.Kind kind);
    Branch Branch(Position pos, Branch.Kind kind, Id label);

    Call Call(Position pos, Id name);
    Call Call(Position pos, Id name, Expr a1);
    Call Call(Position pos, Id name, Expr a1, Expr a2);
    Call Call(Position pos, Id name, Expr a1, Expr a2, Expr a3);
    Call Call(Position pos, Id name, Expr a1, Expr a2, Expr a3, Expr a4);
    Call Call(Position pos, Id name, List<Expr> args);
    
    Call Call(Position pos, Receiver target, Id name);
    Call Call(Position pos, Receiver target, Id name, Expr a1);
    Call Call(Position pos, Receiver target, Id name, Expr a1, Expr a2);
    Call Call(Position pos, Receiver target, Id name, Expr a1, Expr a2, Expr a3);
    Call Call(Position pos, Receiver target, Id name, Expr a1, Expr a2, Expr a3, Expr a4);
    Call Call(Position pos, Receiver target, Id name, List<Expr> args);

    Case Default(Position pos);
    Case Case(Position pos, Expr expr);
    
    Cast Cast(Position pos, TypeNode type, Expr expr);

    Catch Catch(Position pos, Formal formal, Block body);

    CharLit CharLit(Position pos, char value);

    ClassBody ClassBody(Position pos, List<ClassMember> members);

    ClassDecl ClassDecl(Position pos, FlagsNode flags, Id name,
            TypeNode superClass, List<TypeNode> interfaces, ClassBody body);

    ClassLit ClassLit(Position pos, TypeNode typeNode);

    Conditional Conditional(Position pos, Expr cond, Expr consequent, Expr alternative);

    ConstructorCall ThisCall(Position pos, List<Expr> args);
    ConstructorCall ThisCall(Position pos, Expr outer, List<Expr> args);
    ConstructorCall SuperCall(Position pos, List<Expr> args);
    ConstructorCall SuperCall(Position pos, Expr outer, List<Expr> args);
    ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, List<Expr> args);
    ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind,
	                            Expr outer, List<Expr> args);

    ConstructorDecl ConstructorDecl(Position pos, FlagsNode flags, Id name,
            List<Formal> formals, 
            Block body);

    FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name);
    FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init);

    Do Do(Position pos, Stmt body, Expr cond);

    Empty Empty(Position pos);

    Eval Eval(Position pos, Expr expr);

    Field Field(Position pos, Id name);
    Field Field(Position pos, Receiver target, Id name);

    FloatLit FloatLit(Position pos, FloatLit.Kind kind, double value);

    For For(Position pos, List<ForInit> inits, Expr cond, List<ForUpdate> iters, Stmt body);

    Formal Formal(Position pos, FlagsNode flags, TypeNode type, Id name);

    If If(Position pos, Expr cond, Stmt consequent);
    If If(Position pos, Expr cond, Stmt consequent, Stmt alternative);

    Import Import(Position pos, Import.Kind kind, QName name);

    Initializer Initializer(Position pos, FlagsNode flags, Block body);

    Instanceof Instanceof(Position pos, Expr expr, TypeNode type);

    IntLit IntLit(Position pos, IntLit.Kind kind, long value);

    Labeled Labeled(Position pos, Id label, Stmt body);

    Local Local(Position pos, Id name);

    LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl);

    LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name);
    LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init);

    MethodDecl MethodDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name,
            List<Formal> formals,  Block body);

    New New(Position pos, TypeNode type, List<Expr> args);
    New New(Position pos, TypeNode type, List<Expr> args, ClassBody body);

    New New(Position pos, Expr outer, TypeNode objectType, List<Expr> args);
    New New(Position pos, Expr outer, TypeNode objectType, List<Expr> args, ClassBody body);

    NewArray NewArray(Position pos, TypeNode base, List<Expr> dims);
    NewArray NewArray(Position pos, TypeNode base, List<Expr> dims, int addDims);
    NewArray NewArray(Position pos, TypeNode base, int addDims, ArrayInit init);
    NewArray NewArray(Position pos, TypeNode base, List<Expr> dims, int addDims, ArrayInit init);
    
    NodeList NodeList(Position pos, List<Node> nodes);
    NodeList NodeList(Position pos, NodeFactory nf, List<Node> nodes);

    NullLit NullLit(Position pos);

    Return Return(Position pos);
    Return Return(Position pos, Expr expr);

    SourceCollection SourceCollection(Position pos, List<SourceFile> sources);

    SourceFile SourceFile(Position pos, List<TopLevelDecl> decls);
    SourceFile SourceFile(Position pos, List<Import> imports, List<TopLevelDecl> decls);
    SourceFile SourceFile(Position pos, PackageNode packageName, List<Import> imports, List<TopLevelDecl> decls);

    Special This(Position pos);
    Special This(Position pos, TypeNode outer);

    Special Super(Position pos);
    Special Super(Position pos, TypeNode outer);
    Special Special(Position pos, Special.Kind kind);
    Special Special(Position pos, Special.Kind kind, TypeNode outer);

    StringLit StringLit(Position pos, String value);

    Switch Switch(Position pos, Expr expr, List<SwitchElement> elements);

    Synchronized Synchronized(Position pos, Expr expr, Block body);

    Throw Throw(Position pos, Expr expr);

    Try Try(Position pos, Block tryBlock, List<Catch> catchBlocks);
    Try Try(Position pos, Block tryBlock, List<Catch> catchBlocks, Block finallyBlock);

    PackageNode PackageNode(Position pos, Ref<? extends Package> p);

    Unary Unary(Position pos, Unary.Operator op, Expr expr);
    Unary Unary(Position pos, Expr expr, Unary.Operator op);

    While While(Position pos, Expr cond, Stmt body);

    CanonicalTypeNode CanonicalTypeNode(Position position,
            Ref<? extends Type> type);
}

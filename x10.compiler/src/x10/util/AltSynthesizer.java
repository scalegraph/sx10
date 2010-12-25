/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2010.
 */
package x10.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.BooleanLit;
import polyglot.ast.Branch;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FlagsNode;
import polyglot.ast.FloatLit;
import polyglot.ast.For;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
import polyglot.ast.IntLit;
import polyglot.ast.Labeled;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.StringLit;
import polyglot.ast.Term;
import polyglot.ast.Try;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Assign.Operator;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import x10.ast.StmtExpr;
import x10.ast.StmtSeq;
import x10.ast.X10Call;
import x10.ast.X10Cast;
import x10.ast.X10Formal;
import x10.ast.SettableAssign;
import x10.constraint.XFailure;
import x10.constraint.XTerm;
import x10.types.ConstrainedType;
import x10.types.X10FieldInstance;
import x10.types.X10LocalDef;
import x10.types.X10MethodInstance;
import x10.types.X10TypeMixin;
import x10.types.checker.Converter;
import x10.types.constraints.CConstraint;
import x10.visit.ConstantPropagator;
import x10.visit.Desugarer;

/**
 * TODO: merge this class into x10.util.Synthesizer
 * 
 * @author Bowen Alpern
 *
 */
public class AltSynthesizer extends ContextVisitor {

    private static final Name ITERATOR = Name.make("iterator");
    private static final Name HASNEXT  = Name.make("hasNext");
    private static final Name REGION   = Name.make("region");
    private static final Name DIST     = Name.make("dist");
    private static final Name NEXT     = Name.make("next");
    private static final Name MAKE     = Name.make("make");
    private static final Name RANK     = Name.make("rank");
    private static final Name MIN      = Name.make("min");
    private static final Name MAX      = Name.make("max");
    private static final Name SET      = SettableAssign.SET;

    private final TypeSystem  ts;
    private final NodeFactory nf;
    private final Synthesizer synth;

    public AltSynthesizer(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        this.ts = ts;
        this.nf = nf;
        synth   = new Synthesizer(nf, ts);
    }

    /**
     * Turn a formal parameter into local variable declaration.
     * 
     * @param formal the parameter to be transformed
     * @param init an expression representing the initial value of new local variable
     * @return the declaration for a local variable with the same behavior as formal
     * TODO: move into Synthesizer
     */
    public LocalDecl transformFormalToLocalDecl(X10Formal formal, Expr init) {
        return nf.LocalDecl(formal.position(), formal.flags(), formal.type(), formal.name(), init).localDef(formal.localDef());
    }

    /**
     * Create a declaration for a local variable from a local type definition.
     * 
     * @param pos the Position of the declaration
     * @param def the definition of the declared local variable
     * @param init the Expr representing the initial value of the declared local variable
     * @return the LocalDecl representing the declaration of the local variable
     * TODO: move into Synthesizer
     */
    public LocalDecl createLocalDecl(Position pos, LocalDef def, Expr init) {
        return nf.LocalDecl( pos.markCompilerGenerated(), 
                              nf.FlagsNode(pos, def.flags()),
                              nf.CanonicalTypeNode(pos, def.type().get()), 
                              nf.Id(pos, def.name()),
                              init ).localDef(def);
    }

    /**
     * Create a declaration for a local variable from scratch.
     * (A local variable definition is created as a side-effect and may be retrieved from the result.)
     * 
     * @param pos the Position of the declaration
     * @param flags the Flags ("static", "public", "var") for the declared local variable
     * @param name the Name of the declared local variable
     * @param type the Type of the declared local variable
     * @param init an Expr representing the initial value of the declared local variable
     * @return the LocalDecl representing the declaration of the local variable
     * TODO: move into Synthesizer
     */
    public LocalDecl createLocalDecl(Position pos, Flags flags, Name name, Type type, Expr init) {
        LocalDef def = ts.localDef(pos, flags, Types.ref(type), name);
        return createLocalDecl(pos, def, init);
    }

    /**
     * Create a declaration for a local variable using the type of the initializer.
     * 
     * @param pos the Position of the declaration
     * @param flags the Flags ("static", "public", "var") for the declared local variable
     * @param name the Name of the declared local variable
     * @param init an Expr representing the initial value of the declared local variable
     * @return the LocalDecl representing the declaration of the local variable
     * TODO: move into Synthesizer
     */
    public LocalDecl createLocalDecl(Position pos, Flags flags, Name name, Expr init) {
        if (init.type().isVoid()) {
            System.err.println("ERROR: ForLoopOptimizer.createLocalDecl: creating void local assignment for " +init+ " at " +pos);
        }
        return createLocalDecl(pos, flags, name, init.type(), init);
    }

    /** 
     * Create a labeled statement.
     * 
     * @param pos the Position of the statement in the source program
     * @param label the label for the statement
     * @param stmt the statement to label
     * @return the synthesized labeled statement
     * TODO: move into Synthesizer
     */
    public Labeled createLabeledStmt(Position pos, Name label, Stmt stmt) {
        return createLabeledStmt(pos, nf.Id(pos, label), stmt);
    }

    /** 
     * Create a traditional C-style 'for' loop.
     * This form assumes that the update part of the for header is empty.
     * 
     * @param pos the Position of the loop in the source program
     * @param init the declaration that initializes the iterate
     * @param cond the condition governing whether the body should continue to be executed
     * @param body the body of the loop to be executed repeatedly
     * @return the synthesized 'for' loop
     * TODO: move into Synthesizer
     */
    public For createStandardFor(Position pos, LocalDecl init, Expr cond, Stmt body) {
        return nf.For( pos, 
                        Collections.<ForInit>singletonList(init), 
                        cond, 
                        Collections.<ForUpdate>emptyList(), 
                        body );
    }

    /** 
     * Create a traditional C-style 'for' loop.
     * 
     * @param pos the Position of the loop in the source program
     * @param init the declaration that initializes the iterate
     * @param cond the condition governing whether the body should continue to be executed
     * @param update the statement to increment the iterate after each execution of the body
     * @param body the body of the loop to be executed repeatedly
     * @return the synthesized for-loop
     * TODO: move into Synthesizer
     */
    public For createStandardFor(Position pos, LocalDecl init, Expr cond, Expr update, Stmt body) {
        return nf.For( pos, 
                        Collections.<ForInit>singletonList(init), 
                        cond, 
                        Collections.<ForUpdate>singletonList(nf.Eval(pos, update)), 
                        body );
    }

    /** 
     * Create a block of statements from a list.
     * 
     * @param pos the Position of the block in the source code
     * @param stmts the Stmt's to be included in the block
     * @return the synthesized Block
     * TODO: move into Synthesizer
     */
    public Block createBlock(Position pos, List<Stmt> stmts) {
        return nf.Block(pos, stmts);
    }

    /**
     * Create a block of statements from individual terms (statements and/or expressions).
     * If a term is already a Stmt, it is used as is.
     * If it is an Expr, the Stmt is its evaluation.
     * Otherwise an InvalidArgumentException is thrown.
     * 
     * @param pos the Position of the block in the source code
     * @param terms the sequence of terms to become statements of the block
     * @return the synthesized Block of terms turned to statements
     * @throws IllegalArgumentException if one of the terms is not a Stmt or an Expr
     * TODO: move into Synthesizer
     */
    public Block createBlock(Position pos, Term... terms) {
        return createBlock(pos, convertToStmtList(terms));
    }

    /**
     * Convert individual terms (statements and/or expressions) to a list of statements.
     * If a term is already a Stmt, it is used as is.
     * If it is an Expr, the Stmt is its evaluation.
     * Otherwise an InvalidArgumentException is thrown.
     * 
     * @param terms the sequence of terms
     * @return the newly constructed list of statements
     * @throws IllegalArgumentException if one of the terms is not a Stmt or an Expr
     * TODO: move into Synthesizer
     */
    public List<Stmt> convertToStmtList(Term... terms) {
        List<Stmt> stmts = new ArrayList<Stmt> (terms.length);
        for (Term term : terms) {
            if (term instanceof Expr) {
                term = nf.Eval(term.position(), (Expr) term);
            } else if (!(term instanceof Stmt)) {
                throw new IllegalArgumentException("Invalid argument type: "+term.getClass());
            }
            stmts.add((Stmt) term);
        }
        return stmts;
    }

    /**
     * Create a break statement.
     * 
     * @param pos the Position of the break statement in source code
     * @return the synthesized break statement
     * TODO: move to Synthesizer
     */
    public Branch createBreak(Position pos) {
        return nf.Break(pos);
    }

    /**
     * Create a declaration for an uninitialized local variable from scratch.
     * (A local variable definition is created as a side-effect and may be retrieved from the result.)
     * 
     * @param pos the Position of the declaration
     * @param flags the Flags ("static", "public", "var") for the declared local variable
     * @param name the Name of the declared local variable
     * @param type the Type of the declared local variable
     * @return the LocalDecl representing the declaration of the local variable
     * TODO: move into Synthesizer
     */
    public LocalDecl createLocalDecl(Position pos, Flags flags, Name name, Type type) {
        LocalDef def = ts.localDef(pos, flags, Types.ref(type), name);
        return createLocalDecl(pos, def);
    }

 
    /**
     * Create a declaration for a local variable from an uninitialized local type definition.
     * 
     * @param pos the Position of the declaration
     * @param def the definition of the declared local variable
     * @return the LocalDecl representing the declaration of the local variable
     * TODO: move into Synthesizer
     */
    public LocalDecl createLocalDecl(Position pos, LocalDef def) {
        return nf.LocalDecl( pos, 
                              nf.FlagsNode(pos, def.flags()),
                              nf.CanonicalTypeNode(pos, def.type().get()), 
                              nf.Id(pos, def.name()) ).localDef(def);
    }

    /**
     * Create an assignment statement.
     * 
     * @param pos the Position of the assignment in source code
     * @param target the left-hand side of the assignment
     * @param op the assignment operator
     * @param source the right-hand side of the assignment
     * @return the synthesized assignment statement
     * TODO: move to synthesizer
     */
    public Stmt createAssignment(Position pos, Expr target, Operator op, Expr source) {
        return createEval(createAssign(pos, target, op, source));
    }

    /**
     * Create an evaluation statement for a given expression.
     * 
     * @param expr the expression to be evaluated
     * @return a synthesized statement that evaluates expr
     * TODO: move to Synthesizer
     */
    public Stmt createEval(Expr expr) {
        return nf.Eval(expr.position(), expr);
    }

    /**
     * Create a conditional statements.
     * 
     * @param pos the Position of the conditional statement in source code.
     * @param cond the boolean expression to be tested
     * @param thenStmt the statement to execute if cond is true
     * @param elseStmt the statement to execute if cond is false
     * @return the synthesized conditional statement
     * TODO: move to Synthesizer
     */
    public If createIf(Position pos, Expr cond, Stmt thenStmt, Stmt elseStmt) {
        if (null == elseStmt) return nf.If(pos, cond, thenStmt);
        return nf.If(pos, cond, thenStmt, elseStmt);
    }

    /**
     * Turn a single statement into a statement sequence.
     * 
     * @param stmt the statement to be encapsulated
     * @return a synthesized statement sequence comprising stmt
     */
    public StmtSeq toStmtSeq(Stmt stmt) {
        return toStmtSeq(stmt.position(), Collections.singletonList(stmt));
    }

    /**
     * Turn a list of statements into a statement sequence.
     * 
     * @param pos the Position of the statement sequence in source code
     * @param stmts a list of statements
     * @return a synthesized statement sequence comprising stmts
     * TODO: move to Synthesizer
     */
    public StmtSeq toStmtSeq(Position pos, List<Stmt> stmts) {
        return nf.StmtSeq(pos, stmts);
    }

    // helper methods that create subclasses of Expr

    /**
     * Create a statement expression -- a block of statements with a result value.
     * 
     * @param pos the Position of the statement expression in source code
     * @param stmts the statements to proceed evaluation of expr
     * @param expr the result of the statement expression
     * @return a synthesized statement expression comprising stmts and expr
     * TODO: move to Synthesizer
     */
    public StmtExpr createStmtExpr(Position pos, List<Stmt> stmts, Expr expr) {
        if (null == expr) return (StmtExpr) nf.StmtExpr(pos, stmts, null).type(ts.Void());
        return (StmtExpr) nf.StmtExpr(pos, stmts, expr).type(expr.type());
    }

    /**
     * Create an IntLit node representing a given integer literal.
     * 
     * @param val the int value to be represented
     * @return an IntLit node representing the literal integer val
     * TODO: move into Synthesizer
     */
    public IntLit createIntLit(int val) {
        IntLit lit = nf.IntLit(Position.COMPILER_GENERATED, IntLit.INT, val);
        return (IntLit) lit.type(ts.Int());
    }

    /**
     * Create the boolean literal "true".
     * 
     * @param pos the Position of the literal in source code
     * @return the synthesized boolean literal
     * TODO: move to synthesizer
     */
    public BooleanLit createTrue(Position pos) {
        return (BooleanLit) nf.BooleanLit(pos, true).type(ts.Boolean());
    }

    /**
     * Create the boolean literal "false".
     * 
     * @param pos the Position of the literal in source code
     * @return the synthesized boolean literal
     * TODO: move to the synthesizer
     */
    public BooleanLit createFalse(Position pos) {
        return (BooleanLit) nf.BooleanLit(pos, false).type(ts.Boolean());
    }

    /**
     * Create the boolean negation of a given (boolean) expression.
     * 
     * @param expr the boolean expression to be negated
     * @return a synthesized expression which is the boolean negation of expr
     * TODO:  move to synthesizer
     */
    public Expr createNot(Expr expr) {
        return createNot(expr.position(), expr);
    }


    /**
     * Create the boolean negation of a given (boolean) expression.
     * 
     * @param pos the Position of the negated expression in the source code
     * @param expr the boolean expression to be negated
     * @return a synthesized expression that negates expr
     * TODO: move to Synthesizer
     */
    public Expr createNot(Position pos, Expr expr) {
        assert (expr.type().isBoolean());
        return createUnary(pos, Unary.NOT, expr);
    }


    /**
     * Create a unary expression.
     * 
     * @param pos the Position of the unary expression in the source code
     * @param op the unary operation of the expression
     * @param expr the argument to the unary operator
     * @return a synthesized unary expression equivalent to applying op to expr
     * TODO: move to Synthesizer
     */
    public Expr createUnary(Position pos, polyglot.ast.Unary.Operator op, Expr expr) {
        Unary unary = (Unary) nf.Unary(pos, op, expr).type(expr.type());
        return Desugarer.desugarUnary(unary, this);
    }

    /**
     * Create a local variable reference copied from another
     * 
     * @param pos the Position of the new local variable reference in source code.
     * @param local the local variable reference to copy
     * @return a synthesized local variable reference
     * TODO: move to Synthesizer
     */

    public Local createLocal(Position pos, Local local) {
        return synth.createLocal(pos, local.localInstance());
    }

    /** 
     * Create a local variable reference.
     * 
     * @param pos the Position of the reference in the source code
     * @param decl the declaration of the local variable
     * @return the synthesized Local variable reference
     * TODO: move into synthesizer, rewrite others
     */
    public Local createLocal(Position pos, LocalDecl decl) {
        return synth.createLocal(pos, decl.localDef().asInstance());
    }

    /** 
     * Create a local variable reference.
     * 
     * @param pos the Position of the reference in the source code
     * @param li a type system object representing this local variable
     * @return the synthesized Local variable reference
     * TODO: moved into synthesizer, rewrite others
     */
    public Local createLocal(Position pos, LocalInstance li) {
        return synth.createLocal(pos, li);
    }

    /** 
     * Create a binary expression.
     * 
     * @param pos the Position of the expression in the source code
     * @param left the first operand
     * @param op the operator
     * @param right the second operand
     * @return the synthesized Binary expression: (left op right)
     * TODO: move into Synthesizer
     */
    public Expr createBinary(Position pos, Expr left, Binary.Operator op, Expr right) {
        Binary binary = (Binary) nf.Binary(pos, left, op, right).type(left.type());
        return Desugarer.desugarBinary(binary, this);
    }

    /**
     * Create an assignment expression.
     * 
     * @param pos the Position of the assignment in the source code
     * @param target the lval being assigned to
     * @param op the assignment operator
     * @param source the right-hand-side of the assignment
     * @return the synthesized Assign expression: (target op source)
     * TODO: move into Synthesizer
     */
    public Assign createAssign(Position pos, Expr target, Operator op, Expr source) {
        try {
            Assign assign = synth.makeAssign(pos, target, op, source, context());
            return (Assign) Desugarer.desugarAssign(assign, this);
        } catch (SemanticException e) {
            throw new InternalCompilerError("Attempting to synthesize an Assign that cannot be typed", pos, e);
        }
    }

    /**
     * Create a coercion (implicit conversion) expression.
     * 
     * @param pos the Position of the cast in the source code
     * @param expr the Expr being cast
     * @param toType the resultant type
     * @return the synthesized Cast expression: (expr as toType), or null if the conversion is invalid
     * TODO: move into Synthesizer
     */
    public X10Cast createCoercion(Position pos, Expr expr, Type toType) {
        X10Cast cast;
        try {
            // FIXME: Have to typeCheck, because the typechecker has already desugared this to a conversion chain
            cast = nf.X10Cast(pos, nf.CanonicalTypeNode(pos, toType), expr, Converter.ConversionType.UNKNOWN_IMPLICIT_CONVERSION);
            cast = (X10Cast) typeCheck(cast);
            return cast;
        } catch (SemanticException e) {
            // work around for XTENLANG-1335
            try {
                cast = nf.X10Cast(pos, nf.CanonicalTypeNode(pos, toType), expr, Converter.ConversionType.UNCHECKED );
                cast = (X10Cast) typeCheck(cast);
                return cast;
            } catch (SemanticException x) {
                // return null;
            }
            // end work around for XTENLANG-1335
            return null;
        }
    }

    /**
     * Create a reference to a field of an object or struct.
     * If the receiver has a self constraint, propagate the constraint appropriately.
     * 
     * @param pos the Position of the reference
     * @param receiver the object or struct containing the field
     * @param name the Name of the field
     * @return the synthesized Field expression: (container . name), or null if no such field
     * TODO: move into Synthesizer
     */
    public Field createFieldRef(Position pos, Expr receiver, Name name) {
        final Type type = receiver.type();
        X10FieldInstance fi = X10TypeMixin.getProperty(type, name);
        if (null == fi) {
            fi = (X10FieldInstance) type.toClass().fieldNamed(name);
        }
        if (null == fi) return null;
        return createFieldRef(pos, receiver, fi);
    }

    /** 
     * Create a reference to a field of an object or struct.
     * If the receiver has a self constraint, propagate the constraint appropriately.
     * 
     * @param pos the Position of the reference in the source code
     * @param receiver the object or struct containing the field
     * @param fi a type system object representing this field
     * @return the synthesized Field expression
     * TODO: move into Synthesizer
     */
    public Field createFieldRef(Position pos, Expr receiver, X10FieldInstance fi) {
        Field f       = nf.Field(pos, receiver, nf.Id(pos, fi.name())).fieldInstance(fi);
        Type type     = fi.rightType();
        // propagate self binding (if any)
        CConstraint c = X10TypeMixin.realX(receiver.type());
        XTerm term    = X10TypeMixin.selfVarBinding(c);  // the RHS of {self==x} in c
        if (term != null) {
            type = addSelfConstraint(type, ts.xtypeTranslator().trans(c, term, fi));
            assert (null != type);
        }
        return (Field) f.type(type);
    }

    /** 
     * Create a Call to a static method.
     * 
     * @param pos the Position of the call in the source code
     * @param container the class that defines the static method
     * @param name the name of the static method
     * @param args the arguments to the static method
     * @return the synthesized Call to the method of the given type with the required name taking the prescribed arguments,
     * or null if no such method
     * TODO: move to Synthesizer
     */
    public X10Call createStaticCall(Position pos, Type container, Name name, Expr... args) {
        X10MethodInstance mi = createMethodInstance(container, name, args);
        if (null == mi) return null;
        return createStaticCall(pos, mi, args);
    }

    /**
     * Create a Call to a generic static method.
     * 
     * @param pos the Position of the call in the source code
     * @param container the class of the generic static method
     * @param name the name of the generic static method
     * @param typeArgs the type arguments to the generic static method
     * @param args the arguments to the generic static method
     * @return the synthesized Call to the method of the given type
     * with the required name taking the prescribed type arguments and arguments,
     * or null if no such method
     * TODO: move to Synthesizer
     */
    public X10Call createStaticCall(Position pos, Type container, Name name, List<Type> typeArgs, Expr... args) {
        X10MethodInstance mi = createMethodInstance(container, name, typeArgs, args);
        if (null == mi) return null;
        return createStaticCall(pos, mi, args);
    }

    /**
     * Create a Call to a static method.
     * The name and type of method (and any generic type arguments) are contained in the method instance.
     * 
     * @param pos the Position of the call in the source code
     * @param mi a type system object representing the static method being called
     * @param args the arguments to the call to the static method
     * @return the synthesized Call to the specified method taking the prescribed arguments
     * TODO: move to Synthesizer
     */
    public X10Call createStaticCall(Position pos, X10MethodInstance mi, Expr... args) {
        List<Type> typeParams = mi.typeParameters();
        List<TypeNode> typeParamNodes = new ArrayList<TypeNode>();
        for (Type t : typeParams) {
            typeParamNodes.add(nf.CanonicalTypeNode(pos, t));
        }
        return (X10Call) nf.X10Call( pos, 
                                      nf.CanonicalTypeNode(pos, mi.container()),
                                      nf.Id(pos, mi.name()), 
                                      typeParamNodes,
                                      Arrays.asList(args) ).methodInstance(mi).type(mi.returnType());
    }

    /** 
     * Create a Call to an instance method.
     * 
     * @param pos the Position of the call in the source code
     * @param receiver the object on which the instance method is being called
     * @param name the Name of the instance method
     * @param args the arguments to the instance method
     * @return the synthesized Call to a method on the specified receiver with the required name and prescribed arguments,
     * or null if no such method
     * TODO: move to Synthesizer
     */
    public X10Call createInstanceCall(Position pos, Expr receiver, Name name, Expr... args) {
        X10MethodInstance mi = createMethodInstance(receiver, name, args);
        if (null == mi) return null;
        return createInstanceCall(pos, receiver, mi, args);
    }

    /** 
     * Create a Call to a generic instance method.
     * 
     * @param pos the Position of the call in the source code
     * @param receiver the object on which the generic instance method is being called
     * @param name the Name of the generic instance method
     * @param typeArgs the type arguments to the generic instance method
     * @param args the arguments to the generic instance method
     * @return the synthesized Call to the method on the specified receiver 
     * with the required name taking the prescribed type arguments and arguments, 
     * or null if no such method
     * TODO: move to Synthesizer 
     */
    public X10Call createInstanceCall(Position pos, Expr receiver, Name name, List<Type> typeArgs, Expr... args) {
        X10MethodInstance mi = createMethodInstance(receiver, name, typeArgs, args);
        if (null == mi) return null;
        return createInstanceCall(pos, receiver, mi, args);
    }

    /**
     * Create a Call to an instance method.
     * The Name and any type arguments to the instance method are encoded in the method instance.
     * 
     * @param pos the Position of the call in the source code
     * @param receiver the object on which the instance method is being called
     * @param mi a type system object representing the instance method being called
     * @param args the arguments to the instance method
     * @return the synthesized Call to the specified method on the given receiver taking the prescribed arguments,
     * or null if no such method
     * TODO: move to Synthesizer 
     */
    public X10Call createInstanceCall(Position pos, Expr receiver, X10MethodInstance mi, Expr... args) {
        List<Type> typeParams = mi.typeParameters();
        List<TypeNode> typeParamNodes = new ArrayList<TypeNode>();
        for (Type t : typeParams) {
            typeParamNodes.add(nf.CanonicalTypeNode(pos, t));
        }
        return (X10Call) nf.X10Call( pos, 
                                      receiver, 
                                      nf.Id(pos, mi.name()),
                                      typeParamNodes,
                                      Arrays.asList(args) ).methodInstance(mi).type(mi.returnType());
    }

    // helper methods that return type system instances
    
    /**
     * Create a type system object representing a specified Generic method (either static or instance).
     * 
     * @param container the type (static method) or receiver (instance method) of the method call
     * @param name the Name of the method to be called
     * @param typeArgs the type arguments to the method
     * @param args the arguments to the method
     * @return the synthesized method instance for this method call
     * @throws InternalCompilerError if the required method instance cannot be created
     * TODO: move to a type system helper class
     */
    public X10MethodInstance createMethodInstance(Type container, Name name, List<Type> typeArgs, Expr... args) {
        List<Type> argTypes = getExprTypes(args);
        return createMethodInstance(container, name, typeArgs, argTypes);
    }

    /**
     * @param container
     * @param name
     * @param typeArgs
     * @param argTypes
     * @return
     */
    public X10MethodInstance createMethodInstance(Type container, Name name, List<Type> typeArgs, List<Type> argTypes) {
        return createMethodInstance(container, name, typeArgs, argTypes, context());
    }

    /**
        return createMethodInstance(container, name, typeArgs, argTypes, context);
     * @param container
     * @param name
     * @param typeArgs
     * @param argTypes
     * @param context
     * @return
     */
    public X10MethodInstance createMethodInstance(Type container, Name name, List<Type> typeArgs, List<Type> argTypes, Context context) {
        try {
            return ts.findMethod(container, ts.MethodMatcher(container, name, typeArgs, argTypes, context));
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unable to find required method instance", container.position(), e);
        }
    }

    /**
     * Create a type system object representing a specified method (either static or instance).
     * 
     * @param container the type (static method) or receiver (instance method) of the method call
     * @param name the Name of the method to be called
     * @param args the arguments to the method
     * @return the synthesized method instance for this method call
     * throws InternalCompilerError if the required method instance cannot be created
     * TODO: move to a type system helper class
     */
    public X10MethodInstance createMethodInstance(Type container, Name name, Expr... args) {
        List<Type> argTypes = getExprTypes(args);
        try {
            return ts.findMethod(container, ts.MethodMatcher(container, name, argTypes, context()) );
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unable to find required method instance", container.position(), e);
        }
    }

    /**
     * Create a type system object representing a specified Generic instance method.
     * 
     * @param receiver the receiver of the (instance) method call
     * @param name the Name of the method to be called
     * @param typeArgs the type arguments to the method
     * @param args the arguments to the method
     * @return the synthesized method instance for this method call
     * @throws InternalCompilerError if the required method instance cannot be created
     * TODO: move to a type system helper class
     */
    public X10MethodInstance createMethodInstance(Expr receiver, Name name, List<Type> typeArgs, Expr... args) {
        return createMethodInstance(receiver.type(), name, typeArgs, args);
    }

    /**
     * Create a type system object representing a specified instance method.
     * 
     * @param receiver the receiver of the (instance) method call
     * @param name the Name of the method to be called
     * @param args the arguments to the method
     * @return the synthesized method instance for this method call
     * throws InternalCompilerError if the required method instance cannot be created
     * TODO: move to a type system helper class
     */
    public X10MethodInstance createMethodInstance(Expr receiver, Name name, Expr... args) {
        return createMethodInstance(receiver.type(), name, args);
    }

    /**
     * Create a new Name for a temporary variable.
     * 
     * @return the newly created name
     * TODO: move to Synthesizer
     */
    public Name createTemporaryName() {
        return Name.makeFresh("t");
    }

    /** 
     * Find the Types for a sequence of expressions.
     * 
     * @param args the sequence of expressions to be typed
     * @return a List of the Types of the args
     */
    private static List<Type> getExprTypes(Expr... args) {
        List<Type> argTypes = new ArrayList<Type> (args.length);
        for (Expr a : args) {
            argTypes.add(a.type());
        }
        return argTypes;
    }

    // General helper methods

    /** 
     * Obtain the constant value of a property of an expression, if that value is known at compile time.
     * 
     * @param expr the Expr whose property is to be extracted
     * @param name the Name of the property to extract
     * @return the value of the named property of expr if it is a compile-time constant, or null if none
     * TODO: move into ASTQuery
     */
    public Object getPropertyConstantValue(Expr expr, Name name) {
        X10FieldInstance propertyFI = X10TypeMixin.getProperty(expr.type(), name);
        if (null == propertyFI) return null;
        Expr propertyExpr = createFieldRef(expr.position(), expr, propertyFI);
        if (null == propertyExpr) return null;
        return ConstantPropagator.constantValue(propertyExpr);
    }

    /**
     * Add a constraint to the type that binds a given property to a given value.
     * 
     * @param type the Type to be constrained
     * @param name the Name of a property of type
     * @param value the value of the named property for this type
     * @return the type with the additional constraint {name==value}, or null if no such property
     * TODO: move into Synthesizer
     */
    public static Type addPropertyConstraint(Type type, Name name, XTerm value) throws XFailure {
    	// Need to ensure that the argument to find or synthesize is a constrained type
    	// since the property may refer to the type's self variable.
    	ConstrainedType type1 = X10TypeMixin.toConstrainedType(type);
        XTerm property = X10TypeMixin.findOrSynthesize(type1, name);
        if (null == property) return null;
        return X10TypeMixin.addBinding(type1, property, value);
    }

    /**
     * Add a constraint to the type that binds a given property to a given value.
     * 
     * @param type the Type to be constrained
     * @param name the Name of a property of type
     * @param value the value of the named property for this type
     * @return the type with the additional constraint {name==value}, or null if no such property
     * TODO: move into Synthesizer
     */
   /* public static Type addPropertyConstraint(Type type, Name name, Object value) {
        return addPropertyConstraint(type, name, XTerms.makeLit(value));
    }*/

    /**
     * Add a self constraint to the type that binds self to a given value.
     * 
     * @param type the Type to be constrained
     * @param value the value of self for this type
     * @return the type with the additional constraint {self==value}, or null if the proposed
     * binding is inconsistent
     * TODO: move into Synthesizer
     */
    public static Type addSelfConstraint(Type type, XTerm value) {
        try {
            return X10TypeMixin.addSelfBinding(type, value);
        } catch (XFailure e) {
            return null;
        }
    }

    /**
     * @param expr
     * @throws SemanticException 
     */
    private Expr typeCheck(Expr expr) throws SemanticException {
        return (Expr) expr.typeCheck(this);
    }


    /**
     * @param pos
     * @param f
     * @return
     */
    public Local createLocal(Position pos, Formal f) {
        return synth.createLocal(pos, f.localDef().asInstance());
    }

    /**
     * @param pos
     * @param type
     * @return
     */
    private Expr createNull(Position pos, Type type) {
        return nf.NullLit(pos).type(type);
    }

    /**
     * @param pos
     * @param expr
     * @return
     */
    public Expr createIsNull(Position pos, Expr expr) {
        return createBinary(pos, createNull(pos, expr.type()), Binary.EQ, expr).type(ts.Boolean());
    }


    /**
     * @param pos
     * @param expr
     * @return
     */
    public Expr createNotNull(Position pos, Expr expr) {
        return createBinary(pos, createNull(pos, expr.type()), Binary.NE, expr).type(ts.Boolean());
    }

    /**
     * @param pos
     * @param type
     * @return
     */
    public Formal createFormal(Position pos, Type type) {
        return createFormal(pos, Flags.FINAL, type, Name.makeFresh("formal"));
    }


    /**
     * @param pos
     * @param name
     * @param type
     * @return
     */
    private Formal createFormal(Position pos, Flags flags, Type type, Name name) {
        FlagsNode         fn = nf.FlagsNode(pos, flags);
        CanonicalTypeNode tn = nf.CanonicalTypeNode(pos, type);
        Id                id = nf.Id(pos, name);
        X10LocalDef       ld = ts.localDef(pos, flags, Types.ref(type), name);
        return nf.Formal(pos, fn, tn, id).localDef(ld);
    }


    /**
     * @param pos
     * @param expr
     * @param f
     * @return
     */
    public Stmt createThrow(Position pos, Expr expr) {
        return nf.Throw(pos, expr);
    }


    /**
     * @param pos
     * @param f
     * @param body
     * @return
     */
    public Catch createCatch(Position pos, Formal f, Block body) {
        return nf.Catch(pos, f, body);
    }


    /**
     * @param pos
     * @param b
     * @param c
     * @return
     */
    public Try createTry(Position pos, Block b, Catch c) {
        return createTry(pos, b, Collections.<Catch>singletonList(c), null);
    }


    /**
     * @param pos
     * @param b
     * @param catches
     * @param f
     * @return
     */
    private Try createTry(Position pos, Block b, List<Catch> catches, Block f) {
        return nf.Try(pos, b, catches, f);
    }

    /**
     * Create a StringLit node representing a given String literal.
     * 
     * @param string the String value to be represented
     * @return a StringLit node representing the literal String string
     * TODO: move into Synthesizer
     */
    public StringLit createStringLit(String string) {
        return (StringLit) nf.StringLit(Position.COMPILER_GENERATED, string).type(ts.String());
    }


    /**
     * @param pos
     * @return
     */
    public Stmt createReturn(Position pos) {
        return nf.Return(pos);
    }


    /**
     * @param pos
     * @param expr
     * @param type
     * @return
     */
    public Stmt createReturn(Position pos, Expr expr) {
        return nf.Return(pos, expr);
    }


    /**
     * @param pos
     * @param label
     * @return
     */
    public Branch createBreak(Position pos, String label) {
        return nf.Break(pos, nf.Id(pos, label));
    }

    /**
     * @param pos
     * @return
     */
    public Stmt createContinue(Position pos) {
        return nf.Continue(pos);
    }

    /**
     * @param pos
     * @param lit
     * @return
     */
    public Branch createContinue(Position pos, String label) {
        return nf.Continue(pos, nf.Id(pos, label));
    }


    /**
     * @param pos
     * @param expr
     * @param type
     * @return
     */
    public Expr createNotInstanceof(Position pos, Expr expr, ClassType type) {
        return createNot(pos, createInstanceof(pos, expr, type)).type(ts.Boolean());
    }


    /**
     * @param pos
     * @param expr
     * @param type
     * @return
     */
    private Expr createInstanceof(Position pos, Expr expr, ClassType type) {
        return nf.Instanceof(pos, expr, nf.CanonicalTypeNode(pos, type)).type(ts.Boolean());
    }


    /**
     * @param pos
     * @param expr
     * @param tn
     * @return
     */
    public Expr createUncheckedCast(Position pos, Expr expr, Type type) {
        return nf.X10Cast(pos, nf.CanonicalTypeNode(pos, type), expr, Converter.ConversionType.UNCHECKED).type(type);
    }

    /**
     * @param pos
     * @return
     */
    public Id createLabel(Position pos) {
        Id label = nf.Id(pos, Name.makeFresh("L"));
        return label;
    }

    public Expr createLiteral(Position pos, Object value) {
        if (value == null)
            return nf.NullLit(pos).type(ts.Null());
        if (value instanceof Integer)
            return nf.IntLit(pos, IntLit.INT, (long) (int) (Integer) value).type(ts.Int());
        if (value instanceof Long)
            return nf.IntLit(pos, IntLit.LONG, (long) (Long) value).type(ts.Long());
        if (value instanceof Float)
            return nf.FloatLit(pos, FloatLit.FLOAT, (double) (float) (Float) value).type(ts.Float());
        if (value instanceof Double)
            return nf.FloatLit(pos, FloatLit.DOUBLE, (double) (Double) value).type(ts.Double());
        if (value instanceof Character)
            return nf.CharLit(pos, (char) (Character) value).type(ts.Char());
        if (value instanceof Boolean)
            return nf.BooleanLit(pos, (boolean) (Boolean) value).type(ts.Boolean());
        if (value instanceof String)
        //  return nf.StringLit(pos, (String) value).type(ts.String());
            return null; // strings have reference semantics
        if (value instanceof Object[]) {
            Object[] a = (Object[]) value;
            List<Expr> args = new ArrayList<Expr>(a.length);
            for (Object ai : a) {
                Expr ei = createLiteral(pos, ai);
                if (ei == null)
                    return null;
                args.add(ei);
            }
            return nf.Tuple(pos, args).type(ts.arrayOf(ts.Object()));
        }
        return null;
    }

    /**
     * @param position
     * @param label
     * @param body
     * @return
     */
    public Labeled createLabeledStmt(Position position, Id label, Stmt body) {
        return nf.Labeled(position, label, body);
    }

}

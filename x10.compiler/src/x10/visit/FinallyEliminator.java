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
package x10.visit;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Catch;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Labeled;
import polyglot.ast.LocalDecl;
import polyglot.ast.Loop;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.StringLit;
import polyglot.ast.Try;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import x10.ast.Closure;
import x10.errors.Warnings;
import x10.util.AltSynthesizer;

/**
 * @author Bowen Alpern
 *
 */
public class FinallyEliminator extends ContextVisitor {

    private static final boolean INLINE_FOR_BRANCH = false;
    private static final boolean INLINE_FOR_RETURN = false;

    private static final Name THROW_RETURN    = Name.make("throwReturn");
    private static final Name THROW_BREAK     = Name.make("throwBreak");
    private static final Name THROW_CONTINUE  = Name.make("throwContinue");
    private static final Name PLAUSIBLE_THROW = Name.make("plausibleThrow");
    private static final Name IS_BREAK        = Name.make("isBreak");
    private static final Name IS_RETURN       = Name.make("isReturn");
    private static final Name IS_CONTINUE     = Name.make("isCONTINUE");
    private static final Name VALUE           = Name.make("value");
    private static final Name LABEL           = Name.make("label");
    private static final Name EQUALS          = Name.make("equals");
    private static final QName FINALIZATION   = QName.make("x10.compiler.Finalization");

    protected static ClassType finalization;

    protected final TypeSystem ts;
    protected AltSynthesizer syn;
    protected final FinallyEliminatorState fes;

    /**
     * @param job
     * @param ts
     * @param nf
     */
    public FinallyEliminator(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        this.ts  = ts;
        this.syn = new AltSynthesizer(job, ts, nf);
        this.fes = new FinallyEliminatorState();
    }

    /* (non-Javadoc)
     * @see polyglot.visit.ContextVisitor#begin()
     */
    @Override
    public NodeVisitor begin() {
        syn = (AltSynthesizer) syn.begin();
        if (null == finalization) 
            finalization = ts.load("x10.compiler.Finalization");
        return super.begin();
    }

    @Override
    public FinallyEliminator context(Context c) {
        FinallyEliminator res = (FinallyEliminator) super.context(c);
        if (res != this)
            res.syn = (AltSynthesizer) syn.context(c);
        return res;
    }

    @Override
    public FinallyEliminator superEnter(Node parent, Node n) {
        FinallyEliminator res = (FinallyEliminator) super.superEnter(parent, n);
        if (res != this)
            res.syn = (AltSynthesizer) syn.enter(parent, n);
        return res;
    }

    /* (non-Javadoc)
     * @see polyglot.visit.ErrorHandlingVisitor#enterCall(polyglot.ast.Node)
     */
    @Override
    protected NodeVisitor enterCall(Node n) throws SemanticException {
        if (n instanceof Closure) {
            return new FinallyEliminator(job, ts, nodeFactory()).context(context());
        }
        Try t = tryWithFinally(n);
        if (null == t) {
            if (n instanceof MethodDecl)
                this.fes.returnType = ((MethodDecl) n).returnType();
            return this;
        }
        TryVisitor tv = new TryVisitor(this, t.finallyBlock());
        tv = (TryVisitor) tv.begin();
        tv = (TryVisitor) tv.context(context());
        this.fes.childVisitor.push(tv);
        return tv;
    }

    /* (non-Javadoc)
     * @see polyglot.visit.ErrorHandlingVisitor#leaveCall(polyglot.ast.Node)
     */
    @Override
    protected Node leaveCall(Node n) throws SemanticException {
        Try t = tryWithFinally(n);
        if (null == t) {
            if (n instanceof MethodDecl)
                this.fes.returnType = null;
            return super.leaveCall(n);
        }
        if (this.fes.childVisitor.isEmpty())
            throw new InternalCompilerError("Empty child visitor stack in FinallyEliminator", n.position());
        Stmt stmt = rewriteTry(t, this.fes.childVisitor.pop().tvs);
        return stmt;
    }

    /**
     * Determine if argument is a Try node with a Finally clause.
     * 
     * @param n a node, possibly a Try with a Finally clause.
     * @return n as a Try, if n is a Try with a finally clause; null, otherwise
     */
    private Try tryWithFinally(Node n) {
        if (n instanceof Try && null != ((Try) n).finallyBlock())
            return (Try) n;
        return null;
    }

    /**
     * @param t
     * @param tv
     * @return
     */
    private Stmt rewriteTry(Try t, TryVisitorState tvs) {
        Position pos        = t.position();
        Block fb            = t.finallyBlock();
        Stmt s              = t.finallyBlock(null);
        if (t.catchBlocks().isEmpty()) {
            s = t.tryBlock();
        }
        Name name           = Name.makeFresh("throwable");
        Type throwableType  = ts.Throwable();
        LocalDecl throwDecl = syn.createLocalDecl(pos, Flags.NONE, name, throwableType, syn.createLiteral(pos, null));
        Block tryBody       = syn.createBlock(pos, s, syn.createStaticCall(pos, finalization, PLAUSIBLE_THROW));
        Formal f            = syn.createFormal(pos, throwableType);
        Stmt assignment     = syn.createAssignment(pos, syn.createLocal(pos, throwDecl), Assign.ASSIGN, syn.createLocal(pos, f));
        Block catchBody     = syn.createBlock(pos, assignment);
        Catch catchClause   = syn.createCatch(pos, f, catchBody);
        Try wrappedTry      = syn.createTry(pos, tryBody, catchClause);
        Stmt abnormalExit   = (Stmt) handleAbnormalExit(pos, throwDecl, tvs).visit(this);
        List<Stmt> stmts    = new ArrayList<Stmt>();
        stmts.add(throwDecl);
        stmts.add(wrappedTry);
        stmts.add(fb);
        stmts.add(abnormalExit);
        return syn.createBlock(pos, stmts);
    }

    /**
     * @param pos
     * @param throwDecl
     * @param tvs 
     * @return
     */
    private Stmt handleAbnormalExit(Position pos, LocalDecl throwDecl, TryVisitorState tvs) {
        List<Stmt> stmts  = new ArrayList<Stmt>();
        // handle throw
        Expr cond         = syn.createNotInstanceof(pos, syn.createLocal(pos, throwDecl), finalization);
        Stmt cons         = syn.createThrow(pos, syn.createLocal(pos, throwDecl));
        Stmt stmt         = syn.createIf(pos, cond, cons, null);
        stmts.add(stmt);
        // handle return and branch
        Name fin          = Name.makeFresh("fin");
        LocalDecl finDecl = null;
        if (tvs.hasReturn || tvs.hasBreak || tvs.hasContinue) {
            Expr expr = syn.createUncheckedCast(pos, syn.createLocal(pos, throwDecl), finalization);
            finDecl = syn.createLocalDecl(pos, Flags.FINAL, fin, expr);
            stmts.add(finDecl);
        }
        // handle return
        if (tvs.hasReturn) {
            cond              = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), IS_RETURN);
            if (this.fes.returnType.type().isVoid()) {
                cons          = syn.createReturn(pos);
            } else {
                Expr expr     = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), VALUE);
                cons          = syn.createReturn(pos, syn.createUncheckedCast(pos, expr, this.fes.returnType.type()));
            }
            stmt              = syn.createIf(pos, cond, cons, null);
            stmts.add(stmt);
        }
        // handle break
        if (tvs.hasBreak) {
            cond              = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), IS_BREAK);
            List<Stmt> ss     = new ArrayList<Stmt>();
            Field f           = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), LABEL);
            Expr cd           = syn.createIsNull(pos, f);
            Stmt cs           = syn.createBreak(pos);
            stmt              = syn.createIf(pos, cd, cs, null);
            ss.add(stmt);
            for (String label : tvs.breakLabels) {
                if (null == label)
                    continue;
                f             = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), LABEL);
                cd            = syn.createInstanceCall(pos, syn.createStringLit(label), EQUALS, f);
                cs            = syn.createBreak(pos, label);
                stmt          = syn.createIf(pos, cd, cs, null);
                ss.add(stmt);
            }
            cons              = syn.createBlock(pos, ss);
            stmt              = syn.createIf(pos, cond, cons, null);
            stmts.add(stmt);
        }
        // handle continue
        if (tvs.hasContinue) {
            cond              = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), IS_BREAK);
            List<Stmt> ss     = new ArrayList<Stmt>();
            Field f           = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), LABEL);
            Expr cd           = syn.createIsNull(pos, f);
            Stmt cs           = syn.createContinue(pos);
            stmt              = syn.createIf(pos, cd, cs, null);
            ss.add(stmt);
            for (String label : tvs.continueLabels) {
                if (null == label)
                    continue;
                f             = syn.createFieldRef(pos, syn.createLocal(pos, finDecl), LABEL);
                cd            = syn.createInstanceCall(pos, syn.createStringLit(label), EQUALS, f);
                cs            = syn.createContinue(pos, label);
                stmt          = syn.createIf(pos, cd, cs, null);
                ss.add(stmt);
            }
            cons              = syn.createBlock(pos, ss);
            stmt              = syn.createIf(pos, cond, cons, null);
            stmts.add(stmt);
        }
        // TODO: handle error condition
        // Check that execution does not reach the end of stmts
        //    Code synthesized here will only be reached if either:
        //    1) there is a bug in the x10 compiler
        //    2) the x10 programmer concocts and throws an x10.compiler.Finalization.
        //    Both, are in error.
        // handle abnormal exit (brining it all together)
        cond              = syn.createNotNull(pos, syn.createLocal(pos, throwDecl));
        cons              = syn.createBlock(pos, stmts);
        stmt              = syn.createIf(pos, cond, cons, null);
        return stmt;
    }

    /*
     * A visitor for Try nodes with Finally clauses.
     * The Finally clause is visited by an outer visitor.
     * Normal execution and unmarked throws are routed to the Finally block.
     * Return's and Branch's (Break's and Continue's) execute the Finally block either by
     *    replication (and reinstantiation) of the Finally block, or
     *    throwing a Finalization exception.
     * Internal Try nodes with Finally clauses get their own TryVisitor's, 
     * but this one visits their Finally blocks.
     */
    private class TryVisitor extends FinallyEliminator {
    
        private final TryVisitorState tvs;
    
        /**
         * @param ov the outer visitor (will visit the root's Finally block)
         */
        public TryVisitor(FinallyEliminator ov, Block fb) {
            super(ov.job, ov.ts, ov.nodeFactory());
            tvs = new TryVisitorState(ov, fb);
            this.fes.returnType = ov.fes.returnType;
        }
    
        /* (non-Javadoc)
         * @see x10.visit.FinallyEliminator#enterCall(polyglot.ast.Node)
         */
        @Override
        protected NodeVisitor enterCall(Node n) throws SemanticException {
            if (n == tvs.finallyBlock) 
                return tvs.outerVisitor;
            if (n instanceof Labeled) {
                Labeled l = (Labeled) n;
                Id id     = l.labelNode();
                Name name = null != id ? id.id() : Name.make("");
                String label = name.toString();
                tvs.ignoreLabels.add(label);
            }
            if (n instanceof Loop) {
                tvs.loopNesting++;
            }
            return super.enterCall(n);
        }
    
        /**
         * Make sure that exits from the try by Return or Branch (Break or Continue) are routed through the finally block.
         * Two approaches are possible:
         *   1) Replicate and reinstantiate the finally block before the exit, and
         *   2) Create and throw a custom Finalization which will be caught before the finally block and processed after it.
         * 
         * TODO: implement both possibiities and choose between them at compile time.
         * 
         * @see x10.visit.FinallyEliminator#leaveCall(polyglot.ast.Node)
         */
        @Override
        protected Node leaveCall(Node n) throws SemanticException {
            Position pos     = n.position();
            List<Stmt> stmts = new ArrayList<Stmt>();
            if (n instanceof Return) {
                Return r = (Return) n;
                if (INLINE_FOR_RETURN) {
                    if (null == r.expr()) {
                        stmts.add(replicate(pos, tvs.finallyBlock));
                        stmts.add(r);
                    } else {
                        LocalDecl temp = syn.createLocalDecl(pos, Flags.FINAL, Name.makeFresh(), r.expr());
                        stmts.add(temp);
                        stmts.add(replicate(pos, tvs.finallyBlock));
                        stmts.add(r.expr(syn.createLocal(pos, temp)));
                    }
                    return syn.createBlock(pos, stmts);
                } else {
                    tvs.hasReturn = true;
                    if (null == r.expr()) {
                        return syn.createEval(syn.createStaticCall(pos, finalization, THROW_RETURN));
                    } else {
                        return syn.createEval(syn.createStaticCall(pos, finalization, THROW_RETURN, r.expr()));
                    }
                }
            } else if (n instanceof Branch) {
                Branch b     = (Branch) n;
                Id id        = b.labelNode();
                if (null == id && 0 < tvs.loopNesting) return super.leaveCall(n);
                Name name    = null != id   ? id.id()         : null;
                String label = null != name ? name.toString() : null;
                if (null != label && tvs.ignoreLabels.contains(label)) return super.leaveCall(n);
                StringLit lit = null != label ? syn.createStringLit(label) : null;
                if (INLINE_FOR_BRANCH) {
                    stmts.add(replicate(pos, tvs.finallyBlock));
                    stmts.add(b);
                    return syn.createBlock(pos, stmts);
                } else {
                    if (b.kind() == Branch.BREAK) {
                        tvs.hasBreak = true;
                        if (null != label) tvs.breakLabels.add(label);
                        if (null == lit) 
                            return syn.createEval(syn.createStaticCall(pos, finalization, THROW_BREAK));
                        return syn.createEval(syn.createStaticCall(pos, finalization, THROW_BREAK, lit));
                    } else {
                        tvs.hasContinue = true;
                        tvs.continueLabels.add(label);
                        if (null == lit) 
                            return syn.createEval(syn.createStaticCall(pos, finalization, THROW_CONTINUE));
                        return syn.createEval(syn.createStaticCall(pos, finalization, THROW_CONTINUE, lit));
                    }
                }
            }
            if (n instanceof Loop) {
                tvs.loopNesting++;
            }
            return super.leaveCall(n);
        }

        /**
         * @param block
         * @return
         */
        private Stmt replicate(Position pos, Block block) {
            Block b = (Block) block.copy();
            Warnings.issue(job(), "Finally block reinstantiation not yet implemented", pos);
            // TODO: reinstantiate b
            return b;
        }
    
    }

    class FinallyEliminatorState {
        private TypeNode returnType;
        private Stack<TryVisitor> childVisitor = new Stack<TryVisitor>();
    }

    class TryVisitorState {
    
        public FinallyEliminator outerVisitor;
        public Block             finallyBlock;
        public Set<String>       breakLabels    = new HashSet<String>();
        public Set<String>       continueLabels = new HashSet<String>();
        public Set<String>       ignoreLabels   = new HashSet<String>();
        public boolean           hasReturn      = false;
        public boolean           hasBreak       = false;
        public boolean           hasContinue    = false;
        public int               loopNesting    = 0;
    
        TryVisitorState (FinallyEliminator ov, Block fb) {
            outerVisitor = ov;
            finallyBlock = fb;
        }
    }

}

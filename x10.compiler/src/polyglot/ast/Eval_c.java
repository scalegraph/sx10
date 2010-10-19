/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * An <code>Eval</code> is a wrapper for an expression in the context of
 * a statement.
 */
public class Eval_c extends Stmt_c implements Eval
{
    protected Expr expr;

    public Eval_c(Position pos, Expr expr) {
	super(pos);
	assert(expr != null);
	this.expr = expr;
    }

    /** Get the expression of the statement. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression of the statement. */
    public Eval expr(Expr expr) {
	Eval_c n = (Eval_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the statement. */
    protected Eval_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Eval_c n = (Eval_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            return ts.Void();
        }

        return child.type();
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }

    public String toString() {
	return "eval(" + expr.toString() + ");";
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	boolean semi = tr.appendSemicolon(true);

	print(expr, w, tr);

	if (semi) {
	    w.write(";");
	}

	tr.appendSemicolon(semi);
    }

    public Term firstChild() {
        return expr;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        v.visitCFG(expr, this, EXIT);
        return succs;
    }

}

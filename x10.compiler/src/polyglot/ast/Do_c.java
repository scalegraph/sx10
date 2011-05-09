/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * A immutable representation of a Java language <code>do</code> statement. 
 * It contains a statement to be executed and an expression to be tested 
 * indicating whether to reexecute the statement.
 */ 
public abstract class Do_c extends Loop_c implements Do
{
    protected Stmt body;
    protected Expr cond;

    public Do_c(Position pos, Stmt body, Expr cond) {
	super(pos);
	assert(body != null && cond != null);
	this.body = body;
	this.cond = cond;
    }

    /** Get the body of the statement. */
    public Stmt body() {
	return this.body;
    }

    /** Set the body of the statement. */
    public Do body(Stmt body) {
	Do_c n = (Do_c) copy();
	n.body = body;
	return n;
    }

    /** Get the conditional of the statement. */
    public Expr cond() {
	return this.cond;
    }

    /** Set the conditional of the statement. */
    public Do cond(Expr cond) {
	Do_c n = (Do_c) copy();
	n.cond = cond;
	return n;
    }

    /** Reconstruct the statement. */
    protected Do_c reconstruct(Stmt body, Expr cond) {
	if (body != this.body || cond != this.cond) {
	    Do_c n = (Do_c) copy();
	    n.body = body;
	    n.cond = cond;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
        Node body = visitChild(this.body, v);
        if (body instanceof NodeList) body = ((NodeList) body).toBlock();
	Expr cond = (Expr) visitChild(this.cond, v);
	return reconstruct((Stmt) body, cond);
    }

    /** Type check the statement. */
    public abstract Node typeCheck(ContextVisitor tc);

    public String toString() {
	return "do " + body + " while (" + cond + ")";
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr)
    {
	w.write("do ");
	printSubStmt(body, w, tr);
	w.write("while(");
	printBlock(cond, w, tr);
	w.write("); ");
    }


    public Term firstChild() {
        return body;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        v.push(this).visitCFG(body, cond, ENTRY);

        if (condIsConstantTrue()) {
            v.visitCFG(cond, body, ENTRY);
        }
        else {
            v.visitCFG(cond, FlowGraph.EDGE_KEY_TRUE, body, 
                             ENTRY, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
        }

        return succs;
    }

    public Term continueTarget() {
        return cond;
    }

}

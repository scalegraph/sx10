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
 * A <code>Catch</code> represents one half of a <code>try-catch</code>
 * statement.  Specifically, the second half.
 */
public class Catch_c extends Stmt_c implements Catch
{
    protected Formal formal;
    protected Block body;

    public Catch_c(Position pos, Formal formal, Block body) {
	super(pos);
	assert(formal != null && body != null);
	this.formal = formal;
	this.body = body;
    }

    /** Get the catchType of the catch block. */
    public Type catchType() {
        return formal.declType();
    }

    /** Get the formal of the catch block. */
    public Formal formal() {
	return this.formal;
    }

    /** Set the formal of the catch block. */
    public Catch formal(Formal formal) {
	Catch_c n = (Catch_c) copy();
	n.formal = formal;
	return n;
    }

    /** Get the body of the catch block. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the catch block. */
    public Catch body(Block body) {
	Catch_c n = (Catch_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the catch block. */
    protected Catch_c reconstruct(Formal formal, Block body) {
	if (formal != this.formal || body != this.body) {
	    Catch_c n = (Catch_c) copy();
	    n.formal = formal;
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the catch block. */
    public Node visitChildren(NodeVisitor v) {
	Formal formal = (Formal) visitChild(this.formal, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(formal, body);
    }

    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    /** Type check the catch block. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

	if (! catchType().isThrowable()) {
	    throw new SemanticException("Can only throw subclasses of \"" +ts.Throwable() + "\".", formal.position());

	}

	return this;
    }

    public String toString() {
	return "catch (" + formal + ") " + body;
    }

    /** Write the catch block to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write("catch (");
	printBlock(formal, w, tr);
	w.write(")");
	printSubStmt(body, w, tr);
    }

    public Term firstChild() {
        return formal;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        v.visitCFG(formal, body, ENTRY);
        v.visitCFG(body, this, EXIT);
        return succs;
    }

}

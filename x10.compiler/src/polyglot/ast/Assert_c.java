/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.frontend.Globals;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import x10.errors.Errors;

/**
 * An <code>Assert</code> is an assert statement.
 */
public class Assert_c extends Stmt_c implements Assert
{
    protected Expr cond;
    protected Expr errorMessage;

    public Assert_c(Position pos, Expr cond, Expr errorMessage) {
	super(pos);
	assert(cond != null); // errorMessage may be null
	this.cond = cond;
	this.errorMessage = errorMessage;
    }

    /** Get the condition to check. */
    public Expr cond() {
	return this.cond;
    }

    /** Set the condition to check. */
    public Assert cond(Expr cond) {
	Assert_c n = (Assert_c) copy();
	n.cond = cond;
	return n;
    }

    /** Get the error message to report. */
    public Expr errorMessage() {
	return this.errorMessage;
    }

    /** Set the error message to report. */
    public Assert errorMessage(Expr errorMessage) {
	Assert_c n = (Assert_c) copy();
	n.errorMessage = errorMessage;
	return n;
    }

    /** Reconstruct the statement. */
    protected Assert_c reconstruct(Expr cond, Expr errorMessage) {
	if (cond != this.cond || errorMessage != this.errorMessage) {
	    Assert_c n = (Assert_c) copy();
	    n.cond = cond;
	    n.errorMessage = errorMessage;
	    return n;
	}

	return this;
    }

    public Node typeCheck(ContextVisitor tc) {
        if (! cond.type().isBoolean()) {
            Errors.issue(tc.job(),
                    new SemanticException("Condition of assert statement must have boolean type.", cond.position()),
                    this);
        }

        if (errorMessage != null && errorMessage.type().isVoid()) {
            Errors.issue(tc.job(),
                    new SemanticException("Error message in assert statement cannot be void.", errorMessage.position()),
                    this);
        }

        return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == cond) {
            return ts.Boolean();
        }

        /*
        if (child == errorMessage) {
            return ts.String();
        }
        */

        return child.type();
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr cond = (Expr) visitChild(this.cond, v);
	Expr errorMessage = (Expr) visitChild(this.errorMessage, v);
	return reconstruct(cond, errorMessage);
    }

    public String toString() {
	return "assert " + cond.toString() +
                (errorMessage != null
                    ? ": " + errorMessage.toString() : "") + ";";
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("assert ");
	print(cond, w, tr);

        if (errorMessage != null) {
            w.write(": ");
            print(errorMessage, w, tr);
        }

        w.write(";");
    }

    public void translate(CodeWriter w, Translator tr) {
        if (! tr.job().extensionInfo().getOptions().assertions) {
            w.write(";");
        }
        else {
            prettyPrint(w, tr);
        }
    }

    public Term firstChild() {
        return cond;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        if (errorMessage != null) {
            v.visitCFG(cond, errorMessage, ENTRY);
            v.visitCFG(errorMessage, this, EXIT);
        }
        else {
            v.visitCFG(cond, this, EXIT);
        }

        return succs;
    }

}

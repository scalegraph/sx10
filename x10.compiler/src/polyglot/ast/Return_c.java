/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import x10.errors.Errors;

/**
 * A <code>Return</code> represents a <code>return</code> statement in Java.
 * It may or may not return a value.  If not <code>expr()</code> should return
 * null.
 */
public abstract class Return_c extends Stmt_c implements Return
{
    protected Expr expr;

    public Return_c(Position pos, Expr expr) {
	super(pos);
	assert(true); // expr may be null
	this.expr = expr;
    }

    /** Get the expression to return, or null. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to return, or null. */
    public Return expr(Expr expr) {
	Return_c n = (Return_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the statement. */
    protected Return_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Return_c n = (Return_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }

    /** Type check the statement. */
    public abstract Node typeCheck(ContextVisitor tc) throws SemanticException;
  
    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == expr) {
            Context c = av.context();
            CodeDef ci = c.currentCode();

            if (ci instanceof MethodDef) {
                MethodDef mi = (MethodDef) ci;

                TypeSystem ts = av.typeSystem();

                // If expr is an integral constant, we can relax the expected
                // type to the type of the constant.
                if (ts.numericConversionValid(mi.returnType().get(), child.constantValue(), c)) {
                    return child.type();
                }
                else {
                    return mi.returnType().get();
                }
            }
        }

        return child.type();
    }

    public String toString() {
	return "return" + (expr != null ? " " + expr : "") + ";";
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write("return") ;
	if (expr != null) {
	    w.write(" ");
	    print(expr, w, tr);
	}
	w.write(";");
    }

    public Term firstChild() {
        if (expr != null) return expr;
        return null;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        if (expr != null) {
            v.visitCFG(expr, this, EXIT);
        }

        v.visitReturn(this);
        return Collections.<S>emptyList();
    }
    

}

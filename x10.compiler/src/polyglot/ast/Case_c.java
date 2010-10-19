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
 * A <code>Case</code> is a representation of a Java <code>case</code>
 * statement.  It can only be contained in a <code>Switch</code>.
 */
public class Case_c extends Stmt_c implements Case
{
    protected Expr expr;
    protected long value;

    public Case_c(Position pos, Expr expr) {
	super(pos);
	assert(true); // expr may be null for default case
	this.expr = expr;
    }

    /** Returns true iff this is the default case. */
    public boolean isDefault() {
	return this.expr == null;
    }

    /**
     * Get the case label.  This must should a constant expression.
     * The case label is null for the <code>default</code> case.
     */
    public Expr expr() {
	return this.expr;
    }

    /** Set the case label.  This must should a constant expression, or null. */
    public Case expr(Expr expr) {
	Case_c n = (Case_c) copy();
	n.expr = expr;
	return n;
    }

    /**
     * Returns the value of the case label.  This value is only valid
     * after type-checking.
     */
    public long value() {
	return this.value;
    }

    /** Set the value of the case label. */
    public Case value(long value) {
	Case_c n = (Case_c) copy();
	n.value = value;
	return n;
    }

    /** Reconstruct the statement. */
    protected Case_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Case_c n = (Case_c) copy();
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
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        if (expr == null) {
	    return this;
	}

	TypeSystem ts = tc.typeSystem();

	if (! ts.isImplicitCastValid(expr.type(), ts.Int(), tc.context()) && ! ts.isImplicitCastValid(expr.type(), ts.Char(), tc.context())) {
	    throw new SemanticException("Case label must be an byte, char, short, or int.",position());
	}
    
	return this;
    }
    
    public Node checkConstants(ContextVisitor tc) throws SemanticException {
        if (expr == null) {
            return this;
        }
        
        if (expr.isConstant()) {
            Object o = expr.constantValue();
            
            if (o instanceof Number && ! (o instanceof Long) &&
                    ! (o instanceof Float) && ! (o instanceof Double)) {
                
                return value(((Number) o).longValue());
            }
            else if (o instanceof Character) {
                return value(((Character) o).charValue());
            }
        }
        
        throw new SemanticException("Case label must be an integral constant.",position());
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            return ts.Int();
        }

        return child.type();
    }

    public String toString() {
        if (expr == null) {
	    return "default:";
	}
	else {
	    return "case " + expr + ":";
	}
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (expr == null) {
	    w.write("default:");
	}
	else {
	    w.write("case ");
	    print(expr, w, tr);
	    w.write(":");
	}
    }

    public Term firstChild() {
        if (expr != null) return expr;
        return null;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        if (expr != null) {
            v.visitCFG(expr, this, EXIT);
        }

        return succs;
    }

}

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

package x10.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.ast.Expr_c;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

import x10.constraint.XFailure;
import x10.constraint.XTerm;
import x10.constraint.XVar;
import x10.errors.Errors;
import polyglot.types.Context;
import x10.types.X10TypeMixin;
import polyglot.types.TypeSystem;
import x10.types.constraints.CConstraint;
import x10.types.constraints.CConstraint;
import x10.types.constraints.XConstrainedTerm;


/**
 *
 */
public class Here_c extends Expr_c 
    implements Here {

    public Here_c(Position p) {
        super(p);
    }
    
    /**
     * Return the first (sub)term performed when evaluating this
     * term.
     */
    public Term firstChild() {
        return null;
    }

    /**
     * Visit this term in evaluation order.
     */
    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        return succs;
    }
    
    public String toString() {
    	return "here";
    }
    /* (non-Javadoc)
     * @see x10.ast.TranslateWhenDumpedNode#getArgument(int)
     */
    public Node getArgument(int id) {
        assert (false);
        return null;
    }
    
    /** Type check the expression. */
	public Node typeCheck(ContextVisitor tc) {
		TypeSystem ts = (TypeSystem) tc.typeSystem();
		Context xc = (Context) tc.context();

		Type tt = ts.Place();
		XConstrainedTerm h = xc.currentPlaceTerm();
		if (h != null) {
			CConstraint cc = new CConstraint();
			try {
				cc.addSelfBinding(h);
			}
			catch (XFailure e) {
				Errors.issue(tc.job(),
				        new SemanticException("Constraint on here is inconsistent; " + e.getMessage(), position()));
			}
			tt = X10TypeMixin.xclause(X10TypeMixin.baseType(tt), cc);
		}
		
		return type(tt);
	}
    public String translate(Resolver c) {
      return "x10.lang.Runtime.here()";
    }
    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    	w.write(" here ");
    }
}

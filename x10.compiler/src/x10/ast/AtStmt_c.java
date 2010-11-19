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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.CompoundStmt;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.ast.Stmt_c;
import polyglot.main.Report;
import polyglot.types.Context;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.PruningVisitor;
import x10.constraint.XConstraint;
import x10.constraint.XFailure;
import x10.constraint.XVar;
import x10.constraint.XTerm;
import x10.errors.Errors;
import x10.types.ClosureDef;
import x10.types.ParameterType;
import x10.types.X10ProcedureDef;
import polyglot.types.Context;
import x10.types.X10MethodDef;
import x10.types.X10TypeMixin;
import polyglot.types.TypeSystem;
import x10.types.X10Context_c;
import x10.types.checker.PlaceChecker;
import x10.types.constraints.CConstraint;
import x10.types.constraints.XConstrainedTerm;

/**
 * Created on Oct 5, 2004
 *
 * @author Christian Grothoff
 * @author Philippe Charles
 * @author vj
 * @author nystrom
 */

public class AtStmt_c extends Stmt_c implements AtStmt {

	public Expr place;
	public Stmt body;

	public AtStmt_c(Position pos, Expr place, Stmt body) {
		super(pos);
		this.place = place;
		this.body = body;
	}

	public AtStmt_c(Position p) {
		super(p);
	}

	/* (non-Javadoc)
	 * @see x10.ast.Future#body()
	 */
	public Stmt body() {
		return body;
	}

	/**
	 * Set the body of the statement.
	 */
	public AtStmt body(Stmt body) {
		AtStmt_c n = (AtStmt_c) copy();
		n.body = body;
		return n;
	}

	/** Get the RemoteActivity's place. */
	public Expr place() {
		return place;
	}

	/** Set the RemoteActivity's place. */
	public RemoteActivityInvocation place(Expr place) {
		if (place != this.place) {
			AtStmt_c n = (AtStmt_c) copy();
			n.place = place;
			return n;
		}

		return this;
	}

	/** Reconstruct the statement. */
	protected AtStmt reconstruct(Expr place, Stmt body) {
		if (place != this.place || body != this.body) {
			AtStmt_c n = (AtStmt_c) copy();
			n.place = place;
			n.body = body;
			return n;
		}
		return this;
	}

    XConstrainedTerm placeTerm;
    boolean placeError = false;
  
    XConstrainedTerm finishPlaceTerm;
    public boolean isFinishPlace() {
        boolean isFinishPlace = false;
        if (null != finishPlaceTerm) {
            XConstraint constraint = new XConstraint();
            try {
                constraint.addBinding(finishPlaceTerm.term(),placeTerm.term());
                if (placeTerm.constraint().entails(constraint)) {
                    isFinishPlace = true;
                }
            } catch (XFailure xFailure) {}
        }
        return isFinishPlace;
    }

    @Override
    public Node typeCheckOverride(Node parent, ContextVisitor tc) {
    	TypeSystem ts = (TypeSystem) tc.typeSystem();
    	NodeVisitor v = tc.enter(parent, this);
    	
    	if (v instanceof PruningVisitor) {
    		return this;
    	}

        if (placeTerm == null) {
            try {
                placeTerm = PlaceChecker.computePlaceTerm((Expr) visitChild(this.place, v),
                        (Context) tc.context(), ts);
                finishPlaceTerm = tc.context().currentFinishPlaceTerm();
            } catch (SemanticException e) {
                CConstraint d = new CConstraint();
                XTerm term = PlaceChecker.makePlace();
                try {
                    placeTerm = XConstrainedTerm.instantiate(d, term);
                    placeError = true;
                } catch (XFailure z) {
                    throw new InternalCompilerError("Cannot construct placeTerm from term  and constraint.");
                }
            }
        }
    	
    	// now that placeTerm is set in this node, continue visiting children
    	// enterScope will ensure that placeTerm is installed in the context.
    	
    	return null;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) {
        TypeSystem ts = (TypeSystem) tc.typeSystem();
        if (placeError) { // this means we were not able to convert this.place into a term of type Place.
            Errors.issue(tc.job(), 
                    new Errors.AtArgMustBePlace(this.place, ts.Place(), this.position()));
        }
        return this;
    }
    
	/** Visit the children of the statement. */
	public Node visitChildren(NodeVisitor v) {
		Expr place = (Expr) visitChild(this.place, v);
		Stmt body = (Stmt) visitChild(this.body, v);
		return reconstruct(place, body);
	}

    public static Context createDummyAsync(Context c, boolean isAsyncOrAt) {        
        TypeSystem ts = (TypeSystem) c.typeSystem();
        X10MethodDef asyncInstance = (X10MethodDef) ts.asyncCodeInstance(c.inStaticContext());

        if (c.currentCode() instanceof X10ProcedureDef) {
            X10ProcedureDef outer = (X10ProcedureDef) c.currentCode();
            asyncInstance.setThisDef(outer.thisDef());
            List<ParameterType> capturedTypes = outer.typeParameters();
            if (!capturedTypes.isEmpty()) {
                asyncInstance = ((X10MethodDef) asyncInstance.copy());
                asyncInstance.setTypeParameters(capturedTypes);
            }
        }
        c = c.pushCode(asyncInstance);
        ((X10Context_c)c).x10Kind = isAsyncOrAt ? X10Context_c.X10Kind.Async : X10Context_c.X10Kind.At;
        return c;
    }
	public Context enterScope(Context c) {
        return createDummyAsync(c, false);
	}

	@Override
	public Context enterChildScope(Node child, Context c) {
		if (child != this.body) {
			// pop the scope pushed by enterScope.
			c = c.pop();
		} else {
			c = super.enterChildScope(child,c);
			Context xc = (Context) c;
			if (child == body) {
				if (placeTerm != null)
					c = xc.pushPlace(placeTerm);
			}
			addDecls(c);
		}
		return c;
	}


	public Type childExpectedType(Expr child, AscriptionVisitor av) {
		TypeSystem ts = (TypeSystem) av.typeSystem();
		if (child == place) {
			return ts.Place();
		}
		return child.type();
	}

	public String toString() {
		return "at (" + place + ") " + body;
	}

	/** Write the statement to an output file. */
	public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
		w.write("at (");
		printBlock(place, w, tr);
		w.write(") ");
		printSubStmt(body, w, tr);
	}

	/**
	 * Return the first (sub)term performed when evaluating this
	 * term.
	 */
	public Term firstChild() {
		if (place != null) {
			return place;
		}

		return body;
	}

	/**
	 * Visit this term in evaluation order.
	 * [IP] Treat this as a conditional to make sure the following
	 *      statements are always reachable.
	 * FIXME: We should really build our own CFG, push a new context,
	 * and disallow uses of "continue", "break", etc. in asyncs.
	 */
	public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {

		if (place != null) {
			v.visitCFG(place, FlowGraph.EDGE_KEY_TRUE, body,
					ENTRY, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
		}

		v.visitCFG(body, this, EXIT);

		return succs;
	}

	private static final Collection<String> TOPICS =
		CollectionUtil.list(Report.types, Report.context);
}


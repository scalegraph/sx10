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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.Stmt_c;
import polyglot.ast.Term;
import polyglot.main.Report;
import polyglot.types.CodeDef;
import polyglot.types.Context;
import polyglot.types.Def;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.PruningVisitor;
import polyglot.visit.TypeBuilder;
import x10.errors.Errors;
import x10.types.AsyncDef;
import x10.types.X10ClassDef;
import x10.types.X10Context_c;
import x10.types.X10MemberDef;
import x10.types.X10MethodDef;

/**
 * Created on Oct 5, 2004
 *
 * @author Christian Grothoff
 * @author Philippe Charles
 * @author vj
 */

public class Async_c extends Stmt_c implements Async {
	protected Stmt body;
	protected List<Expr> clocks;
	protected boolean clocked; // should be equal to (clocks != null && clocks.size() > 0)
	protected AsyncDef asyncDef;

	public Async_c(Position pos, List<Expr> clocks, Stmt body) {
		super(pos);
		this.clocks = clocks;
		this.body = body;
	}

	public Async_c(Position pos, Stmt body, boolean clocked) {
		super(pos);
		this.clocked = true;
		this.body = body;
		// temporary. Needs to be initialized with clock from environment.
		this.clocks = new ArrayList<Expr>();
	}

	public boolean clocked() { return clocked;}
	public Async_c(Position p) {
		super(p);
	}

	/* (non-Javadoc)
	 * @see x10.ast.Future#body()
	 */
	public Stmt body() {
		return body;
	}

	/** Expression */
	public List<Expr> clocks() {
		return this.clocks;
	}

	/** clock */
	public Clocked clocks(List<Expr> clocks) {
		Async_c n = (Async_c) copy();
		n.clocks = clocks;
		return n;
	}

	/**
	 * Set the body of the statement.
	 */
	public Async body(Stmt body) {
		Async_c n = (Async_c) copy();
		n.body = body;
		return n;
	}

	public AsyncDef asyncDef() {
	    return this.asyncDef;
	}

	public Async asyncDef(AsyncDef ci) {
	    if (ci == this.asyncDef) return this;
	    Async_c n = (Async_c) copy();
	    n.asyncDef = ci;
	    return n;
	}

	/** Reconstruct the statement. */
	protected Async reconstruct(List<Expr> clocks, Stmt body) {
		if ( body != this.body || clocks != this.clocks) {
			Async_c n = (Async_c) copy();
			n.clocks = clocks;
			n.body = body;
			return n;
		}
		return this;
	}

	/** Visit the children of the statement. */
	public Node visitChildren(NodeVisitor v) {
		List<Expr> clocks = visitList(this.clocks, v);
		Stmt body = (Stmt) visitChild(this.body, v);
		return reconstruct(clocks, body);
	}

	@Override
	public Node buildTypesOverride(TypeBuilder tb) {
	    TypeSystem ts = (TypeSystem) tb.typeSystem();

	    X10ClassDef ct = (X10ClassDef) tb.currentClass();
	    assert ct != null;

	    Def def = tb.def();

	    if (def instanceof FieldDef) {
	        // FIXME: is this possible?
	        FieldDef fd = (FieldDef) def;
	        def = fd.initializer();
	    }

	    if (!(def instanceof CodeDef)) {
	        Errors.issue(tb.job(), new SemanticException("Async cannot occur outside code body.", position()));
	        // Fake it
	        def = ts.initializerDef(position(), Types.ref(ct.asType()), Flags.STATIC);
	    }

	    CodeDef code = (CodeDef) def;

	    AsyncDef mi = (AsyncDef) AtStmt_c.createDummyAsync(position(), ts, ct.asType(), code, code.staticContext(), true);

	    // Unlike methods and constructors, do not create new goals for resolving the signature and body separately;
	    // since closures don't have names, we'll never have to resolve the signature.  Just push the code context.
	    TypeBuilder tb2 = tb.pushCode(mi);

	    Async_c n = (Async_c) this.del().visitChildren(tb2);

	    if (code instanceof X10MemberDef) {
	        assert mi.thisDef() == ((X10MemberDef) code).thisDef();
	    }

	    return n.asyncDef(mi);
	}

	@Override
	public Node typeCheckOverride(Node parent, ContextVisitor tc) {
	    TypeSystem ts = (TypeSystem) tc.typeSystem();
	    NodeVisitor v = tc.enter(parent, this);

	    if (v instanceof PruningVisitor) {
	        return this;
	    }


	    // now that placeTerm is set in this node, continue visiting children
	    // enterScope will ensure that placeTerm is installed in the context.

	    return null;
	}

	/**
	 * The evaluation of place and list of clocks is not in the scope of the async.
	 */
	public Context enterChildScope(Node child, Context c) {
	    if (Report.should_report(TOPICS, 5))
	        Report.report(5, "enter async scope");
	    if (child == this.body) {
	        c = c.pushCode(asyncDef);
	        ((X10Context_c)c).x10Kind = X10Context_c.X10Kind.Async;
	        return c;
	    }
	    return c;
	}

	public Node typeCheck(ContextVisitor tc) {
		TypeSystem ts = (TypeSystem) tc.typeSystem();
		NodeFactory nf = (NodeFactory) tc.nodeFactory();

		Context c = (Context) tc.context();
		if (clocked() && ! c.inClockedFinishScope())
			Errors.issue(tc.job(),
			        new SemanticException("clocked async must be invoked inside a statically enclosing clocked finish.", position()));
			
        for (Expr e : clocks()) {
            Type t = e.type();
            if (!t.isSubtype(ts.Clock(), tc.context())) {
                Errors.issue(tc.job(),
                        new SemanticException("Type \"" + t + "\" must be x10.lang.clock.", e.position()));
            }
        }

		return this;
	}

	
	public Type childExpectedType(Expr child, AscriptionVisitor av) {
		TypeSystem ts = (TypeSystem) av.typeSystem();
		return child.type();
	}

	public String toString() {
		return "async  { ... }";
	}

	/** Write the statement to an output file. */
	public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
		w.write("async ");
		if (clocks != null && ! clocks.isEmpty()) {
			w.write("clocked (");
			w.begin(0);

			for (Iterator<Expr> i = clocks.iterator(); i.hasNext(); ) {
			    Expr e = i.next();
			    print(e, w, tr);

			    if (i.hasNext()) {
				w.write(",");
				w.allowBreak(0, " ");
			    }
			}

			w.end();
			w.write(")");
			
		}
		printSubStmt(body, w, tr);
	}

	/**
	 * Return the first (sub)term performed when evaluating this
	 * term.
	 */
	public Term firstChild() {
		
		if (clocks() == null || clocks().isEmpty()) {
                        return body;
		} else {
                        return (Term) clocks().get(0);
		}
	}

	 

	/**
	 * Visit this term in evaluation order.
	 * [IP] Treat this as a conditional to make sure the following
	 *      statements are always reachable.
	 * FIXME: We should really build our own CFG, push a new context,
	 * and disallow uses of "continue", "break", etc. in asyncs.
	 */
	public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
		if (clocks() == null || clocks().isEmpty()) {
			v.push(this).visitCFG(body, this, EXIT);
		} else {
			v.visitCFGList(clocks, body, ENTRY);
			v.push(this).visitCFG(body, this, EXIT);
		}
        v.edge(v,this,ENTRY,this,EXIT,FlowGraph.EDGE_KEY_FALSE); // a trick to make sure we treat Async like a conditional for the purpose of initialization. see InitChecker.
		
		return succs;
	}

	private static final Collection<String> TOPICS =
		CollectionUtil.list(Report.types, Report.context);
}


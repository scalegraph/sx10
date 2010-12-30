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

package x10.visit;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.UnknownType;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;
import x10.errors.Errors;

/**
 * @author vj
 *
 */
public class X10TypeChecker extends TypeChecker {

	/**
	 * @param job
	 * @param ts
	 * @param nf
	 */
	private X10TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
	    this(job, ts, nf, new HashMap<Node, Node>());
	}
	/**
	 * @param job
	 * @param ts
	 * @param nf
	 * @param memo
	 */
	public X10TypeChecker(Job job, TypeSystem ts, NodeFactory nf, Map<Node, Node> memo) {
	    this(job, ts, nf, memo, false);
	}
	public X10TypeChecker(Job job, TypeSystem ts, NodeFactory nf,
			Map<Node, Node> memo, boolean isFragmentChecker) {
		super(job, ts, nf, memo);
		this.extensionInfo = (x10.ExtensionInfo) job.extensionInfo();
		this.memo = memo;
		this.isFragmentChecker = isFragmentChecker;
	}
	boolean isFragmentChecker = false;
	public boolean isFragmentChecker() { return isFragmentChecker;}
	
	private x10.ExtensionInfo extensionInfo;
	
	// TODO: record the top-level node in a memo only if typechecking a fragment
	public Node override(Node parent, Node n) {
	    Node n_ = memo.get(n);
	    if (n_ != null) {
	        this.addDecls(n_);
	        return n_;
	    }

	    try {
	        if (Report.should_report(Report.visit, 2))
	            Report.report(2, ">> " + this + "::override " + n);

	        Node m = n.del().typeCheckOverride(parent, this);

	        if (m != null) {
//	            memo.put(n, m);
//	            memo.put(m, m);
	        }

	        return m;
	    }
	    catch (SemanticException e) {
	        Errors.issue(job(), e);
	        // continue, errors have been reported, maybe you will find more errors.
	        return n;
	    }
	}

	protected NodeVisitor enterCall(Node n) throws SemanticException {
	    try {
	        return super.enterCall(n);
	    } catch (SemanticException z) {
	        boolean newp = extensionInfo.errorSet().add(z);
	        if (newp)
	            throw z;
	        else throw new SemanticException();
	    }
	}
	    
	protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
	    try {
	        final TypeChecker tc = (TypeChecker) v;
	        // Inline the super call without checking for expressions with unknown type
	        Node m = n;
	        m = m.del().disambiguate(tc);
	        m = m.del().typeCheck(tc);
	        m = m.del().checkConstants(tc);
	        // Record the new node in the memo table.
//	        memo.put(old, m);
//	        memo.put(n, m);
//	        memo.put(m, m);
	        return m;
	    } catch (SemanticException z) {
	        boolean newp = extensionInfo.errorSet().add(z);
	        if (newp)
	            throw z;
	    }
	    // continue, errors have been reported, maybe you will find more errors.
	    return n;
	}

	public static X10TypeChecker getTypeChecker(ContextVisitor tc) {
	    return (X10TypeChecker)
	        (tc instanceof X10TypeChecker ? tc :
	            new X10TypeChecker(tc.job(), tc.typeSystem(), tc.nodeFactory()).context(tc.context()));
	}
}

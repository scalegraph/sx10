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

import polyglot.ast.Expr;
import polyglot.ast.Term;
import polyglot.ast.Expr_c;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;

/**
 * @author vj Dec 9, 2004
 * 
 */
public class Range_c extends Expr_c implements Range {
	Expr lb;
	Expr ub;
	Expr stride;

	/**
	 * @param pos
	 */
	public Range_c(Position pos) {
		super(pos);
	}
	public Range_c( Position pos, Expr lb, Expr ub, Expr stride ) {
		super( pos);
		this.lb = lb;
		this.ub = ub;
		this.stride = stride;
	}

	/* (non-Javadoc)
	 * @see polyglot.ast.Term#entry()
	 */
	public Term firstChild() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see polyglot.ast.Term#acceptCFG(polyglot.visit.CFGBuilder, java.util.List)
	 */
	public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
		// TODO Auto-generated method stub
		return succs;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#lowerBound()
	 */
	public Expr lowerBound() {
		return this.lb;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#upperBound()
	 */
	public Expr upperBound() {
		return this.ub;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#stride()
	 */
	public Expr stride() {
			return this.stride;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#lowerBound(polyglot.ast.Expr)
	 */
	public Range lowerBound(Expr lb) {
		Range_c n  = (Range_c) this.copy();
		n.lb = lb;
		return n;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#upperBound(polyglot.ast.Expr)
	 */
	public Range upperBound(Expr ub) {
		Range_c n  = (Range_c) this.copy();
		n.ub = ub;
		return n;
	}

	/* (non-Javadoc)
	 * @see x10.ast.Range#stride(polyglot.ast.Expr)
	 */
	public Range stride(Expr stride) {
		Range_c n  = (Range_c) this.copy();
		n.stride = stride;
		return n;

	}

}

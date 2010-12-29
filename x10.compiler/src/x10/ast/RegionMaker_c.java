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

import java.util.Collections;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.TypeChecker;
import x10.constraint.XTerms;
import x10.constraint.XVar;
import x10.types.ConstrainedType;
import polyglot.types.TypeSystem;

/**
 * @author vj
 *
 */
public class RegionMaker_c extends X10Call_c implements RegionMaker {

	/**
	 * @param pos
	 * @param target
	 * @param name
	 * @param arguments
	 */
	public RegionMaker_c(Position pos, Receiver target, Id name,
			List<Expr> arguments) {
		super(pos, target, name, Collections.<TypeNode>emptyList(), arguments);
	
	}
	public Node typeCheck(ContextVisitor tc) {
		TypeSystem xts = (TypeSystem) tc.typeSystem();
		RegionMaker_c n = (RegionMaker_c) super.typeCheck(tc);
		Expr left = (Expr) n.arguments.get(0);
		Type type = n.type();
		Type lType = left.type();
		if (Types.entails(lType, Types.self(lType), xts.ZERO())) {
		    if (!xts.isUnknown(type)) {
		    	ConstrainedType result = Types.toConstrainedType(type);
		        XVar self = Types.self(result);
		        result = (ConstrainedType) Types.addTerm(result, result.makeZeroBased());
		        result = (ConstrainedType) Types.addTerm(result, result.makeRail());
		        n= (RegionMaker_c) n.type(result);
		    }
		}
		return n;
	}
}

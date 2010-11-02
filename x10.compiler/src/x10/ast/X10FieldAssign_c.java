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

import polyglot.ast.Assign;
import polyglot.ast.Assign_c;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Assign.Operator;
import polyglot.types.FieldInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import x10.types.X10ClassType;
import x10.types.X10Context;
import x10.types.X10FieldInstance;
import x10.types.X10Flags;

import x10.types.X10TypeMixin;
import x10.types.checker.Checker;
import x10.types.checker.PlaceChecker;
import x10.visit.X10TypeChecker;
import x10.errors.Errors;

public class X10FieldAssign_c extends FieldAssign_c {
    
    public X10FieldAssign_c(NodeFactory nf, Position pos, Receiver target, Id name, Operator op, Expr right) {
        super(nf, pos, target, name, op, right);
    }
    
    @Override
    public Assign typeCheckLeft(ContextVisitor tc) {
    	X10Context cxt = (X10Context) tc.context();
    	if (cxt.inDepType()) {
    	    SemanticException e = new Errors.NoAssignmentInDepType(this, this.position());
    	    Errors.issue(tc.job(), e, this);
    	}
    	
        tc = tc.context(((X10Context) tc.context()).pushAssignment());
        Assign res = this;
        try {
            res = super.typeCheckLeft(tc);
        } catch (SemanticException e) {
            Errors.issue(tc.job(), e, this);
        }
        return res;
    }

    /** Type check the expression. */
    public Node typeCheck(ContextVisitor tc) {
    
    	TypeSystem ts = tc.typeSystem();
        X10FieldAssign_c n = (X10FieldAssign_c) typeCheckLeft(tc);
        Type t =  n.leftType();
        // Check that the field being assigned to is not a property.
        // Such fields can only be set in a property() statement.
        X10FieldInstance fd = (X10FieldInstance) n.fieldInstance();
        if (fd.isProperty()) {
            SemanticException e = new Errors.CannotAssignToProperty(fd, n.position());
            Errors.issue(tc.job(), e, n);
        }

        if (t == null)
            t =  ts.unknownType(n.position());

    	X10Field_c target = (X10Field_c) n.left();
    	// Not needed in the orthogonal locality proposal.
    	//try {
    	//    target = PlaceChecker.makeFieldAccessLocalIfNecessary(target, tc);
    	//} catch (SemanticException e) {
    	//    Errors.issue(tc.job(), e, this);
    	//}
    	n = (X10FieldAssign_c) n.reconstruct(target.target(), n.name());
    	t = n.leftType();
    	n = (X10FieldAssign_c) n.type(t);
    	return Checker.typeCheckAssign(n, tc);
    }

}

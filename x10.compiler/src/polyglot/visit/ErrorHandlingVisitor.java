/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This file was originally derived from the Polyglot extensible compiler framework.
 *
 *  (C) Copyright 2000-2007 Polyglot project group, Cornell University
 *  (C) Copyright IBM Corporation 2007-2014.
 */

package polyglot.visit;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.main.Reporter;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.*;

/**
 */
public class ErrorHandlingVisitor extends NodeVisitor
{
    protected boolean error;
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;
    protected Reporter reporter;
    

    public ErrorHandlingVisitor(Job job, TypeSystem ts, NodeFactory nf) {
        super();
        this.job = job;
        this.ts = ts;
        this.nf = nf;
        this.reporter = ts.extensionInfo().getOptions().reporter;
    }
    
    public Job job() {
        return job;
    }
    
    /**
     * Part of the initialization done by begin() in an ErrorHandlingVisitor
     * method is initializing the error-handling state.
     */
    @Override
    public NodeVisitor begin() {
        this.error = false;
        return super.begin();
    }

    /** Returns the <code>ErrorQueue</code> for the current Job.
     *
     * @see polyglot.util.ErrorQueue
     */
    public ErrorQueue errorQueue() {
        return job().compiler().errorQueue();
    }
    
    /**
     * Returns true if some errors have been reported, even if cleared.
     */
    public boolean hasErrors() {
	if (goal() != null && ! goal().isReachable())
	    return true;
	return false;
//        return errorQueue().hasErrors();
    }

    /** Returns the <code>NodeFactory</code> that this Visitor is using.
     *
     * @see polyglot.ast.NodeFactory
     */
    public NodeFactory nodeFactory() {
        return nf;
    }

    /** Returns the <code>TypeSystem</code> that this Visitor is using.
     *
     * @see polyglot.types.TypeSystem
     */
    public TypeSystem typeSystem() {
        return ts;
    }

    /** Replaces the functionality of the <code>enter()</code> method; all 
      * sub-classes should over-ride this method instead of 
      * <code>enter()</code> if there is any chance of exceptions being
      * generated.
      *
      * This method is the replacement for the <code>enter()</code> method, 
      * so that all of its subclasses gain the error handling capabilities 
      * of this visitor without having to rewrite it for the 
      * <code>enter()</code> for each sub-class.
      *
      * This method allows for a <code>SemanticException</code> to be 
      * thrown in the body, while <code>enter()</code> does not.
      * 
      * @see polyglot.visit.NodeVisitor#enter(Node, Node)
      * @throws SemanticException
      * @param n The root of the subtree to be traversed.
      * @return The <code>ErrorHandlingVisitor</code> which should be 
      * used to visit the children of <code>n</code>.
     */
    protected NodeVisitor enterCall(Node parent, Node n)
                throws SemanticException {
        if (reporter.should_report(Reporter.visit, 3))
            reporter.report(3, "enter: " + parent + " -> " + n);
        return enterCall(n);
    }

    protected NodeVisitor enterCall(Node n) throws SemanticException {
        return this;
    }

    /** This method determines what should be returned by <code>enter()</code>
      * should its call to <code>enterCall()</code> throw a
      * <code>SemanticException</code>.
      *
      * @param n The root of the subtree that was traversed.
      * @return The <code>ErrorHandlingVisitor</code> which should be
      * used to visit the childre of <code>n</code>.
      */
    protected NodeVisitor enterError(Node n) {
	return this;
    }

    /** Contains all of the functionality that can be done in the <code> leave
      * </code> method, but allows <code>SemanticExceptions</code> to be
      * thrown.
      *
      * This method is in addition to the <code>leave</code> method, 
      * and allows the compiler writer to write code that can throw errors
      * and let the polyglot infrastructure handle the exceptions.
      *
      * @see polyglot.visit.NodeVisitor#leave(Node, Node, NodeVisitor)
      * @throws SemanticException
      * @param old The original state of root of the current subtree.
      * @param n The current state of the root of the current subtree.
      * @param v The <code>NodeVisitor</code> object used to visit the children.
      * @return The final result of the traversal of the tree rooted at
      * <code>n</code>.
      */
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v)
        throws SemanticException {

	return leaveCall(old, n, v);
    }
    
    protected Node leaveCall(Node old, Node n, NodeVisitor v)
        throws SemanticException {

	return leaveCall(n);
    }

    protected Node leaveCall(Node n) throws SemanticException {
	return n;
    }

    /** Return true if we should catch errors thrown when visiting the node. */
    protected boolean catchErrors(Node n) {
	return n instanceof Stmt
	    || n instanceof ClassMember
	    || n instanceof ClassDecl
	    || n instanceof Import
	    || n instanceof SourceFile;
    }

    /**
     * Begin normal traversal of a subtree rooted at <code>n</code>. This gives
     * the visitor the option of changing internal state or returning a new
     * visitor which will be used to visit the children of <code>n</code>.
     *
     * This method delegates all responsibility of functionality to the
     * <code>enterCall</code> method, and handles and reports any exceptions
     * generated by <code>enterCall</code>.
     *
     * In overriding this method, unless the class explicitly does not
     * want to maintain any of the error handling aspects of this class, a call
     * <code>super.enter</code> should be embedded within the method at the 
     * end.
     *
     * @param n The root of the subtree to be traversed.
     * @return The <code>NodeVisitor</code> which should be used to visit the
     * children of <code>n</code>.
     */
    @Override
    public NodeVisitor enter(Node parent, Node n) {
        if (reporter.should_report(Reporter.visit, 5))
            reporter.report(5, "enter(" + n + ")");

        if (catchErrors(n)) {
            this.error = false;
        }

        try {
            // should copy the visitor
            return enterCall(parent, n);
        }
	catch (SemanticException e) {
            if (e.getMessage() != null) {
                Position position = e.position();

                if (position == null) {
                    position = n.position();
                }

                errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                    e.getMessage(), position);
            }
            else {
                // silent error; these should be thrown only
                // when the error has already been reported 
            }
            
            // IMPORTANT: Mark the goal as failed, otherwise we may run dependent goals
            // that depend on this pass completing successfully.
            if (goal() != null)
        	goal().fail();

            if (! catchErrors(n)) {
                this.error = true;
            }

	    return enterError(n);
        }
    }

    public Goal goal() {
        return job().extensionInfo().scheduler().currentGoal();
    }
    
    /**
     * This method is called after all of the children of <code>n</code>
     * have been visited. In this case, these children were visited by the
     * visitor <code>v</code>. This is the last chance for the visitor to
     * modify the tree rooted at <code>n</code>. This method will be called
     * exactly the same number of times as <code>entry</code> is called.
     * That is, for each node that is not overriden, <code>enter</code> and
     * <code>leave</code> are each called exactly once.
     * <p>
     * Note that if <code>old == n</code> then the vistior should make a copy
     * of <code>n</code> before modifying it. It should then return the
     * modified copy.
     *
     * This method delegates all responsibility of functionality to the
     * <code>leaveCall</code> method, and handles and reports any exceptions
     * generated by <code> leaveCall</code>.
     *
     * In overriding this method, unless the class explicitly does not
     * want to maintain any of the error handling aspects of this class, a call
     * <code>super.leave</code> should be embedded within the method at the 
     * end.
     *
     * @param old The original state of root of the current subtree.
     * @param n The current state of the root of the current subtree.
     * @param v The <code>NodeVisitor</code> object used to visit the children.
     * @return The final result of the traversal of the tree rooted at
     * <code>n</code>.
     */
 
    @Override
    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        try {
            if (v instanceof ErrorHandlingVisitor &&
                ((ErrorHandlingVisitor) v).error) {

                if (reporter.should_report(Reporter.visit, 5))
                    reporter.report(5, "leave(" + n + "): error below");

                if (catchErrors(n)) {
                    this.error = false;
                    ((ErrorHandlingVisitor) v).error = false;
                }
                else {
                    // propagate the error outward
                    this.error = true;
                }

                // don't visit the node
                return n;
            }

            if (reporter.should_report(Reporter.visit, 5))
                reporter.report(5, "leave(" + n + "): calling leaveCall");

            return leaveCall(parent, old, n, v);
	}
	catch (SemanticException e) {
            if (e.getMessage() != null) {
                Position position = e.position();

                if (position == null) {
                    position = n.position();
                }

                errorQueue().enqueue(new CodedErrorInfo(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), position, e.attributes()));
            }
            else {
                // silent error; these should be thrown only
                // when the error has already been reported 
            }

            // IMPORTANT: Mark the goal as failed, otherwise we may run dependent goals
            // that depend on this pass completing successfully.
            if (goal() != null)
        	goal().fail();

            if (catchErrors(n)) {
                this.error = false;
                ((ErrorHandlingVisitor) v).error = false;
            }
            else {
                this.error = true;
                ((ErrorHandlingVisitor) v).error = true;
            }

            return n;
        }
    }
}


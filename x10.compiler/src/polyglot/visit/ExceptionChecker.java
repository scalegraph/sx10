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

import java.util.*;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.*;
import x10.util.CollectionFactory;

/** Visitor which checks if exceptions are caught or declared properly. */
public class ExceptionChecker extends ErrorHandlingVisitor
{
    protected ExceptionChecker outer;
    
    /**
     * Set of exceptions that can be caught. Combined with the outer
     * field, these sets form a stack of exceptions, representing
     * all and only the exceptions that may be thrown at this point in
     * the code.
     * 
     * Note: Consider the following code, where A,B,C,D are Exception subclasses.
     *    void m() throws A, B {
     *       try {
     *          ...
     *       }
     *       catch (C ex) { ... }
     *       catch (D ex) { ... }
     *    }
     *    
     *  Inside the try-block, the stack of catchable sets is:
     *     { C }
     *     { D }
     *     { A, B }
     */
    protected Set<Type> catchable;
    

    /**
     * The throws set, calculated bottom up.
     */
    protected SubtypeSet throwsSet;
    
    /**
     * Responsible for creating an appropriate exception.
     */
    protected UncaughtReporter reporter;
    
    /**
     * Should the propogation of eceptions upwards go past this point?
     */
    protected boolean catchAllThrowable;
    
    public ExceptionChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        this.outer = null;
        this.catchAllThrowable = false;
    }
    
    public ExceptionChecker push(UncaughtReporter reporter) {
        ExceptionChecker ec = this.push();
        ec.reporter = reporter;
        ec.throwsSet = new SubtypeSet(ts.CheckedThrowable());
        return ec;
    }
    public ExceptionChecker push(Type catchableType) {
        ExceptionChecker ec = this.push();
        ec.catchable = Collections.<Type>singleton(catchableType);
        ec.throwsSet = new SubtypeSet(ts.CheckedThrowable());
        return ec;
    }
    public ExceptionChecker push(Collection<Type> catchableTypes) {
        ExceptionChecker ec = this.push();
        ec.catchable = CollectionFactory.newHashSet(catchableTypes);
        ec.throwsSet = new SubtypeSet(ts.CheckedThrowable());
        return ec;
    }
    public ExceptionChecker pushCatchAllThrowable() {
        ExceptionChecker ec = this.push();
        ec.throwsSet = new SubtypeSet(ts.CheckedThrowable());
        ec.catchAllThrowable = true;
        return ec;
    }
    
    public ExceptionChecker push() {
        throwsSet(); // force an instantiation of the throwsset.
        ExceptionChecker ec = (ExceptionChecker) this.shallowCopy();
        ec.outer = this;
        ec.catchable = null;
        ec.catchAllThrowable = false;
        return ec;
    }

    public ExceptionChecker pop() {
        return outer;
    }

    protected NodeVisitor enterCall(Node n) throws SemanticException {
        return n.del().exceptionCheckEnter(this);
    }

    protected NodeVisitor enterError(Node n) {
        return push();
    }

    /**
     * Call exceptionCheck(ExceptionChecker) on the node.
     *
     * @param old The original state of root of the current subtree.
     * @param n The current state of the root of the current subtree.
     * @param v The <code>NodeVisitor</code> object used to visit the children.
     * @return The final result of the traversal of the tree rooted at 
     *  <code>n</code>.
     */
    protected Node leaveCall(Node old, Node n, NodeVisitor v)
	throws SemanticException {
        if (v instanceof PruningVisitor) {
            // [DC] it seems this means no children were visited
            // this means we are probably in a try, but we must visit the try
            // doing the following to force that... probably wrong...
            return n.del().exceptionCheck(this);
        }
        
        ExceptionChecker inner = (ExceptionChecker) v;

        {
            // code in this block checks the invariant that
            // this ExceptionChecker must be an ancestor of inner, i.e.,
            // inner must be the result of zero or more pushes.
            boolean isAncestor = false;
            ExceptionChecker ec = inner;
            while (!isAncestor && ec != null) {
                isAncestor = isAncestor || (ec == this);
                ec = ec.outer;
            }
            if (!isAncestor) {
                throw new InternalCompilerError("oops!");
            }
        }
        
        // gather exceptions from this node.
        return n.del().exceptionCheck(inner);        
    }

    /**
     * The ast nodes will use this callback to notify us that they throw an
     * exception of type t. This method will throw a SemanticException if the
     * type t is not allowed to be thrown at this point; the exception t will be
     * added to the throwsSet of all exception checkers in the stack, up to (and
     * not including) the exception checker that catches the exception.
     * @param t The type of exception that the node throws.
     * 
     * @throws SemanticException
     */
    public void throwsException(Type t, Position pos) throws SemanticException {
        if (! t.isUncheckedException()) {            
            // go through the stack of catches and see if the exception
            // is caught.
            boolean exceptionCaught = false;
            ExceptionChecker ec = this;
            while (!exceptionCaught && ec != null) {
                if (ec.catchable != null) {
                    for (Iterator<Type> iter = ec.catchable.iterator(); iter.hasNext(); ) {
                        Type catchType = (Type)iter.next();
                        if (ts.isSubtype(t, catchType, ts.emptyContext())) {
                            exceptionCaught = true;
                            break;
                        }
                    }
                }           
                if (!exceptionCaught && ec.throwsSet != null) {
                    // add t to ec's throwsSet.
                    ec.throwsSet.add(t); 
                }
                if (ec.catchAllThrowable) {
                    // stop the propagation
                    exceptionCaught = true;
                }
                ec = ec.pop();
            }
            if (! exceptionCaught) {
                reportUncaughtException(t, pos);
            }
        }
    }

    public SubtypeSet throwsSet() {
        if (this.throwsSet == null) {
            this.throwsSet = new SubtypeSet(ts.CheckedThrowable());
        }
        return this.throwsSet;
    }
    
    protected void reportUncaughtException(Type t, Position pos) throws SemanticException {
        ExceptionChecker ec = this;
        UncaughtReporter ur = null;
        while (ec != null && ur == null) {
            ur = ec.reporter;
            ec = ec.outer;
        }
        if (ur == null) {
            ur = new UncaughtReporter();
        }
        ur.uncaughtType(t, pos);
    }

    public static class UncaughtReporter {
        /**
         * This method must throw a SemanticException, reporting
         * that the Exception type t must be caught.
         * @throws SemanticException 
         */
        void uncaughtType(Type t, Position pos) throws SemanticException {
            throw new SemanticException("The exception \"" + t +"\" must either be caught or declared to be thrown.", pos);
        }
    }
    public static class CodeTypeReporter extends UncaughtReporter {
        public final String codeType;
        public CodeTypeReporter(String codeType) {
            this.codeType = codeType;
        }
        void uncaughtType(Type t, Position pos) throws SemanticException {
        	SemanticException e = new SemanticException(codeType + " cannot throw a \"" + t + "\"; the exception must either be caught or declared to be thrown.", pos);
            Map<String, Object> map = CollectionFactory.newHashMap();
            map.put(CodedErrorInfo.ERROR_CODE_KEY, CodedErrorInfo.ERROR_CODE_SURROUND_THROW);
            map.put("TYPE", t.toString());
            e.setAttributes(map);
            throw e;
        }
    }
    
}

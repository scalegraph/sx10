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

package polyglot.ast;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import polyglot.frontend.ExtensionInfo;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.visit.*;

/**
 * <code>JL_c</code> is the super class of JL node delegates objects.
 * It defines default implementations of the methods which implement compiler
 * passes, dispatching to the node to perform the actual work of the pass.
 * Language extensions may subclass <code>JL_c</code> for individual node
 * classes or may reimplement all compiler passes in a new class implementing
 * the <code>JL</code> interface.
 */
public class JL_c extends Ext_c implements JL {
    public JL_c() {
    }

    /** The <code>JL</code> object we dispatch to, by default, the node
     * itself, but possibly another delegate.
     */
    public JL jl() {
        return node();
    }

    /**
     * Visit the children of the node.
     *
     * @param v The visitor that will traverse/rewrite the AST.
     * @return A new AST if a change was made, or <code>this</code>.
     */
    public Node visitChildren(NodeVisitor v) {
        return jl().visitChildren(v);
    }

    /**
     * Push a new scope upon entering this node, and add any declarations to the
     * context that should be in scope when visiting children of this node.
     * This should <i>not</i> update the old context
     * imperatively.  Use <code>addDecls</code> when leaving the node
     * for that.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting this node. 
     */
    public Context enterScope(Context c) {
        return jl().enterScope(c);
    }

    /**
     * Push a new scope for visiting the child node <code>child</code>. 
     * The default behavior is to delegate the call to the child node, and let
     * it add appropriate declarations that should be in scope. However,
     * this method gives parent nodes have the ability to modify this behavior.
     * @param child the child node about to be entered.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting node 
     *           <code>child</code>
     */
    public Context enterChildScope(Node child, Context c) {
        return jl().enterChildScope(child, c);
    }

    /**
     * Add any declarations to the context that should be in scope when
     * visiting later sibling nodes.
     * @param c The context to which to add declarations.
     */
    public void addDecls(Context c) {
        jl().addDecls(c);
    }

    /**
     * Collects classes, methods, and fields from the AST rooted at this node
     * and constructs type objects for these.  These type objects may be
     * ambiguous.  Inserts classes into the <code>TypeSystem</code>.
     *
     * This method is called by the <code>enter()</code> method of the
     * visitor.  The * method should perform work that should be done
     * before visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node on which
     * <code>visitChildren()</code> and <code>leave()</code> will be
     * invoked.
     *
     * @param tb The visitor which adds new type objects to the
     * <code>TypeSystem</code>.
     */
    public NodeVisitor buildTypesEnter(TypeBuilder tb) {
	return jl().buildTypesEnter(tb);
    }

    /**
     * Collects classes, methods, and fields from the AST rooted at this node
     * and constructs type objects for these.  These type objects may be
     * ambiguous.  Inserts classes into the <code>TypeSystem</code>.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * @param tb The visitor which adds new type objects to the
     * <code>TypeSystem</code>.
     */
    public Node buildTypes(TypeBuilder tb) {
	return jl().buildTypes(tb);
    }

    public Node buildTypesOverride(TypeBuilder tb) {
        return jl().buildTypesOverride(tb);
    }
    
    /**
     * Remove any remaining ambiguities from the AST.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * @param ar The visitor which disambiguates.
     */
    public Node disambiguate(ContextVisitor ar) {
    	return jl().disambiguate(ar);
    }

    public Node setResolverOverride(Node parent, TypeCheckPreparer v) {
    	return jl().setResolverOverride(parent, v);
    }
    
    public void setResolver(Node parent, TypeCheckPreparer v) {
    	jl().setResolver(parent, v);
    }

    /**
     * Type check the AST.
     *
     * This method is called by the <code>enter()</code> method of the
     * visitor.  The * method should perform work that should be done
     * before visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node on which
     * <code>visitChildren()</code> and <code>leave()</code> will be
     * invoked.
     *
     * @param tc The type checking visitor.
     */
    public Node typeCheckOverride(Node parent, ContextVisitor tc) {
	return jl().typeCheckOverride(parent, tc);
    }
    
    public NodeVisitor typeCheckEnter(TypeChecker tc) {
	return jl().typeCheckEnter(tc);
    }

    /**
     * Type check the AST.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * @param tc The type checking visitor.
     */
    public Node typeCheck(ContextVisitor tc) {
	return jl().typeCheck(tc);
    }

    public Node checkConstants(ContextVisitor tc) {
        return jl().checkConstants(tc);
    }
    
    public Node conformanceCheck(ContextVisitor tc) {
	return jl().conformanceCheck(tc);
    }
    
    /**
     * Check that exceptions are properly propagated throughout the AST.
     *
     * This method is called by the <code>enter()</code> method of the
     * visitor.  The * method should perform work that should be done
     * before visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node on which
     * <code>visitChildren()</code> and <code>leave()</code> will be
     * invoked.
     *
     * @param ec The visitor.
     */
    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec) {
	return jl().exceptionCheckEnter(ec);
    }

    /**
     * Check that exceptions are properly propagated throughout the AST.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * @param ec The visitor.
     */
    public Node exceptionCheck(ExceptionChecker ec) {
	return jl().exceptionCheck(ec);
    }

    /** 
     * List of Types of exceptions that might get thrown.  The result is
     * not necessarily correct until after type checking. 
     */
    public List<Type> throwTypes(TypeSystem ts) {
	   return jl().throwTypes(ts);
    }

    /** Dump the AST for debugging. */
    public void dump(OutputStream os) {
            jl().dump(os);
    }
    
    /** Dump the AST for debugging. */
    public void dump(Writer w) {
            jl().dump(w);
    }
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(OutputStream os) {
            jl().prettyPrint(os);
    }
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(Writer w) {
            jl().prettyPrint(w);
    }

    /**
     * Pretty-print the AST using the given code writer.
     *
     * @param w The code writer to which to write.
     * @param pp The pretty printer.  This is <i>not</i> a visitor.
     */
    public void prettyPrint(CodeWriter w, PrettyPrinter pp) {
        jl().prettyPrint(w, pp);
    }

    /**
     * Translate the AST using the given code writer.
     *
     * @param w The code writer to which to write.
     * @param tr The translation pass.  This is <i>not</i> a visitor.
     */
    public void translate(CodeWriter w, Translator tr) {
        jl().translate(w, tr);
    }
    
    public Node copy(NodeFactory nf) {
        return jl().copy(nf);
    }
    public Node copy(ExtensionInfo extInfo) throws SemanticException {
        return jl().copy(extInfo);
    }
    
}

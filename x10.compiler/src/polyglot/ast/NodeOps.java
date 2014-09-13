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
 * A <code>Node</code> represents an AST node.  All AST nodes must implement
 * this interface.  Nodes should be immutable: methods which set fields
 * of the node should copy the node, set the field in the copy, and then
 * return the copy.
 */
public interface NodeOps
{
    /**
     * Visit the children of the node.
     *
     * @param v The visitor that will traverse/rewrite the AST.
     * @return A new AST if a change was made, or <code>this</code>.
     */
    Node visitChildren(NodeVisitor v);

    /**
     * Push a new scope upon entering this node, and add any declarations to the
     * context that should be in scope when visiting children of this node.
     * This should <i>not</i> update the old context
     * imperatively.  Use <code>addDecls</code> when leaving the node
     * for that.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting this node. 
     */
    public Context enterScope(Context c);

    /**
     * Push a new scope for visiting the child node <code>child</code>. 
     * The default behavior is to delegate the call to the child node, and let
     * it add appropriate declarations that should be in scope. However,
     * this method gives parent nodes have the ability to modify this behavior.
     * @param child The child node about to be entered.
     * @param c The current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting node 
     *           <code>child</code>
     */
    public Context enterChildScope(Node child, Context c);

    /**
     * Add any declarations to the context that should be in scope when
     * visiting later sibling nodes.
     * @param c The context to which to add declarations.
     */
    void addDecls(Context c);

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
    NodeVisitor buildTypesEnter(TypeBuilder tb);

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
    Node buildTypesOverride(TypeBuilder tb);
    Node buildTypes(TypeBuilder tb);

    /**
     * Remove any remaining ambiguities from the AST.
     * This method is called after the compute types pass and
     * reconstructs the node from the type information computed there.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * The node should not assume that its children have been disambiguated.
     * If it depends on a child being disambiguated,
     * it may just return <code>this</code> without doing any work.
     *
     * @param ar The visitor which disambiguates.
     */
    Node disambiguate(ContextVisitor ar);

    Node setResolverOverride(Node parent, TypeCheckPreparer v);
    void setResolver(Node parent, TypeCheckPreparer v);

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
    NodeVisitor typeCheckEnter(TypeChecker tc);

    /**
     * Type check the AST.
     *
     * This method is called by the <code>override()</code> method of the
     * visitor.  If this method returns non-null, the node's children
     * will not be visited automatically.  Thus, the method should check
     * both the node <code>this</code> and it's children, usually by
     * invoking <code>visitChildren</code> with <code>tc</code> or
     * with another visitor, returning a non-null node.  OR, the method
     * should do nothing and simply return <code>null</code> to allow
     * <code>enter</code>, <code>visitChildren</code>, and <code>leave</code>
     * to be invoked on the node.
     *
     * The default implementation returns <code>null</code>.
     * Overriding of this method is discouraged, but sometimes necessary.
     *
     * @param tc The type checking visitor.
     */
    Node typeCheckOverride(Node parent, ContextVisitor tc);

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
    Node typeCheck(ContextVisitor tc);
    Node conformanceCheck(ContextVisitor tc);

    /**
     * Check if the node is a compile-time constant.
     *
     * This method is called by the <code>leave()</code> method of the
     * visitor.  The method should perform work that should be done
     * after visiting the children of the node.  The method may return
     * <code>this</code> or a new copy of the node which will be
     * installed as a child of the node's parent.
     *
     * @param tc The constant checking visitor.
     */
    Node checkConstants(ContextVisitor tc);
    
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
    NodeVisitor exceptionCheckEnter(ExceptionChecker ec);

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
    Node exceptionCheck(ExceptionChecker ec);

    /** 
     * List of Types of exceptions that might get thrown.  The result is
     * not necessarily correct until after type checking. 
     */
    List<Type> throwTypes(TypeSystem ts);

    /** Dump the AST for debugging. */
    public void dump(OutputStream os);
    
    /** Dump the AST for debugging. */
    public void dump(Writer w);
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(OutputStream os);
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(Writer w);
 
    /**
     * Pretty-print the AST using the given code writer.
     *
     * @param w The code writer to which to write.
     * @param pp The pretty printer.  This is <i>not</i> a visitor.
     */
    void prettyPrint(CodeWriter w, PrettyPrinter pp);

    /**
     * Translate the AST using the given code writer.
     *
     * @param w The code writer to which to write.
     * @param tr The translation pass.  This is <i>not</i> a visitor.
     */
    void translate(CodeWriter w, Translator tr);
    
    /**
     * Produce a copy of this node using the given NodeFactory.
     */
    Node copy(NodeFactory nf);
    /**
     * Produce a copy of this node using the given ExtensionInfo.
     * This will typically be implemented by calling
     * copy(NodeFactory nf), and then potentially copying over
     * type information.
     * @throws SemanticException If the type information cannot be copied.
     */
    Node copy(ExtensionInfo extInfo) throws SemanticException;
    

}

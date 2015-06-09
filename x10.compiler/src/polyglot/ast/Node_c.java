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
import java.util.*;

import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import x10.errors.Errors;

/**
 * A <code>Node</code> represents an AST node.  All AST nodes must implement
 * this interface.  Nodes should be immutable: methods which set fields
 * of the node should copy the node, set the field in the copy, and then
 * return the copy.
 */
public abstract class Node_c implements Node
{
    protected Position position;
    protected JL del;
    protected Ext ext;
    protected boolean error;

    public final int hashCode() {
    	return super.hashCode();
    }
    
    public final boolean equals(Object o) {
    	return this == o;
    }
    
    public Node_c(Position pos) {
    	assert(pos != null);
        this.position = pos;
        this.error = false;
    }

    public Node setResolverOverride(Node parent, TypeCheckPreparer v) {
    	return null;
    }
    
    public void setResolver(Node parent, TypeCheckPreparer v) {
    }
    
    public void init(Node node) {
        if (node != this) {
            throw new InternalCompilerError("Cannot use a Node as a delegate or extension.");
        }
    }

    public Node node() {
        return this;
    }

    public JL del() {
        return del != null ? del : this;
    }

    public Node del(JL del) {
        if (this.del == del) {
            return this;
        }

        JL old = this.del;
        this.del = null;

        Node_c n = (Node_c) copy();

        n.del = del != this ? del : null;

        if (n.del != null) {
            n.del.init(n);
        }

        this.del = old;

        return n;
    }

    public Ext ext(int n) {
        if (n < 1) throw new InternalCompilerError("n must be >= 1");
        if (n == 1) return ext();
        return ext(n-1).ext();
    }

    public Node ext(int n, Ext ext) {
        if (n < 1)
            throw new InternalCompilerError("n must be >= 1");
        if (n == 1)
            return ext(ext);

        Ext prev = this.ext(n-1);
        if (prev == null)
            throw new InternalCompilerError("cannot set the nth extension if there is no (n-1)st extension");
        return this.ext(n-1, prev.ext(ext));
    }

    public Ext ext() {
        return ext;
    }

    public Node ext(Ext ext) {
        if (this.ext == ext) {
            return this;
        }

        Ext old = this.ext;
        this.ext = null;

        Node_c n = (Node_c) copy();

        n.ext = ext;

        if (n.ext != null) {
            n.ext.init(n);
        }

        this.ext = old;

        return n;
    }

    public Object copy() {
        try {
            Node_c n = (Node_c) super.clone();

            if (this.del != null) {
                n.del = (JL) this.del.copy();
                n.del.init(n);
            }

            if (this.ext != null) {
                n.ext = (Ext) this.ext.copy();
                n.ext.init(n);
            }

            return n;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.");
        }
    }

    public Position position() {
	return this.position;
    }

    public Node position(Position position) {
	Node_c n = (Node_c) copy();
	n.position = position;
	return n;
    }

    public boolean error() {
        return error;
    }

    public Node error(boolean flag) {
        Node_c n = (Node_c) copy();
        n.error = flag;
        return n;
    }
    
    public Node visitChild(Node n, NodeVisitor v) {
	if (n == null) {
	    return null;
	}

	return v.visitEdge(this, n);
    }

    public Node visit(NodeVisitor v) {
	return v.visitEdge(null, this);
    }

    /** 
     * @deprecated Call {@link Node#visitChild(Node, NodeVisitor)} instead.
     */
    public Node visitEdge(Node parent, NodeVisitor v) {
	Node n = v.override(parent, this);

	if (n == null) {
	    NodeVisitor v_ = v.enter(parent, this);

	    if (v_ == null) {
		throw new InternalCompilerError(
		    "NodeVisitor.enter() returned null.");
	    }

	    n = this.del().visitChildren(v_);

	    if (n == null) {
		throw new InternalCompilerError(
		    "Node_c.visitChildren() returned null.");
	    }

	    n = v.leave(parent, this, n, v_);

	    if (n == null) {
		throw new InternalCompilerError(
		    "NodeVisitor.leave() returned null.");
	    }
	}
    
	return n;
    }

    /**
     * Visit all the elements of a list.
     * @param l The list to visit.
     * @param v The visitor to use.
     * @return A new list with each element from the old list
     *         replaced by the result of visiting that element.
     *         If <code>l</code> is a <code>TypedList</code>, the
     *         new list will also be typed with the same type as 
     *         <code>l</code>.  If <code>l</code> is <code>null</code>,
     *         <code>null</code> is returned.
     */
    public <T extends Node> List<T> visitList(List<T> l, NodeVisitor v) {
	if (l == null) {
	    return null;
	}

	List<T> result = l;
	List<T> vl = new ArrayList<T>(l.size());
	
	for (Iterator<T> i = l.iterator(); i.hasNext(); ) {
	    T n = i.next();
	    Node m = visitChild(n, v);
	    if (n != m) {
	        result = vl;
	    }
            if (m instanceof NodeList) {
                vl.addAll((List<T>)((NodeList) m).nodes());
            } else if (m != null) {
	        vl.add((T)m);
	    }
	}

	return result;
    }

    public Node visitChildren(NodeVisitor v) {
	return this;
    }

    /**
     * Push a new scope upon entering this node, and add any declarations to the
     * context that should be in scope when visiting children of this node.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting this node. 
     */
    public Context enterScope(Context c) { return c; }

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
        return child.del().enterScope(c); 
    }

    /**
     * Add any declarations to the context that should be in scope when
     * visiting later sibling nodes.
     */
    public void addDecls(Context c) { }

    // These methods override the methods in Ext_c.
    // These are the default implementation of these passes.

    public Node buildTypesOverride(TypeBuilder tb) {
        return null;
    }
    
    public NodeVisitor buildTypesEnter(TypeBuilder tb) {
	return tb;
    }

    public Node buildTypes(TypeBuilder tb) {
	return this;
    }

    public Node disambiguate(ContextVisitor ar) {
	return this;
    }

    /** Type check the AST. */
    public Node typeCheckOverride(Node parent, ContextVisitor tc) {
        return null;
    }
    
    public NodeVisitor typeCheckEnter(TypeChecker tc) {
	return tc;
    }

    public Node typeCheck(ContextVisitor tc) {
	return this;
    }
    
    public Node checkConstants(ContextVisitor tc) {
        return this;
    }

    public Node conformanceCheck(ContextVisitor tc) {
	return this;
    }

    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec) {
	return ec.push();
    }

    public Node exceptionCheck(ExceptionChecker ec) { 
        List<Type> l = this.del().throwTypes(ec.typeSystem());
        for (Type t : l) {
            try {
                ec.throwsException(t, position());
            } catch (SemanticException e) {
                Errors.issue(ec.job(), e, this);
            }
        }
    	return this;
    }

    public List<Type> throwTypes(TypeSystem ts) {
       return Collections.<Type>emptyList();
    }
    
    /** Dump the AST for debugging. */
    public void dump(OutputStream os) {
        CodeWriter cw = Compiler.createCodeWriter(os);
        NodeVisitor dumper = new DumpAst(cw);
        dumper = dumper.begin();
        this.visit(dumper);
        cw.newline();
        dumper.finish();
    }
    
    /** Dump the AST for debugging. */
    public void dump(Writer w) {
        CodeWriter cw = Compiler.createCodeWriter(w);
        NodeVisitor dumper = new DumpAst(cw);
        dumper = dumper.begin();
        this.visit(dumper);
        cw.newline();
        dumper.finish();
    }
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(OutputStream os) {
        try {
            CodeWriter cw = Compiler.createCodeWriter(os);
            this.del().prettyPrint(cw, new PrettyPrinter());
            cw.flush();
        }
        catch (java.io.IOException e) { }
    }
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(Writer w) {
        try {
            CodeWriter cw = Compiler.createCodeWriter(w);
            this.del().prettyPrint(cw, new PrettyPrinter());
            cw.flush();
        }
        catch (java.io.IOException e) { }
    }

    /** Pretty-print the AST using the given <code>CodeWriter</code>. */
    public void prettyPrint(CodeWriter w, PrettyPrinter pp) { }

    public void printBlock(Node n, CodeWriter w, PrettyPrinter pp) {
        w.begin(0);
        print(n, w, pp);
        w.end();
    }

    public void printSubStmt(Stmt stmt, CodeWriter w, PrettyPrinter pp) {
        if (stmt instanceof Block) {
            w.write(" ");
            print(stmt, w, pp);
        } else {
            w.allowBreak(4, " ");
            printBlock(stmt, w, pp);
        }
    }

    public void print(Node child, CodeWriter w, PrettyPrinter pp) {
        pp.print(this, child, w);
    }
    
    /** Translate the AST using the given <code>CodeWriter</code>. */
    public void translate(CodeWriter w, Translator tr) {
        // By default, just rely on the pretty printer.
        this.del().prettyPrint(w, tr);
    }

    public void dump(CodeWriter w) {
        w.write(StringUtil.getShortNameComponent(getClass().getName()));

        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(del ");
        if (del() == this) w.write("*");
        else w.write(del().toString());
	w.write(")");
        w.end();

        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(ext ");
	if (ext() == null) w.write("null");
	else ext().dump(w);
	w.write(")");
        w.end();

        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(position " + (position != null ? position.toString()
                                                  : "UNKNOWN") + ")");
        w.end();
    }

    public String toString() {
          // This is really slow and so you are encouraged to override.
          // return new StringPrettyPrinter(5).toString(this);

          // Not slow anymore.
          return getClass().getName();
    }
    public final Node copy(NodeFactory nf) {
        throw new InternalCompilerError("Unimplemented operation. This class " +
                                        "(" + this.getClass().getName() + ") does " +
                                        "not implement copy(NodeFactory). This compiler extension should" +
                                        " either implement the method, or not invoke this method.");
    }
    public final Node copy(ExtensionInfo extInfo) {
        return this.del().copy(extInfo.nodeFactory());
    }

}

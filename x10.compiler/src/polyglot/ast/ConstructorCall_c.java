/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>ConstructorCall_c</code> represents a direct call to a constructor.
 * For instance, <code>super(...)</code> or <code>this(...)</code>.
 */
public class ConstructorCall_c extends Stmt_c implements ConstructorCall
{
    protected Kind kind;
    protected Expr qualifier;
    protected List<Expr> arguments;
    protected ConstructorInstance ci;

    public ConstructorCall_c(Position pos, Kind kind, Expr qualifier, List<Expr> arguments) {
	super(pos);
	assert(kind != null && arguments != null); // qualifier may be null
	this.kind = kind;
	this.qualifier = qualifier;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
    }
    
    /** Get the qualifier of the constructor call. */
    public Expr qualifier() {
	return this.qualifier;
    }

    /** Set the qualifier of the constructor call. */
    public ConstructorCall qualifier(Expr qualifier) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.qualifier = qualifier;
	return n;
    }

    /** Get the kind of the constructor call. */
    public Kind kind() {
	return this.kind;
    }

    /** Set the kind of the constructor call. */
    public ConstructorCall kind(Kind kind) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.kind = kind;
	return n;
    }

    /** Get the actual arguments of the constructor call. */
    public List<Expr> arguments() {
	return Collections.unmodifiableList(this.arguments);
    }

    /** Set the actual arguments of the constructor call. */
    public ConstructorCall arguments(List<Expr> arguments) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	return n;
    }

    public ConstructorInstance procedureInstance() {
	return constructorInstance();
    }

    /** Get the constructor we are calling. */
    public ConstructorInstance constructorInstance() {
        return ci;
    }

    public ConstructorCall procedureInstance(ProcedureInstance<? extends ProcedureDef> pi) {
        return constructorInstance((ConstructorInstance) pi);
    }

    /** Set the constructor we are calling. */
    public ConstructorCall constructorInstance(ConstructorInstance ci) {
        if (ci == this.ci) return this;
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.ci = ci;
	return n;
    }

    /** Reconstruct the constructor call. */
    protected ConstructorCall_c reconstruct(Expr qualifier, List<Expr> arguments) {
	if (qualifier != this.qualifier || ! CollectionUtil.allEqual(arguments, this.arguments)) {
	    ConstructorCall_c n = (ConstructorCall_c) copy();
	    n.qualifier = qualifier;
	    n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the call. */
    public Node visitChildren(NodeVisitor v) {
	Expr qualifier = (Expr) visitChild(this.qualifier, v);
	List<Expr> arguments = visitList(this.arguments, v);
	return reconstruct(qualifier, arguments);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();

        // Remove super() calls for java.lang.Object.
        if (kind == SUPER && tb.currentClass() == ts.Object()) {
            return tb.nodeFactory().Empty(position());
        }

        ConstructorCall_c n = (ConstructorCall_c) super.buildTypes(tb);

        ConstructorInstance ci = ts.createConstructorInstance(position(), new ErrorRef_c<ConstructorDef>(ts, position(), "Cannot get ConstructorDef before type-checking constructor call."));
        return n.constructorInstance(ci);
    }

    /** Type check the call. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
	ConstructorCall_c n = this;
	
	TypeSystem ts = tc.typeSystem();
	Context c = tc.context();

	ClassType ct = c.currentClass();
	Type superType = ct.superClass();

        // The qualifier specifies the enclosing instance of this inner class.
        // The type of the qualifier must be the outer class of this
        // inner class or one of its super types.
        //
        // Example:
        //
        // class Outer {
        //     class Inner { }
        // }
        //
        // class ChildOfInner extends Outer.Inner {
        //     ChildOfInner() { (new Outer()).super(); }
        // }
        if (qualifier != null) {
            if (kind != SUPER) {
                throw new SemanticException("Can only qualify a \"super\" constructor invocation.", position());
            }
            
            if (!superType.isClass() || !superType.toClass().isInnerClass() ||
                superType.toClass().inStaticContext()) {
                throw new SemanticException("A qualified constructor invocation can be used only for non-static inner classes.", position());
            }

            Type qt = qualifier.type();

            if (! qt.isClass() || !qt.isSubtype(superType.toClass().outer(), c)) {
                throw new SemanticException("The type of the qualifier \"" + qt + "\" does not match the immediately enclosing class of the super class \"" +superType.toClass().outer() + "\".", qualifier.position());
            }
        }

	if (kind == SUPER) {
	    if (! superType.isClass()) {
	        throw new SemanticException("Super type of " + ct +" is not a class.", position());
	    }
	    
	    Expr q = qualifier;

            // If the super class is an inner class (i.e., has an enclosing
            // instance of its container class), then either a qualifier 
            // must be provided, or ct must have an enclosing instance of the
            // super class's container class, or a subclass thereof.
            if (q == null && superType.isClass() && superType.toClass().isInnerClass()) {
                ClassType superContainer = superType.toClass().outer();
                // ct needs an enclosing instance of superContainer, 
                // or a subclass of superContainer.
                ClassType e = ct;
                
                while (e != null) {
                    if (e.isSubtype(superContainer, c) && ct.hasEnclosingInstance(e)) {
                        NodeFactory nf = tc.nodeFactory();
                        q = nf.This(position(), nf.CanonicalTypeNode(position(), e)).type(e);

                        break; 
                    }
                    e = e.outer();
                }
                
                if (e == null) {
                    throw new SemanticException(ct + " must have an enclosing instance that is a subtype of " + superContainer, position());
                }               
                if (e == ct) {
                    throw new SemanticException(ct + " is a subtype of " + superContainer + "; an enclosing instance that is a subtype of " + superContainer +" must be specified in the super constructor call.", position());
                }
            }

            if (qualifier != q)
                n = (ConstructorCall_c) n.qualifier(q);
	}

	List<Type> argTypes = new ArrayList<Type>();
	
	for (Iterator<Expr> iter = n.arguments.iterator(); iter.hasNext();) {
	    Expr e = iter.next();
	    argTypes.add(e.type());
	}
	
	if (kind == SUPER) {
	    ct = ct.superClass().toClass();
	}
	
	ConstructorInstance ci = ts.findConstructor(ct, ts.ConstructorMatcher(ct, argTypes, c));

	return n.constructorInstance(ci);
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == qualifier) {
            // FIXME: Can be more specific
            return ts.Object();
        }

        Iterator<Expr> i = this.arguments.iterator();
        Iterator<Type> j = ci.formalTypes().iterator();

        while (i.hasNext() && j.hasNext()) {
	    Expr e = i.next();
	    Type t = j.next();

            if (e == child) {
                return t;
            }
        }

        return child.type();
    }

    public String toString() {
	return (qualifier != null ? qualifier + "." : "") + kind + "(...)";
    }

    /** Write the call to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	if (qualifier != null) {
	    print(qualifier, w, tr);
	    w.write(".");
	} 

	w.write(kind + "(");

	w.begin(0);

	for (Iterator<Expr> i = arguments.iterator(); i.hasNext(); ) {
	    Expr e = i.next();
	    print(e, w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();

	w.write(");");
    }

    public Term firstChild() {
        if (qualifier != null) {
            return qualifier;
        } else {
            return listChild(arguments, null);
        }
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        if (qualifier != null) {
            if (!arguments.isEmpty()) {
                v.visitCFG(qualifier, listChild(arguments, null), ENTRY);
                v.visitCFGList(arguments, this, EXIT);
            } else {
                v.visitCFG(qualifier, this, EXIT);
            }
        } else {
            if (!arguments.isEmpty()) {
                v.visitCFGList(arguments, this, EXIT);
            }
        }

        return succs;
    }

}

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
import x10.util.CollectionFactory;

/**
 * A <code>Switch</code> is an immutable representation of a Java
 * <code>switch</code> statement.  Such a statement has an expression which
 * is evaluated to determine where to branch to, an a list of labels
 * and block statements which are conditionally evaluated.  One of the
 * labels, rather than having a constant expression, may be lablled
 * default.
 */
public class Switch_c extends Stmt_c implements Switch
{
    protected Expr expr;
    protected List<SwitchElement> elements;

    public Switch_c(Position pos, Expr expr, List<SwitchElement> elements) {
	super(pos);
	assert(expr != null && elements != null);
	this.expr = expr;
	this.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
    }

    /** Get the expression to switch on. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to switch on. */
    public Switch expr(Expr expr) {
	Switch_c n = (Switch_c) copy();
	n.expr = expr;
	return n;
    }

    /** Get the switch elements of the statement. */
    public List<SwitchElement> elements() {
	return Collections.unmodifiableList(this.elements);
    }

    /** Set the switch elements of the statement. */
    public Switch elements(List<SwitchElement> elements) {
	Switch_c n = (Switch_c) copy();
	n.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
	return n;
    }

    /** Reconstruct the statement. */
    protected Switch_c reconstruct(Expr expr, List<SwitchElement> elements) {
	if (expr != this.expr || ! CollectionUtil.allEqual(elements, this.elements)) {
	    Switch_c n = (Switch_c) copy();
	    n.expr = expr;
	    n.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
	    return n;
	}

	return this;
    }

    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	List<SwitchElement> elements = visitList(this.elements, v);
	return reconstruct(expr, elements);
    }

    /** Type check the statement. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
	Context context = tc.context();

	if (! ts.isImplicitCastValid(expr.type(), ts.Int(), context) && ! ts.isImplicitCastValid(expr.type(), ts.Char(), context)) {
            throw new SemanticException("Switch index must be an integer.", position());
        }
        
        return this;
    }

    public Node checkConstants(ContextVisitor tc) throws SemanticException {
        Collection<Object> labels = CollectionFactory.newHashSet();

        // Check for duplicate labels.
        for (Iterator<SwitchElement> i = elements.iterator(); i.hasNext();) {
            SwitchElement s = i.next();
            
            if (s instanceof Case) {
                Case c = (Case) s;
                Object key;
                String str;
                
                if (c.isDefault()) {
                    key = "default";
                    str = "default";
                }
                else if (c.expr().isConstant()) {
                    key = Long.valueOf(c.value());
                    str = c.expr().toString() + " (" + c.value() + ")";
                }
                else {
                    continue;
                }
                
                if (labels.contains(key)) {
                    throw new SemanticException("Duplicate case label: " +str + ".", c.position());
                }
                
                labels.add(key);
            }
        }
        
        return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            return ts.Int();
        }

        return child.type();
    }

    public String toString() {
	return "switch (" + expr + ") { ... }";
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write("switch (");
	printBlock(expr, w, tr);
	w.write(") {");
        w.unifiedBreak(4);
	w.begin(0);

        boolean lastWasCase = false;
        boolean first = true;

	for (Iterator<SwitchElement> i = elements.iterator(); i.hasNext();) {
            SwitchElement s = i.next();
            if (s instanceof Case) {
                if (lastWasCase) w.unifiedBreak(0);
                else if (! first) w.unifiedBreak(0);
                printBlock(s, w, tr);
                lastWasCase = true;
            }
            else {
                w.unifiedBreak(4);
                print(s, w, tr);
                lastWasCase = false;
            }

            first = false;
	}

	w.end();
        w.unifiedBreak(0);
	w.write("}");
    }

    public Term firstChild() {
        return expr;
    }

    public <S> List<S> acceptCFG(CFGBuilder v, List<S> succs) {
        List<Term> cases = new ArrayList<Term>(elements.size()+1);
        List<Integer> entry = new ArrayList<Integer>(elements.size()+1);
        boolean hasDefault = false;

        for (Iterator<SwitchElement> i = elements.iterator(); i.hasNext(); ) {
            SwitchElement s = i.next();

            if (s instanceof Case) {
                cases.add(s);
                entry.add(Integer.valueOf(ENTRY));
                
                if (((Case) s).expr() == null) {
                    hasDefault = true;
                }
            }
        }

        // If there is no default case, add an edge to the end of the switch.
        if (! hasDefault) {
            cases.add(this);
            entry.add(new Integer(EXIT));
        }

        v.visitCFG(expr, FlowGraph.EDGE_KEY_OTHER, cases, entry);
        v.push(this).visitCFGList(elements, this, EXIT);

        return succs;
    }
    

}

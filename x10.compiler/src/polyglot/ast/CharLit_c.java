/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

/** 
 * An <code>CharLit</code> represents a literal in java of
 * <code>char</code> type.
 */
public class CharLit_c extends NumLit_c implements CharLit
{
    public CharLit_c(Position pos, char value) {
	super(pos, value);
    }

    /** Get the value of the expression. */
    public char value() {
	return (char) longValue();
    }

    /** Set the value of the expression. */
    public CharLit value(char value) {
	CharLit_c n = (CharLit_c) copy();
	n.value = value;
	return n;
    }

    /** Type check the expression. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
	return type(tc.typeSystem().Char());
    }  

    public String toString() {
        return "'" + StringUtil.escape((char) value) + "'";
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("'");
	w.write(StringUtil.escape((char) value));
        w.write("'");
    }

    public Object constantValue() {
      return Character.valueOf((char) value);
    }

}

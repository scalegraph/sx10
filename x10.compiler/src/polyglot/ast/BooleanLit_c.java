/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

/**
 * A <code>BooleanLit</code> represents a boolean literal expression.
 */
public class BooleanLit_c extends Lit_c implements BooleanLit
{
  protected boolean value;

  public BooleanLit_c(Position pos, boolean value) {
    super(pos);
    this.value = value;
  }

  /** Get the value of the expression. */
  public boolean value() {
    return this.value;
  }

  /** Set the value of the expression. */
  public BooleanLit value(boolean value) {
    BooleanLit_c n = (BooleanLit_c) copy();
    n.value = value;
    return n;
  }

  /** Type check the expression. */
  public Node typeCheck(ContextVisitor tc) throws SemanticException {
    return type(tc.typeSystem().Boolean());
  }

  public String toString() {
    return String.valueOf(value);
  }

  /** Write the expression to an output file. */
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    w.write(String.valueOf(value));
  }

  /** Dumps the AST. */
  public void dump(CodeWriter w) {
    super.dump(w);

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(value " + value + ")");
    w.end();
  }

  public Object constantValue() {
    return Boolean.valueOf(value);
  }

}

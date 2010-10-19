/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.lex;

import polyglot.util.Position;

/** A token class for int literals. */
public class IntegerLiteral extends NumericLiteral {
  public IntegerLiteral(Position position, int i, int sym) {
      super(position, sym);
      this.val = new Integer(i);
  }
}

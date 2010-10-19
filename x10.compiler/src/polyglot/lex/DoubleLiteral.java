/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.lex;

import polyglot.util.Position;

/** Token class for double literals. */
public class DoubleLiteral extends NumericLiteral {
  public DoubleLiteral(Position position, double d, int sym) {
      super(position, sym);
      this.val = new Double(d);
  }
}
